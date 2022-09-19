package net.cimadai.iroha

import acyclic.pkg

object IrohaImplicits {
  import net.cimadai.iroha.Iroha.{Account, Amount, Asset, Description, Domain, PeerAddress, Role}
  import iroha.protocol.primitive.Peer
  import net.cimadai.crypto.{KeyPair, PrivateKey, PublicKey}
  import scala.language.implicitConversions
  import scala.util.Try

  implicit def formatPeerAddress(value: PeerAddress): Peer = value.toPeer
  implicit def formatDomain(value: Domain): String = value.toString
  implicit def formatAccount(value: Account): String = value.toString
  implicit def formatAsset(value: Asset): String = value.toString
  implicit def formatRole(value: Role): String = value.toString
  implicit def formatAmount(value: Amount): String = value.toString
  implicit def formatDescription(value: Description): String = value.toString

  implicit def unwrapPublicKey (keypair: Try[KeyPair]): Try[PublicKey]  = keypair.map(kp => kp.publicKey)
  implicit def unwrapPrivateKey(keypair: Try[KeyPair]): Try[PrivateKey] = keypair.map(kp => kp.privateKey)
}
