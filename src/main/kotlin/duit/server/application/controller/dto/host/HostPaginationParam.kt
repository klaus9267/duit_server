package duit.server.application.controller.dto.host

import duit.server.application.controller.dto.pagination.PaginationField
import duit.server.application.controller.dto.pagination.PaginationParam

data class HostPaginationParam(
    override val field: PaginationField = PaginationField.NAME
) : PaginationParam(
    field = field
)
