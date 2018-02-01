package net.cimadai.iroha

import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import iroha.protocol.block.Transaction
import iroha.protocol.endpoint.{CommandServiceGrpc, QueryServiceGrpc}
import iroha.protocol.primitive.{Amount, uint256}
import iroha.protocol.queries.Query
import iroha.protocol.responses.QueryResponse
import net.cimadai.iroha.Iroha._
import net.i2p.crypto.eddsa.Utils
import org.bouncycastle.jcajce.provider.digest.SHA3
import org.scalatest.FunSpec

class IrohaSpec extends FunSpec {
  private val grpcHost: String = sys.env.getOrElse("GRPC_HOST", "127.0.0.1")
  private val grpcPort: Int = sys.env.getOrElse("GRPC_PORT", "50051").toInt
  private val nodeNum: Int = sys.env.getOrElse("NODE_NUM", "4").toInt
  private val isSkipTxTest: Boolean = sys.env.getOrElse("SKIP_TX_TEST", "true").toBoolean

  private val channel: ManagedChannel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort).usePlaintext(true).build()
  private val commandGrpc: CommandServiceGrpc.CommandServiceBlockingStub = CommandServiceGrpc.blockingStub(channel)
  private val queryGrpc: QueryServiceGrpc.QueryServiceBlockingClient = QueryServiceGrpc.blockingStub(channel)

  describe("IrohaSpec") {
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

    def sendTransaction(tx: Transaction): Unit = {
      println("== Tx ==")
      println(tx)
      println("========")
      commandGrpc.torii(tx)
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

    it("tx and query run right") {
      if (!isSkipTxTest) {
        val domain = IrohaDomainName("test.domain")
        val adminName = IrohaAccountName("admin")
        val user1Name = IrohaAccountName("testuser1")
        val user2Name = IrohaAccountName("testuser2")
        val assetName = IrohaAssetName("coina")
        val adminId = IrohaAccountId(adminName, domain)
        val user1Id = IrohaAccountId(user1Name, domain)
        val user2Id = IrohaAccountId(user2Name, domain)
        val assetId = IrohaAssetId(assetName, domain)

        val privateHex = "1d7e0a32ee0affeb4d22acd73c2c6fb6bd58e266c8c2ce4fa0ffe3dd6a253ffb"
        val publicHex  = "407e57f50ca48969b08ba948171bb2435e035d82cec417e18e4a38f5fb113f83"
        val adminKeyPair = Iroha.createKeyPairFromBytes(new SHA3.Digest512().digest(Utils.hexToBytes(privateHex)))
        assert(adminKeyPair.toHex.publicKey == publicHex)

        val user1keyPair = Iroha.createNewKeyPair()
        val user2keyPair = Iroha.createNewKeyPair()
        println(user1keyPair.toHex.publicKey)

        val precision = IrohaAssetPrecision(3) // 小数点以下の桁数

        // 2
        sendTransaction(Iroha.CommandService.createAccount(adminId, adminKeyPair, user1keyPair.publicKey, user1Name, domain))

        // 3
        sendTransaction(Iroha.CommandService.createAccount(adminId, adminKeyPair, user2keyPair.publicKey, user2Name, domain))

        // 4
        sendTransaction(Iroha.CommandService.appendRole(adminId, adminKeyPair, user1Id, "money_creator"))

        // 5
        sendTransaction(Iroha.CommandService.appendRole(adminId, adminKeyPair, user2Id, "money_creator"))

        // 6
        sendTransaction(Iroha.CommandService.createAsset(adminId, adminKeyPair, assetName, domain, precision))

        // 7
        // creatorとuserは同じじゃないとダメ
        sendTransaction(Iroha.CommandService.addAssetQuantity(adminId, adminKeyPair, adminId, assetId, IrohaAmount(Some(uint256(0L, 0L, 0L, Long.MaxValue)), precision)))

        // 8
        // creatorとuserは同じじゃないとダメ
        sendTransaction(Iroha.CommandService.addAssetQuantity(user1Id, user1keyPair, user1Id, assetId, IrohaAmount(Some(uint256(0L, 0L, 0L, 123456L)), precision)))

        // 9
        // creatorとuserは同じじゃないとダメ
        sendTransaction(Iroha.CommandService.addAssetQuantity(user2Id, user2keyPair, user2Id, assetId, IrohaAmount(Some(uint256(0L, 0L, 0L, 111111L)), precision)))

        // creatorとuserは同じじゃないとダメ
        val user1keyPair2 = Iroha.createNewKeyPair()
        // 10
        sendTransaction(Iroha.CommandService.addSignatory(user1Id, user1keyPair, user1Id, user1keyPair2.publicKey))

        // 11
        sendTransaction(Iroha.CommandService.removeSignatory(user1Id, user1keyPair, user1Id, user1keyPair.publicKey))

        // creatorとuserは同じじゃないとダメ
        val user2keyPair2 = Iroha.createNewKeyPair()
        // 12
        sendTransaction(Iroha.CommandService.addSignatory(user2Id, user2keyPair, user2Id, user2keyPair2.publicKey))

        // 13
        sendTransaction(Iroha.CommandService.removeSignatory(user2Id, user2keyPair, user2Id, user2keyPair.publicKey))

        // 14
        sendTransaction(Iroha.CommandService.transferAsset(user1Id, user1keyPair2, user1Id, user2Id, assetId, "purpose", IrohaAmount(Some(uint256(0, 0, 0, 10010L)), precision)))

        // 正しいノードを登録する。
        //        val node0KeyPair = Iroha.createNewKeyPair()
        //        val node1KeyPair = Iroha.createNewKeyPair()
        //        sendTransaction(Iroha.CommandService.addPeer(adminId, adminKeyPair, "10.5.0.10", node0KeyPair.publicKey))
        //        sendTransaction(Iroha.CommandService.addPeer(adminId, adminKeyPair, "10.5.0.11", node1KeyPair.publicKey))

        // wait for consensus completion
        if (!isSkipTxTest) {
          Thread.sleep(10000) // TODO: FIXME. Please fix more smart way to wait for consensus completion.
        }

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
        assert(queryRes3.response.transactionsResponse.get.transactions.length == 4)

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

        // TODO: いまはsender_idでしか検索できないためこのテストは実行しない。
        val queryRes7 = sendQuery(Iroha.QueryService.getAccountAssetTransactions(user2Id, user2keyPair2, user2Id, assetId))
        assert(queryRes7.response.isTransactionsResponse)
        assert(queryRes7.response.transactionsResponse.isDefined)
        assert(queryRes7.response.transactionsResponse.get.transactions.length == 1)

        val queryRes8 = sendQuery(Iroha.QueryService.getAccountTransactions(user2Id, user2keyPair2, user2Id))
        assert(queryRes8.response.isTransactionsResponse)
        assert(queryRes8.response.transactionsResponse.isDefined)
        assert(queryRes8.response.transactionsResponse.get.transactions.length == 3)

        val queryRes9 = sendQuery(Iroha.QueryService.getSignatories(user2Id, user2keyPair2, user2Id))
        assert(queryRes9.response.isSignatoriesResponse)
        assert(queryRes9.response.signatoriesResponse.isDefined)
        assert(queryRes9.response.signatoriesResponse.get.keys.length == 1)
      }
    }
  }
}
