package org.specs2
package guide
import examples._
import specification._

class SpecStructure extends Specification { def is =
  "Specification structure".title                                                                                       ^
                                                                                                                        """
### Presentation

<toc/>

In this chapter you will learn how to:

 * declare examples
 * share examples
 * add arguments for execution and reporting
 * format the layout of your specification
 * include or link specifications
 * give a title to your specification
 * define contexts and actions to execute before/after examples

### Declare examples

As seen in the [Quick Start](org.specs2.guide.QuickStart.html), a specification is a list of *fragments* separated by `^`:

      "this is my specification"                          ^
        "and example 1"                                   ! e1^
        "and example 2"                                   ! e2

      def e1 = success
      def e2 = success

In this style of specification, the "body" of each example is provided by 2 methods, separated from the specification
text. There is no specific recommendation on how you should name those methods but you can either use short names or
use the backtick notation for better readability:

      "this is my specification"                          ^
        "and example 1"                                   ! `first example`^
        "and example 2"                                   ! `second example`

      def `first example` = success
      def `second example` = success

You can even push this idea further by writing:

      "this is my specification"                          ^
        `and example 1`                                   ^
        `and example 2`

      def `and example 1` = success
      def `and example 2` = success

*(an IDE with good refactoring capabilities is a must-have in that case,...)*

###### Standard results

So the first way to create an Example is to follow a piece of text with `!` and provide anything of type `org.specs2.execute.Result`.
The simplest `Result` values are provided by the `StandardResults` trait, and match the 5 types of results provided by ***specs2***:

  * success: the example is ok
  * failure: there is a non-met expectation
  * anError: a non-expected exception occurred
  * skipped: the example is skipped possibly at runtime because some conditions are not met
  * pending: usually means "not implemented yet"

Two additional results are also available to track the progress of features:

  * done: a Success with the message "DONE"
  * todo: a Pending with the message "TODO"

###### Matcher results

Usually the body of an example is made of *expectations* using matchers:

     def e1 = 1 must_== 1

You can refer to the [Matchers](org.specs2.guide.Matchers.html)  guide to learn all about matchers and how to create expectations. There is however one
important point to note here. Because of the functional nature of ***specs2*** the result of an example is always the last
statement of its body. This example will never fail because the first expectation is "lost":

      "my example on strings" ! e1                // will never fail!

      def e1 = {
        "hello" must have size(10000)             // because this expectation will not be returned,...
        "hello" must startWith("hell")
      }

So the correct way of writing the example is:

      "my example on strings" ! e1               // will fail

      def e1 = "hello" must have size(10000) and
                            startWith("hell")

This can be seen as a restriction but this actually encourages a specification style where every expectation is carefully
specified (see [Mutable Specifications](#Mutable+Specifications) if you really can't live with that).

###### Auto-Examples

There is a handy functionality when your specification is about showing the use of a DSL or of an API. If your expectation
fits on one line, you can use it directly, as if it was an example. This is used in ***specs2*** to specify matchers:

     "beNone checks if an element is None"                             ^
     { None must beNone }                                              ^
     { Some(1) must not be none }                                      ^

In that case, the text of the example will be extracted from the source file and the output will be:

     beNone checks if an element is None
       + None must beNone
       + Some(1) must not be none

A few things to remember about this feature:

 * the source file is expected to be found in the `src/test/scala` directory.
   This can be overriden by specifying the `specs2.srcTestDir` system property

 * the extraction of the source code is very rudimentary and will just extract one line of code.

 * for more robustness, but different results, you can use the `descFromExpectations` arguments (creates an
   `args(fromSource=false)`) to take the "ok message" from the expectation as the example description:

         // outputs: List(1, 2) must contain(1)
         { List(1, 2) must contain(1) }

         // outputs: 'List(1, 2)' contains '1'
         descFromExpectations ^
         { List(1, 2) must contain(1) }

###### Using the text of the Example

It is possible to use the text of an example to extract meaningful values, use them in the example body and avoid
repeating oneself:

    "Bob should pay 12"   ! e1

    val toPay = Map("Bob"->"12", "Bill"->"10")           // a "database" of expected values
    val ShouldPay = "(.*) should pay (\\d+)".r           // a regular expression for extracting the name and price

    def e1 = (s: String) => {
      val ShouldPay(name, price) = s                     // extracting the values
      toPay(name) must_== price                          // using them for the expectation
    }

In that case the argument passed to the `!` method is a function taking a String and returning a Result.

###### Given / When / Then

In the same fashion, the Given/When/Then style of writing specifications is supported, albeit using a mutable object to
collect the successive states of the system:

      "Given that the customer buys 3 books at 10 dollars each"                                        ! c1.buyBook^
      "Given that the customer buys 1 book at 20 dollars"                                              ! c1.buyBook^
      "When he checks out"                                                                             ! c1.checkout^
      "Then the total price must be 50 dollars"                                                        ! c1.total^
                                                                                                       end

      case object c1 {
        val BuyBooks = ".* buys (\\d+) book.? at (\\d+) .*".r     // a regular expression for extracting the quantity and price
        val TotalBooks = ".* must be (\\d+) .*".r                 // a regular expression for extracting the total price
        val books: scala.collection.mutable.Map[Int, Int] = new scala.collection.mutable.HashMap[Int, Int]()

        def buyBook = (s: String) => {
          val BuyBooks(qty, price) = s
          books += qty.toInt -> price.toInt
          success
        }
        def checkout = books.pp must not be empty
        def total = (s: String) => {
          val TotalBooks(total) = s
          books.foldLeft(0)((res, cur) => res + cur._1 * cur._2) must_== total.toInt
        }
      }

###### For ***specs*** afficionados

If you come from a ***specs*** background, it might seem difficult at first to "translate" the way you used to write
specifications to the new way. Here's a quickstart, you need to:

 * replace `should` by `^` and `in` by `!`
 * chain examples with `^`
 * separate blocks of examples with `p^`

        "'Hello world' should" ^ {
          "contain 11 characters" ! {
            "Hello world" must have size(11)
          }^
          "start with 'Hello'" ! {
            "Hello world" must startWith("Hello")
          }^
          "with 'world'" ! {
            "Hello world" must endWith("world")
          }
        }^
        p^
        "'Hey you' should" ^ {
          "contain 7 characters" ! {
            "Hey you" must have size(7)
          }
        }

### Share examples

In a given specification some examples may look similar enough that you would like to "factor" them out and share
them between different parts of your specification. The best example of this situation is a specification for a Stack of
limited size:

        class StackSpec extends SpecificationWithJUnit { def is =
          "Specification for a Stack with a limited capacity".title                 ^
                                                                                    p^
          "An empty stack should"                                                   ^
            "behave like an empty stack"                                            ^ isEmpty^
                                                                                    endp^
          "A non-empty stack should"                                                ^
            "behave like a non empty stack"                                         ^ isNonEmpty(normal)^
                                                                                    endp^
          "A stack below full capacity should"                                      ^
            "behave like a non empty stack"                                         ^ isNonEmpty(normal)^
            "behave like a stack below capacity"                                    ^ isNotFull(normal)^
                                                                                    endp^
          "A full stack should"                                                     ^
            "behave like a non empty stack"                                         ^ isNonEmpty(full)^
            "behave like a full stack"                                              ^ isFull(full)^
                                                                                    end

          def normal = Stack(10, 2)
          def full = Stack(10, 10)

          def isEmpty =
            "throw an exception when sent #top"                                     ! empty().e1^
            "throw an exception when sent #pop"                                     ! empty().e2

          def isNonEmpty(s: =>SizedStack) =
            "not be empty"                                                          ! nonempty(s).size^
            "return the top item when sent #top"                                    ! nonempty(s).top1^
            "not remove the top item when sent #top"                                ! nonempty(s).top2^
            "return the top item when sent #pop"                                    ! nonempty(s).pop1^
            "remove the top item when sent #pop"                                    ! nonempty(s).pop2

          def isNotFull(s: =>SizedStack) =
            "add to the top when sent #push"                                        ! notfull(s).e1

          def isFull(s: =>SizedStack) =
            "throw an exception when sent #push"                                    ! fullStack(s).e1

          case class empty() {
            val stack = new SizedStack(10)
            def e1 = stack.top must throwA[NoSuchElementException]
            def e2 = stack.pop must throwA[NoSuchElementException]
          }
          case class nonempty(stack: SizedStack) {
            def size = !stack.isEmpty
            def top1 = stack.top must_== stack.size
            def top2 = {
              stack.top
              stack.top must_== stack.size
            }
            def pop1 = {
              val topElement = stack.size
              stack.pop must_== topElement
            }
            def pop2 = {
              stack.pop
              stack.top must_== stack.size
            }
          }
          case class notfull(stack: SizedStack) {
            def e1 = {
              stack push (stack.size + 1)
              stack.top must_== stack.size
            }
          }
          case class fullStack(stack: SizedStack) {
            def e1 = stack push (stack.size + 1) must throwAn[Error]
          }
        }

### Declare arguments

At the beginning of a specification you can declare arguments which configure the execution and reporting of the specification.
For example, you can turn off the concurrent execution of examples with the `args` method:

       class ExamplesOneByOne extends Specification { def is =
         args(sequential=true)              ^
         "first example"                    ! e1 ^
         "the the second one"               ! e2 ^
                                            end
       }

For the complete list of arguments and shortcut methods read the [Runners](org.specs2.guide.Runners.html) page.

### Layout

The layout of text in ***specs2*** is mostly done automatically so that the text in the source code should look like the
displayed text after execution. You can turn off that automatic layout by adding the `noindent` arguments at the beginning
of your specification

      class MySpecWithNoIndent extends Specification {
        def is = noindent ^ ....
      }

##### The rules

By default the layout of a specification will be computed automatically based on intuitive rules:

  * when an example follows a text, it is indented
  * 2 successive examples will be at the same indentation level
  * when a text follows an example, this means that you want to describe a "subcontext", so the next examples will be
    indented with one more level

Let's see a standard example of this. The following fragments:

    "this is some presentation text"      ^
      "and the first example"             ! success^
      "and the second example"            ! success

will be executed and displayed as:

    this is some presentation text
    + and the first example
    + and the second example

If you specify a "subcontext", you will get one more indentation level:

    "this is some presentation text"      ^
      "and the first example"             ! success^
      "and the second example"            ! success^
      "and in this specific context"      ^
        "one more example"                ! success^

will be executed and displayed as:

    this is some presentation text
    + and the first example
    + and the second example
      and in this specific context
      + one more example

##### The formatting fragments

Given the rules above, you might need to use some *formatting fragments* to adjust the display

###### Separating groups of examples

The best way to separate blocks of examples is to add a blank line between them by using `p` (as in "paragraph"):

    "this is some presentation text"      ^
      "and the first example"             ! success^
      "and the second example"            ! success^
                                          p^
    "And another block of examples"       ^
      "with this example"                 ! success^
      "and that example"                  ! success

This will be displayed as:

    this is some presentation text
    + and the first example
    + and the second example

    And another block of examples
    + with this example
    + and that example

That looks remarkably similar to the specification code, doesn't it? What `p` does is:

 * add a blank line (this can also be done with a simple `br`)
 * decrement the current indentation level by 1 (Otherwise the new Text would be seen as a subcontext)

###### Reset the levels

When you start having deep levels of indentation, you might need to start the next group of examples at level 0. For
example, in this specification

    "There are several options for displaying the text"      ^
      "xonly displays nothing but failures"                  ! success^
      "there is also a color option"                         ^
        "rgb=value uses that value to color the text"        ! rgb^
        "nocolor dont color anything"                        ! nocolor^
                                                             p^
    "There are different ways of hiding the text"            ^
        "by tagging the text"                                ! hideTag

Even with `p` the next group of examples will not start at level 0. What you need to do in that case is use `end`:

    "There are several options for displaying the text"      ^
      "xonly displays nothing but failures"                  ! success^
      "there is also a color option"                         ^              // this text will be indented
        "rgb=value uses that value to color the text"        ! rgb^         // and the following examples as well
        "nocolor dont color anything"                        ! nocolor^
                                                             end^
    "There are different ways of hiding the text"            ^              // this text will be properly indented now
      "by tagging the text"                                  ! hideTag^
                                                             end

This will be displayed as:

    There are several options for displaying the text
    + xonly displays nothing but failures
      there is also a color option
      + rgb=value uses that value to color the text
      + nocolor dont color anything
    There are different ways of hiding the text
    + by tagging the text

And if you want to reset the indentation level *and* add a blank line you can use `end ^ br` (or `endbr` as seen in
"Combinations" below).

###### Changing the indentation level

If, for whatever reason, you wish to have more or less indentation, you can use the `t` and `bt` fragments (as in "tab" and
"backtab"):

    "this text"                                     ^ bt^
    "doesn't actually have an indented example"     ! success

    "this text"                                     ^ t^
        "has a very indented example"               ! success

 The number of indentation levels (characterized as 2 spaces on screen) can also be specified by using `t(n)` or `bt(n)`.

###### Combinations

Some formatting elements can be combined:

 * `p` is actually `br ^ bt`
 * `endbr` is `end ^ br`
 * `endp` is `end ^ p`  (same effect as `endbr` but shorter :-))

### Include or link specifications

###### Include specifications

There is a simple mechanism for including "children" specification in a given specification. You use the `include` method,
as if you were adding a new fragment:

    "This is an included specification"     ^
      include(childSpec)

The effect of doing so is that all the fragments of the children specification will be inlined in the parent one. This
is exactly what is done in this page of the user guide, but with a twist

    include(xonly, exampleTextExtraction)        ^
    include(xonly, new GivenWhenThenSpec)        ^
    include(xonly, exampleTextIndentation)       ^
    include(xonly, resetTextIndentation)         ^

In this case I give specific arguments to the included specification so that it is only displayed when there are failures.

###### Link specifications

In order to create a User Guide such as this one, you might want to have the "included" specification being written to
another html file. The syntax to do this is the following:

    "a " ~ ("quick start guide", new QuickStart)                                            ^
    "how to " ~ ("structure your specification", new SpecStructure)                         ^
    "how to use " ~ ("matchers", new Matchers)                                              ^
    "how to use " ~ ("mock objects", new Mocks)                                             ^

In this case the `~` operator is used to create a HtmlLink where:

 * "a" is the beginning of the text
 * "quick start guide" is the text that will be highlighted as a url link
 * `new QuickStart` is the specification to include, the url being derived from the specification class name

Note that if you want to add some text after the url link, you can use the more general form:

     "before text" ~ ("text to highlight", specification, "after text")
     // or
     "before text" ~ ("text to highlight", specification, "after text", "tooltip")

And if there's no "before text":

     "text to highlight" ~ specification
     // or
     "text to highlight" ~ (specification, "after text")
     // or
     "text to highlight" ~ (specification, "after text", "tooltip")


### Specification title

Usually the title of a specification is derived from the specification class name. However if you want to give a more
readable name to your specification report you can do the following:

     class MySpec extends Specification { def is =
        "My beautiful specifications".title                           ^
                                                                      p^
        "The rest of the spec goes here"                              ^ end
     }

### Contexts

There are some situations when we want to make sure that some actions are always done before or after each example, like
opening a database connection or deleting a file. ***specs2*** offers a support for those actions with specific traits:

 * `Before`
 * `After`
 * `Around`
 * and all combinations of the above traits

Let's see how to use them.

##### Defining `Before` actions

Let's say that you want to create a specific file before executing each example of your specification. You define a class
inheriting from the `Before` trait and containing your examples:

    case class withFile extends Before {
      def before = createFile("test")
    }

The `Before` trait requires you to define a `before` method defining an action to do before every call to the `apply`
method. Then, there are many ways to use this context class. Here's one of them:

    "this is a first example where I need a file"          ! withFile(e1)
    "and another one"                                      ! withFile(e2)

    def e1 = readFile("test") must_== "success"
    def e2 = readFile("missing") must_== "failed"

Or if you need "local variables" as well in your examples:

    "this is a first example where I need a file"          ! withFile(c().e1)
    "and another one"                                      ! withFile(c().e2)

    case class c() {
      val (okFile, koFile = ("test", "missing")
      def e1 = readFile(okFile) must_== "success"
      def e2 = readFile(koFile) must_== "failed"
    }

`Before` actions can also fail for several reasons. When that happens examples are not executed and their result instead
is the result of the `before` action:

 * if an exception occurs during the `before` action, an `Error` is created
 * if some prerequisites are not met (not the right type of database for example), you can return a `Skipped` result to
   abort the execution of all the examples:

          def before = {
            val db = openDatabase
            db.databaseType must be oneOf("H2", "Oracle").orSkip("not the appropriate database type")
          }

##### Defining `After` actions

Actions to execute after examples are not declared very differently from `Before` ones. Just extend the `After` trait:

    case class withCleanup extends After {
      def after = deleteFile("test")
    }

##### Defining `Around` actions

Another use case for "contextual" actions are actions which must executed in a given context like an Http session. In order
to define this type of action you must extend the `Around` trait and specify a `around` function:

    case class http extends Around {
      def around[T <% Result](t: =>T) = openHttpSession("test") {
        t  // execute t inside a http session
      }
    }

##### Composing contexts

Note that you can also compose contexts in order to reuse them to build more complex scenarios:

    case class withFile extends Before {
      def before = createFile("test")
    }
    case class withDatabase extends Before {
      def before = openDatabase("test")
    }
    val init = withFile() compose withDatabase()

    "Do something on the full system"                   ! init(success)

##### Actions

Some setup actions are very time consuming and should be executed only once for the whole specification. This can be achieved
by inserting some silent `Action` in between fragements:

    class DatabaseSpec extends Specification { def is =

      "This specification opens a database and execute some tests"     ^ Action(openDatabase) ^
        "example 1"                                                    ! success ^
        "example 2"                                                    ! success ^
                                                                       Action(closeDatabase)^
                                                                       end
    }

The examples are (by default) executed concurrently between the 2 actions and the "result" of actions will never be
reported unless if there is a failure.

#### Mutable Specifications

If you've read the [Philosophy](org.specs2.guide.Philosophy.html) page you're fully aware of the danger of using side effects to create and execute specifications.
However assuming that you won't try to execute the same Specification concurrently, there is a way to create Specifications
which almost look like ***specs*** specifications. Here is a fully commented example showing how to do it:

      import mutable._
      import specification._
      import execute.Success

      /**
       * This specification shows how to use the mutable.Specification trait to create a specs-like Specification
       * where the fragments are built using a mutable variable
       */
      class MutableSpec extends SpecificationWithJUnit {
        // arguments are simply declared at the beginning of the specification if needed
        args(xonly=true)
        // an action to execute before the specification must be declared before any example
        action {
          // setup database here
          success
        }

        "'Hello world'" should {
          "contain 11 characters" in {
            "Hello world" must have size(11)
          }
          "start with 'Hello'" in {
            "Hello world" must startWith("Hello")
          }
          /**
           * a failing example will stop right away, without having to "chain" expectations
           */
          "with 'world'" in {
            // uncommenting this line will stop the execution right away with a Failure
            // "Hello world" must startWith("Hi")
            "Hello world" must endWith("world")
          }
        }
        /**
         * There's no "context management", so you need case classes to manage setup and variables
         */
        "'Hey you'" should {
          // this one uses a "before" method
          "contain 7 characters" in context {
            "Hey you" must have size(7)
          }
          // System is a Success result. If the expectations fail when building the object, the example will fail
          "contain 7 characters" in new system {
            string must have size(7)
          }
          // otherwise a case class can be used but the example body will be further down the file
          "contain 7 characters" in system2().e1
        }
        // you can add links to other specifications with `link`
        link("how" ~ ("to do hello world", new HelloWorldSpec))
        // you can include other specifications with `include`
        include(new HelloWorldSpec)

        // an action to execute after the specification must be declared after all examples
        action {
          // close the database here
          success
        }


        object context extends Before {
          def before = () // do something to setup the context
        }
        trait system extends Success {
          val string = "Hey you"
        }
        case class system2() {
          val string = "Hey you"
          def e1 = string must have size(7)
        }
      }

As you can see in the specification above, any failing expectation will stop the evaluation of an Example. This behavior
is provided by a trait named `org.specs2.matcher.MustThrownMatchers` that you can reuse in a regular Specification if you
want the same behavior.


 - - -
                                                                                                                        """^
                                                                                                                        br^
  include(xonly, exampleTextExtraction)                                                                                 ^
  include(xonly, new GivenWhenThenSpec)                                                                                 ^
  include(xonly, exampleTextIndentation)                                                                                ^
  include(xonly, resetTextIndentation)                                                                                  ^
  include(xonly, pTextIndentation)                                                                                      ^
  include(xonly, databaseSpec)                                                                                          ^
                                                                                                                        end

