package net.cimadai.crypto

import java.security.{MessageDigest, NoSuchAlgorithmException}
import java.util

import net.i2p.crypto.eddsa.math.{Curve, GroupElement, ScalarOps}
import net.i2p.crypto.eddsa.spec.{EdDSANamedCurveTable, EdDSAParameterSpec, EdDSAPrivateKeySpec}

class SHA3EdDSAPrivateKeySpec(seed: Array[Byte], h: Array[Byte], a: Array[Byte], A: GroupElement, spec: EdDSAParameterSpec) extends
  EdDSAPrivateKeySpec(seed, h, a, A, spec) {
}

object SHA3EdDSAPrivateKeySpec {
  val b = 256

  /**
    * @param seed the private key
    * @param spec the parameter specification for this key
    * @throws IllegalArgumentException if seed length is wrong or hash algorithm is unsupported
    */
  def apply(seed: Array[Byte], spec: EdDSAParameterSpec): SHA3EdDSAPrivateKeySpec = {
    if (seed.length != b/8) {
      throw new IllegalArgumentException("seed length is wrong")
    }

    try {
      val hash = MessageDigest.getInstance("SHA-512")

      // H(k)
      val h = hash.digest(seed)
      //edDsaPrivateKeySha3_256Spec.h = hash.digest(seed)

      /*a = BigInteger.valueOf(2).pow(b-2);
      for (int i=3;i<(b-2);i++) {
          a = a.add(BigInteger.valueOf(2).pow(i).multiply(BigInteger.valueOf(Utils.bit(h,i))));
      }*/
      // Saves ~0.4ms per key when running signing tests.
      // TODO: are these bitflips the same for any hash function?
      h(0) = (h(0) & 248.toByte).toByte
      h((b/8)-1) = (h((b/8)-1) & 63.toByte).toByte
      h((b/8)-1) = (h((b/8)-1) | 64.toByte).toByte
      val a = util.Arrays.copyOfRange(h, 0, b/8)
      val A = spec.getB.scalarMultiply(a)

      new SHA3EdDSAPrivateKeySpec(seed, h, a, A, spec)
    } catch {
      case e: NoSuchAlgorithmException =>
        throw new IllegalArgumentException("Unsupported hash algorithm")
    }
  }

  /**
    * Initialize directly from the hash.
    * getSeed() will return null if this constructor is used.
    *
    * @param spec the parameter specification for this key
    * @param h    the private key
    * @throws IllegalArgumentException if hash length is wrong
    * @since 0.1.1
    */
  def apply(spec: EdDSAParameterSpec, h: Array[Byte]): SHA3EdDSAPrivateKeySpec = {
    if (h.length != b/4)
      throw new IllegalArgumentException("hash length is wrong")

    h(0) = (h(0) & 248.toByte).toByte
    h((b/8)-1) = (h((b/8)-1) & 63.toByte).toByte
    h((b/8)-1) = (h((b/8)-1) | 64.toByte).toByte

    val a = util.Arrays.copyOfRange(h, 0, b/8)
    val A = spec.getB.scalarMultiply(a)

    new SHA3EdDSAPrivateKeySpec(null, h, a, A, spec)
  }

}

object SHA3EdDSAParameterSpec {
  private val spec = EdDSANamedCurveTable.getByName("Ed25519")
  val curve: Curve = spec.getCurve
  val hashAlgo: String = spec.getHashAlgorithm
  val sc: ScalarOps = spec.getScalarOps
  val B: GroupElement = spec.getB
}

class SHA3EdDSAParameterSpec extends
  EdDSAParameterSpec(SHA3EdDSAParameterSpec.curve, SHA3EdDSAParameterSpec.hashAlgo,
    SHA3EdDSAParameterSpec.sc, SHA3EdDSAParameterSpec.B) {

  override def getHashAlgorithm: String = "SHA3-512"
}

