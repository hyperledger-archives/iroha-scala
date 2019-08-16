package net.cimadai.iroha


import utest._

object IrohaSpec extends TestSuite with TestHelpers {
  import monix.eval.Task
  import scala.util.{Failure, Success, Try}

  val tests = this {
    val host = "localhost"
    val port = 50051

    val legacy = true
    val adminDomainName = "test"
    val adminAccountName = "admin"
    val adminRoleName = "admin"

    val domainName = adminDomainName //FIXME: should be "example.com". See: https://github.com/frgomes/iroha-scala/issues/5

    val adminPublicKeyHexa  = "43eeb17f0bab10dd51ab70983c25200a1742d31b3b7b54c38c34d7b827b26eed"
    val adminPrivateKeyHexa = "0000000000000000000000000000000000000000000000000000000000000000"

    import io.grpc.{ManagedChannel, ManagedChannelBuilder}
    import iroha.protocol.endpoint.{CommandService_v1Grpc, QueryService_v1Grpc}
    implicit val channel: ManagedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build

    "create a new domain"- ignored {
      import iroha.protocol.endpoint.ToriiResponse
      import net.cimadai.crypto.KeyPair
      import net.cimadai.iroha.Iroha.{Account, CmdStub, Domain, QryStub, Role}

      implicit val cmdStub: CmdStub = CommandService_v1Grpc.stub(channel)
      implicit val qryStub: QryStub = QueryService_v1Grpc.stub(channel)

      val adminKeypair = KeyPair.apply(adminPublicKeyHexa, adminPrivateKeyHexa)
      val adminDomain  = Domain(adminDomainName, legacy)
      val adminAccount = Account(adminAccountName, adminDomain)
      val adminRole    = Role(adminRoleName)

      val domain    = Domain(domainName, legacy)

      val cb = Iroha.CommandBuilder
      val tb = Iroha.TransactionBuilder
      val api = Iroha.CommandService

      val tryTask: Try[Task[ToriiResponse]] =
        for {
          a <- adminAccount; d <- domain; r <- adminRole; kp <- adminKeypair
          cmd <- cb.createDomain(d, r)
          tx <- tb.transaction(a, kp, cmd)
          //TODO: checkTransactionCommit(tx)
        } yield {
          api.send(tx)(cmdStub)
        }

      val result = tryTaskNow(tryTask)
      result match {
        case Success(response) =>
          import iroha.protocol.endpoint.TxStatus
          val status = response.txStatus
          assert(status == TxStatus.COMMITTED)
        case Failure(t) => throw t
      }
    }

    "create a new account"- {
      import iroha.protocol.endpoint.ToriiResponse
      import net.cimadai.crypto.KeyPair
      import net.cimadai.iroha.Iroha.{Account, CmdStub, Domain, QryStub}

      implicit val cmdStub: CmdStub = CommandService_v1Grpc.stub(channel)
      implicit val qryStub: QryStub = QueryService_v1Grpc.stub(channel)

      val keypair = KeyPair.apply(adminPublicKeyHexa, adminPrivateKeyHexa)

      val adminDomain = Domain(adminDomainName, legacy)
      val adminAccount = Account(adminAccountName, adminDomain)

      val domain   = Domain(domainName, legacy)
      val username = createRandomName(10)
      val user     = Account("aaabbbccc", domain)

      val cb = Iroha.CommandBuilder
      val tb = Iroha.TransactionBuilder
      val api = Iroha.CommandService

      val tryTask: Try[Task[ToriiResponse]] =
        for {
          a <- adminAccount; u <- user; d <- domain; kp <- keypair
          cmd <- cb.createAccount(u, d, kp.publicKey)
          tx <- tb.transaction(a, kp, cmd)
          //TODO: checkTransactionCommit(tx)
        } yield {
          api.send(tx)(cmdStub)
        }

      val result = tryTaskNow(tryTask)
      result match {
        case Success(response) =>
          import iroha.protocol.endpoint.TxStatus
          val status = response.txStatus
          assert(status == TxStatus.COMMITTED)
        case Failure(t) => throw t
      }
    }

  }


