package net.cimadai.iroha

import iroha.protocol

case class Asset(name: String, domain: String) {
  def toIrohaString: String = f"$name%s#$domain%s"
  def toIroha: protocol.Asset = protocol.Asset(name, domain)
}

object Asset {
  def apply(nameDomain: String): Asset = {
    val split = nameDomain.split("#")
    Asset(split(0), split(1))
  }
}
