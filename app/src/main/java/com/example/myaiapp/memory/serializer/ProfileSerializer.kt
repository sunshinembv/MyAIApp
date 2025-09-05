package com.example.myaiapp.memory.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.example.myaiapp.Profile
import java.io.InputStream
import java.io.OutputStream

object ProfileSerializer : Serializer<Profile> {
    override val defaultValue: Profile = Profile.getDefaultInstance()
    override suspend fun readFrom(input: InputStream) =
        try { Profile.parseFrom(input) } catch (e: Exception) {
            throw CorruptionException("Cannot read Profile.", e)
        }
    override suspend fun writeTo(t: Profile, output: OutputStream) = t.writeTo(output)
}