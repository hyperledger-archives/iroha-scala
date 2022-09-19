package net.cimadai.iroha

import java.security.KeyPair

import iroha.protocol
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import net.cimadai.iroha.Utils.{hash, toHex}

object Signature {
  def sign(t: protocol.Transaction.Payload, kp: KeyPair): protocol.Signature = {
    val rawSignature = new Ed25519Sha3().rawSign(hash(t), kp)
    protocol.Signature(toHex(kp.getPublic.getEncoded), toHex(rawSignature))
  }

  def sign(t: protocol.Query, kp: KeyPair): protocol.Signature = {
    val rawSignature = new Ed25519Sha3().rawSign(hash(t), kp)
    protocol.Signature(toHex(kp.getPublic.getEncoded), toHex(rawSignature))
  }
}
