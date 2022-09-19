package net.cimadai.crypto

import utest._

object CryptoSpec extends TestSuite {
  import scala.util.{Failure, Success}

  val tests = this {
    "Ability to generate a random KeyPair"- {
      for {
        keypair <- KeyPair.random
      } yield {
        val hexaPrivateKey = keypair.privateKey.hexa
        val hexaPublicKey  = keypair.publicKey.hexa
        // println("1234567890123456789012345678901234567890123456789012345678901234")
        // println(hexaPrivateKey)
        // println(hexaPublicKey)
        assert(hexaPrivateKey.length == 64)
        assert(hexaPublicKey.length == 64)
        assert(hexaPrivateKey.forall(c => (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')))
        assert(hexaPublicKey .forall(c => (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')))
      }
    }

    "SHA3EdDSAKeyPair should match a known, valid, existing key pair"- {
      //                     1234567890123456789012345678901234567890123456789012345678901234
      val givenPrivateKey = "FD3E07032D62B932C5CDDDAFC242AC6E4A4573DC7A00B38312BDB22C5B6F957D".toLowerCase
      val givenPublicKey  = "A447BDA11CC533D7804FDCF3D5E70832AAA795BDFA1F114F7D7992219DFF3FA1".toLowerCase
      for {
        keypair    <- KeyPair   .apply(givenPrivateKey)
        privateKey <- PrivateKey.apply(givenPrivateKey)
        publicKey  <- PublicKey .apply(givenPublicKey)
      } yield {
        val hexaPrivateKey = privateKey.hexa
        val hexaPublicKey  = publicKey.hexa
        // println("1234567890123456789012345678901234567890123456789012345678901234")
        // println(hexaPrivateKey)
        // println(hexaPublicKey)
        assert(hexaPrivateKey.length == 64)
        assert(hexaPublicKey.length == 64)
        assert(hexaPrivateKey.forall(c => (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')))
        assert(hexaPublicKey .forall(c => (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')))

        assert(hexaPrivateKey == givenPrivateKey)
        assert(hexaPublicKey  == givenPublicKey)
        assert(keypair.privateKey.hexa == givenPrivateKey)
        assert(keypair.publicKey.hexa  == givenPublicKey)
      }
    }

    "SHA3EdDSAKeyPair should be able to sign and verify messages"- {
      //                     1234567890123456789012345678901234567890123456789012345678901234
      val givenPrivateKey = "FD3E07032D62B932C5CDDDAFC242AC6E4A4573DC7A00B38312BDB22C5B6F957D".toLowerCase
      val givenPublicKey  = "A447BDA11CC533D7804FDCF3D5E70832AAA795BDFA1F114F7D7992219DFF3FA1".toLowerCase
      for {
        keypair    <- KeyPair.apply(givenPrivateKey)
      } yield {
        assert(keypair.privateKey.hexa == givenPrivateKey)
        assert(keypair.publicKey.hexa  == givenPublicKey)
        val message1 = "This is a test message"
        keypair.privateKey.sign(message1) match {
          case Success(signed) =>
            keypair.publicKey.verify(signed, message1) match {
              case Success(verified) =>
                assert(verified)
                val message2 = "This is another test message."
                keypair.privateKey.sign(message2) match {
                  case Success(signed) =>
                    keypair.publicKey.verify(signed, message2) match {
                      case Success(verified) => assert(verified)
                      case Failure(t) => throw t
                    }
                  case Failure(t) => throw t
                }
              case Failure(t) => throw t
            }
          case Failure(t) => throw t
        }
      }
    }
  }
}
