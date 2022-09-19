package net.cimadai.iroha

/*
import iroha.protocol.endpoint.{ToriiResponse, TxStatus, TxStatusRequest}
import iroha.protocol.endpoint.CommandService_v1Grpc.{CommandService_v1BlockingStub => CommandService}
import iroha.protocol.endpoint.QueryService_v1Grpc.{QueryService_v1BlockingStub => QueryService}
import iroha.protocol.qry_responses.QueryResponse
import iroha.protocol.queries.Query
import iroha.protocol.transaction.Transaction
import net.cimadai.iroha.Iroha.{Account, Domain, ToriiError}
import net.i2p.crypto.eddsa.Utils
import org.spongycastle.jcajce.provider.digest.SHA3

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration
*/

import scala.util.Random

trait TestHelpers {

/*
  case class IrohaTestAccount(accountName: String, domainName: String, privateKey: String, publicKey: String) {

    import monix.eval.Task
    import net.cimadai.crypto.SHA3EdDSAKeyPair

    val keypair: SHA3EdDSAKeyPair = SHA3EdDSAKeyPair(privateKey)
    assert(keypair.toHex.publicKey == publicKey)

    def accountId: Task[Account] = {
      Account(accountName, domainName)
    }
  }

  /**
    * @todo Verbose property in scalatest without this context val?
    */
  case class IrohaTestContext(grpcHost: String, grpcPort: Int, adminPrivateKey: String, adminPublicKey: String, verbose: Boolean) {
    val testDomain: String = "test"
    val adminAccount: IrohaTestAccount = IrohaTestAccount("admin", testDomain, adminPrivateKey, adminPublicKey)
  }

  private type Context = IrohaTestContext

  def sendTransaction(tx: Transaction)(implicit commandGrpc: CommandService, context: Context, ec :ExecutionContext): Future[Boolean] = {
    if (context.verbose) {
      println("== Tx ==")
      println(tx)
      println("========")
    }
    commandGrpc.torii(tx)
    checkTransactionCommit(tx)
  }

  def askTransactionStatus(txStatusRequest: TxStatusRequest)(implicit commandGrpc: CommandService): ToriiResponse = {
    commandGrpc.status(txStatusRequest)
  }


  def sendQuery(query: Query)(implicit queryGrpc: QueryService, context: Context): QueryResponse = {
    if (context.verbose) {
      println("== Qry ==")
      println(query)
      println("---------")
    }
    val resp = queryGrpc.find(query)
    if (context.verbose) {
      println(resp)
      println("=========")
    }
    resp
  }

  def isCommitted(tx: Transaction)(implicit commandGrpc: CommandService): Boolean = {
    val response = askTransactionStatus(Iroha.CommandService.txStatusRequest(tx))
    response match {
      case ToriiError(error) => throw error
      case r => r.txStatus == TxStatus.COMMITTED
    }
  }

  def awaitUntilTransactionCommitted(tx: Transaction, counter: Int = 0)(implicit commandGrpc: CommandService, context: Context): Boolean = {
    if (counter >= 20) {
      false
    } else if (isCommitted(tx)) {
      true
    } else {
      Thread.sleep(1000)
      awaitUntilTransactionCommitted(tx, counter + 1)
    }
  }

  def checkTransactionCommit(tx: Transaction)(implicit commandGrpc: CommandService, context: Context, ec: ExecutionContext): Future[Boolean] = Future {
    awaitUntilTransactionCommitted(tx)
  }

  def assertTxFutures(futures: Iterable[Future[Boolean]]): Unit = {
    futures.foreach(f => assert(Await.result(f, Duration.Inf), true))
  }
  */

  def createRandomName(length: Int, prefix: String = "z"): String = {
    prefix + Random.alphanumeric.take(length - 1).mkString.toLowerCase
  }

  def createRandomAlphaName(length: Int): String = {
    val rand = new scala.util.Random(System.nanoTime)
    val ab = "abcdefghijklmnopqrstuvwxyz"
    ("" /: (0 until length)) { (acc, _) => acc + ab(rand.nextInt(ab.length)) }
  }


  //--------------------------------------------------------------------------------------------------------------------


  def ignored(action: ⇒ Unit): Unit = ignored
  def ignoredif(condition: Boolean)(action: ⇒ Unit): Unit = if(condition) ignored else action
  def pending(action: ⇒ Unit): Unit = {
    try {
      action
      pending("PENDING", new TestPendingException)
    } catch {
      case t: Throwable => pending("FAILED", new TestFailedException(t))
    }
  }

  private def ignored: Unit = {
    val t = new TestIgnoredException
    val it = t.getStackTrace.iterator
    val e = (1 to 5).map(_ => it.next).last
    report("IGNORED", e)
  }

  private def pending(state: String, t: Throwable): Unit = {
    val pos = t.getStackTrace.zipWithIndex.find(p => p._1.getMethodName == "pending").get._2
    val it = t.getStackTrace.iterator
    val e = (1 to pos+4).map(_ => it.next).last
    report(state, e)
  }

  private def report(state: String, e: StackTraceElement): Unit = {
    val _class  = e.getClassName
    val _method = e.getMethodName
    val _file   = e.getFileName
    val _line   = e.getLineNumber
    System.err.println(s"@@@@@@@@ ${state}:: ${_class}.${_method} at ${_file}:${_line}")
  }

class TestPendingException extends Exception
class TestIgnoredException extends Exception
class TestFailedException(cause: Throwable) extends Exception(cause)

}
