package net.cimadai.iroha

import iroha.protocol
import java.security.KeyPair

import iroha.protocol.Transaction.Payload.BatchMeta
import iroha.protocol.Transaction.Payload.BatchMeta.BatchType

object Batch {
  /**
    * Create Ordered Batch of transactions created by single user from iterable
    */
  def createTxOrderedBatch(list: Seq[protocol.Transaction], keyPair: KeyPair): Seq[protocol.Transaction] = createBatch(list, BatchType.ORDERED)

  /**
    * Create Atomic Batch of transactions created by single user from iterable
    */
  def createTxAtomicBatch(list: Seq[protocol.Transaction]): Seq[protocol.Transaction] = createBatch(list, BatchType.ATOMIC)

  def getBatchHashesHex(list: Seq[protocol.Transaction]): Seq[String] = list.map(Utils.reducedHash).map(Utils.toHex)

  private def createBatch(list: Seq[protocol.Transaction], batchType: BatchType) = {
    val batchHashes = getBatchHashesHex(list)
    list.map(tx => tx.copy(
      tx.payload.map(_.withBatch(BatchMeta(batchType, batchHashes)))
    ))
  }
}
