package net.cimadai.crypto

/**
  * Copyright Daisuke SHIMADA, Richard Gomes -  All Rights Reserved.
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

sealed trait PrivateKey {
  import jp.co.soramitsu.crypto.ed25519.EdDSAPrivateKey
  import scala.util.Try
  import java.nio.charset.Charset
  val inner: EdDSAPrivateKey
  /** Returns the public key as a byte array. */
  def publicKeyBytes: Array[Byte]
  /** Returns the public key as an hexadecimal [String]. */
  def publicKeyHexa: String
  /** Returns the private key as a byte array. */
  def bytes: Array[Byte]
  /** Returns the private key as an hexadecimal [String]. */
  def hexa: String

  /** Signs a message [String] under a certain [Charset]. */
  def sign(message: String, charset: Charset): Try[Array[Byte]]
  /** Signs a message [String]. */
  def sign(message: String): Try[Array[Byte]]
  /** Signs a byte array. */
  def sign(bytes: Array[Byte]): Try[Array[Byte]]
}
object PrivateKey {
  import Implicits._
  import jp.co.soramitsu.crypto.ed25519.EdDSAPrivateKey
  import jp.co.soramitsu.crypto.ed25519.spec.EdDSAPrivateKeySpec
  import scala.util.Try

  private case class impl(inner: EdDSAPrivateKey) extends PrivateKey {
    import scala.util.Try
    import java.nio.charset.Charset

    def publicKeyBytes: Array[Byte] = inner.getAbyte
    def publicKeyHexa: String = publicKeyBytes.hexa
    def bytes: Array[Byte] = inner.geta
    def hexa: String = bytes.hexa

    def sign(message: String, charset: Charset): Try[Array[Byte]] = sign(message.getBytes(charset))
    def sign(message: String): Try[Array[Byte]] = sign(message.getBytes)
    def sign(bytes: Array[Byte]): Try[Array[Byte]] = Try {
      val hash = Crypto.digest256.digest(bytes)
      Crypto.engine512.initSign(inner)
      Crypto.engine512.signOneShot(hash)
    }
  }

  /**
    * Create a [SHA3EdDSAPrivateKey] from a [EdDSAPrivateKey].
    * @param seed is the private key
    */
  def apply(privateKey: EdDSAPrivateKey): PrivateKey =
    impl(privateKey)

  /**
    * Create a [SHA3EdDSAPrivateKey] from a [String].
    * @param seed is the private key
    */
  def apply(seed: String): Try[PrivateKey] =
    seed.bytes.flatMap(apply)

  /**
    * Create a [SHA3EdDSAPrivateKey] from a byte array.
    * @param seed the private key
    */
  def apply(seed: Array[Byte]): Try[PrivateKey] =
    Try {
      import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
      impl(new EdDSAPrivateKey(new EdDSAPrivateKeySpec(seed, Ed25519Sha3.spec))) }
}
