package net.cimadai.iroha

import utest._

object IrohaValidatorSpec extends TestSuite with TestHelpers {
  import scala.util.Try

  val tests = this {
    "Domain names: Should validate input in legacy mode: "-{
      class wrapper extends Iroha.Validation
      val o = new wrapper
      "$abc"             -{ assert(isFailure(o.parseDomainName(_, legacy=true))) }
      "morgan%stanley"   -{ assert(isFailure(o.parseDomainName(_, legacy=true))) }
      "japan$05"         -{ assert(isFailure(o.parseDomainName(_, legacy=true))) }
      //---
      "abc"              -{ assert(isSuccess(o.parseDomainName(_, legacy=true))) }
      "test"             -{ assert(isSuccess(o.parseDomainName(_, legacy=true))) }
      "morgan_stanley"   -{ assert(isSuccess(o.parseDomainName(_, legacy=true))) }
      "japan05"          -{ assert(isSuccess(o.parseDomainName(_, legacy=true))) }
      //---
      "abc.xx"           -{ assert(isFailure(o.parseDomainName(_, legacy=true))) }
      "abc.xx.yy"        -{ assert(isFailure(o.parseDomainName(_, legacy=true))) }
      "abc.xx.yy.zz"     -{ assert(isFailure(o.parseDomainName(_, legacy=true))) }
      "xn--abc.xx.yy.zz" -{ assert(isFailure(o.parseDomainName(_, legacy=true))) }
    }

    "Domain names: Should validate input according to RFC1035 and RFC1183: "-{
      class wrapper extends Iroha.Validation
      val o = new wrapper
      "$abc"             -{ assert(isFailure(o.parseDomainName)) }
      "morgan%stanley"   -{ assert(isFailure(o.parseDomainName)) }
      "japan$05"         -{ assert(isFailure(o.parseDomainName)) }
      //---
      "abc"              -{ assert(isFailure(o.parseDomainName)) }
      "test"             -{ assert(isFailure(o.parseDomainName)) }
      "morgan_stanley"   -{ assert(isFailure(o.parseDomainName)) }
      "japan05"          -{ assert(isFailure(o.parseDomainName)) }
      //---
      "abc.xx"           -{ assert(isSuccess(o.parseDomainName)) }
      "abc.xx.yy"        -{ assert(isSuccess(o.parseDomainName)) }
      "abc.xx.yy.zz"     -{ assert(isSuccess(o.parseDomainName)) }
      "xn--abc.xx.yy.zz" -{ assert(isSuccess(o.parseDomainName)) }
    }


    "Asset names: Should reject invalid input: "- {
      class wrapper extends Iroha.Validation
      val o = new wrapper
      ""                                  -{ assert(isFailure(o.parseAssetName)) }
      "a23456789012345678901234567890123" -{ assert(isFailure(o.parseAssetName)) }
      "$2345678901234567890123456789012"  -{ assert(isFailure(o.parseAssetName)) }
      "silver.coin"                       -{ assert(isFailure(o.parseAssetName)) }
    }
    "Asset names: Should accept valid input: "-{
      class wrapper extends Iroha.Validation
      val o = new wrapper
      "a2345678901234567890123456789012"  -{ assert(isSuccess(o.parseAssetName)) }
      "A2345678901234567890123456789012"  -{ assert(isSuccess(o.parseAssetName)) }
      "coin"                              -{ assert(isSuccess(o.parseAssetName)) }
    }

    "Account names: Should reject invalid input: "- {
      class wrapper extends Iroha.Validation
      val o = new wrapper
      ""                                  -{ assert(isFailure(o.parseAccountName)) }
      "a23456789012345678901234567890123" -{ assert(isFailure(o.parseAccountName)) }
      "A2345678901234567890123456789012"  -{ assert(isFailure(o.parseAccountName)) }
      "$2345678901234567890123456789012"  -{ assert(isFailure(o.parseAccountName)) }
      "john.smith"                        -{ assert(isFailure(o.parseAccountName)) }
    }
    "Account names: Should accept valid input: "-{
      class wrapper extends Iroha.Validation
      val o = new wrapper
      "a2345678901234567890123456789012"  -{ assert(isSuccess(o.parseAccountName)) }
      "jsmith"                            -{ assert(isSuccess(o.parseAccountName)) }
    }

    "Role names: Should reject invalid input: "- {
      class wrapper extends Iroha.Validation
      val o = new wrapper
      ""                                               -{ assert(isFailure(o.parseRoleName)) }
      "a234567890123456789012345678901234567890123456" -{ assert(isFailure(o.parseRoleName)) }
      "$23456789012345678901234567890123456789012345"  -{ assert(isFailure(o.parseRoleName)) }
      "super.user"                                     -{ assert(isFailure(o.parseRoleName)) }
    }
    "Role names: Should accept valid input: "-{
      class wrapper extends Iroha.Validation
      val o = new wrapper
      "a23456789012345678901234567890123456789012345"  -{ assert(isSuccess(o.parseRoleName)) }
      "A23456789012345678901234567890123456789012345"  -{ assert(isSuccess(o.parseRoleName)) }
      "superuser"                                      -{ assert(isSuccess(o.parseRoleName)) }
    }

    "Amount: Should reject invalid input: "- {
      class wrapper extends Iroha.Validation
      val o = new wrapper
      ""          -{ assert(isFailure(o.parseAmount)) }
      "-1.05"     -{ assert(isFailure(o.parseAmount)) }
      "-0.0"      -{ assert(isFailure(o.parseAmount)) }
      "Infinity"  -{ assert(isFailure(o.parseAmount)) }
      "-Infinity" -{ assert(isFailure(o.parseAmount)) }
      "+Infinity" -{ assert(isFailure(o.parseAmount)) }
      "NaN"       -{ assert(isFailure(o.parseAmount)) }
      "zero"      -{ assert(isFailure(o.parseAmount)) }
    }
    "Amount: Should accept valid input: "-{
      class wrapper extends Iroha.Validation
      val o = new wrapper
      "0"         -{ assert(isSuccess(o.parseAmount)) }
      "0.0"       -{ assert(isSuccess(o.parseAmount)) }
      "1"         -{ assert(isSuccess(o.parseAmount)) }
      "1.0"       -{ assert(isSuccess(o.parseAmount)) }
    }

    "Description: Should reject invalid input: "- {
      class wrapper extends Iroha.Validation
      val o = new wrapper
      ""                                                                  -{ assert(isFailure(o.parseDescription)) }
      "12345678901234567890123456789012345678901234567890123456789012345" -{ assert(isFailure(o.parseDescription)) }
    }
    "Description: Should accept valid input: "-{
      class wrapper extends Iroha.Validation
      val o = new wrapper
      "1234567890123456789012345678901234567890123456789012345678901234"  -{ assert(isSuccess(o.parseDescription)) }
      "It can be any text, given that it is limited to 64 characters. ="  -{ assert(isSuccess(o.parseDescription)) }
    }

    "IPv4 address: Should reject invalid input: "- {
      class wrapper extends Iroha.Validation
      val o = new wrapper
      ""                                    -{ assert(isFailure(o.parseIPv4)) }
      "0"                                   -{ assert(isFailure(o.parseIPv4)) }
      "0.0"                                 -{ assert(isFailure(o.parseIPv4)) }
      "0.0.0"                               -{ assert(isFailure(o.parseIPv4)) }
      "0.0.0.0"                             -{ assert(isFailure(o.parseIPv4)) }
    }
    "IPv4 address: Should accept valid input: "-{
      class wrapper extends Iroha.Validation
      val o = new wrapper
      "127.0.0.1"                           -{ assert(isSuccess(o.parseIPv4)) }
      "192.168.0.1"                         -{ assert(isSuccess(o.parseIPv4)) }
      "172.10.20.30"                        -{ assert(isSuccess(o.parseIPv4)) }
      "10.10.20.30"                         -{ assert(isSuccess(o.parseIPv4)) }
    }

    "IPv6 address: Should reject invalid input: "- {
      class wrapper extends Iroha.Validation
      val o = new wrapper
      ""                                    -{ assert(isFailure(o.parseIPv6)) }
      "::"                                  -{ assert(isSuccess(o.parseIPv6)) }
      "::0"                                 -{ assert(isSuccess(o.parseIPv6)) }
    }
    "IPv6 address: Should accept valid input: "-{
      class wrapper extends Iroha.Validation
      val o = new wrapper
      "::1"                                 -{ assert(isSuccess(o.parseIPv6)) }
      "fdfa:ffee:1a8b::1"                   -{ assert(isSuccess(o.parseIPv6)) }
      "2001:470:1f1c:b61::2"                -{ assert(isSuccess(o.parseIPv6)) }
      "2001:470:195e:0:be5f:f4ff:fef9:b2f6" -{ assert(isSuccess(o.parseIPv6)) }
    }

    "Hostname address: Should reject invalid input: "- {
      class wrapper extends Iroha.Validation
      val o = new wrapper
      ""                                    -{ assert(isFailure(o.parseHostname)) }
      "0"                                   -{ assert(isFailure(o.parseHostname)) }
      "0.0"                                 -{ assert(isFailure(o.parseHostname)) }
      "0.0.0"                               -{ assert(isFailure(o.parseHostname)) }
      "0.0.0.0"                             -{ assert(isFailure(o.parseHostname)) }
      "::"                                  -{ assert(isFailure(o.parseHostname)) }
      "::0"                                 -{ assert(isFailure(o.parseHostname)) }
      "invalid.example.com"                 -{ assert(isFailure(o.parseHostname)) }
    }
    "Hostname address: Should accept valid input: "-{
      class wrapper extends Iroha.Validation
      val o = new wrapper
      "localhost"                           -{ assert(isSuccess(o.parseHostname)) }
      "www.google.com"                      -{ assert(isSuccess(o.parseHostname)) }
      "terra.mathminds.io"                  -{ assert(isSuccess(o.parseHostname)) }
    }

    "Peer address: Should reject invalid input: "- {
      class wrapper extends Iroha.Validation
      val o = new wrapper
      ""                                    -{ assert(isFailure(o.parsePeerAddress)) }
      "0"                                   -{ assert(isFailure(o.parsePeerAddress)) }
      "0.0"                                 -{ assert(isFailure(o.parsePeerAddress)) }
      "0.0.0"                               -{ assert(isFailure(o.parsePeerAddress)) }
      "0.0.0.0"                             -{ assert(isFailure(o.parsePeerAddress)) }
      "::"                                  -{ assert(isSuccess(o.parsePeerAddress)) }
      "::0"                                 -{ assert(isSuccess(o.parsePeerAddress)) }
      "invalid.example.com"                 -{ assert(isFailure(o.parsePeerAddress)) }
    }
    "Peer address: Should accept valid input: "-{
      class wrapper extends Iroha.Validation
      val o = new wrapper
      // IPv4
      "127.0.0.1"                           -{ assert(isSuccess(o.parsePeerAddress)) }
      "192.168.0.1"                         -{ assert(isSuccess(o.parsePeerAddress)) }
      "172.10.20.30"                        -{ assert(isSuccess(o.parsePeerAddress)) }
      "10.10.20.30"                         -{ assert(isSuccess(o.parsePeerAddress)) }
      // IPv6
      "::1"                                 -{ assert(isSuccess(o.parsePeerAddress)) }
      "fdfa:ffee:1a8b::1"                   -{ assert(isSuccess(o.parsePeerAddress)) }
      "2001:470:1f1c:b61::2"                -{ assert(isSuccess(o.parsePeerAddress)) }
      "2001:470:195e:0:be5f:f4ff:fef9:b2f6" -{ assert(isSuccess(o.parsePeerAddress)) }
      // Should be able to resolve these names
      "localhost"                           -{ assert(isSuccess(o.parsePeerAddress)) }
      "www.google.com"                      -{ assert(isSuccess(o.parsePeerAddress)) }
      "terra.mathminds.io"                  -{ assert(isSuccess(o.parsePeerAddress)) }
    }
  }

  def isSuccess(check: String => Try[String])
               (implicit testPath: utest.framework.TestPath): Boolean =
    check(testPath.value.last).isSuccess

  def isFailure(check: String => Try[String])
               (implicit testPath: utest.framework.TestPath): Boolean =
    check(testPath.value.last).isFailure
}
