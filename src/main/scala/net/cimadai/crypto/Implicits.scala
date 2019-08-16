package net.cimadai.crypto

import acyclic.pkg

object Implicits {
  implicit class ImplicitByteArray(bytes: Array[Byte]) {


    /** Converts an [Array[Byte]] to its corresponding hexadecimal representation. */
    implicit def hexa: String = transform(bytes)

    /** Calculates a hash of a given byte array. */
    implicit def hash: Array[Byte] = Crypto.digest256.digest(bytes)

    private def transform(data: Array[Byte]): String =
      data
        .map(byte => "%02x".format(byte))
        .mkString

    //XXX def validate(hexa: String): Try[String] =
    //XXX   if(hexa.forall(c => (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')))
    //XXX     Success(hexa)
    //XXX   else
    //XXX     Failure(new InternalError("this should never happen: invalid hexadecimal key"))

    //XXX private def size(data: Array[Byte]): Try[Array[Byte]] =
    //XXX   if(data.length == lengthByteArray)
    //XXX     Success(data)
    //XXX   else
    //XXX     Failure(new IllegalArgumentException(s"key should be ${lengthByteArray} bytes long."))

    //XXX private val lengthByteArray = 32
  }


  implicit class ImplicitHexaString(text: String) {
    import scala.util.{Try, Failure, Success}

    /** Converts  an hexadecimal representation to its corresponding [Array[Byte]]. */
    implicit def bytes: Try[Array[Byte]] = transform(text)

    private def transform(data: String): Try[Array[Byte]] = Try {
      data.sliding(2, 2).toArray
        .map(pair => Integer.parseInt(pair, 16).toByte)
    }

    //XXX private def size(data: String): Try[String] =
    //XXX   if(data.length == lengthHexa)
    //XXX     Success(data)
    //XXX   else
    //XXX     Failure(new IllegalArgumentException(s"key should be ${lengthHexa} bytes long."))

    //XXX private val lengthHexa = 64
  }
}
