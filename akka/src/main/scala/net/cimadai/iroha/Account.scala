package net.cimadai.iroha

import iroha.protocol

case class Account(name: String, domain: String) {
  def toIrohaString: String = f"$name%s@$domain%s"
  def toIroha: protocol.Account = protocol.Account(name, domain)
}

object Account {
  def apply(nameDomain: String): Account = {
    val split = nameDomain.split("@")
    Account(split(0), split(1))
  }
}
