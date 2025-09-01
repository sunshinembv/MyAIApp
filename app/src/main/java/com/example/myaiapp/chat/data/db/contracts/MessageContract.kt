package com.example.myaiapp.chat.data.db.contracts

object MessageContract {
    const val MESSAGE_TABLE_NAME = "messages"

    object Columns {
        const val ID = "id"
        const val ROLE = "role"
        const val CONTENT = "content"
        const val CREATED_AT = "created_at"
    }
}