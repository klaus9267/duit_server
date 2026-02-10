package duit.server.support.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions
import org.springframework.test.web.servlet.MvcResult

private val objectMapper = ObjectMapper()

fun extractValuesFromResponse(result: MvcResult, jsonPath: String): List<Any> {
    val responseBody = result.response.contentAsString
    val jsonNode = objectMapper.readTree(responseBody)
    val contentNode = jsonNode.get("content")

    if (!contentNode.isArray) return emptyList()

    val values = mutableListOf<Any>()
    for (item in contentNode) {
        val pathParts = jsonPath.split(".")
        var currentNode: JsonNode = item

        for (part in pathParts) {
            if (currentNode.has(part)) {
                currentNode = currentNode.get(part)
            } else {
                break
            }
        }

        when {
            currentNode.isInt -> values.add(currentNode.asInt())
            currentNode.isLong -> values.add(currentNode.asLong())
            currentNode.isTextual -> values.add(currentNode.asText())
            currentNode.isDouble -> values.add(currentNode.asDouble())
            else -> values.add(currentNode.toString())
        }
    }
    return values
}

fun extractCursorFromResponse(result: MvcResult): String? {
    val responseBody = result.response.contentAsString
    val jsonNode = objectMapper.readTree(responseBody)
    return jsonNode.get("pageInfo")?.get("nextCursor")?.asText()
}

fun extractHasNextFromResponse(result: MvcResult): Boolean {
    val responseBody = result.response.contentAsString
    val jsonNode = objectMapper.readTree(responseBody)
    return jsonNode.get("pageInfo")?.get("hasNext")?.asBoolean() ?: false
}

fun assertDescendingOrder(values: List<Any>, fieldName: String) {
    if (values.size <= 1) return

    val intValues = values.mapNotNull {
        when (it) {
            is Int -> it
            is Long -> it.toInt()
            is String -> it.toIntOrNull()
            else -> null
        }
    }

    for (i in 0 until intValues.size - 1) {
        Assertions.assertTrue(
            intValues[i] >= intValues[i + 1],
            "${fieldName}이 내림차순으로 정렬되지 않았습니다: ${intValues[i]} >= ${intValues[i + 1]}"
        )
    }
}

fun assertDateDescendingOrder(values: List<Any>, fieldName: String) {
    if (values.size <= 1) return

    val dateStrings = values.mapNotNull { it as? String }

    for (i in 0 until dateStrings.size - 1) {
        Assertions.assertTrue(
            dateStrings[i] >= dateStrings[i + 1],
            "${fieldName}이 최신순으로 정렬되지 않았습니다: ${dateStrings[i]} >= ${dateStrings[i + 1]}"
        )
    }
}

fun assertDateAscendingOrder(values: List<Any>, fieldName: String) {
    if (values.size <= 1) return

    val dateStrings = values.mapNotNull { it as? String }

    for (i in 0 until dateStrings.size - 1) {
        Assertions.assertTrue(
            dateStrings[i] <= dateStrings[i + 1],
            "${fieldName}이 오름차순으로 정렬되지 않았습니다: ${dateStrings[i]} <= ${dateStrings[i + 1]}"
        )
    }
}
