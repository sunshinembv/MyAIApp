package com.example.myaiapp.chat.data.json_extractor

class JsonExtractor(private val moshi: com.squareup.moshi.Moshi) {

    fun <T> parseFirstJson(text: String, clazz: Class<T>): T {
        val start = text.indexOf('{')
        val end   = text.lastIndexOf('}')
        require(start >= 0 && end > start) { "No JSON object found in: $text" }
        val slice = text.substring(start, end + 1)
        val adapter = moshi.adapter(clazz)
        return adapter.fromJson(slice) ?: error("Failed to parse JSON into ${clazz.simpleName}")
    }
}