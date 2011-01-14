package org.specs2
package form

import scala.xml._
import collection.Listx._
import ::>._
import xml.Nodex._
import execute._
import main.Arguments
import StandardResults._

/**
 * A Form is a container for Rows (@see Row) where each row contain some Cell (@see Cell).
 * It has an optional title and possibly no rows.
 * 
 * A Form can be executed by executing each row and collecting the results.
 */
case class Form(val title: Option[String] = None, val rows: List[Row] = (Nil: List[Row])) extends Executable with Text {

  /** @return the labels of all rows to build a header for the form */
  def header: List[Cell] = if (rows.isEmpty) Nil else rows(0).header.flatten
  
  /** @return all rows, including the header */
  lazy val allRows = title.map(t => Row.tr(TextCell(t))).toList ::: rows

  /** @return the maximum cell size, column by column */
  lazy val maxSizes = allRows.map(_.cells).safeTranspose.map(l => l.map(_.text.size).max[Int])

  /** @return a Form where every Row is executed with a Success */
  def setSuccess = new Form(title, rows.map(_.setSuccess))
  /** @return a Form where every Row is executed with a Failure */
  def setFailure = new Form(title, rows.map(_.setFailure))

  /** add a new Header, with at least one Field */
  def th(h1: Field[_], hs: Field[_]*): Form = tr(FieldCell(h1.header), hs.map((f: Field[_]) => FieldCell(f.header)):_*)
  /** add a new Header, with at least one Field */
  def th(h1: String, hs: String*): Form = th(Field(h1), hs.map(Field(_)):_*)
  /** add a new Row, with at least one Cell */
  def tr(c1: Cell, cs: Cell*): Form = {
    new Form(title, this.rows :+ Row.tr(c1, cs:_*))
  }
  /** add the rows of a form */
  def tr(f: Form): Form = {
    val oldRowsAndTitle = f.title.map(t => tr(new TextCell(t))).getOrElse(this).rows
    new Form(title, oldRowsAndTitle ++ f.rows)
  }
  
  /** 
   * execute all rows
   * @return a logical and on all results 
   */
  def execute = rows.foldLeft(success: Result) { (res, cur) => res and cur.execute }
  /**
   * execute all rows
   * @return a logical and on all results
   */
  def executeForm = Form(title, rows.map(_.executeRow))

  /** @return the printed form with a padding space size to use for each cell */
  def padText(size: Option[Int]): String = FormCell(this).padText(size)

  /** @return an xml description of this form */
  def toXml(implicit args: Arguments = Arguments()) = Form.toXml(this)(args)

  def subset[T <: Any { def form: Form }](f1: Seq[T], f2: Seq[T]): Form = {
    addLines(FormDiffs.subset(f1.map(_.form), f2.map(_.form)))
  }
  def subsequence[T <: Any { def form: Form }](f1: Seq[T], f2: Seq[T]): Form = {
    addLines(FormDiffs.subsequence(f1.map(_.form), f2.map(_.form)))
  }
  def set[T <: Any { def form: Form }](f1: Seq[T], f2: Seq[T]): Form = {
    addLines(FormDiffs.set(f1.map(_.form), f2.map(_.form)))
  }
  def sequence[T <: Any { def form: Form }](f1: Seq[T], f2: Seq[T]): Form = {
    addLines(FormDiffs.sequence(f1.map(_.form), f2.map(_.form)))
  }
  private def addLines(fs: Seq[Form]) = fs.foldLeft(this) { (res, cur) =>  res.tr(cur) }

  override def equals(a: Any) = a match {
    case f: Form => f.title == title && rows == f.rows
    case _       => false
  }
}
/**
 * Companion object of a Form to create:
 *   * an empty Form
 *   * a Form with no rows but a title
 *   * a Form with no title but one row
 *
 */
case object Form {

  /** @return an empty form */
  def apply() = new Form(None, Nil)
  /** @return an empty form with a title */
  def apply(title: String) = new Form(Some(title), Nil)
  /** @return a Form with one row */
  def tr(c1: Cell, c: Cell*) = new Form().tr(c1, c:_*)
  /** @return a Form with one row and cells formatted as header cells */
  def th(h1: Field[_], hs: Field[_]*) = new Form().th(h1, hs:_*)
  /** @return a Form with one row and cells formatted as header cells */
  def th(h1: String, hs: String*) = new Form().th(h1, hs:_*)

  /**
   * This method creates an xml representation of a Form as an Html table
   *
   * If the Form has issues, stacktraces are written and hidden under the table
   *
   * @return the xml representation of a Form
   */
  def toXml(form: Form)(implicit args: Arguments) = {
    val colnumber = FormCell(form).colnumber
    val formStacktraces = stacktraces(form)
    <form>
      <table  class="dataTable">
        {title(form, colnumber)}
        {rows(form, colnumber)}
      </table>
      {<i>[click on failed cells to see the stacktraces]</i> unless formStacktraces.isEmpty}
      {formStacktraces}
    </form>
  }

  /**
   * Private methods for building the Form xml
   */
  private def title(form: Form, colnumber: Int) = form.title.map(t => <tr><th colspan={colnumber.toString}>{t}</th></tr>).toList.reduce
  private def rows(form: Form, colnumber: Int)(implicit args: Arguments) = form.rows.map(row(_, colnumber)).reduce
  private def row(r: Row, colnumber: Int)(implicit args: Arguments) = {
    val spanned = r.cells.dropRight(1).map(cell(_)) ++ cell(r.cells.last, colnumber - r.cells.size)
    <tr>{spanned}</tr>
  }

  private def cell(c: Cell, colnumber: Int = 0)(implicit args: Arguments) = {
    if (colnumber > 1) {
      c.xml(args).toList match {
      case start ::> (e: Elem) => start ++ (e % new UnprefixedAttribute("colspan", colnumber.toString, Null))
        case other                         => other
      }
    } else
      c.xml(args).toList
  }

  /** @return the stacktraces for a Form */
  def stacktraces(form: Form)(implicit args: Arguments): NodeSeq = form.rows.map(stacktraces(_)(args)).reduce

  private def stacktraces(row: Row)(implicit args: Arguments): NodeSeq   = row.cells.map(stacktraces(_)(args)).reduce
  private def stacktraces(cell: Cell)(implicit args: Arguments): NodeSeq = cell.stacktraces(args)

}
