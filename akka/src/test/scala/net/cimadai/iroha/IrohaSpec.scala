package net.cimadai.iroha

import java.security.KeyPair

import iroha.protocol.Query.Payload.Query
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import iroha.protocol.{CommandService_v1, CommandService_v1Client, QueryService_v1, QueryService_v1Client, Transaction, TxList, TxStatusRequest}
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class IrohaSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with Eventually with ScalaFutures {
  implicit val sys = ActorSystem("IrohaSpec")
  implicit val mat = ActorMaterializer()
  implicit val ec = sys.dispatcher

  // Take details how to connect to the service from the config.
  private val clientSettings = GrpcClientSettings.fromConfig("iroha")
  // Create a client-side stub for the service
  private val commandClient: CommandService_v1 = CommandService_v1Client(clientSettings)
  private val queryClient: QueryService_v1 = QueryService_v1Client(clientSettings)
  private val crypto = new Ed25519Sha3()


  "IrohaSpec" when {
    "GetAccount" should {
      "query admin account" in {
        import iroha.protocol.Query.Payload.Query.GetAccount
        import iroha.protocol.QueryPayloadMeta
        import iroha.protocol.Query
        import iroha.protocol.Query.Payload
        val createdTime = System.currentTimeMillis()
        val payloadMeta = QueryPayloadMeta(
          createdTime = createdTime,
          creatorAccountId = "admin@test",
          queryCounter = 1
        )
        val pk = Utils.parseHexKeypair(
          "313a07e6384776ed95447710d15e59148473ccfc052a681317a72a69f2a49910",
          "f101537e319568c765b2cc89698325604991dca57b9716b58016b253506cab70"
        )
        val p = Payload(
          Some(payloadMeta),
          GetAccount(iroha.protocol.GetAccount("admin@test"))
        )

        val q = Query(Some(p))
        val sig = Utils.sign(q, pk)

        queryClient.find(q.withSignature(sig)).map { r =>
          println(r)
          assert(r.response.isAccountResponse)
        }
      }
    }
    "AddAsset" should {
      "add asset to admin account" in {
        import iroha.protocol.Command
        import iroha.protocol.Command.Command.AddAssetQuantity
        import iroha.protocol.Transaction.Payload
        import iroha.protocol.Transaction.Payload.ReducedPayload
        val createdTime = System.currentTimeMillis()
        val command = Command(
          AddAssetQuantity(iroha.protocol.AddAssetQuantity("coin#test", "100.0"))
        )
        val pk = Utils.parseHexKeypair(
          "313a07e6384776ed95447710d15e59148473ccfc052a681317a72a69f2a49910",
          "f101537e319568c765b2cc89698325604991dca57b9716b58016b253506cab70"
        )
        val tx = Transaction(Some(
          Payload(
            Some(ReducedPayload(Seq(command), "admin@test", createdTime, 1))
          )
        ))
//        val pload = Utils.createTxOrderedBatch(Seq(tx), pk)
//        commandClient.listTorii(TxList(pload)).flatMap { _ =>
        commandClient.torii(tx.withSignatures(Seq(Utils.sign(tx.getPayload, pk)))).flatMap { _ =>
          Thread.sleep(2000)
          commandClient.statusStream(TxStatusRequest(Utils.toHex(Utils.hash(tx.getPayload))))
            .runWith(Sink.foreach(println))
              .map {
                _ => assert(true)
              }
        }
      }
    }
  }
}

