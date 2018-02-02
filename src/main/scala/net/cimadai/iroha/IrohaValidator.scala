package net.cimadai.iroha

/**
  * Copyright Daisuke SHIMADA All Rights Reserved.
  * https://github.com/cimadai/iroha-scala
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *      http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

import scala.util.parsing.combinator.RegexParsers

object IrohaValidator {
  object DomainParser extends RegexParsers {
    //<domain> ::= <subdomain> | " "
    private def domain: Parser[String] = label ~ ("." ~> label).* ^^ {
      case label ~ (remaining@(_ :: _)) => s"$label.${remaining.mkString(".")}"
      case label ~ (Nil) => label
    }

    //<subdomain> ::= <label> | <subdomain> "." <label>

    //<label> ::= <letter> [ [ <ldh-str> ] <let-dig> ]
    private def label: Parser[String] = ((letter.+ ~ ldh_str.*) | (ldh_str.* ~ letter.+)) ^^ {
      case xs ~ ys => xs.mkString + ys.mkString
    }

    //<ldh-str> ::= <let-dig-hyp> | <let-dig-hyp> <ldh-str>
    private def ldh_str: Parser[String] = let_dig_hyp | let_dig_hyp ~ ldh_str.* ^^ {
      case xs ~ ys => xs.toString + ys.mkString
    }

    //<let-dig-hyp> ::= <let-dig> | "-"
    private def let_dig_hyp: Parser[String] = let_dig | "-"

    //<let-dig> ::= <letter> | <digit>
    private def let_dig: Parser[String] = letter | digit

    //<letter> ::= any one of the 52 alphabetic characters A through Z in upper case and a through z in lower case
    private def letter: Parser[String] = """[a-zA-Z]""".r

    //<digit> ::= any one of the ten digits 0 through 9
    private def digit: Parser[String] = """[0-9]""".r

    def apply(input: String): Either[String, Any] = parseAll(domain, input) match {
      case Success(expressionList, _) => Right(expressionList)
      case NoSuccess(errorMessage, next) => Left(s"$errorMessage on line ${next.pos.line} on column ${next.pos.column}")
    }
  }

  // This emulates std::isalnum.
  // See: http://en.cppreference.com/w/cpp/string/byte/isalnum
  def isAlphabetAndNumber(str: String): Boolean = {
    str.matches("""^[a-zA-Z0-9]+$""")
  }

  // This emulates std::islower.
  // See: http://en.cppreference.com/w/cpp/string/byte/islower
  def isLowerCase(str: String): Boolean = {
    str.matches("""^[a-z]+$""")
  }

  // Validate with RFC1305
  def isValidDomain(str: String): Boolean = {
    DomainParser(str).isRight
  }
}
