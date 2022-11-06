package cc.knowship.subscribe.util

/*IOException, */
import java.io.{PrintWriter, StringWriter}

object DebugUtils {

  def prettify[E](throwable: E)(implicit e: E <:< Throwable): String = {
    val sw = new StringWriter
    throwable.printStackTrace(new PrintWriter(sw))

    s"${scala.Console.BOLD}Cause: ${scala.Console.RESET}\n ${sw.toString}"
  }
}