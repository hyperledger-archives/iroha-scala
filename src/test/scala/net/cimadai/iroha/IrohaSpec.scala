package net.cimadai.iroha

import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import iroha.protocol.block.Transaction
import iroha.protocol.endpoint._
import iroha.protocol.primitive.{Amount, uint256}
import iroha.protocol.queries.Query
import iroha.protocol.responses.QueryResponse
import net.cimadai.iroha.Iroha._
import net.i2p.crypto.eddsa.Utils
import org.bouncycastle.jcajce.provider.digest.SHA3
import org.scalatest.FunSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Random

class IrohaSpec extends FunSpec {
  private val grpcHost: String = sys.env.getOrElse("GRPC_HOST", "127.0.0.1")
  private val grpcPort: Int = sys.env.getOrElse("GRPC_PORT", "50051").toInt
  private val nodeNum: Int = sys.env.getOrElse("NODE_NUM", "4").toInt
  private val isSkipTxTest: Boolean = sys.env.getOrElse("SKIP_TX_TEST", "true").toBoolean

  private val channel: ManagedChannel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort).usePlaintext(true).build()
  private val commandGrpc: CommandServiceGrpc.CommandServiceBlockingStub = CommandServiceGrpc.blockingStub(channel)
  private val queryGrpc: QueryServiceGrpc.QueryServiceBlockingClient = QueryServiceGrpc.blockingStub(channel)

  describe("IrohaSpec") {
    it("domain validator") {
      assert(IrohaValidator.DomainParser("abc").isRight, true)
      assert(IrohaValidator.DomainParser("abc.xx").isRight, true)
      assert(IrohaValidator.DomainParser("abc.xx.yy").isRight, true)
      assert(IrohaValidator.DomainParser("abc.xx.yy.zz").isRight, true)
    }

    it("sign and verify run right with create new key pair") {
      val sha3_256 = new SHA3.Digest256()
      val message = "This is test string".getBytes()
      val messageHash = sha3_256.digest(message)

      val keyPair = Iroha.createNewKeyPair()
      val keyPair2 = keyPair.toHex.toKey

      // sign by keyPair and verify by keyPair
      assert(Iroha.verify(keyPair, Iroha.sign(keyPair, messageHash), messageHash), true)

      // sign by keyPair and verify by keyPair2
      assert(Iroha.verify(keyPair2, Iroha.sign(keyPair, messageHash), messageHash), true)
    }

    it("sign and verify run right with load from private key") {
      val sha3_256 = new SHA3.Digest256()
      val message = "This is test string".getBytes()
      val messageHash = sha3_256.digest(message)

      val priHex = "a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a"
      val pubHex = "b89e03b83bdcfde56d56b9fdb99a12c7f33c5032e787ba4af4a297b7782d24ed"

      val sha3_512 = new SHA3.Digest512()
      val priHash = sha3_512.digest(Utils.hexToBytes(priHex))
      val keyPair = Iroha.createKeyPairFromBytes(priHash)
      val keyPair2 = keyPair.toHex.toKey
      assert(keyPair.toHex.publicKey == pubHex)

      // sign by keyPair and verify by keyPair
      assert(Iroha.verify(keyPair, Iroha.sign(keyPair, messageHash), messageHash), true)

      // sign by keyPair and verify by keyPair2
      assert(Iroha.verify(keyPair2, Iroha.sign(keyPair, messageHash), messageHash), true)
    }

    def sendTransaction(tx: Transaction): Future[Boolean] = {
      println("== Tx ==")
      println(tx)
      println("========")
      commandGrpc.torii(tx)
      checkTransactionCommit(tx)
    }

    def askTransactionStatus(txStatusRequest: TxStatusRequest): ToriiResponse = {
      commandGrpc.status(txStatusRequest)
    }


    def sendQuery(query: Query): QueryResponse = {
      println("== Qry ==")
      println(query)
      println("---------")
      val resp = queryGrpc.find(query)
      println(resp)
      println("=========")
      resp
    }

    def isCommitted(tx: Transaction): Boolean = {
      val response = askTransactionStatus(Iroha.CommandService.txStatusRequest(tx))
      response.txStatus == TxStatus.COMMITTED
    }

    def awaitUntilTransactionCommitted(tx: Transaction, counter: Int = 0): Boolean = {
      if (counter >= 20) {
        false
      } else if (isCommitted(tx)) {
        true
      } else {
        Thread.sleep(1000)
        awaitUntilTransactionCommitted(tx, counter + 1)
      }
    }

    def checkTransactionCommit(tx: Transaction): Future[Boolean] = Future {
      awaitUntilTransactionCommitted(tx)
    }

    def assertTxFutures(futures: Iterable[Future[Boolean]]): Unit = {
      futures.foreach(f => assert(Await.result(f, Duration.Inf), true))
    }

    def createRandomName(length: Int): String = {
      "z" + Random.alphanumeric.take(length - 1).mkString.toLowerCase
    }

    it("ask transaction status") {
      if (!isSkipTxTest) {
        val domain = IrohaDomainName("test.domain")
        val adminName = IrohaAccountName("admin")
        val adminId = IrohaAccountId(adminName, domain)
        val user1Name = IrohaAccountName(createRandomName(10))
        val privateHex = "1d7e0a32ee0affeb4d22acd73c2c6fb6bd58e266c8c2ce4fa0ffe3dd6a253ffb"
        val publicHex = "407e57f50ca48969b08ba948171bb2435e035d82cec417e18e4a38f5fb113f83"
        val adminKeyPair = Iroha.createKeyPairFromBytes(new SHA3.Digest512().digest(Utils.hexToBytes(privateHex)))
        assert(adminKeyPair.toHex.publicKey == publicHex)

        val user1keyPair = Iroha.createNewKeyPair()
        val createAccount = Iroha.CommandService.createAccount(user1keyPair.publicKey, user1Name, domain)
        val transaction = Iroha.CommandService.createTransaction(adminId, adminKeyPair, Seq(createAccount))
        val futureCommitted = sendTransaction(transaction)
        assert(Await.result(futureCommitted, Duration.Inf), true)
      }
    }

    it("tx and query run right") {
      if (!isSkipTxTest) {
        val domain = IrohaDomainName("test.domain")
        val adminName = IrohaAccountName("admin")
        val user1Name = IrohaAccountName(createRandomName(10))
        val user2Name = IrohaAccountName(createRandomName(10))
        val assetName = IrohaAssetName(createRandomName(8))
        val adminId = IrohaAccountId(adminName, domain)
        val user1Id = IrohaAccountId(user1Name, domain)
        val user2Id = IrohaAccountId(user2Name, domain)
        val assetId = IrohaAssetId(assetName, domain)

        val privateHex = "1d7e0a32ee0affeb4d22acd73c2c6fb6bd58e266c8c2ce4fa0ffe3dd6a253ffb"
        val publicHex = "407e57f50ca48969b08ba948171bb2435e035d82cec417e18e4a38f5fb113f83"
        val adminKeyPair = Iroha.createKeyPairFromBytes(new SHA3.Digest512().digest(Utils.hexToBytes(privateHex)))
        assert(adminKeyPair.toHex.publicKey == publicHex)

        val user1keyPair = Iroha.createNewKeyPair()
        val user2keyPair = Iroha.createNewKeyPair()
        println(user1keyPair.toHex.publicKey)

        val precision = IrohaAssetPrecision(3) // Number of digits after the decimal point

        val commands1 = Seq(
          Iroha.CommandService.createAccount(user1keyPair.publicKey, user1Name, domain),
          Iroha.CommandService.createAccount(user2keyPair.publicKey, user2Name, domain),
          Iroha.CommandService.appendRole(user1Id, "money_creator"),
          Iroha.CommandService.appendRole(user2Id, "money_creator"),
          Iroha.CommandService.createAsset(assetName, domain, precision),
          Iroha.CommandService.addAssetQuantity(adminId, assetId, IrohaAmount(Some(uint256(0L, 0L, 0L, Long.MaxValue)), precision))
        )

        val f01 = sendTransaction(Iroha.CommandService.createTransaction(adminId, adminKeyPair, commands1))
        // wait for consensus completion
        assertTxFutures(Iterable(f01))

        val user1keyPair2 = Iroha.createNewKeyPair()
        val commands2 = Seq(
          // Tx creator must equal addAssetQuantity target account.
          Iroha.CommandService.addAssetQuantity(user1Id, assetId, IrohaAmount(Some(uint256(0L, 0L, 0L, 123456L)), precision)),
          Iroha.CommandService.addSignatory(user1Id, user1keyPair2.publicKey),
          Iroha.CommandService.removeSignatory(user1Id, user1keyPair.publicKey)
        )

        val f02 = sendTransaction(Iroha.CommandService.createTransaction(user1Id, user1keyPair, commands2))

        val user2keyPair2 = Iroha.createNewKeyPair()
        val commands3 = Seq(
          // Tx creator must equal addAssetQuantity target account.
          Iroha.CommandService.addAssetQuantity(user2Id, assetId, IrohaAmount(Some(uint256(0L, 0L, 0L, 111111L)), precision)),
          Iroha.CommandService.addSignatory(user2Id, user2keyPair2.publicKey),
          Iroha.CommandService.removeSignatory(user2Id, user2keyPair.publicKey)
        )

        val f03 = sendTransaction(Iroha.CommandService.createTransaction(user2Id, user2keyPair, commands3))

        // wait for consensus completion
        assertTxFutures(Iterable(f02, f03))

        val commands4 = Seq(
          Iroha.CommandService.transferAsset(user1Id, user2Id, assetId, "purpose", IrohaAmount(Some(uint256(0, 0, 0, 10010L)), precision))
        )

        val f04 = sendTransaction(Iroha.CommandService.createTransaction(user1Id, user1keyPair2, commands4))

        // wait for consensus completion
        assertTxFutures(Iterable(f04))

        //////////////////////////////////
        val queryRes0 = sendQuery(Iroha.QueryService.getAccount(adminId, adminKeyPair, user1Id))
        assert(queryRes0.response.isAccountResponse)
        assert(queryRes0.response.accountResponse.isDefined)
        assert(queryRes0.response.accountResponse.get.account.isDefined)
        assert(queryRes0.response.accountResponse.get.account.get.accountId == user1Id.toString)
        assert(queryRes0.response.accountResponse.get.account.get.domainId == user1Id.domain.value)
        assert(queryRes0.response.accountResponse.get.account.get.quorum == 1)

        val queryRes1 = sendQuery(Iroha.QueryService.getAccountAssets(user1Id, user1keyPair2, user1Id, assetId))
        assert(queryRes1.response.isAccountAssetsResponse)
        assert(queryRes1.response.accountAssetsResponse.isDefined)
        assert(queryRes1.response.accountAssetsResponse.get.accountAsset.isDefined)
        assert(queryRes1.response.accountAssetsResponse.get.accountAsset.get.accountId == user1Id.toString)
        assert(queryRes1.response.accountAssetsResponse.get.accountAsset.get.assetId == assetId.toString)
        assert(queryRes1.response.accountAssetsResponse.get.accountAsset.get.balance.isDefined)
        assert(queryRes1.response.accountAssetsResponse.get.accountAsset.get.balance.get == Amount(Some(uint256(0L, 0L, 0L, 113446L)), precision.value))

        val queryRes2 = sendQuery(Iroha.QueryService.getAccountAssetTransactions(user1Id, user1keyPair2, user1Id, assetId))
        assert(queryRes2.response.isTransactionsResponse)
        assert(queryRes2.response.transactionsResponse.isDefined)
        assert(queryRes2.response.transactionsResponse.get.transactions.length == 1)

        val queryRes3 = sendQuery(Iroha.QueryService.getAccountTransactions(user1Id, user1keyPair2, user1Id))
        assert(queryRes3.response.isTransactionsResponse)
        assert(queryRes3.response.transactionsResponse.isDefined)
        assert(queryRes3.response.transactionsResponse.get.transactions.length == 2)

        val queryRes4 = sendQuery(Iroha.QueryService.getSignatories(user1Id, user1keyPair2, user1Id))
        assert(queryRes4.response.isSignatoriesResponse)
        assert(queryRes4.response.signatoriesResponse.isDefined)
        assert(queryRes4.response.signatoriesResponse.get.keys.length == 1)

        val queryRes5 = sendQuery(Iroha.QueryService.getAccount(user2Id, user2keyPair2, user2Id))
        assert(queryRes5.response.isAccountResponse)
        assert(queryRes5.response.accountResponse.isDefined)
        assert(queryRes5.response.accountResponse.get.account.isDefined)
        assert(queryRes5.response.accountResponse.get.account.get.accountId == user2Id.toString)
        assert(queryRes5.response.accountResponse.get.account.get.domainId == user2Id.domain.value)
        assert(queryRes5.response.accountResponse.get.account.get.quorum == 1)

        val queryRes6 = sendQuery(Iroha.QueryService.getAccountAssets(user2Id, user2keyPair2, user2Id, assetId))
        assert(queryRes6.response.isAccountAssetsResponse)
        assert(queryRes6.response.accountAssetsResponse.isDefined)
        assert(queryRes6.response.accountAssetsResponse.get.accountAsset.isDefined)
        assert(queryRes6.response.accountAssetsResponse.get.accountAsset.get.accountId == user2Id.toString)
        assert(queryRes6.response.accountAssetsResponse.get.accountAsset.get.assetId == assetId.toString)
        assert(queryRes6.response.accountAssetsResponse.get.accountAsset.get.balance.isDefined)
        assert(queryRes6.response.accountAssetsResponse.get.accountAsset.get.balance.get == Amount(Some(uint256(0L, 0L, 0L, 121121L)), precision.value))

        val queryRes7 = sendQuery(Iroha.QueryService.getAccountAssetTransactions(user2Id, user2keyPair2, user2Id, assetId))
        assert(queryRes7.response.isTransactionsResponse)
        assert(queryRes7.response.transactionsResponse.isDefined)
        assert(queryRes7.response.transactionsResponse.get.transactions.length == 1)

        val queryRes8 = sendQuery(Iroha.QueryService.getAccountTransactions(user2Id, user2keyPair2, user2Id))
        assert(queryRes8.response.isTransactionsResponse)
        assert(queryRes8.response.transactionsResponse.isDefined)
        assert(queryRes8.response.transactionsResponse.get.transactions.length == 1)

        val queryRes9 = sendQuery(Iroha.QueryService.getSignatories(user2Id, user2keyPair2, user2Id))
        assert(queryRes9.response.isSignatoriesResponse)
        assert(queryRes9.response.signatoriesResponse.isDefined)
        assert(queryRes9.response.signatoriesResponse.get.keys.length == 1)
      }
    }

    it("tx_counter is to unique to each transaction creater.") {
      if (!isSkipTxTest) {
        val domain = IrohaDomainName("test.domain")
        val adminName = IrohaAccountName("admin")
        val user1Name = IrohaAccountName(s"u${Random.alphanumeric.take(9).mkString}")
        val user2Name = IrohaAccountName(s"u${Random.alphanumeric.take(9).mkString}")
        val assetName = IrohaAssetName(s"${Random.alphanumeric.filter(_.isLetterOrDigit).take(4).mkString}")
        val adminId = IrohaAccountId(adminName, domain)
        val user1Id = IrohaAccountId(user1Name, domain)
        val user2Id = IrohaAccountId(user2Name, domain)
        val assetId = IrohaAssetId(assetName, domain)

        val privateHex = "1d7e0a32ee0affeb4d22acd73c2c6fb6bd58e266c8c2ce4fa0ffe3dd6a253ffb"
        val publicHex = "407e57f50ca48969b08ba948171bb2435e035d82cec417e18e4a38f5fb113f83"
        val adminKeyPair = Iroha.createKeyPairFromBytes(new SHA3.Digest512().digest(Utils.hexToBytes(privateHex)))
        assert(adminKeyPair.toHex.publicKey == publicHex)

        val user1keyPair = Iroha.createNewKeyPair()
        val user2keyPair = Iroha.createNewKeyPair()

        val precision = IrohaAssetPrecision(3) // 小数点以下の桁数

        val commands = Iterable(
          Iroha.CommandService.createAccount(user1keyPair.publicKey, user1Name, domain),
          Iroha.CommandService.createAccount(user2keyPair.publicKey, user2Name, domain),
          Iroha.CommandService.appendRole(user1Id, "money_creator"),
          Iroha.CommandService.appendRole(user2Id, "money_creator"),
          Iroha.CommandService.createAsset(assetName, domain, precision)
        )

        val transaction = Iroha.CommandService.createTransaction(adminId, adminKeyPair, commands.toSeq)
        sendTransaction(transaction)

        val txCounter = Iroha.txCounter.getAndIncrement()

        // Use the same txCounter to check if its unique to the creater.
        val transactionForUser1 = Iroha.CommandService.createTransaction(
          user1Id, user1keyPair,
          Seq(Iroha.CommandService.addAssetQuantity(user1Id, assetId, IrohaAmount(Some(uint256(0L, 0L, 0L, 123456L)), precision))),
          txCounter
        )

        val transactionForUser2 = Iroha.CommandService.createTransaction(
          user2Id, user2keyPair,
          Seq(Iroha.CommandService.addAssetQuantity(user2Id, assetId, IrohaAmount(Some(uint256(0L, 0L, 0L, 111111L)), precision))),
          txCounter
        )

        val f1 = sendTransaction(transactionForUser1)
        val f2 = sendTransaction(transactionForUser2)

        val r = for {
          r1 <- f1
          r2 <- f2
        } yield r1 && r2

        assert(Await.result(r, Duration.Inf))
      }
    }

    it("adding and subtracting the same amount result in the no change in balance.") {
      if (!isSkipTxTest) {
        val domain = IrohaDomainName("test.domain")
        val adminId = IrohaAccountId(IrohaAccountName("admin"), domain)
        val privateHex = "1d7e0a32ee0affeb4d22acd73c2c6fb6bd58e266c8c2ce4fa0ffe3dd6a253ffb"
        val adminKeyPair = Iroha.createKeyPairFromBytes(new SHA3.Digest512().digest(Utils.hexToBytes(privateHex)))
        val assetName = IrohaAssetName(s"${Random.alphanumeric.filter(_.isLetterOrDigit).take(4).mkString}")
        val assetId = IrohaAssetId(assetName, domain)
        val precision = IrohaAssetPrecision(3) // 小数点以下の桁数

        val amount = IrohaAmount(Some(uint256(0L, 0L, 111L)), precision)

        val commands = Seq(
          Iroha.CommandService.createAsset(assetName, domain, precision),
          Iroha.CommandService.addAssetQuantity(adminId, assetId, amount),
          Iroha.CommandService.subtractAssetQuantity(adminId, assetId, amount)
        )

        val r = sendTransaction(Iroha.CommandService.createTransaction(adminId, adminKeyPair, commands, Iroha.txCounter.getAndIncrement()))
          .map(
            _ => sendQuery(Iroha.QueryService.getAccountAssets(adminId, adminKeyPair, adminId, assetId))
          )
          .map(qr => (for {
            response <- qr.response.accountAssetsResponse
            asset <- response.accountAsset
            balance <- asset.balance
          } yield balance == Amount(Some(uint256()), balance.precision)
            ).getOrElse(false)
          )

        assert(Await.result(r, Duration.Inf))
      }
    }
  }
}
