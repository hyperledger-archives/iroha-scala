package net.cimadai.iroha


import utest._

object ImplicitsSpec extends TestSuite with TestHelpers {
  import scala.util.{Failure, Success}

  val tests = this {
    val publicKeyHexa = "43eeb17f0bab10dd51ab70983c25200a1742d31b3b7b54c38c34d7b827b26eed"
    val privateKeyHexa = "0000000000000000000000000000000000000000000000000000000000000000"

    "ability to convert hexa inty bytes and back again" - {
      import net.cimadai.crypto.Implicits._

      val tryPublicKeyBytes = publicKeyHexa.bytes
      tryPublicKeyBytes match {
        case Success(publicKeyBytes) =>
          val actual = publicKeyBytes.hexa
          assert(publicKeyHexa == actual)
        case Failure(t) => throw t
      }

      val tryprivateKeyBytes = privateKeyHexa.bytes
      tryprivateKeyBytes match {
        case Success(privateKeyBytes) =>
          val actual = privateKeyBytes.hexa
          assert(privateKeyHexa == actual)
        case Failure(t) => throw t
      }
    }
  }
}
