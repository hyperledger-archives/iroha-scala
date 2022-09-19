package net.cimadai.iroha

import java.security.{KeyPair, PrivateKey, PublicKey}

import iroha.protocol
import iroha.protocol.Transaction.Payload.ReducedPayload
import javax.xml.bind.DatatypeConverter
import javax.xml.bind.DatatypeConverter.parseHexBinary
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3.{privateKeyFromBytes, publicKeyFromBytes}
import org.spongycastle.jcajce.provider.digest.SHA3

/**
  * Scala version of https://github.com/hyperledger/iroha-java/blob/master/client/src/main/java/jp/co/soramitsu/iroha/java/Utils.java
  */
object Utils {
  private val IROHA_FRIENDLY_QUOTE = "\\\""
  private val IROHA_FRIENDLY_NEW_LINE = "\\n"

  /**
    * Parse a keypair from hex strings
    *
    * @param hexPublicKey  64-byte (128-symbol) hexstring, public key
    * @param hexPrivateKey 64-byte (128-symbol) hexstring, private key
    * @return Ed25519-Sha3 KeyPair instance
    */
  def parseHexKeypair(hexPublicKey: String, hexPrivateKey: String) = new KeyPair(parseHexPublicKey(hexPublicKey), parseHexPrivateKey(hexPrivateKey))

  /**
    * Parse a public key from hexstring
    *
    * @param hexPublicKey 64-byte (128-symbol) hexstring, public key
    * @return Ed25519-Sha3 PublicKey instance
    */
  def parseHexPublicKey(hexPublicKey: String): PublicKey = publicKeyFromBytes(parseHexBinary(hexPublicKey))

  /**
    * Parse a private key from hexstring
    *
    * @param hexPrivateKey 64-byte (128-symbol) hexstring, private key
    * @return Ed25519-Sha3 PrivateKey instance
    */
  def parseHexPrivateKey(hexPrivateKey: String): PrivateKey = privateKeyFromBytes(parseHexBinary(hexPrivateKey))

  /**
    * Calculate SHA3-256 reduced hash of {@link iroha.protocol.Transaction}
    *
    * @param tx Protobuf transaction
    * @return 32 bytes hash
    */
  def reducedHash(tx: protocol.Transaction): Array[Byte] = reducedHash(tx.getPayload.getReducedPayload)

  /**
    * Calculate SHA3-256 hash of {@link iroha.protocol.Transaction.Payload.ReducedPayload}
    *
    * @param rp Protobuf of ReducedPayload
    * @return 32 bytes hash
    */
  def reducedHash(rp: ReducedPayload): Array[Byte] = {
    val digest = new SHA3.Digest256()
    digest.digest(rp.toByteArray)
  }

  /**
    * Calculate SHA3-256 hash of {@link iroha.protocol.Transaction.Payload}
    *
    * @param p Protobuf Payload
    * @return 32 bytes hash
    */
  def hash(p: protocol.Transaction.Payload): Array[Byte] = {
    val digest = new SHA3.Digest256()
    digest.digest(p.toByteArray)
  }

  def hash(p: Option[protocol.Transaction.Payload]): Array[Byte] = {
    p.map(hash) getOrElse Array.empty[Byte]
  }

  /**
    * Calculate SHA3-256 hash of {@link iroha.protocol.Transaction}
    *
    * @param tx Protobuf Transaction
    * @return 32 bytes hash
    */
  def hash(tx: protocol.Transaction): Array[Byte] = {
    val digest = new SHA3.Digest256()
    digest.digest(tx.getPayload.toByteArray)
  }

  /**
    * Calculate SHA3-256 hash of {@link Block_v1}
    *
    * @param b BlockV1
    * @return 32 bytes hash
    */
  def hash(b: protocol.Block_v1): Array[Byte] = {
    val digest = new SHA3.Digest256()
    digest.digest(b.getPayload.toByteArray)
  }

  /**
    * Calculate SHA3-256 hash of {@link Block}
    *
    * @param b Protobuf Block
    * @return 32 bytes hash
    */
  def hash(b: protocol.Block): Array[Byte] = b.blockVersion match {
    case protocol.Block.BlockVersion.BlockV1(_) =>
      hash(b.getBlockV1)
    case _ =>
      throw new IllegalArgumentException(String.format("Block has undefined version: %s", b.blockVersion.toString))
  }

  /**
    * Calculate SHA3-256 hash of {@link Queries.Query}
    *
    * @param q Protobuf Query
    * @return 32 bytes hash
    */
  def hash(q: protocol.Query): Array[Byte] = {
    val digest = new SHA3.Digest256()
    digest.digest(q.getPayload.toByteArray)
  }

  /**
    * Helper method to create {@link TxList} from Seq
    *
    * @param list list of protobuf transactions
    * @return { @link TxList}
    */
  def createTxList(list: Seq[protocol.Transaction]): protocol.TxList = protocol.TxList(list)

  /**
    * Convert bytes to hexstring
    */
  def toHex(b: Array[Byte]): String = DatatypeConverter.printHexBinary(b).toLowerCase

  /**
    * Get transaction hash hexstring
    *
    * @param transaction
    * @return lowercase hexstring
    */
  def toHexHash(transaction: protocol.Transaction): String = toHex(hash(transaction))

  /**
    * Get query hash hexstring
    *
    * @param query
    * @return lowercase hexstring
    */
  def toHexHash(query: protocol.Query): String = toHex(hash(query))

  /**
    * Get block_v1 hash hexstring
    *
    * @param block_v1
    * @return lowercase hexstring
    */
  def toHexHash(block_v1: protocol.Block_v1): String = toHex(hash(block_v1))

  /**
    * Get block hash hexstring
    *
    * @param block
    * @return lowercase hexstring
    */
  def toHexHash(block: protocol.Block): String = toHex(hash(block))

  /**
    * Escapes symbols reserved in JSON so it can be used in Iroha
    *
    * @param str input string to escape
    * @return escaped string
    */
  def irohaEscape(str: String): String = str.replace("\"", IROHA_FRIENDLY_QUOTE).replace("\n", IROHA_FRIENDLY_NEW_LINE)

  /**
    * Reverse to irohaEscape(), unescape symbols reserved in JSON so it can be used in Iroha
    *
    * @param str input escaped string
    * @return unescaped string
    */
  def irohaUnEscape(str: String): String = str.replace(IROHA_FRIENDLY_QUOTE, "\"").replace(IROHA_FRIENDLY_NEW_LINE, "\n")
}
