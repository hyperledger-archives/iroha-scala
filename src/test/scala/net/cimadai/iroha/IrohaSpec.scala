package net.cimadai.iroha

import io.grpc.ManagedChannelBuilder
import iroha.network.proto.loader.LoaderGrpc
import iroha.protocol.block.Transaction
import iroha.protocol.endpoint.{CommandServiceGrpc, QueryServiceGrpc}
import iroha.protocol.primitive.{Amount, RolePermission, uint256}
import iroha.protocol.queries.Query
import iroha.protocol.responses.QueryResponse
import net.cimadai.iroha.Iroha._
import org.bouncycastle.jcajce.provider.digest.SHA3
import org.scalatest.FunSpec

class IrohaSpec extends FunSpec {
  private val grpcHost = sys.env.getOrElse("GRPC_HOST", "127.0.0.1")
  private val grpcPort = sys.env.getOrElse("GRPC_PORT", "50051").toInt
  private val isSkipTxTest = sys.env.getOrElse("SKIP_TX_TEST", "true").toBoolean
  private lazy val channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort).usePlaintext(true).build()
  private lazy val commandGrpc = CommandServiceGrpc.blockingStub(channel)
  private lazy val queryGrpc = QueryServiceGrpc.blockingStub(channel)
  private lazy val loaderGrpc = LoaderGrpc.blockingStub(channel)

  describe("IrohaSpec") {
    it("sign and verify run right") {
      val keyPair = Iroha.createNewKeyPair()
      val keyPair2 = keyPair.toHex.toKey
      val message = "This is test string".getBytes()

      val sha3_256 = new SHA3.Digest256()
      val hash = sha3_256.digest(message)
      assert(Iroha.verify(keyPair, Iroha.sign(keyPair, hash), hash), true)
      assert(Iroha.verify(keyPair2, Iroha.sign(keyPair, hash), hash), true)
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

        val domain = IrohaDomainName("test")
        val adminName = IrohaAccountName("admin")
        val user1Name = IrohaAccountName("test-user-1-abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxz")
        val user2Name = IrohaAccountName("test-user-2")
        val assetName = IrohaAssetName("coina")
        val adminId = IrohaAccountId(adminName, domain)
        val user1Id = IrohaAccountId(user1Name, domain)
        val user2Id = IrohaAccountId(user2Name, domain)
        val assetId = IrohaAssetId(assetName, domain)

        val adminKeyPair = Iroha.createKeyPairFromHex(
          "881e179b8d6900f892b43c7010c77e47731711735dae462ba9a8d12a9d956e58ab5bce5597a9914b0a743fccd3c56e521c1b59d73391d8ae33cc3667c5068539"
        )

        val roleName = "test_role"
        // TODO: なぜか 04 08 1a で送ってるのが、サーバー側で04 1a 08でhexstring化されるのでハッシュが狂う
        //sendTransaction(Iroha.CommandService.createRole(adminId, adminKeyPair, roleName, rolePermissions))

        // ugly hack
        sendTransaction(Iroha.CommandService.createRole(adminId, adminKeyPair, roleName, Seq(RolePermission.can_add_signatory)))
        sendTransaction(Iroha.CommandService.createRole(adminId, adminKeyPair, roleName, Seq(RolePermission.can_remove_signatory)))
        sendTransaction(Iroha.CommandService.createRole(adminId, adminKeyPair, roleName, Seq(RolePermission.can_grant_set_quorum)))

        val user1keyPair = Iroha.createNewKeyPair()
        val user2keyPair = Iroha.createNewKeyPair()

        sendTransaction(Iroha.CommandService.createAccount(adminId, adminKeyPair, user1keyPair.publicKey, user1Name, domain))

        sendTransaction(Iroha.CommandService.createAccount(adminId, adminKeyPair, user2keyPair.publicKey, user2Name, domain))

        sendTransaction(Iroha.CommandService.appendRole(adminId, adminKeyPair, user1Id, "money_creator"))

        sendTransaction(Iroha.CommandService.appendRole(adminId, adminKeyPair, user2Id, "money_creator"))

        val precision = IrohaAssetPrecision(3) // 小数点以下の桁数
        sendTransaction(Iroha.CommandService.createAsset(adminId, adminKeyPair, assetName, domain, precision))

        // creatorとuserは同じじゃないとダメ
        sendTransaction(Iroha.CommandService.addAssetQuantity(adminId, adminKeyPair, adminId, assetId, IrohaAmount(Some(uint256(0L, 0L, 0L, Long.MaxValue)), precision)))

        // creatorとuserは同じじゃないとダメ
        sendTransaction(Iroha.CommandService.addAssetQuantity(user1Id, user1keyPair, user1Id, assetId, IrohaAmount(Some(uint256(0L, 0L, 0L, 123456L)), precision)))

        // creatorとuserは同じじゃないとダメ
        sendTransaction(Iroha.CommandService.addAssetQuantity(user2Id, user2keyPair, user2Id, assetId, IrohaAmount(Some(uint256(0L, 0L, 0L, 111111L)), precision)))

        // creatorとuserは同じじゃないとダメ
        val user1keyPair2 = Iroha.createNewKeyPair()
        sendTransaction(Iroha.CommandService.addSignatory(user1Id, user1keyPair, user1Id, user1keyPair2.publicKey))

        sendTransaction(Iroha.CommandService.removeSignatory(user1Id, user1keyPair, user1Id, user1keyPair.publicKey))

        // creatorとuserは同じじゃないとダメ
        val user2keyPair2 = Iroha.createNewKeyPair()
        sendTransaction(Iroha.CommandService.addSignatory(user2Id, user2keyPair, user2Id, user2keyPair2.publicKey))

        sendTransaction(Iroha.CommandService.removeSignatory(user2Id, user2keyPair, user2Id, user2keyPair.publicKey))

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
//        val queryRes7 = sendQuery(Iroha.QueryService.getAccountAssetTransactions(user2Id, user2keyPair2, user2Id, assetId))
//        assert(queryRes7.response.isTransactionsResponse)
//        assert(queryRes7.response.transactionsResponse.isDefined)
//        assert(queryRes7.response.transactionsResponse.get.transactions.length == 1)

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
