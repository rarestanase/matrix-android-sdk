package org.matrix.androidsdk.rest.json

import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.internal.LinkedTreeMap
import java.lang.reflect.Type

class ObjectMapDeserializer : JsonDeserializer<Map<String, Any>> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Map<String, Any>? {
        return read(json) as? Map<String, Any>
    }

    private fun read(input: JsonElement?): Any? {
        return when {
            input == null -> null
            input.isJsonArray -> mapArray(input.asJsonArray)
            input.isJsonObject -> mapObject(input.asJsonObject)
            input.isJsonPrimitive -> mapPrimitive(input.asJsonPrimitive)
            else -> null
        }
    }

    private fun mapArray(jsonArray: JsonArray): Any {
        return jsonArray.map { read(it) }
    }

    private fun mapObject(jsonObject: JsonObject): Any {
        val result = LinkedTreeMap<String, Any>()
        jsonObject.entrySet().forEach {
            result.put(it.key, read(it.value))
        }
        return result
    }

    private fun mapPrimitive(jsonPrimitive: JsonPrimitive): Any? {
        return when {
            jsonPrimitive.isBoolean -> jsonPrimitive.asBoolean
            jsonPrimitive.isString -> jsonPrimitive.asString
            jsonPrimitive.isNumber -> mapNumber(jsonPrimitive.asString)
            else -> null
        }
    }

    private fun mapNumber(number: String): Any {
        return try {
            Integer.valueOf(number)
        } catch (exception: NumberFormatException) {
            try {
                java.lang.Long.valueOf(number)
            } catch (exception: NumberFormatException) {
                number.toDouble()
            }
        }
    }
}
