package net.cimadai.iroha

import java.security.KeyPair
import java.util.UUID
import iroha.protocol.Query.Payload.Query
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.BeforeAndAfterAll
import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import iroha.protocol.{CommandService_v1, CommandService_v1Client, QueryService_v1, QueryService_v1Client, ToriiResponse, Transaction, TxList, TxStatus, TxStatusRequest}
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import Implicits._
import akka.testkit.TestKit
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success}
import scala.collection.immutable

class IrohaSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with Eventually with ScalaFutures {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  implicit lazy val system = ActorSystem("IrohaSpec")
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
        import iroha.protocol.Command.Command.CreateDomain
        import iroha.protocol
        val command = CreateDomain(protocol.CreateDomain(irohaDomain, irohaRole))

        val tx = Payload.createFromCommand(Command.of(command), irohaAdminAccount).transaction
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
        import iroha.protocol.Command.Command.CreateAsset
        import iroha.protocol
        val command = CreateAsset(protocol.CreateAsset(irohaIrohaAsset1, irohaDomain, 2))

        val tx = Payload.createFromCommand(Command.of(command), irohaAdminAccount).transaction

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
        import iroha.protocol.Command.Command.CreateAccount
        import iroha.protocol
        val command = CreateAccount(protocol.CreateAccount(irohaAccount1, irohaDomain, irohaAccount1Keypair.getPublic.hashHex))

        val tx = Payload.createFromCommand(Command.of(command), irohaAdminAccount).transaction

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
        import iroha.protocol.Command.Command.CreateAccount
        import iroha.protocol
        val command = CreateAccount(protocol.CreateAccount(irohaAccount2, irohaDomain, irohaAccount2Keypair.getPublic.hashHex))

        val tx = Payload.createFromCommand(Command.of(command), irohaAdminAccount).transaction

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
        import iroha.protocol.Command.Command.AddAssetQuantity
        import iroha.protocol
        val command = AddAssetQuantity(protocol.AddAssetQuantity(Asset(irohaIrohaAsset1, irohaDomain).toIrohaString, "100.24"))

        val tx = Payload.createFromCommand(Command.of(command), irohaAdminAccount).transaction

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
        import iroha.protocol.Command.Command.TransferAsset
        import iroha.protocol
        val command = TransferAsset(protocol.TransferAsset(irohaAdminAccount.toIrohaString, Account(irohaAccount1, irohaDomain).toIrohaString, Asset(irohaIrohaAsset1, irohaDomain).toIrohaString, "Transfer test", "100.24"))

        val tx = Payload.createFromCommand(Command.of(command), irohaAdminAccount).transaction

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
        import iroha.protocol.Command.Command.AppendRole
        import iroha.protocol
        val command = AppendRole(protocol.AppendRole(Account(irohaAccount2, irohaDomain).toIrohaString, "money_creator"))

        val tx = Payload.createFromCommand(Command.of(command), irohaAdminAccount).transaction

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
        import iroha.protocol.Command.Command.{TransferAsset, AddAssetQuantity, CreateAccount}
        import iroha.protocol

        val addAssetQtyCommand = AddAssetQuantity(
          protocol.AddAssetQuantity(Asset(irohaIrohaAsset1, irohaDomain).toIrohaString, "100123.01")
        )

        val createAccountCommand = CreateAccount(
          protocol.CreateAccount(irohaAccount3, irohaDomain, irohaAccount3Keypair.getPublic.hashHex)
        )

        val transferCommand = TransferAsset(
          protocol.TransferAsset(Account(irohaAccount2, irohaDomain).toIrohaString, Account(irohaAccount3, irohaDomain).toIrohaString, Asset(irohaIrohaAsset1, irohaDomain).toIrohaString, "Transfer test 2", "100123.01")
        )

        val transferBackCommand = TransferAsset(
          protocol.TransferAsset(Account(irohaAccount3, irohaDomain).toIrohaString, Account(irohaAccount1, irohaDomain).toIrohaString, Asset(irohaIrohaAsset1, irohaDomain).toIrohaString, "Transfer test 3", "100123.01")
        )

        val batch = Batch.createTxOrderedBatch(Seq(
          Payload.createFromCommand(Command.of(addAssetQtyCommand), Account(irohaAccount2, irohaDomain)).transaction,
          Payload.createFromCommand(Command.of(createAccountCommand), irohaAdminAccount).transaction,
          Payload.createFromCommand(Command.of(transferCommand), Account(irohaAccount2, irohaDomain)).transaction,
          Payload.createFromCommand(Command.of(transferBackCommand), Account(irohaAccount3, irohaDomain)).transaction
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

      "query admin account" in {
        import iroha.protocol.GetAccountAssets
        val payload = Payload.createEmptyQuery(irohaAdminAccount).update(_.getAccountAssets.set(GetAccountAssets(irohaAdminAccount.toIrohaString)))

        queryClient.find(payload.toQuery.sign(irohaAdminKeypair))
          .map { r =>
            println(r)
            r
          }
          .collect {
            case QueryResponse(QueryResponse.AccountAssetsResponse(response)) =>
              println(response)
              assert(true)
          }
      }

      "query admin account transactions" in {
        import iroha.protocol.GetAccountTransactions
        val payload = Payload.createEmptyQuery(irohaAdminAccount).update(_.getAccountTransactions.set(GetAccountTransactions(irohaAdminAccount.toIrohaString)))

        queryClient.find(payload.toQuery.sign(irohaAdminKeypair))
          .map { r =>
            println(r)
            r
          }
          .collect {
            case QueryResponse(QueryResponse.TransactionsPageResponse(response)) =>
              println(response)
              assert(true)
          }
      }
    }
  }
}

