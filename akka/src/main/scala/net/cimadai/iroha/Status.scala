package net.cimadai.iroha

import iroha.protocol.{ToriiResponse, TxStatus, TxStatusRequest}

object Status {
  /**
    * Helper method to create {@link TxStatusRequest} from byte array
    *
    * @param hash tx hash
    * @return { @link TxStatusRequest}
    */
  def createTxStatusRequest(hash: Array[Byte]): TxStatusRequest = TxStatusRequest(Utils.toHex(hash))

  val finalStatuses = Seq(TxStatus.COMMITTED, TxStatus.MST_EXPIRED, TxStatus.REJECTED, TxStatus.STATEFUL_VALIDATION_FAILED, TxStatus.STATELESS_VALIDATION_FAILED)
  def isNotFinalStatus(txStatus: TxStatus): Boolean = !finalStatuses.contains(txStatus)
  def isNotFinalStatus(toriiResponse: ToriiResponse): Boolean = isNotFinalStatus(toriiResponse.txStatus)
}