  import iroha.protocol.endpoint.ToriiResponse
  def tryTaskNow(tryTask: Try[Task[ToriiResponse]]): Try[ToriiResponse] = {
    val result = Try {
      tryTask match {
        case Success(task) =>
        import monix.execution.Scheduler
        import scala.concurrent.Await
        import scala.concurrent.duration._
        import scala.language.postfixOps
          implicit val sc: Scheduler = Scheduler.global
          task.coeval.value match {
            case Left(future)  => Await.result(future, 3 seconds)
            case Right(result) => result
          }
      case Failure(t) => throw t
      }
    }

    result match {
      case Success(response) =>
        import iroha.protocol.endpoint.TxStatus
        if(response.txStatus != TxStatus.COMMITTED) {
          println(response.toProtoString) //FIXME: should use some logging framework
          Thread.sleep(1000)
        }
      case Failure(t) => throw t
    }

    result
  }

}

      /*
      val domain = IrohaDomainName(context.testDomain)
      val user1Name = IrohaAccountName(createRandomName(10))
      val user1Id = IrohaAccountId(user1Name, domain)

      val user1keyPair = Iroha.createNewKeyPair()
      val createAccount = Iroha.CommandService.createAccount(user1keyPair.publicKey, user1Name, domain)
      val transaction = Iroha.CommandService.createTransaction(context.adminAccount.accountId, context.adminAccount.keypair, Seq(createAccount))
      val accountQuery = Iroha.QueryService.getAccount(context.adminAccount.accountId, context.adminAccount.keypair, user1Id)
      println(TestFormatter.command(createAccount))
      for {
          sent <- sendTransaction(transaction)
          committed <- Future(sent).collect({ case true => awaitUntilTransactionCommitted(transaction) })
          query <- Future(committed).collect({ case true => sendQuery(accountQuery) })
          account = Some(query).collect({ case Iroha.QueryResponse(AccountResponse(x)) => x }).flatMap(_.account)
        } yield {
          println(TestFormatter.queryResponse(query))
          assert(sent, true)
          assert(committed, true)
          assert(account.map(_.accountId.split("@").head).contains(user1Name.value))
          assert(account.map(_.domainId).contains(domain.value))
        }
*/


