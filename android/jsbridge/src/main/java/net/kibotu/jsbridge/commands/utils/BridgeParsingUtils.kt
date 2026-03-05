package net.kibotu.jsbridge.commands.utils

import android.widget.Toast
import org.json.JSONObject

/**
 * Utility class for parsing bridge command parameters.
 */
object BridgeParsingUtils {

    fun parseString(content: Any?, key: String): String {
        return when (content) {
            is JSONObject -> content.optString(key, "")
            is String -> if (key == "text" || key == "message" || key == "url") content else ""
            else -> ""
        }
    }

    fun parseBoolean(content: Any?, key: String): Boolean? {
        return when (content) {
            is JSONObject -> if (content.has(key)) content.optBoolean(key) else null
            is Boolean -> content
            else -> null
        }
    }

    fun parseStringArray(content: Any?, key: String): List<String> {
        if (content !is JSONObject) return emptyList()

        val array = content.optJSONArray(key) ?: return emptyList()
        val result = mutableListOf<String>()
        for (i in 0 until array.length()) {
            result.add(array.optString(i, ""))
        }
        return result
    }

    fun parseMap(content: Any?, key: String): Map<String, String> {
        if (content !is JSONObject) return emptyMap()

        val obj = content.optJSONObject(key) ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        obj.keys().forEach { k ->
            result[k] = obj.optString(k, "")
        }
        return result
    }

    fun parseDuration(content: Any?): Int {
        val duration = parseString(content, "duration")
        return when (duration.lowercase()) {
            "long" -> Toast.LENGTH_LONG
            else -> Toast.LENGTH_SHORT
        }
    }
}

