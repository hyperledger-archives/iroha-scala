package net.cimadai.iroha

sealed trait QueryResponse

object QueryResponse {
  import iroha.protocol

  case class AccountAssetsResponse(response: protocol.AccountAssetResponse) extends QueryResponse
  case class AccountDetailResponse(response: protocol.AccountDetailResponse) extends QueryResponse
  case class AccountResponse(response: protocol.AccountResponse) extends QueryResponse
  case class ErrorResponse(response: protocol.ErrorResponse) extends QueryResponse
  case class SignatoriesResponse(response: protocol.SignatoriesResponse) extends QueryResponse
  case class TransactionsResponse(response: protocol.TransactionsResponse) extends QueryResponse
  case class AssetResponse(response: protocol.AssetResponse) extends QueryResponse
  case class RolesResponse(response: protocol.RolesResponse) extends QueryResponse
  case class RolePermissionsResponse(response: protocol.RolePermissionsResponse) extends QueryResponse
  case class TransactionsPageResponse(response: protocol.TransactionsPageResponse) extends QueryResponse
  case class PendingTransactionsPageResponse(response: protocol.PendingTransactionsPageResponse) extends QueryResponse
  case class BlockResponse(response: protocol.BlockResponse) extends QueryResponse
  case class PeersResponse(response: protocol.PeersResponse) extends QueryResponse

  def unapply(arg: protocol.QueryResponse): Option[QueryResponse] = arg.response match {
    case r if r.isAccountAssetsResponse => arg.response.accountAssetsResponse.map(AccountAssetsResponse.apply)
    case r if r.isAccountDetailResponse => arg.response.accountDetailResponse.map(AccountDetailResponse.apply)
    case r if r.isAccountResponse => arg.response.accountResponse.map(AccountResponse.apply)
    case r if r.isErrorResponse => arg.response.errorResponse.map(ErrorResponse.apply)
    case r if r.isSignatoriesResponse => arg.response.signatoriesResponse.map(SignatoriesResponse.apply)
    case r if r.isTransactionsResponse => arg.response.transactionsResponse.map(TransactionsResponse.apply)
    case r if r.isAssetResponse => arg.response.assetResponse.map(AssetResponse.apply)
    case r if r.isRolesResponse => arg.response.rolesResponse.map(RolesResponse.apply)
    case r if r.isRolePermissionsResponse => arg.response.rolePermissionsResponse.map(RolePermissionsResponse.apply)
    case r if r.isTransactionsPageResponse => arg.response.transactionsPageResponse.map(TransactionsPageResponse.apply)
    case r if r.isPendingTransactionsPageResponse => arg.response.pendingTransactionsPageResponse.map(PendingTransactionsPageResponse.apply)
    case r if r.isBlockResponse => arg.response.blockResponse.map(BlockResponse.apply)
    case r if r.isPeersResponse => arg.response.peersResponse.map(PeersResponse.apply)
    case _ => None
  }
}
