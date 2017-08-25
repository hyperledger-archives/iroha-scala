package net.cimadai.iroha

import java.security.MessageDigest
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.util.concurrent.atomic.AtomicLong

import com.google.protobuf.ByteString
import iroha.network.proto.loader.BlocksRequest
import iroha.protocol.block.{Header, Transaction}
import iroha.protocol.commands.Command.Command._
import iroha.protocol.commands.{Amount, Command}
import iroha.protocol.primitive.{Permissions, Signature}
import iroha.protocol.{commands, queries}
import iroha.protocol.queries.Query
import iroha.protocol.queries.Query.Query._
import net.i2p.crypto.eddsa.spec.{EdDSANamedCurveTable, EdDSAPrivateKeySpec, EdDSAPublicKeySpec}
import net.i2p.crypto.eddsa.{EdDSAEngine, EdDSAPrivateKey, EdDSAPublicKey}
import org.bouncycastle.util.encoders.Hex

/**
  * Copyright Daisuke SHIMADA All Rights Reserved.
  * https://github.com/cimadai/iroha-scala
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *      http://www.apache.org/licenses/LICENSE-2.0
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

case class ValidationError(reason: String)

object Iroha {
  private val txCounter = new AtomicLong(1)
  private val queryCounter = new AtomicLong(1)

  case class Ed25519KeyPair(privateKey: EdDSAPrivateKey, publicKey: EdDSAPublicKey) {
    def toHex: Ed25519KeyPairHex =
      Ed25519KeyPairHex(
        Hex.toHexString(privateKey.getEncoded),
        Hex.toHexString(publicKey.getEncoded)
      )
  }

  case class Ed25519KeyPairHex(privateKeyHex: String, publicKeyHex: String) {
    def toKey: Ed25519KeyPair =
      Ed25519KeyPair(
        new EdDSAPrivateKey(new PKCS8EncodedKeySpec(Hex.decode(privateKeyHex))),
        new EdDSAPublicKey(new X509EncodedKeySpec(Hex.decode(publicKeyHex)))
      )
  }

  case class IrohaDomainName(value: String) {
    assert(0 < value.length && value.length <= 10, "domainName length must be between 1 to 10")
    assert(isAlphabetAndNumber(value), "domainName must be only alphabet or number")
  }

  case class IrohaAssetName(value: String) {
    assert(0 < value.length && value.length <= 10, "assetName length must be between 1 to 10")
    assert(isAlphabetAndNumber(value), "assetName must be only alphabet or number")
  }

  case class IrohaAccountName(value: String) {
    assert(0 < value.length && value.length <= 8, "accountName length must be between 1 to 8")
    assert(isAlphabetAndNumber(value), "accountName must be only alphabet or number")
  }

  case class IrohaAccountId(accountName: IrohaAccountName, domain: IrohaDomainName) {
    override def toString: String = s"${accountName.value}@${domain.value}"
  }

  case class IrohaAssetId(assetName: IrohaAssetName, domain: IrohaDomainName) {
    override def toString: String = s"${assetName.value}#${domain.value}"
  }

  case class IrohaAmount(intPart: Long, flacPart: Long) {
    assert(intPart + flacPart > 0, "amount must be greater than 0")
  }

  // This emulates std::alnum.
  private def isAlphabetAndNumber(str: String): Boolean = {
    str.matches("""^[a-zA-Z0-9]+$""")
  }

  private val spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.CURVE_ED25519_SHA512)
  private def withEd25519[T](f: EdDSAEngine => T): T = {
    val signature = new EdDSAEngine(MessageDigest.getInstance(spec.getHashAlgorithm))
    f(signature)
  }

  def createNewKeyPair(): Ed25519KeyPair = {
    val seed = Array.fill[Byte](32) {0x0}
    new scala.util.Random(new java.security.SecureRandom()).nextBytes(seed)
    val sKey = new EdDSAPrivateKey(new EdDSAPrivateKeySpec(seed, spec))
    val vKey = new EdDSAPublicKey(new EdDSAPublicKeySpec(sKey.getAbyte, spec))
    Ed25519KeyPair(sKey, vKey)
  }

  def createKeyPairFromHex(privateKeyHex: String, publicKeyHex: String): Ed25519KeyPair = {
    Ed25519KeyPairHex(privateKeyHex, publicKeyHex).toKey
  }

  def sign(keyPair: Ed25519KeyPair, message: Array[Byte]): Array[Byte] = {
    withEd25519 { ed25519 =>
      ed25519.initSign(keyPair.privateKey)
      ed25519.signOneShot(message)
    }
  }

  def verify(keyPair: Ed25519KeyPair, signature: Array[Byte], message: Array[Byte]): Boolean = {
    withEd25519 { ed25519 =>
      ed25519.initVerify(keyPair.publicKey)
      ed25519.verifyOneShot(message, signature)
    }
  }

  object CommandService {
    private def createTransaction(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, commands: Seq[Command]): Transaction = {
      val signature = Iroha.sign(creatorKeyPair, commands.map(c => c.toByteArray).foldLeft(Array[Byte]())((accu, each) => accu ++ each))
      val sig = Signature(ByteString.copyFrom(creatorKeyPair.publicKey.getAbyte), ByteString.copyFrom(signature))
      // TODO: ugly hack. irohaノードより未来のタイムスタンプを渡すと失敗する。
      val header = Header(System.currentTimeMillis() - 5000, signatures = Seq(sig))
      val meta = Transaction.Meta(creatorAccountId.toString, txCounter.incrementAndGet())
      val body = Transaction.Body(commands)
      Transaction(header = Some(header), meta = Some(meta), body = Some(body))
    }

    def addAssetQuantity(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, accountId: IrohaAccountId, assetId: IrohaAssetId, amount: IrohaAmount): Transaction = {
      val command = Command(AddAssetQuantity(commands.AddAssetQuantity(accountId.toString, assetId.toString, Some(Amount(amount.intPart, amount.flacPart)))))
      createTransaction(creatorAccountId, creatorKeyPair, Seq(command))
    }

    def addPeer(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, address: String, peerKey: String): Transaction = {
      val command = Command(AddPeer(commands.AddPeer(address, ByteString.copyFrom(peerKey.getBytes()))))
      createTransaction(creatorAccountId, creatorKeyPair, Seq(command))
    }

    def addSignatory(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, accountId: IrohaAccountId, publicKey: EdDSAPublicKey): Transaction = {
      val command = Command(AddSignatory(commands.AddSignatory(accountId.toString, ByteString.copyFrom(publicKey.getAbyte))))
      createTransaction(creatorAccountId, creatorKeyPair, Seq(command))
    }

    def assignMasterKey(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, accountId: IrohaAccountId, publicKey: EdDSAPublicKey): Transaction = {
      val command = Command(AccountAssignMk(commands.AssignMasterKey(accountId.toString, ByteString.copyFrom(publicKey.getAbyte))))
      createTransaction(creatorAccountId, creatorKeyPair, Seq(command))
    }

    def createAsset(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, assetName: IrohaAssetName, domainName: IrohaDomainName, precision: Int): Transaction = {
      val command = Command(CreateAsset(commands.CreateAsset(assetName.value, domainName.value, precision)))
      createTransaction(creatorAccountId, creatorKeyPair, Seq(command))
    }

    def createAccount(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, publicKey: EdDSAPublicKey, accountName: IrohaAccountName, domainName: IrohaDomainName): Transaction = {
      val command = Command(CreateAccount(commands.CreateAccount(accountName.value, domainName.value, mainPubkey = ByteString.copyFrom(publicKey.getAbyte))))
      createTransaction(creatorAccountId, creatorKeyPair, Seq(command))
    }

    def createDomain(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, domainName: IrohaDomainName): Transaction = {
      val command = Command(CreateDomain(commands.CreateDomain(domainName.value)))
      createTransaction(creatorAccountId, creatorKeyPair, Seq(command))
    }

    def removeSignatory(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, accountId: IrohaAccountId, publicKey: EdDSAPublicKey): Transaction = {
      val command = Command(RemoveSign(commands.RemoveSignatory(accountId.toString, ByteString.copyFrom(publicKey.getAbyte))))
      createTransaction(creatorAccountId, creatorKeyPair, Seq(command))
    }

    def setAccountPermissions(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, accountId: IrohaAccountId, permissions: Permissions): Transaction = {
      val command = Command(SetPermission(commands.SetAccountPermissions(accountId.toString, Some(permissions))))
      createTransaction(creatorAccountId, creatorKeyPair, Seq(command))
    }

    def setAccountQuorum(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, accountId: IrohaAccountId, quorum: Int): Transaction = {
      val command = Command(SetQuorum(commands.SetAccountQuorum(accountId.toString, quorum)))
      createTransaction(creatorAccountId, creatorKeyPair, Seq(command))
    }

    def transferAsset(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, srcAccountId: IrohaAccountId, destAccountId: IrohaAccountId, assetId: IrohaAssetId, amount: IrohaAmount): Transaction = {
      val command = Command(TransferAsset(commands.TransferAsset(srcAccountId.toString, destAccountId.toString, assetId.toString, Some(Amount(amount.intPart, amount.flacPart)))))
      createTransaction(creatorAccountId, creatorKeyPair, Seq(command))
    }
  }

  object QueryService {
    private def createQuery(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, query: Query.Query): Query = {
      val queryBytes = (query match {
        case q if q.isGetAccount => q.getAccount.map(_.toByteArray)
        case q if q.isGetAccountAssets => q.getAccountAssets.map(_.toByteArray)
        case q if q.isGetAccountAssetTransactions => q.getAccountAssetTransactions.map(_.toByteArray)
        case q if q.isGetAccountSignatories => q.getAccountSignatories.map(_.toByteArray)
        case q if q.isGetAccountTransactions => q.getAccountTransactions.map(_.toByteArray)
      }).getOrElse(Array.empty)

      val signature = Iroha.sign(creatorKeyPair, creatorAccountId.toString.getBytes() ++ queryBytes)
      val sig = Signature(ByteString.copyFrom(creatorKeyPair.publicKey.getAbyte), ByteString.copyFrom(signature))
      // TODO: ugly hack. irohaノードより未来のタイムスタンプを渡すと失敗する。
      val header = Query.Header(System.currentTimeMillis() - 5000, signature = Some(sig))
      Query(
        header = Some(header),
        creatorAccountId = creatorAccountId.toString,
        queryCounter = queryCounter.incrementAndGet(),
        query = query
      )
    }

    def getAccount(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, accountId: IrohaAccountId): Query = {
      createQuery(creatorAccountId, creatorKeyPair, GetAccount(queries.GetAccount(accountId.toString)))
    }

    def getAccountAssets(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, accountId: IrohaAccountId, assetId: IrohaAssetId): Query = {
      createQuery(creatorAccountId, creatorKeyPair, GetAccountAssets(queries.GetAccountAssets(accountId.toString, assetId.toString)))
    }

    def getAccountAssetTransactions(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, accountId: IrohaAccountId, assetId: IrohaAssetId): Query = {
      createQuery(creatorAccountId, creatorKeyPair, GetAccountAssetTransactions(queries.GetAccountAssetTransactions(accountId.toString, assetId.toString)))
    }

    def getAccountTransactions(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, accountId: IrohaAccountId): Query = {
      createQuery(creatorAccountId, creatorKeyPair, GetAccountTransactions(queries.GetAccountTransactions(accountId.toString)))
    }

    def getSignatories(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, accountId: IrohaAccountId): Query = {
      createQuery(creatorAccountId, creatorKeyPair, GetAccountSignatories(queries.GetSignatories(accountId.toString)))
    }
  }

  object Loader {
    def blockRequest(height: Long = 0L): BlocksRequest = {
      BlocksRequest(height)
    }
  }
}

