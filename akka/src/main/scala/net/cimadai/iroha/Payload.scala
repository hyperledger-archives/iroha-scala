package net.cimadai.iroha

import iroha.protocol
import iroha.protocol.Transaction.Payload.ReducedPayload

object Payload {
  def createFromSeq(commands: Seq[protocol.Command], creator: Account, createdTime: Long = System.currentTimeMillis(), quorum: Int = 1): protocol.Transaction.Payload = {
    protocol.Transaction.Payload(
      Some(ReducedPayload(commands, creator.toIrohaString, createdTime, quorum))
    )
  }

  def createFromCommand(command: protocol.Command, creator: Account, createdTime: Long = System.currentTimeMillis(), quorum: Int = 1): protocol.Transaction.Payload = {
    createFromSeq(Seq(command), creator, createdTime, quorum)
  }
}
