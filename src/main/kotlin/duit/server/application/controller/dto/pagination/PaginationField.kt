package duit.server.application.controller.dto.pagination

enum class PaginationField(val displayName: String) {
    ID("id"),
    NAME("name");

    companion object{
        const val CONST_ID = "ID"
        const val CONST_NAME = "NAME"
    }
}