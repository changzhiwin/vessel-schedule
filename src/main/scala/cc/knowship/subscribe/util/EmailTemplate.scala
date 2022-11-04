package cc.knowship.subscribe.util

import zio.http.html._

object EmailTemplate {

  //"""<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">""" + container(trs).encode
  
  /**
    * 邮件布局
    * @param paragraphs 允许多个段落
    */
  def container(paragraphs: Html): Html = {
    html(
      xmlnsAttr := "http://www.w3.org/1999/xhtml",
      head(
        meta(httpEquivAttr := "Content-Type", contentAttr := "Multipart/Alternative; charset=UTF-8"),
        meta(nameAttr := "viewport", contentAttr := "width=device-width, initial-scale=1.0")
      ),
      body(
        styles := Seq("margin" -> "0", "padding" -> "0"),
        table(
          cellpaddingAttr := "0", cellspacingAttr := "0",
          borderAttr := "0", widthAttr :="100%",
          tr(
            td(
              table(
                alignAttr :="center", borderAttr :="0", widthAttr := "95%",
                cellpaddingAttr := "0", cellspacingAttr := "0",
                styles := Seq("border-collapse" -> "collapse;"),
                paragraphs
              )
            )
          )
        )
      ),
    )
  }

  private val lineStyles = Seq("margin-left" -> "0", "margin-right" -> "0", "font-size" -> "15px")

  def paragraph(content: String): Dom = {
    tr(
      td(
        p(styles := lineStyles, content)
      )
    )
  }

  def paragraph_hr: Dom = {
    tr(
      td(
        hr()
      )
    )
  }

  def paragraph_ul(dataList: Seq[String]): Dom = {
    tr(
      td(
        ul(
          dataList.map { item =>
            li(item)
          }
        )
      )
    )
  }

  def paragraph_2cols(dataList: Seq[(String, String)]): Dom = {
    tr(
      styles := Seq("vertical-align" -> "middle", "text-align" -> "center"),
      td(
        table(
          alignAttr :="center", borderAttr :="1", widthAttr := "95%",
          cellpaddingAttr := "0", cellspacingAttr := "0",
          styles := Seq("table-layout" -> "fixed"),

          dataList.map { item =>
            tr(
              td(
                // #87CEFA  #66CDAA
                styles := Seq("background-color" -> "#87CEFA", "vertical-align" -> "middle", "text-align" -> "center"),
                p(styles := lineStyles, item._1)
              ),
              td(
                p(
                  styles := lineStyles ++ Seq("vertical-align" -> "middle", "text-align" -> "center"),
                  item._2
                )
              )
            )
          }
        )
      )
    )
  }
}