  val exampleTextExtraction = new Specification { def is =
    "Text extraction".title     ^
    "Bob should pay 12"         ! e1

    val toPay = Map("Bob"->"12", "Bill"->"10")           // a "database" of expected values
    val ShouldPay = "(.*) should pay (\\d+)".r           // a regular expression for extracting the name and price

    def e1 = (s: String) => {
      val ShouldPay(name, price) = s                     // extracting the values
      toPay(name) must_== price                          // using them for the expectation
    }
  }

  val exampleTextIndentation = new Specification { def is =
    "Text indentation".title              ^
    "this is some presentation text"      ^
      "and the first example"             ! success^
      "and the second example"            ! success
  }

  val resetTextIndentation = new Specification { def is =
    "Reset indentation".title                                ^
    "There are several options for displaying the text"      ^
      "xonly displays nothing but failures"                  ! success^
      "there is also a color option"                         ^              // this text will be indented
        "rgb=value uses that value to color the text"        ! rgb^         // and the following examples as well
        "nocolor dont color anything"                        ! nocolor^ end^
    "There are different ways of hiding the text"            ^              // this text will be properly indented now
        "by tagging the text"                                ! hideTag^
                                                             end
    def rgb = success
    def nocolor = success
    def hideTag = success
  }

  val pTextIndentation = new Specification { def is =
    "Text paragraph".title                ^
    "this is some presentation text"      ^
      "and the first example"             ! success^
      "and the second example"            ! success^
                                          p^
    "And another block of examples"       ^
      "with this example"                 ! success^
      "and that example"                  ! success^
                                          end
  }

  val databaseSpec = new  Specification { def is =
    "Database specification".title                                   ^
    "This specification opens a database and execute some tests"     ^
                                                                     Action(openDatabase) ^
      "example 1"                                                    ! success ^
      "example 2"                                                    ! success ^
                                                                     Action(closeDatabase)^
                                                                     end
    def openDatabase = success
    def closeDatabase = success
  }

}
