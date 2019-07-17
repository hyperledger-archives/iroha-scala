package net.cimadai.iroha
/*
import com.google.protobuf.ByteString
import iroha.protocol.commands.Command.{Command => Commands}
import iroha.protocol.commands.{Command => IrohaCommand}
import iroha.protocol.qry_responses.{QueryResponse => IrohaQueryResponse}
import net.i2p.crypto.eddsa.Utils

object TestFormatter {
  import Commands._

  private def pubKey(bytes: String): String = s"[$bytes]"

  def command(irohaCommand: IrohaCommand): String = irohaCommand match {
    case IrohaCommand(CreateAccount(value)) => "[TX] Creating account " + value.accountName + "@" + value.domainId + " " + pubKey(value.publicKey)
    case IrohaCommand(CreateRole(value)) => "[TX] Creating role " + value.roleName + " with permissions: " + value.permissions.map(_.name).mkString(", ")
    case IrohaCommand(AppendRole(value)) => "[TX] Appending role " + value.roleName + " to account " + value.accountId
    case _ => "[TX] Unknown Command"
  }

  import Iroha.MatchedResponse._
  def queryResponse(irohaQueryResponse: IrohaQueryResponse): String = irohaQueryResponse match {
    case Iroha.QueryResponse(AccountResponse(r)) => "[RES] " + r.account.map(a => "Account " + a.accountId + "@" + a.domainId)
    case Iroha.QueryResponse(RolesResponse(r)) => "[RES] " + r.roles.length + " roles"
    case Iroha.QueryResponse(RolePermissionsResponse(r)) => "[RES] role permissions: " + r.permissions.map(_.name).mkString(", ")
    case _ => "[RES] Unknown Query Response"
  }
}
*/