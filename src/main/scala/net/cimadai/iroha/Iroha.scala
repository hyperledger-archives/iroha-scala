package net.cimadai.iroha

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

import java.util.concurrent.atomic.AtomicLong

import com.google.protobuf.ByteString
import iroha.protocol.block.Transaction
import iroha.protocol.commands.Command
import iroha.protocol.commands.Command.Command._
import iroha.protocol.endpoint.TxStatusRequest
import iroha.protocol.primitive._
import iroha.protocol.queries.Query
import iroha.protocol.{commands, queries}
import net.cimadai.crypto.{SHA3EdDSAParameterSpec, SHA3EdDSAPrivateKeySpec}
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import net.i2p.crypto.eddsa.{EdDSAEngine, EdDSAPrivateKey, EdDSAPublicKey, Utils}
import org.bouncycastle.jcajce.provider.digest.SHA3

import scala.language.postfixOps

object Iroha {
  val txCounter = new AtomicLong(1)
  private val queryCounter = new AtomicLong(1)

  implicit class EdDSAPublicKeyExt(pub: EdDSAPublicKey) {
    def toPublicKeyBytes: Array[Byte] = pub.getAbyte

    def toPublicKeyHex: String = Utils.bytesToHex(pub.toPublicKeyBytes)
  }

  implicit class EdDSAPrivateKeyExt(priv: EdDSAPrivateKey) {
    def toPublicKeyBytes: Array[Byte] = priv.getAbyte

    def toPublicKeyHex: String = Utils.bytesToHex(priv.toPublicKeyBytes)

    def toPrivateKeyBytes: Array[Byte] = priv.getH

    def toPrivateKeyHex: String = Utils.bytesToHex(priv.toPrivateKeyBytes)
  }

  case class Ed25519KeyPair(privateKey: EdDSAPrivateKey, publicKey: EdDSAPublicKey) {
    def toHex: Ed25519KeyPairHex = Ed25519KeyPairHex(privateKey.toPrivateKeyHex)
  }

  object Ed25519KeyPairHex {
    def apply (privateKeyHex: String): Ed25519KeyPairHex = {
      Ed25519KeyPairHex(Utils.hexToBytes(privateKeyHex))
    }
  }

  case class Ed25519KeyPairHex(privateKeyBytes: Array[Byte]) {
    private val sKey = new EdDSAPrivateKey(SHA3EdDSAPrivateKeySpec(spec, privateKeyBytes))
    private val pKey = new EdDSAPublicKey(new EdDSAPublicKeySpec(sKey.toPublicKeyBytes, spec))

    val publicKey: String = sKey.toPublicKeyHex
    val privateKey: String = sKey.toPrivateKeyHex

    def toKey: Ed25519KeyPair =
      Ed25519KeyPair(sKey, pKey)
  }

  case class IrohaDomainName(value: String) {
    assert(0 < value.length && value.length <= 164, "domainName length must be between 1 to 164")
    assert(IrohaValidator.isValidDomain(value), "domainName must satisfy the domain specifications (RFC1305).")
  }

  case class IrohaAssetName(value: String) {
    assert(0 < value.length && value.length <= 9, "assetName length must be between 1 to 9")
    assert(IrohaValidator.isAlphabetAndNumber(value), "assetName must be only alphabet or number. [a-zA-Z0-9]")
  }

  case class IrohaAccountName(value: String) {
    assert(0 < value.length && value.length <= 32, "accountName length must be between 1 to 32")
    assert(IrohaValidator.isValidDomain(value), "accountName must satisfy the domain specifications (RFC1305).")
  }

  case class IrohaRoleName(value: String) {
    assert(0 < value.length && value.length <= 7, "roleName length must be between 1 to 7")
    assert(IrohaValidator.isAlphabetAndNumber(value) && IrohaValidator.isLowerCase(value), "roleName must be only lower alphabet. [a-z]")
  }

  case class IrohaAssetPrecision(value: Int) {
    assert(0 <= value && value <= 255, "precision must be between 0 to 255")
  }

  case class IrohaAccountId(accountName: IrohaAccountName, domain: IrohaDomainName) {
    override def toString: String = s"${accountName.value}@${domain.value}"
  }

  case class IrohaAssetId(assetName: IrohaAssetName, domain: IrohaDomainName) {
    override def toString: String = s"${assetName.value}#${domain.value}"
  }

