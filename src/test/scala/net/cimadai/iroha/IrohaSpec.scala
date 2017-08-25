package net.cimadai.iroha

import iroha.network.proto.loader.LoaderGrpc
import iroha.protocol.block.Transaction
import iroha.protocol.endpoint.{CommandServiceGrpc, QueryServiceGrpc}
import net.cimadai.iroha.Iroha._
import io.grpc.ManagedChannelBuilder
import iroha.protocol.primitive.Permissions
import iroha.protocol.responses.ErrorResponse
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
      assert(Iroha.verify(keyPair, Iroha.sign(keyPair, message), message), true)
      assert(Iroha.verify(keyPair2, Iroha.sign(keyPair, message), message), true)
    }

    it("tx and query run right") {
      if (!isSkipTxTest) {
        def sendTransaction(tx: Transaction): Unit = {
          println(tx)
          println(commandGrpc.torii(tx))
        }

        val domain = IrohaDomainName("test")
        val adminName = IrohaAccountName("admin")
        val user1Name = IrohaAccountName("user1")
        val user2Name = IrohaAccountName("user2")
        val assetName = IrohaAssetName("coin2")
        val adminId = IrohaAccountId(adminName, domain)
        val user1Id = IrohaAccountId(user1Name, domain)
        val user2Id = IrohaAccountId(user2Name, domain)
        val assetId = IrohaAssetId(assetName, domain)

        val adminKeyPair = Iroha.createNewKeyPair()
        val user1keyPair = Iroha.createKeyPairFromHex(
          "302f020100300806032b65640a01010420bc87a15afa46cb524668603a2bcc296548c59c57da093ae7961af0633784a75b",
          "302d300806032b65640a0101032100413728d8d6a682e8f711638b08e322a70f8032e352743787b91fc9bc7831f04e"
        )
        val user2keyPair = Iroha.createKeyPairFromHex(
          "302f020100300806032b65640a01010420520af16ee317954bc9b968441f43032bf443c7fca4bfc76da9276593988e5932",
          "302d300806032b65640a0101032100e253357eae26b3bef4d100b91f50cba0386bd46356aaf98303cd77a8734fa122"
        )

        sendTransaction(Iroha.CommandService.createAccount(adminId, adminKeyPair, user1keyPair.publicKey, user1Name, domain))

        sendTransaction(Iroha.CommandService.createAccount(adminId, adminKeyPair, user2keyPair.publicKey, user2Name, domain))

        val precision = 3 // 小数点以下の桁数
        sendTransaction(Iroha.CommandService.createAsset(adminId, adminKeyPair, assetName, domain, precision))

        sendTransaction(Iroha.CommandService.addAssetQuantity(adminId, adminKeyPair, user1Id, assetId, IrohaAmount(123, 456)))

        sendTransaction(Iroha.CommandService.addAssetQuantity(adminId, adminKeyPair, user2Id, assetId, IrohaAmount(111, 111)))

        val permissions = Permissions(
          issueAssets = true,
          createAssets = true,
          createAccounts = true,
          createDomains = true,
          readAllAccounts = true,
          addSignatory = true,
          removeSignatory = true,
          setPermissions = true,
          setQuorum = true,
          canTransfer = true
        )
        sendTransaction(Iroha.CommandService.setAccountPermissions(adminId, adminKeyPair, user1Id, permissions))

        // creatorとuserは同じじゃないとダメ
        // masterkeyは既存と新が同じだとダメ
        val user1keyPair2 = Iroha.createNewKeyPair()
        sendTransaction(Iroha.CommandService.addSignatory(user1Id, user1keyPair, user1Id, user1keyPair2.publicKey))

        // addSignatoryに入れているものじゃないとダメ
        sendTransaction(Iroha.CommandService.assignMasterKey(user1Id, user1keyPair, user1Id, user1keyPair2.publicKey))

        sendTransaction(Iroha.CommandService.removeSignatory(user1Id, user1keyPair, user1Id, user1keyPair.publicKey))

        sendTransaction(Iroha.CommandService.setAccountPermissions(adminId, adminKeyPair, user2Id, permissions))

        // creatorとuserは同じじゃないとダメ
        val user2keyPair2 = Iroha.createNewKeyPair()
        sendTransaction(Iroha.CommandService.addSignatory(user2Id, user2keyPair, user2Id, user2keyPair2.publicKey))

        sendTransaction(Iroha.CommandService.assignMasterKey(user2Id, user2keyPair, user2Id, user2keyPair2.publicKey))

        sendTransaction(Iroha.CommandService.removeSignatory(user2Id, user2keyPair, user2Id, user2keyPair.publicKey))

        sendTransaction(Iroha.CommandService.transferAsset(user1Id, user1keyPair, user1Id, user2Id, assetId, IrohaAmount(10, 10)))

        // wait for consensus completion
        Thread.sleep(10000) // TODO: FIXME. Please fix more smart way to wait for consensus completion.

        //////////////////////////////////
        val queryRes0 = queryGrpc.find(Iroha.QueryService.getAccount(adminId, adminKeyPair, user1Id))
        assert(queryRes0.response.isAccountResponse)
        assert(queryRes0.response.accountResponse.isDefined)
        assert(queryRes0.response.accountResponse.get.account.isDefined)
        assert(queryRes0.response.accountResponse.get.account.get.accountId == user1Id.toString)
        assert(queryRes0.response.accountResponse.get.account.get.permissions.isDefined)
        assert(queryRes0.response.accountResponse.get.account.get.permissions.get.issueAssets == permissions.issueAssets)
        assert(queryRes0.response.accountResponse.get.account.get.permissions.get.createAssets == permissions.createAssets)
        assert(queryRes0.response.accountResponse.get.account.get.permissions.get.createAccounts == permissions.createAccounts)
        assert(queryRes0.response.accountResponse.get.account.get.permissions.get.createDomains == permissions.createDomains)
        assert(queryRes0.response.accountResponse.get.account.get.permissions.get.readAllAccounts == permissions.readAllAccounts)
        assert(queryRes0.response.accountResponse.get.account.get.permissions.get.addSignatory == permissions.addSignatory)
        assert(queryRes0.response.accountResponse.get.account.get.permissions.get.removeSignatory == permissions.removeSignatory)
        assert(queryRes0.response.accountResponse.get.account.get.permissions.get.setPermissions == permissions.setPermissions)
        assert(queryRes0.response.accountResponse.get.account.get.permissions.get.setQuorum == permissions.setQuorum)
        assert(queryRes0.response.accountResponse.get.account.get.permissions.get.canTransfer == permissions.canTransfer)
        assert(queryRes0.response.accountResponse.get.account.get.quorum == 1)

        val queryRes1 = queryGrpc.find(Iroha.QueryService.getAccountAssets(user1Id, user1keyPair, user1Id, assetId))
        assert(queryRes1.response.isAccountAssetsResponse)
        assert(queryRes1.response.accountAssetsResponse.isDefined)
        assert(queryRes1.response.accountAssetsResponse.get.accountAsset.isDefined)
        assert(queryRes1.response.accountAssetsResponse.get.accountAsset.get.accountId == user1Id.toString)
        assert(queryRes1.response.accountAssetsResponse.get.accountAsset.get.assetId == assetId.toString)
        assert(queryRes1.response.accountAssetsResponse.get.accountAsset.get.balance == 113446)

        val queryRes2 = queryGrpc.find(Iroha.QueryService.getAccountAssetTransactions(user1Id, user1keyPair, user1Id, assetId))
        assert(queryRes2.response.isErrorResponse)
        assert(queryRes2.response.errorResponse.isDefined)
        assert(queryRes2.response.errorResponse.get.reason == ErrorResponse.Reason.NOT_SUPPORTED)

        val queryRes3 = queryGrpc.find(Iroha.QueryService.getAccountTransactions(user1Id, user1keyPair, user1Id))
        assert(queryRes3.response.isTransactionsResponse)
        assert(queryRes3.response.transactionsResponse.isDefined)
        assert(queryRes3.response.transactionsResponse.get.transactions.length == 4)

        val queryRes4 = queryGrpc.find(Iroha.QueryService.getSignatories(user1Id, user1keyPair, user1Id))
        assert(queryRes4.response.isSignatoriesResponse)
        assert(queryRes4.response.signatoriesResponse.isDefined)
        assert(queryRes4.response.signatoriesResponse.get.keys.length == 1)

        val queryRes5 = queryGrpc.find(Iroha.QueryService.getAccount(user2Id, user2keyPair, user2Id))
        assert(queryRes5.response.isAccountResponse)
        assert(queryRes5.response.accountResponse.isDefined)
        assert(queryRes5.response.accountResponse.get.account.isDefined)
        assert(queryRes5.response.accountResponse.get.account.get.accountId == user2Id.toString)
        assert(queryRes5.response.accountResponse.get.account.get.permissions.isDefined)
        assert(queryRes5.response.accountResponse.get.account.get.permissions.get.issueAssets == permissions.issueAssets)
        assert(queryRes5.response.accountResponse.get.account.get.permissions.get.createAssets == permissions.createAssets)
        assert(queryRes5.response.accountResponse.get.account.get.permissions.get.createAccounts == permissions.createAccounts)
        assert(queryRes5.response.accountResponse.get.account.get.permissions.get.createDomains == permissions.createDomains)
        assert(queryRes5.response.accountResponse.get.account.get.permissions.get.readAllAccounts == permissions.readAllAccounts)
        assert(queryRes5.response.accountResponse.get.account.get.permissions.get.addSignatory == permissions.addSignatory)
        assert(queryRes5.response.accountResponse.get.account.get.permissions.get.removeSignatory == permissions.removeSignatory)
        assert(queryRes5.response.accountResponse.get.account.get.permissions.get.setPermissions == permissions.setPermissions)
        assert(queryRes5.response.accountResponse.get.account.get.permissions.get.setQuorum == permissions.setQuorum)
        assert(queryRes5.response.accountResponse.get.account.get.permissions.get.canTransfer == permissions.canTransfer)
        assert(queryRes5.response.accountResponse.get.account.get.quorum == 1)

        val queryRes6 = queryGrpc.find(Iroha.QueryService.getAccountAssets(user2Id, user2keyPair, user2Id, assetId))
        assert(queryRes6.response.isAccountAssetsResponse)
        assert(queryRes6.response.accountAssetsResponse.isDefined)
        assert(queryRes6.response.accountAssetsResponse.get.accountAsset.isDefined)
        assert(queryRes6.response.accountAssetsResponse.get.accountAsset.get.accountId == user2Id.toString)
        assert(queryRes6.response.accountAssetsResponse.get.accountAsset.get.assetId == assetId.toString)
        assert(queryRes6.response.accountAssetsResponse.get.accountAsset.get.balance == 121121)

        val queryRes7 = queryGrpc.find(Iroha.QueryService.getAccountAssetTransactions(user2Id, user2keyPair, user2Id, assetId))
        assert(queryRes7.response.isErrorResponse)
        assert(queryRes7.response.errorResponse.isDefined)
        assert(queryRes7.response.errorResponse.get.reason == ErrorResponse.Reason.NOT_SUPPORTED)

        val queryRes8 = queryGrpc.find(Iroha.QueryService.getAccountTransactions(user2Id, user2keyPair, user2Id))
        assert(queryRes8.response.isTransactionsResponse)
        assert(queryRes8.response.transactionsResponse.isDefined)
        assert(queryRes8.response.transactionsResponse.get.transactions.length == 3)

        val queryRes9 = queryGrpc.find(Iroha.QueryService.getSignatories(user2Id, user2keyPair, user2Id))
        assert(queryRes9.response.isSignatoriesResponse)
        assert(queryRes9.response.signatoriesResponse.isDefined)
        assert(queryRes9.response.signatoriesResponse.get.keys.length == 1)
      }
    }
  }
}
