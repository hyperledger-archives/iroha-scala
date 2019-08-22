package net.cimadai.iroha

import java.security.{Key, KeyPair}

import iroha.protocol

object Implicits {
  implicit class SignTransaction(transaction: protocol.Transaction) {
    def sign(keyPair: KeyPair): protocol.Transaction = {
      transaction.withSignatures(transaction.signatures :+ Signature.sign(transaction.getPayload, keyPair))
    }
  }

  implicit class SignTransactionSeq(transactions: Seq[protocol.Transaction]) {
    def signSeq(keyPair: KeyPair): Seq[protocol.Transaction] = {
      transactions.map(_.sign(keyPair))
    }
  }

  implicit class PayloadTransaction(payload: protocol.Transaction.Payload) {
    def transaction: protocol.Transaction = protocol.Transaction(Some(payload))
  }

  implicit class HashTransaction(transaction: protocol.Transaction) {
    def hashBytes: Array[Byte] = Utils.hash(transaction)
    def hashHex: String = Utils.toHex(hashBytes)
  }

  implicit class HexJavaKey(key: Key) {
    def hashHex: String = Utils.toHex(key.getEncoded)
  }
}
