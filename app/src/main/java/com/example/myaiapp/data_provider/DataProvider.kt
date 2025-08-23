package com.example.myaiapp.data_provider

import android.content.Context
import com.example.myaiapp.R
import java.io.File
import javax.inject.Inject

interface DataProvider {
    fun getUserName(): String
    fun getKeyAbsolutePath(): String
}

class DataProviderImpl @Inject constructor(private val context: Context): DataProvider {

    override fun getUserName(): String {
        val loginFile = File(context.filesDir, "u_name")
        if (!loginFile.exists()) {
            context.resources.openRawResource(R.raw.u_name).use { input ->
                loginFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            loginFile.setReadable(true, true)
        }
        return loginFile.readText(Charsets.UTF_8).trim()
    }

    override fun getKeyAbsolutePath(): String {
        val keyFile = File(context.filesDir, "android_agent")
        if (!keyFile.exists()) {
            context.resources.openRawResource(R.raw.rsa).use { input ->
                keyFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            keyFile.setReadable(true, true)
        }
        return keyFile.absolutePath
    }
}