/*
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import iroha.protocol.endpoint._
import iroha.protocol.primitive.RolePermission
import net.cimadai.iroha.Iroha._
import net.cimadai.iroha.Tags.TxTest
import org.bouncycastle.jcajce.provider.digest.SHA3
import org.scalatest.AsyncWordSpec

import scala.concurrent.Future

class IrohaSpec extends AsyncWordSpec {
  import TestHelpers._
  import Iroha.MatchedResponse._

  private implicit val context = IrohaTestContext(
    sys.env.getOrElse("GRPC_HOST", "127.0.0.1"),
    sys.env.getOrElse("GRPC_PORT", "50051").toInt,
    adminPrivateKey = "f101537e319568c765b2cc89698325604991dca57b9716b58016b253506cab70",
    adminPublicKey = "313a07e6384776ed95447710d15e59148473ccfc052a681317a72a69f2a49910",
    sys.env.getOrElse("VERBOSE_TX", "false").toBoolean
  )

  private implicit val channel: ManagedChannel = ManagedChannelBuilder.forAddress(context.grpcHost, context.grpcPort)
    .usePlaintext(true)
    .build()
  private implicit val commandGrpc: CommandService_v1Grpc.CommandService_v1BlockingStub = CommandService_v1Grpc.blockingStub(channel)
  private implicit val queryGrpc: QueryService_v1Grpc.QueryService_v1BlockingStub = QueryService_v1Grpc.blockingStub(channel)

  "Iroha" when {
    "verify" should {
      "sign and verify run right with create new key pair" in {
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
      "sign and verify run right with load from private key" in {
        val sha3_256 = new SHA3.Digest256()
        val message = "This is test string".getBytes()
        val messageHash = sha3_256.digest(message)

        val keyPair = context.adminAccount.keypair
        val keyPair2 = keyPair.toHex.toKey

        // sign by keyPair and verify by keyPair
        assert(Iroha.verify(keyPair, Iroha.sign(keyPair, messageHash), messageHash), true)

        // sign by keyPair and verify by keyPair2
        assert(Iroha.verify(keyPair2, Iroha.sign(keyPair, messageHash), messageHash), true)
      }
  }
    "CommandService" when {
      "create a new account" taggedAs TxTest in {
        val domain = IrohaDomainName(context.testDomain)
        val user1Name = IrohaAccountName(createRandomName(10))
        val user1Id = IrohaAccountId(user1Name, domain)

        val user1keyPair = Iroha.createNewKeyPair()
        val createAccount = Iroha.CommandService.createAccount(user1keyPair.publicKey, user1Name, domain)
        val transaction = Iroha.CommandService.createTransaction(context.adminAccount.accountId, context.adminAccount.keypair, Seq(createAccount))
        val accountQuery = Iroha.QueryService.getAccount(context.adminAccount.accountId, context.adminAccount.keypair, user1Id)
        println(TestFormatter.command(createAccount))
        for {
          sent <- sendTransaction(transaction)
          committed <- Future(sent).collect({ case true => awaitUntilTransactionCommitted(transaction) })
          query <- Future(committed).collect({ case true => sendQuery(accountQuery) })
          account = Some(query).collect({ case Iroha.QueryResponse(AccountResponse(x)) => x }).flatMap(_.account)
        } yield {
          println(TestFormatter.queryResponse(query))
          assert(sent, true)
          assert(committed, true)
          assert(account.map(_.accountId.split("@").head).contains(user1Name.value))
          assert(account.map(_.domainId).contains(domain.value))
        }
      }

      "create a new role" taggedAs TxTest in {
        val roleName = createRandomAlphaName(7)
        val permissions = Seq(RolePermission.can_append_role, RolePermission.can_transfer)
        val roleId = IrohaRoleId(IrohaRoleName(roleName))

        val createRole = Iroha.CommandService.createRole(roleName, permissions)
        val transaction = Iroha.CommandService.createTransaction(context.adminAccount.accountId, context.adminAccount.keypair, Seq(createRole))
        val rolesQuery = Iroha.QueryService.getRoles(context.adminAccount.accountId, context.adminAccount.keypair)
        val rolePermissions = Iroha.QueryService.getRolePermissions(context.adminAccount.accountId, context.adminAccount.keypair, roleId)
        println(TestFormatter.command(createRole))
        for {
          sent <- sendTransaction(transaction)
          committed <- Future(sent).collect({ case true => awaitUntilTransactionCommitted(transaction) })
          rolesQuery <- Future(committed).collect({ case true => sendQuery(rolesQuery) })
          roles = Some(rolesQuery).collect({ case Iroha.QueryResponse(RolesResponse(x)) => x.roles })
          permissionsQuery <- Future(committed).collect({ case true => sendQuery(rolePermissions) })
          responsePermissions = Some(permissionsQuery).collect({ case Iroha.QueryResponse(RolePermissionsResponse(x)) => x.permissions })
        } yield {
          println(TestFormatter.queryResponse(rolesQuery))
          println(TestFormatter.queryResponse(permissionsQuery))
          assert(sent, true)
          assert(committed, true)
          assert(roles.exists(_.contains(roleName)))
          assert(responsePermissions.map(_.toList).contains(permissions))
        }
      }

      "append a new role to account" taggedAs TxTest in {
        // Create Account
        val domain = IrohaDomainName(context.testDomain)
        val user1Name = IrohaAccountName(createRandomName(10))
        val user1Id = IrohaAccountId(user1Name, domain)

        val user1keyPair = Iroha.createNewKeyPair()
        val createAccount = Iroha.CommandService.createAccount(user1keyPair.publicKey, user1Name, domain)
        println(TestFormatter.command(createAccount))

        // Create Role
        val roleName = createRandomAlphaName(7)
        val permissions = Seq(RolePermission.can_append_role, RolePermission.can_get_roles, RolePermission.can_transfer)

        val createRole = Iroha.CommandService.createRole(roleName, permissions)
        println(TestFormatter.command(createRole))

        // Append that Role
        val appendRole = Iroha.CommandService.appendRole(user1Id, roleName)
        println(TestFormatter.command(appendRole))

        val commands = Seq(createAccount, createRole, appendRole)
        val transaction = Iroha.CommandService.createTransaction(context.adminAccount.accountId, context.adminAccount.keypair, commands)
        val rolesQuery = Iroha.QueryService.getRoles(user1Id, user1keyPair)

        for {
          sent <- sendTransaction(transaction)
          committed <- Future(sent).collect({ case true => awaitUntilTransactionCommitted(transaction) })
          rolesQuery <- Future(committed).collect({ case true => sendQuery(rolesQuery) })
          roles = Some(rolesQuery).collect({ case Iroha.QueryResponse(RolesResponse(x)) => x.roles })
        } yield {
          println(TestFormatter.queryResponse(rolesQuery))
          assert(sent, true)
          assert(committed, true)
          assert(roles.exists(_.contains(roleName)))
        }
      }
    }
  }
//
//    it("tx and query run right", TxTest) {
//      val domain = IrohaDomainName(context.testDomain)
//      val adminName = IrohaAccountName("admin")
//      val user1Name = IrohaAccountName(createRandomName(10))
//      val user2Name = IrohaAccountName(createRandomName(10))
//      val assetName = IrohaAssetName(createRandomName(8))
//      val adminId = IrohaAccountId(adminName, domain)
//      val user1Id = IrohaAccountId(user1Name, domain)
//      val user2Id = IrohaAccountId(user2Name, domain)
//      val assetId = IrohaAssetId(assetName, domain)
//
//      val user1keyPair = Iroha.createNewKeyPair()
//      val user2keyPair = Iroha.createNewKeyPair()
//      println(user1keyPair.toHex.publicKey)
//
//      val precision = IrohaAssetPrecision(3) // Number of digits after the decimal point
//
//      val commands1 = Seq(
//        Iroha.CommandService.createAccount(user1keyPair.publicKey, user1Name, domain),
//        Iroha.CommandService.createAccount(user2keyPair.publicKey, user2Name, domain),
//        Iroha.CommandService.appendRole(user1Id, "money_creator"),
//        Iroha.CommandService.appendRole(user2Id, "money_creator"),
//        Iroha.CommandService.createAsset(assetName, domain, precision),
//        Iroha.CommandService.addAssetQuantity(assetId, IrohaAmount(Long.MaxValue.toString, precision))
//      )
//
//      val f01 = sendTransaction(Iroha.CommandService.createTransaction(adminId, context.adminAccount.keypair, commands1))
//      // wait for consensus completion
//      assertTxFutures(Iterable(f01))
//
//      val user1keyPair2 = Iroha.createNewKeyPair()
//      val commands2 = Seq(
//        // Tx creator must equal addAssetQuantity target account.
//        Iroha.CommandService.addAssetQuantity(assetId, IrohaAmount("123456", precision)),
//        Iroha.CommandService.addSignatory(user1Id, user1keyPair2.publicKey),
//        Iroha.CommandService.removeSignatory(user1Id, user1keyPair.publicKey)
//      )
//
//      val f02 = sendTransaction(Iroha.CommandService.createTransaction(user1Id, user1keyPair, commands2))
//
//      val user2keyPair2 = Iroha.createNewKeyPair()
//      val commands3 = Seq(
//        // Tx creator must equal addAssetQuantity target account.
//        Iroha.CommandService.addAssetQuantity(assetId, IrohaAmount("111111", precision)),
//        Iroha.CommandService.addSignatory(user2Id, user2keyPair2.publicKey),
//        Iroha.CommandService.removeSignatory(user2Id, user2keyPair.publicKey)
//      )
//
//      val f03 = sendTransaction(Iroha.CommandService.createTransaction(user2Id, user2keyPair, commands3))
//
//      // wait for consensus completion
//      assertTxFutures(Iterable(f02, f03))
//
//      val commands4 = Seq(
//        Iroha.CommandService.transferAsset(user1Id, user2Id, assetId, "purpose", IrohaAmount("10010", precision))
//      )
//
//      val f04 = sendTransaction(Iroha.CommandService.createTransaction(user1Id, user1keyPair2, commands4))
//
//      // wait for consensus completion
//      assertTxFutures(Iterable(f04))
//
//      //////////////////////////////////
//      val queryRes0 = sendQuery(Iroha.QueryService.getAccount(adminId, context.adminAccount.keypair, user1Id))
//      assert(queryRes0.response.isAccountResponse)
//      assert(queryRes0.response.accountResponse.isDefined)
//      assert(queryRes0.response.accountResponse.get.account.isDefined)
//      assert(queryRes0.response.accountResponse.get.account.get.accountId == user1Id.toString)
//      assert(queryRes0.response.accountResponse.get.account.get.domainId == user1Id.domain.value)
//      assert(queryRes0.response.accountResponse.get.account.get.quorum == 1)
//
//      val queryRes1 = sendQuery(Iroha.QueryService.getAccountAssets(user1Id, user1keyPair2, user1Id))
//      assert(queryRes1.response.isAccountAssetsResponse)
//      assert(queryRes1.response.accountAssetsResponse.isDefined)
//
//      val asset = queryRes1.response.accountAssetsResponse.get.accountAssets.find(_.assetId == assetId.toString)
//
//      assert(asset.map(_.accountId).contains(user1Id.toString))
//      assert(asset.map(_.assetId).contains(assetId.toString))
//      assert(asset.exists(_.balance.nonEmpty))
//      assert(asset.map(_.balance).contains("113446"))
//
//      val queryRes2 = sendQuery(Iroha.QueryService.getAccountAssetTransactions(user1Id, user1keyPair2, user1Id, assetId))
//      assert(queryRes2.response.isTransactionsResponse)
//      assert(queryRes2.response.transactionsResponse.isDefined)
//      assert(queryRes2.response.transactionsResponse.get.transactions.length == 1)
//
//      val queryRes3 = sendQuery(Iroha.QueryService.getAccountTransactions(user1Id, user1keyPair2, user1Id))
//      assert(queryRes3.response.isTransactionsResponse)
//      assert(queryRes3.response.transactionsResponse.isDefined)
//      assert(queryRes3.response.transactionsResponse.get.transactions.length == 2)
//
//      val queryRes4 = sendQuery(Iroha.QueryService.getSignatories(user1Id, user1keyPair2, user1Id))
//      assert(queryRes4.response.isSignatoriesResponse)
//      assert(queryRes4.response.signatoriesResponse.isDefined)
//      assert(queryRes4.response.signatoriesResponse.get.keys.length == 1)
//
//      val queryRes5 = sendQuery(Iroha.QueryService.getAccount(user2Id, user2keyPair2, user2Id))
//      assert(queryRes5.response.isAccountResponse)
//      assert(queryRes5.response.accountResponse.isDefined)
//      assert(queryRes5.response.accountResponse.get.account.isDefined)
//      assert(queryRes5.response.accountResponse.get.account.get.accountId == user2Id.toString)
//      assert(queryRes5.response.accountResponse.get.account.get.domainId == user2Id.domain.value)
//      assert(queryRes5.response.accountResponse.get.account.get.quorum == 1)
//
//      val queryRes6 = sendQuery(Iroha.QueryService.getAccountAssets(user2Id, user2keyPair2, user2Id))
//      assert(queryRes6.response.isAccountAssetsResponse)
//      assert(queryRes6.response.accountAssetsResponse.isDefined)
//
//      val asset6 = queryRes6.response.accountAssetsResponse.get.accountAssets.find(_.assetId == assetId.toString)
//
//      assert(asset6.map(_.accountId).contains(user2Id.toString))
//      assert(asset6.map(_.assetId).contains(assetId.toString))
//      assert(asset6.exists(_.balance.nonEmpty))
//      assert(asset6.map(_.balance).contains("121121"))
//
//      val queryRes7 = sendQuery(Iroha.QueryService.getAccountAssetTransactions(user2Id, user2keyPair2, user2Id, assetId))
//      assert(queryRes7.response.isTransactionsResponse)
//      assert(queryRes7.response.transactionsResponse.isDefined)
//      assert(queryRes7.response.transactionsResponse.get.transactions.length == 1)
//
//      val queryRes8 = sendQuery(Iroha.QueryService.getAccountTransactions(user2Id, user2keyPair2, user2Id))
//      assert(queryRes8.response.isTransactionsResponse)
//      assert(queryRes8.response.transactionsResponse.isDefined)
//      assert(queryRes8.response.transactionsResponse.get.transactions.length == 1)
//
//      val queryRes9 = sendQuery(Iroha.QueryService.getSignatories(user2Id, user2keyPair2, user2Id))
//      assert(queryRes9.response.isSignatoriesResponse)
//      assert(queryRes9.response.signatoriesResponse.isDefined)
//      assert(queryRes9.response.signatoriesResponse.get.keys.length == 1)
//    }
//
//    it("tx_counter is to unique to each transaction creater.", TxTest) {
//      val domain = IrohaDomainName(context.testDomain)
//      val adminName = IrohaAccountName("admin")
//      val user1Name = IrohaAccountName(createRandomName(9, "u"))
//      val user2Name = IrohaAccountName(createRandomName(9, "u"))
//      val assetName = IrohaAssetName(createRandomName(3, "a"))
//      val adminId = IrohaAccountId(adminName, domain)
//      val user1Id = IrohaAccountId(user1Name, domain)
//      val user2Id = IrohaAccountId(user2Name, domain)
//      val assetId = IrohaAssetId(assetName, domain)
//
//      val user1keyPair = Iroha.createNewKeyPair()
//      val user2keyPair = Iroha.createNewKeyPair()
//
//      val precision = IrohaAssetPrecision(3) // 小数点以下の桁数
//
//      val commands = Iterable(
//        Iroha.CommandService.createAccount(user1keyPair.publicKey, user1Name, domain),
//        Iroha.CommandService.createAccount(user2keyPair.publicKey, user2Name, domain),
//        Iroha.CommandService.appendRole(user1Id, "money_creator"),
//        Iroha.CommandService.appendRole(user2Id, "money_creator"),
//        Iroha.CommandService.createAsset(assetName, domain, precision)
//      )
//
//      val transaction = Iroha.CommandService.createTransaction(adminId, context.adminAccount.keypair, commands.toSeq)
//      sendTransaction(transaction)
//
//      // Use the same txCounter to check if its unique to the creater.
//      val transactionForUser1 = Iroha.CommandService.createTransaction(
//        user1Id, user1keyPair,
//        Seq(Iroha.CommandService.addAssetQuantity(assetId, IrohaAmount("123456", precision)))
//      )
//
//      val transactionForUser2 = Iroha.CommandService.createTransaction(
//        user2Id, user2keyPair,
//        Seq(Iroha.CommandService.addAssetQuantity(assetId, IrohaAmount("111111", precision)))
//      )
//
//      val f1 = sendTransaction(transactionForUser1)
//      val f2 = sendTransaction(transactionForUser2)
//
//      val r = for {
//        r1 <- f1
//        r2 <- f2
//      } yield r1 && r2
//
//      assert(Await.result(r, Duration.Inf))
//    }
//
//    it("adding and subtracting the same amount result in the no change in balance.", TxTest) {
//      val domain = IrohaDomainName(context.testDomain)
//      val adminId = IrohaAccountId(IrohaAccountName("admin"), domain)
//      val assetName = IrohaAssetName(createRandomName(3, "a"))
//      val assetId = IrohaAssetId(assetName, domain)
//      val precision = IrohaAssetPrecision(3) // 小数点以下の桁数
//
//      val amount = IrohaAmount("111", precision)
//
//      val commands = Seq(
//        Iroha.CommandService.createAsset(assetName, domain, precision),
//        Iroha.CommandService.addAssetQuantity(assetId, amount),
//        Iroha.CommandService.subtractAssetQuantity(assetId, amount)
//      )
//
//      val r = sendTransaction(Iroha.CommandService.createTransaction(adminId, context.adminAccount.keypair, commands))
//        .map(
//          _ => sendQuery(Iroha.QueryService.getAccountAssets(adminId, context.adminAccount.keypair, adminId))
//        )
//        .map({
//          qr =>
//          val response = qr.response.accountAssetsResponse
//          val asset = response.flatMap(_.accountAssets.find(_.assetId == assetId.toString))
//          val balance = asset.map(_.balance).map(BigDecimal.apply)
//          balance.contains(BigDecimal(0))
//        })
//
//      assert(Await.result(r, Duration.Inf))
//    }
//  }
//}
*/