package net.cimadai.iroha

import java.security.KeyPair
import java.util.UUID

import iroha.protocol.Query.Payload.Query
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{Assertion, AsyncWordSpec, BeforeAndAfterAll, Matchers, WordSpecLike}
import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import iroha.protocol.{CommandService_v1, CommandService_v1Client, QueryService_v1, QueryService_v1Client, ToriiResponse, Transaction, TxList, TxStatus, TxStatusRequest}
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import Implicits._
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{TestKit, TestProbe}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.collection.immutable

class IrohaSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with Eventually with ScalaFutures {
  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  implicit lazy val system = ActorSystem("IrohaSpec")
  implicit lazy val mat = ActorMaterializer()
  implicit lazy val ec = system.dispatcher

  // Take details how to connect to the service from the config.
  private lazy val clientSettings = GrpcClientSettings.fromConfig("iroha")
  // Create a client-side stub for the service
  private lazy val commandClient: CommandService_v1 = CommandService_v1Client(clientSettings)
  private lazy val queryClient: QueryService_v1 = QueryService_v1Client(clientSettings)
  private val crypto = new Ed25519Sha3()

  private def successfulResponses(hashHex: String) = immutable.Seq(
    ToriiResponse(TxStatus.ENOUGH_SIGNATURES_COLLECTED, hashHex),
    ToriiResponse(TxStatus.STATEFUL_VALIDATION_SUCCESS, hashHex),
    ToriiResponse(TxStatus.COMMITTED, hashHex)
  )

  "IrohaSpec" when {
    val irohaDomain = s"uuid${UUID.randomUUID().toString}.test"
    val irohaRole = "admin"
    val irohaAdminKeypair = Utils.parseHexKeypair(
      "313a07e6384776ed95447710d15e59148473ccfc052a681317a72a69f2a49910",
      "f101537e319568c765b2cc89698325604991dca57b9716b58016b253506cab70"
    )
    val irohaAdminAccount = Account("admin@test")

    "Setup new domain, assets and add amounts" should {
      "create new domain" in {
        import iroha.protocol.Command
        import iroha.protocol.CreateDomain
        val command = Command().update(_.createDomain.set(
          CreateDomain(irohaDomain, irohaRole)
        ))

        val tx = Payload.createFromCommand(command, irohaAdminAccount).transaction
        //        val pload = Utils.createTxOrderedBatch(Seq(tx), pk)
        //        commandClient.listTorii(TxList(pload)).flatMap { _ =>

        commandClient.torii(tx.sign(irohaAdminKeypair)).flatMap { _ =>
          val probe = commandClient
            .statusStream(TxStatusRequest(tx.hashHex))
            .completionTimeout(30.seconds)
            .runWith(TestSink.probe[ToriiResponse])

          probe.request(3).expectNextN(3) should ===(successfulResponses(tx.hashHex))
          probe.expectComplete()
          succeed
        }
      }
    }
//    "GetAccount" should {
//      "query admin account" in {
//        import iroha.protocol.Query.Payload.Query.GetAccount
//        import iroha.protocol.QueryPayloadMeta
//        import iroha.protocol.Query
//        import iroha.protocol.Query.Payload
//        val createdTime = System.currentTimeMillis()
//        val payloadMeta = QueryPayloadMeta(
//          createdTime = createdTime,
//          creatorAccountId = "admin@test",
//          queryCounter = 1
//        )
//        val pk = Utils.parseHexKeypair(
//          "313a07e6384776ed95447710d15e59148473ccfc052a681317a72a69f2a49910",
//          "f101537e319568c765b2cc89698325604991dca57b9716b58016b253506cab70"
//        )
//        val p = Payload(
//          Some(payloadMeta),
//          GetAccount(iroha.protocol.GetAccount("admin@test"))
//        )
//
//        val q = Query(Some(p))
//        val sig = Utils.sign(q, pk)
//
//        queryClient.find(q.withSignature(sig)).map { r =>
//          println(r)
//          assert(r.response.isAccountResponse)
//        }
//      }
//    }
//    "AddAsset" should {
//      "add asset to admin account" in {
//        import iroha.protocol.Command
//        import iroha.protocol.AddAssetQuantity
//        val command = Command().update(_.addAssetQuantity.set(
//          AddAssetQuantity("coin#test", "100.0")
//        ))
//
//        val pk = Utils.parseHexKeypair(
//          "313a07e6384776ed95447710d15e59148473ccfc052a681317a72a69f2a49910",
//          "f101537e319568c765b2cc89698325604991dca57b9716b58016b253506cab70"
//        )
//        val payload = Payload.createFromCommand(command, Account("admin@test"))
//        val tx = Transaction.createFromPayload(payload, pk)
////        val pload = Utils.createTxOrderedBatch(Seq(tx), pk)
////        commandClient.listTorii(TxList(pload)).flatMap { _ =>
//        commandClient.torii(tx).flatMap { _ =>
//          Thread.sleep(2000)
//          commandClient.statusStream(TxStatusRequest(Utils.toHex(Utils.hash(payload))))
//            .runWith(Sink.foreach(println))
//              .map {
//                _ => assert(true)
//              }
//        }
//      }
//    }
  }
}

