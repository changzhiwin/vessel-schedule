package zio.doreal.vessel.utils

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
//import java.io.IOException;
//import java.io.FileNotFoundException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import zio._
import scala.reflect.ClassTag

object FileHelper {

  def readList[A]()(implicit cTag: ClassTag[A]): ZIO[Any, Throwable, List[A]] = {
    
    val tableFile = tablePath + getTableName(cTag) + ".data"

    ZIO.ifZIO( ZIO.attempt( (new File(tableFile)).exists() ) )(
      onFalse = ZIO.succeed(List.empty[A]),

      onTrue  = {
        ZIO.acquireReleaseWith {
          for {
            fstream <- ZIO.attempt(new FileInputStream(new File(tableFile)))
            ostream <- ZIO.attempt(new ObjectInputStream(fstream))
          } yield (fstream, ostream)
          
        } {
          stream => ZIO.succeed(stream._2.close()) *> ZIO.succeed(stream._1.close())
        } {
          stream => {
            val os = stream._2
            for {
              count <- ZIO.attempt(os.readInt())
              objList <- ZIO.loop(0)(_ < count, _ + 1) { _ =>
                ZIO.attempt(os.readObject().asInstanceOf[A])
              }
            } yield objList
          }
        }
      } // end onTrue
    ) // end ZIO.ifZIO
  }

  def writeList[A](objs: List[A])(implicit cTag: ClassTag[A]): ZIO[Any, Throwable, Unit] = {
    val tabeName = getTableName(cTag)
    val tableFileTemp = tabeName + ".temp"
    val tableFileDone = tabeName + ".data"
    val tableFilePrev = tabeName + ".prev"
    val sourceTemp: Path = Paths.get(tablePath + tableFileTemp)
    val sourceDone: Path = Paths.get(tablePath + tableFileDone)

    val writeEffect = ZIO.acquireReleaseWith {
      for {
        fstream <- ZIO.attempt(new FileOutputStream(new File(tableFileTemp)))
        ostream <- ZIO.attempt(new ObjectOutputStream(fstream))
      } yield (fstream, ostream)
      
    } {
      stream => ZIO.succeed(stream._2.close()) *> ZIO.succeed(stream._1.close())
    } {
      stream => {
        val os = stream._2
        for {
          count <- ZIO.attempt(os.writeInt(objs.length)).debug("Write size: ")
          _     <- ZIO.foreachDiscard(objs) { item =>
            ZIO.attempt(os.writeObject(item))
          }
        } yield ()
      }
    }

    val renameEffect = for {
      _ <- ZIO.attempt( Files.move(sourceDone, sourceDone.resolveSibling(tableFilePrev), StandardCopyOption.REPLACE_EXISTING) )
      _ <- ZIO.attempt( Files.move(sourceTemp, sourceTemp.resolveSibling(tableFileDone), StandardCopyOption.REPLACE_EXISTING) )
    } yield ()

    writeEffect *> renameEffect
  }

  private def getTableName[A](cTag: ClassTag[A]): String = {
    val className = cTag.runtimeClass.getName.toLowerCase.split("\\.").last
    s"tb-${className}"
  }

  val tablePath = s"./lib/"
}

// ref:
// 1, https://mkyong.com/java/how-to-read-and-write-java-object-to-a-file/
// 2, https://www.programiz.com/java-programming/objectinputstream
// 3, https://mkyong.com/java/how-to-rename-file-in-java/