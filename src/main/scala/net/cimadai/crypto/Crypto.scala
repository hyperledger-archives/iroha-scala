package net.cimadai.crypto

sealed trait Crypto {
  import jp.co.soramitsu.crypto.ed25519.{Ed25519Sha3, EdDSAEngine}
  import org.spongycastle.jcajce.provider.digest.SHA3.DigestSHA3
  val digest: DigestSHA3
  val engine: EdDSAEngine
  val crypto: Ed25519Sha3
}
object Crypto {
  import jp.co.soramitsu.crypto.ed25519.{Ed25519Sha3, EdDSAEngine}
  import org.spongycastle.jcajce.provider.digest.SHA3
  import org.spongycastle.jcajce.provider.digest.SHA3.DigestSHA3
  val digest256: DigestSHA3  = new SHA3.Digest256
  val digest512: DigestSHA3  = new SHA3.Digest512
  val engine512: EdDSAEngine = new EdDSAEngine(digest512)
  val crypto   : Ed25519Sha3 = new Ed25519Sha3()
}
