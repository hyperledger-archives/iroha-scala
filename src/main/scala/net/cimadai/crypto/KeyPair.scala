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

sealed trait KeyPair {
  val inner: java.security.KeyPair
  val publicKey : PublicKey
  val privateKey: PrivateKey
}
object  KeyPair {
  import jp.co.soramitsu.crypto.ed25519.{EdDSAPrivateKey, EdDSAPublicKey}
  import scala.util.Try
  import Implicits._

  private case class impl(inner: java.security.KeyPair, publicKey: PublicKey, privateKey: PrivateKey) extends KeyPair

  /** Create a [SHA3EdDSAKeyPair] from [SHA3EdDSAPublicKey] and [SHA3EdDSAPrivateKey]. */
  def apply(publicKey: PublicKey, privateKey: PrivateKey): KeyPair =
    new impl(new java.security.KeyPair(publicKey.inner, privateKey.inner), publicKey, privateKey)

  /**
    * Create a [SHA3EdDSAKeyPair] from a [String].
    * @param privateKeyHexa is the private key
    */
  def apply(privateKeyHexa: String): Try[KeyPair] =
    privateKeyHexa.bytes
      .flatMap(bytes => apply(bytes))

  /**
    * Create a [SHA3EdDSAKeyPair] from a byte array.
    * @param privateKeyBytes the private key
    */
  def apply(privateKeyBytes: Array[Byte]): Try[KeyPair] = {
    assume(privateKeyBytes.length == 32)
    assume(privateKeyBytes.hexa.forall(c => (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')))
    for {
      privateKey <- PrivateKey(privateKeyBytes)
      publicKey  <- PublicKey(privateKey.publicKeyBytes)
    } yield {
      apply(publicKey, privateKey)
    }
  }

  /**
    * Create a [SHA3EdDSAKeyPair] from a byte arrays containing the public key and the private key.
    * @param publicKeyBytes the public key
    * @param privateKeyBytes the private key
    */
  def apply(publicKeyBytes: Array[Byte], privateKeyBytes: Array[Byte]): Try[KeyPair] = Try {
    import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
    // Notice that we expose public key and private key in this order, which is the order
    // adopted by java.security.KeyPair, whilst Ed25519Sha3.keyPairFromBytes swaps the order.
    val kp: java.security.KeyPair = Ed25519Sha3.keyPairFromBytes(privateKeyBytes, publicKeyBytes)
    new impl(
      kp,
      PublicKey(kp.getPublic.asInstanceOf[EdDSAPublicKey]),
      PrivateKey(kp.getPrivate.asInstanceOf[EdDSAPrivateKey]))
  }

  /**
    * Create a [SHA3EdDSAKeyPair] from strings containing the public key and the private key.
    * @param publicKeyHexa the public key
    * @param privateKeyHexa the private key
    */
  def apply(publicKeyHexa: String, privateKeyHexa: String): Try[KeyPair] =
    for {
      publicKeyBytes  <- publicKeyHexa.bytes
      privateKeyBytes <- privateKeyHexa.bytes
      keypair <- apply(publicKeyBytes, privateKeyBytes)
    } yield {
      keypair
    }

  /**
    * Create a random [SHA3EdDSAKeyPair].
    */
  def random: Try[KeyPair] =
    randomKeyPair
      .map { kp =>
        apply(
          PublicKey(kp.getPublic.asInstanceOf[EdDSAPublicKey]),
          PrivateKey(kp.getPrivate.asInstanceOf[EdDSAPrivateKey])) }

  private def randomKeyPair: Try[java.security.KeyPair] =
    Try { Crypto.crypto.generateKeypair }

}
