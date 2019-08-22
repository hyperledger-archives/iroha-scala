package net.cimadai.iroha

import java.util.concurrent.atomic.AtomicLong

import iroha.protocol
import iroha.protocol.Transaction.Payload.ReducedPayload

object Payload {
  private val queryCounter = new AtomicLong(1)

  def createFromSeq(commands: Seq[protocol.Command], creator: Account, createdTime: Long = System.currentTimeMillis(), quorum: Int = 1): protocol.Transaction.Payload = {
    protocol.Transaction.Payload(
      Some(ReducedPayload(commands, creator.toIrohaString, createdTime, quorum))
    )
  }

  def createFromCommand(command: protocol.Command, creator: Account, createdTime: Long = System.currentTimeMillis(), quorum: Int = 1): protocol.Transaction.Payload = {
    createFromSeq(Seq(command), creator, createdTime, quorum)
  }

  def createEmptyQuery(creator: Account, createdTime: Long = System.currentTimeMillis()): protocol.Query.Payload = {
    protocol.Query.Payload(
      meta = Some(protocol.QueryPayloadMeta(
        createdTime = createdTime,
        creatorAccountId = creator.toIrohaString,
        queryCounter = queryCounter.getAndIncrement()
      ))
    )
  }
}