  case class IrohaRoleId(roleName: IrohaRoleName) {
    override def toString: String = s"${roleName.value}"
  }

  case class IrohaAmount(value: Option[uint256], precision: IrohaAssetPrecision) {
    private val isZeroOrPositive = {
      val v = value.getOrElse(uint256())
      (v.first >= 0 && v.second >= 0 && v.third >= 0 && v.fourth >= 0) &&
        (v.first + v.second + v.third + v.fourth) >= 0
    }
    assert(isZeroOrPositive, "amount must be greater equal than 0")
  }

  private val spec = new SHA3EdDSAParameterSpec

  private def withEd25519[T](f: EdDSAEngine => T): T = {
    val signature = new EdDSAEngine(new SHA3.Digest512())
    f(signature)
  }

  def createNewKeyPair(): Ed25519KeyPair = {
    val seed = Array.fill[Byte](32) {0x0}
    new scala.util.Random(new java.security.SecureRandom()).nextBytes(seed)
    val sKey = new EdDSAPrivateKey(SHA3EdDSAPrivateKeySpec(seed, spec))
    val vKey = new EdDSAPublicKey(new EdDSAPublicKeySpec(sKey.toPublicKeyBytes, spec))
    Ed25519KeyPair(sKey, vKey)
  }

  def createKeyPairFromHex(privateKeyHex: String): Ed25519KeyPair = {
    Ed25519KeyPairHex(privateKeyHex).toKey
  }

  def createKeyPairFromBytes(privateKeyBytes: Array[Byte]): Ed25519KeyPair = {
    Ed25519KeyPairHex(privateKeyBytes).toKey
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
    private def txHash(transaction: Transaction): Array[Byte] = {
      new SHA3.Digest256().digest(transaction.payload.get.toByteArray)
    }

    def createTransaction(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, commands: Seq[Command], txCounter: Long = Iroha.txCounter.getAndIncrement()): Transaction = {
      val payload = Transaction.Payload(
        commands = commands,
        creatorAccountId = creatorAccountId.toString,
        txCounter,
        createdTime = System.currentTimeMillis() - 2000000) // TODO: ugly hack. irohaノードより未来のタイムスタンプを渡すと失敗する。

      val sha3_256 = new SHA3.Digest256()
      val hash = sha3_256.digest(payload.toByteArray)
      val sig = Signature(
        ByteString.copyFrom(creatorKeyPair.publicKey.toPublicKeyBytes),
        ByteString.copyFrom(Iroha.sign(creatorKeyPair, hash))
      )
      Transaction(Some(payload), Seq(sig))
    }

    def appendRole(accountId: IrohaAccountId, roleName: String): Command =
      Command(AppendRole(commands.AppendRole(accountId.toString, roleName)))

    def createRole(roleName: String, permissions: Seq[RolePermission]): Command =
      Command(CreateRole(commands.CreateRole(roleName, permissions)))

    def grantPermission(accountId: IrohaAccountId, permissions: GrantablePermission): Command =
      Command(GrantPermission(commands.GrantPermission(accountId.toString, permissions)))

    def revokePermission(accountId: IrohaAccountId, permissions: GrantablePermission): Command =
      Command(RevokePermission(commands.RevokePermission(accountId.toString, permissions)))

    def addAssetQuantity(accountId: IrohaAccountId, assetId: IrohaAssetId, amount: IrohaAmount): Command =
      Command(AddAssetQuantity(commands.AddAssetQuantity(accountId.toString, assetId.toString, Some(Amount(amount.value, amount.precision.value)))))

    def addPeer(address: String, peerKey: EdDSAPublicKey): Command =
      Command(AddPeer(commands.AddPeer(address, ByteString.copyFrom(peerKey.toPublicKeyBytes))))

    def addSignatory(accountId: IrohaAccountId, publicKey: EdDSAPublicKey): Command =
      Command(AddSignatory(commands.AddSignatory(accountId.toString, ByteString.copyFrom(publicKey.toPublicKeyBytes))))

    def createAccount(publicKey: EdDSAPublicKey, accountName: IrohaAccountName, domainName: IrohaDomainName): Command =
      Command(CreateAccount(commands.CreateAccount(accountName.value, domainName.value, mainPubkey = ByteString.copyFrom(publicKey.toPublicKeyBytes))))

    def createAsset(assetName: IrohaAssetName, domainName: IrohaDomainName, precision: IrohaAssetPrecision): Command =
      Command(CreateAsset(commands.CreateAsset(assetName.value, domainName.value, precision.value)))

    def createDomain(domainName: IrohaDomainName): Command =
      Command(CreateDomain(commands.CreateDomain(domainName.value)))

    def removeSignatory(accountId: IrohaAccountId, publicKey: EdDSAPublicKey): Command =
      Command(RemoveSign(commands.RemoveSignatory(accountId.toString, ByteString.copyFrom(publicKey.toPublicKeyBytes))))

    def setQuorum(accountId: IrohaAccountId, quorum: Int): Command =
      Command(SetQuorum(commands.SetAccountQuorum(accountId.toString, quorum)))

    def subtractAssetQuantity(accountId: IrohaAccountId, assetId: IrohaAssetId, amount: IrohaAmount): Command =
      Command(SubtractAssetQuantity(commands.SubtractAssetQuantity(accountId.toString, assetId.toString, Some(Amount(amount.value, amount.precision.value)))))

    def transferAsset(srcAccountId: IrohaAccountId, destAccountId: IrohaAccountId, assetId: IrohaAssetId, description: String, amount: IrohaAmount): Command =
      Command(TransferAsset(commands.TransferAsset(
        srcAccountId.toString,
        destAccountId.toString,
        assetId.toString,
        description,
        Some(Amount(amount.value, amount.precision.value)))))

    def txStatusRequest(transaction: Transaction): TxStatusRequest =
      TxStatusRequest(ByteString.copyFrom(Iroha.CommandService.txHash(transaction)))
  }

