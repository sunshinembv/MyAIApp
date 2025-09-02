package com.example.myaiapp.chat.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.myaiapp.chat.data.db.contracts.MessageContract
import com.example.myaiapp.chat.data.model.Role

@Entity(
    tableName = MessageContract.MESSAGE_TABLE_NAME,
    indices = [Index(value = [MessageContract.Columns.CREATED_AT], unique = true)]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = MessageContract.Columns.ID)
    val id: Long = 0,
    @ColumnInfo(name = MessageContract.Columns.ROLE)
    val role: Role,
    @ColumnInfo(name = MessageContract.Columns.CONTENT)
    val content: String,
    @ColumnInfo(name = MessageContract.Columns.CREATED_AT)
    val createdAt: Long = System.currentTimeMillis()
)

class RoleConverter {
    @TypeConverter fun fromRole(v: Role?): String? = v?.name
    @TypeConverter fun toRole(v: String?): Role? = v?.let(Role::valueOf)
}