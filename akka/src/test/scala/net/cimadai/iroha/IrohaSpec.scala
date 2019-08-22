package net.cimadai.iroha

import java.security.KeyPair
import java.util.UUID

import iroha.protocol.Query.Payload.Query
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{Assertion, AsyncWordSpec, BeforeAndAfterAll, Matchers}
import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import iroha.protocol.{CommandService_v1, CommandService_v1Client, QueryService_v1, QueryService_v1Client, ToriiResponse, Transaction, TxList, TxStatus, TxStatusRequest}
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import Implicits._
import akka.testkit.TestKit

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success}
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
  private def randomString(length: Int = 64) = Random.alphanumeric.take(length).mkString("")

  private def successfulResponses(hashHex: String) = immutable.Seq(
    ToriiResponse(TxStatus.ENOUGH_SIGNATURES_COLLECTED, hashHex),
    ToriiResponse(TxStatus.STATEFUL_VALIDATION_SUCCESS, hashHex),
    ToriiResponse(TxStatus.COMMITTED, hashHex)
  )

  "IrohaSpec" when {
    val irohaDomain = s"uuid${UUID.randomUUID().toString}.test"
    val irohaRole = "user"
    val irohaAdminKeypair = Utils.parseHexKeypair(
      "313a07e6384776ed95447710d15e59148473ccfc052a681317a72a69f2a49910",
      "f101537e319568c765b2cc89698325604991dca57b9716b58016b253506cab70"
    )
    val irohaAdminAccount = Account("admin@test")
    val irohaIrohaAsset1 = randomString(6).toLowerCase
    val irohaAccount1 = s"r${randomString(5).toLowerCase}m"
    val irohaAccount1Keypair = crypto.generateKeypair()
    val irohaAccount2 = s"r${randomString(5).toLowerCase}m"
    val irohaAccount2Keypair = crypto.generateKeypair()
    val irohaAccount3 = s"r${randomString(5).toLowerCase}m"
    val irohaAccount3Keypair = crypto.generateKeypair()

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
          commandClient
            .statusStream(TxStatusRequest(tx.hashHex))
            .takeWhile(Status.isNotFinalStatus, inclusive = true)
            .completionTimeout(30.seconds)
            .runWith(Sink.seq)
            .map { statuses =>
              statuses should ===(successfulResponses(tx.hashHex))
            }
        }
      }
      "create new asset" in {
        import iroha.protocol.Command
        import iroha.protocol.CreateAsset
        val command = Command().update(_.createAsset.set(
          CreateAsset(irohaIrohaAsset1, irohaDomain, 2)
        ))

        val tx = Payload.createFromCommand(command, irohaAdminAccount).transaction

        commandClient.torii(tx.sign(irohaAdminKeypair)).flatMap { _ =>
          commandClient
            .statusStream(TxStatusRequest(tx.hashHex))
            .takeWhile(Status.isNotFinalStatus, inclusive = true)
            .completionTimeout(30.seconds)
            .runWith(Sink.seq)
            .map { statuses =>
              statuses should ===(successfulResponses(tx.hashHex))
            }
        }
      }
      "create new first account" in {
        import iroha.protocol.Command
        import iroha.protocol.CreateAccount
        val command = Command().update(_.createAccount.set(
          CreateAccount(irohaAccount1, irohaDomain, irohaAccount1Keypair.getPublic.hashHex)
        ))

        val tx = Payload.createFromCommand(command, irohaAdminAccount).transaction

        commandClient.torii(tx.sign(irohaAdminKeypair)).flatMap { _ =>
          commandClient
            .statusStream(TxStatusRequest(tx.hashHex))
            .takeWhile(Status.isNotFinalStatus, inclusive = true)
            .completionTimeout(30.seconds)
            .runWith(Sink.seq)
            .map { statuses =>
              statuses should ===(successfulResponses(tx.hashHex))
            }
        }
      }
      "create new second account" in {
        import iroha.protocol.Command
        import iroha.protocol.CreateAccount
        val command = Command().update(_.createAccount.set(
          CreateAccount(irohaAccount2, irohaDomain, irohaAccount2Keypair.getPublic.hashHex)
        ))

        val tx = Payload.createFromCommand(command, irohaAdminAccount).transaction

        commandClient.torii(tx.sign(irohaAdminKeypair)).flatMap { _ =>
          commandClient
            .statusStream(TxStatusRequest(tx.hashHex))
            .takeWhile(Status.isNotFinalStatus, inclusive = true)
            .completionTimeout(30.seconds)
            .runWith(Sink.seq)
            .map { statuses =>
              statuses should ===(successfulResponses(tx.hashHex))
            }
        }
      }
      "add asset amount of 100.24 to account" in {
        import iroha.protocol.Command
        import iroha.protocol.AddAssetQuantity
        val command = Command().update(_.addAssetQuantity.set(
          AddAssetQuantity(Asset(irohaIrohaAsset1, irohaDomain).toIrohaString, "100.24")
        ))

        val tx = Payload.createFromCommand(command, irohaAdminAccount).transaction

        commandClient.torii(tx.sign(irohaAdminKeypair)).flatMap { _ =>
          commandClient
            .statusStream(TxStatusRequest(tx.hashHex))
            .takeWhile(Status.isNotFinalStatus, inclusive = true)
            .completionTimeout(30.seconds)
            .runWith(Sink.seq)
            .map { statuses =>
              statuses should ===(successfulResponses(tx.hashHex))
            }
        }
      }
      "transfer amount of 100.24 from admin account" in {
        import iroha.protocol.Command
        import iroha.protocol.TransferAsset
        val command = Command().update(_.transferAsset.set(
          TransferAsset(irohaAdminAccount.toIrohaString, Account(irohaAccount1, irohaDomain).toIrohaString, Asset(irohaIrohaAsset1, irohaDomain).toIrohaString, "Transfer test", "100.24")
        ))

        val tx = Payload.createFromCommand(command, irohaAdminAccount).transaction

        commandClient.torii(tx.sign(irohaAdminKeypair)).flatMap { _ =>
          commandClient
            .statusStream(TxStatusRequest(tx.hashHex))
            .takeWhile(Status.isNotFinalStatus, inclusive = true)
            .completionTimeout(30.seconds)
            .runWith(Sink.seq)
            .map { statuses =>
              statuses should ===(successfulResponses(tx.hashHex))
            }
        }
      }
      "append money_creator role for second account" in {
        import iroha.protocol.Command
        import iroha.protocol.AppendRole
        val command = Command().update(_.appendRole.set(
          AppendRole(Account(irohaAccount2, irohaDomain).toIrohaString, "money_creator")
        ))

        val tx = Payload.createFromCommand(command, irohaAdminAccount).transaction

        commandClient.torii(tx.sign(irohaAdminKeypair)).flatMap { _ =>
          commandClient
            .statusStream(TxStatusRequest(tx.hashHex))
            .takeWhile(Status.isNotFinalStatus, inclusive = true)
            .completionTimeout(30.seconds)
            .runWith(Sink.seq)
            .map { statuses =>
              statuses should ===(successfulResponses(tx.hashHex))
            }
        }
      }
      "add amount of 100123.01 to admin account and transfer to new account and transfer back to first account" in {
        import iroha.protocol.Command
        import iroha.protocol.{TransferAsset, AddAssetQuantity, CreateAccount}

        val addAssetQtyCommand = Command().update(_.addAssetQuantity.set(
          AddAssetQuantity(Asset(irohaIrohaAsset1, irohaDomain).toIrohaString, "100123.01")
        ))

        val createAccountCommand = Command().update(_.createAccount.set(
          CreateAccount(irohaAccount3, irohaDomain, irohaAccount3Keypair.getPublic.hashHex)
        ))

        val transferCommand = Command().update(_.transferAsset.set(
          TransferAsset(Account(irohaAccount2, irohaDomain).toIrohaString, Account(irohaAccount3, irohaDomain).toIrohaString, Asset(irohaIrohaAsset1, irohaDomain).toIrohaString, "Transfer test 2", "100123.01")
        ))

        val transferBackCommand = Command().update(_.transferAsset.set(
          TransferAsset(Account(irohaAccount3, irohaDomain).toIrohaString, Account(irohaAccount1, irohaDomain).toIrohaString, Asset(irohaIrohaAsset1, irohaDomain).toIrohaString, "Transfer test 3", "100123.01")
        ))

        val batch = Batch.createTxOrderedBatch(Seq(
          Payload.createFromCommand(addAssetQtyCommand, Account(irohaAccount2, irohaDomain)).transaction,
          Payload.createFromCommand(createAccountCommand, irohaAdminAccount).transaction,
          Payload.createFromCommand(transferCommand, Account(irohaAccount2, irohaDomain)).transaction,
          Payload.createFromCommand(transferBackCommand, Account(irohaAccount3, irohaDomain)).transaction
        ))

        val signedBatch = Seq(
          batch(0).sign(irohaAccount2Keypair),
          batch(1).sign(irohaAdminKeypair),
          batch(2).sign(irohaAccount2Keypair),
          batch(3).sign(irohaAccount3Keypair)
        )

        for {
          _ <- commandClient.listTorii(TxList(signedBatch))
          statuses1 <- commandClient
            .statusStream(TxStatusRequest(signedBatch(0).hashHex))
            .takeWhile(Status.isNotFinalStatus, inclusive = true)
            .completionTimeout(30.seconds)
            .runWith(Sink.seq)

          statuses2 <- commandClient
            .statusStream(TxStatusRequest(signedBatch(1).hashHex))
            .takeWhile(Status.isNotFinalStatus, inclusive = true)
            .completionTimeout(30.seconds)
            .runWith(Sink.seq)

          statuses3 <- commandClient
            .statusStream(TxStatusRequest(signedBatch(2).hashHex))
            .takeWhile(Status.isNotFinalStatus, inclusive = true)
            .completionTimeout(30.seconds)
            .runWith(Sink.seq)

          statuses4 <- commandClient
            .statusStream(TxStatusRequest(signedBatch(3).hashHex))
            .takeWhile(Status.isNotFinalStatus, inclusive = true)
            .completionTimeout(30.seconds)
            .runWith(Sink.seq)
        } yield {
          assert(statuses1.contains(ToriiResponse(TxStatus.COMMITTED, signedBatch(0).hashHex)))
          assert(statuses2.contains(ToriiResponse(TxStatus.COMMITTED, signedBatch(1).hashHex)))
          assert(statuses3.contains(ToriiResponse(TxStatus.COMMITTED, signedBatch(2).hashHex)))
          assert(statuses4.contains(ToriiResponse(TxStatus.COMMITTED, signedBatch(3).hashHex)))
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