  object QueryService {
    private def createQuery(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, query: Query.Payload.Query): Query = {
      val payload = Query.Payload(
        createdTime = System.currentTimeMillis() - 2000000, // TODO: ugly hack. irohaノードより未来のタイムスタンプを渡すと失敗する。
        creatorAccountId = creatorAccountId.toString,
        queryCounter = queryCounter.getAndIncrement(),
        query
      )

      val sha3_256 = new SHA3.Digest256()
      val hash = sha3_256.digest(payload.toByteArray)
      val sig = Signature(
        ByteString.copyFrom(creatorKeyPair.publicKey.toPublicKeyBytes),
        ByteString.copyFrom(Iroha.sign(creatorKeyPair, hash))
      )
      Query(Some(payload), Some(sig))
    }

    def getAccount(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, accountId: IrohaAccountId): Query = {
      createQuery(creatorAccountId, creatorKeyPair, Query.Payload.Query.GetAccount(queries.GetAccount(accountId.toString)))
    }

    def getAccountAssets(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, accountId: IrohaAccountId, assetId: IrohaAssetId): Query = {
      createQuery(creatorAccountId, creatorKeyPair, Query.Payload.Query.GetAccountAssets(queries.GetAccountAssets(accountId.toString, assetId.toString)))
    }

    def getAccountAssetTransactions(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, accountId: IrohaAccountId, assetId: IrohaAssetId): Query = {
      createQuery(creatorAccountId, creatorKeyPair, Query.Payload.Query.GetAccountAssetTransactions(queries.GetAccountAssetTransactions(accountId.toString, assetId.toString)))
    }

    def getAccountTransactions(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, accountId: IrohaAccountId): Query = {
      createQuery(creatorAccountId, creatorKeyPair, Query.Payload.Query.GetAccountTransactions(queries.GetAccountTransactions(accountId.toString)))
    }

    def getAssetInfo(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, assetId: IrohaAssetId): Query = {
      createQuery(creatorAccountId, creatorKeyPair, Query.Payload.Query.GetAssetInfo(queries.GetAssetInfo(assetId.toString)))
    }

    def getRolePermissions(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, roleId: IrohaRoleId): Query = {
      createQuery(creatorAccountId, creatorKeyPair, Query.Payload.Query.GetRolePermissions(queries.GetRolePermissions(roleId.toString)))
    }

    def getRoles(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair): Query = {
      createQuery(creatorAccountId, creatorKeyPair, Query.Payload.Query.GetRoles(queries.GetRoles()))
    }

    def getSignatories(creatorAccountId: IrohaAccountId, creatorKeyPair: Ed25519KeyPair, accountId: IrohaAccountId): Query = {
      createQuery(creatorAccountId, creatorKeyPair, Query.Payload.Query.GetAccountSignatories(queries.GetSignatories(accountId.toString)))
    }

  }
}
