package com.example.myaiapp.memory.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.example.myaiapp.AgentPrefs
import java.io.InputStream
import java.io.OutputStream

object AgentPrefsSerializer : Serializer<AgentPrefs> {
    override val defaultValue: AgentPrefs = AgentPrefs.getDefaultInstance()
    override suspend fun readFrom(input: InputStream) =
        try { AgentPrefs.parseFrom(input) } catch (e: Exception) {
            throw CorruptionException("Cannot read AgentPrefs.", e)
        }
    override suspend fun writeTo(t: AgentPrefs, output: OutputStream) = t.writeTo(output)
}