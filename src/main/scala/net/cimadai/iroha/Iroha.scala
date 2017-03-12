package net.cimadai.iroha

import java.security.MessageDigest

import Api.api.BaseObject.Value.{ValueInt, ValueString}
import Api.api._
import net.cimadai.iroha.MethodType.MethodType
import net.i2p.crypto.eddsa.spec.{EdDSANamedCurveTable, EdDSAPrivateKeySpec, EdDSAPublicKeySpec}
import net.i2p.crypto.eddsa.{EdDSAEngine, EdDSAPrivateKey, EdDSAPublicKey}

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

object MethodType {
  sealed abstract class MethodType(val name: String)
  case object ADD extends MethodType("add")
  case object TRANSFER extends MethodType("transfer")
  case object UPDATE extends MethodType("update")
  case object REMOVE extends MethodType("remove")
  case object CONTRACT extends MethodType("contract")
}

object Iroha {
  case class Ed25519KeyPair(privateKey: EdDSAPrivateKey, publicKey: EdDSAPublicKey)
  private val spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.CURVE_ED25519_SHA512)
  private def withEd25519[T](f: EdDSAEngine => T): T = {
    val signature = new EdDSAEngine(MessageDigest.getInstance(spec.getHashAlgorithm))
    f(signature)
  }

  def createKeyPair(): Ed25519KeyPair = {
    val seed = Array.fill[Byte](32) {0x0}
    new scala.util.Random(new java.security.SecureRandom()).nextBytes(seed)
    val sKey = new EdDSAPrivateKey(new EdDSAPrivateKeySpec(seed, spec))
    val vKey = new EdDSAPublicKey(new EdDSAPublicKeySpec(sKey.getAbyte, spec))
    Ed25519KeyPair(sKey, vKey)
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

  def createTransaction(methodType: MethodType, publicKeyBase64: String): Transaction = {
    Transaction(
      txSignatures = Seq.empty,
      `type` = methodType.name,
      senderPubkey = publicKeyBase64,
      timestamp = System.currentTimeMillis(),
      asset = None,
      simpleAsset = None,
      domain = None,
      account = None,
      peer = None
    )
  }

  def createAccountQuery(publicKeyBase64: String): Query = {
    Query(
      `type` = "account",
      senderPubkey = publicKeyBase64
    )
  }

  def createAssetQuery(publicKeyBase64: String, assetName: String): Query = {
    Query(
      `type` = "asset",
      value = Map("name" -> BaseObject(ValueString(assetName))),
      senderPubkey = publicKeyBase64
    )
  }

  def createAccount(publicKeyBase64: String, accountName: String, assetNames: Seq[String]): Account = {
    Account(publicKey = publicKeyBase64, name = accountName, assets = assetNames)
  }

  def createAsset(assetName: String, amount: Long): Asset = {
    Asset(name = assetName, value = Map("value" -> BaseObject(ValueInt(amount))))
  }
}

