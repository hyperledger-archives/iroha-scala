package net.cimadai.iroha

import iroha.protocol.TxStatusRequest

object Status {
  /**
    * Helper method to create {@link TxStatusRequest} from byte array
    *
    * @param hash tx hash
    * @return { @link TxStatusRequest}
    */
  def createTxStatusRequest(hash: Array[Byte]): TxStatusRequest = TxStatusRequest(Utils.toHex(hash))
}
