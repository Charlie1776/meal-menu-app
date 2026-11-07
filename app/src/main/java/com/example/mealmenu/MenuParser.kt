package com.example.mealmenu

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

data class Meal(
    val name: String,
    val calories: Int,
    val protein: Int,
    var eaten: Boolean = false
)

/** A menu for one day. [date] is yyyy-MM-dd, or null for legacy bare-list files. */
data class Menu(val date: String?, val meals: List<Meal>)

class MenuFormatException(message: String) : Exception(message)

object MenuParser {
    private val DATE = Regex("""\d{4}-\d{2}-\d{2}""")

    /**
     * Accepts {"date":"YYYY-MM-DD","meals":[{name,calories,protein,eaten}]}
     * and, for backward compatibility, a bare list of meals.
     */
    fun parse(text: String): Menu {
        val root: JsonElement = try {
            JsonParser.parseString(text)
        } catch (e: Exception) {
            throw MenuFormatException("Not valid JSON")
        }
        return when {
            root.isJsonObject -> {
                val obj = root.asJsonObject
                val date = obj.get("date")
                    ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
                    ?: throw MenuFormatException("Missing \"date\"")
                if (!DATE.matches(date)) throw MenuFormatException("Bad date: $date")
                val meals = obj.get("meals")?.takeIf { it.isJsonArray }?.asJsonArray
                    ?: throw MenuFormatException("Missing \"meals\" array")
                Menu(date, parseMeals(meals))
            }
            root.isJsonArray -> Menu(null, parseMeals(root.asJsonArray))
            else -> throw MenuFormatException("Unexpected JSON root")
        }
    }

    private fun parseMeals(arr: JsonArray): List<Meal> {
        if (arr.size() == 0) throw MenuFormatException("No meals")
        return arr.mapIndexed { i, el ->
            if (!el.isJsonObject) throw MenuFormatException("Meal $i is not an object")
            val o: JsonObject = el.asJsonObject
            val name = str(o, "name", i).trim()
            if (name.isEmpty()) throw MenuFormatException("Meal $i has empty name")
            val cal = int(o, "calories", i)
            val pro = int(o, "protein", i)
            if (cal < 0 || pro < 0) throw MenuFormatException("Meal $i has negative values")
            val eaten = o.get("eaten")?.let {
                if (it.isJsonPrimitive && it.asJsonPrimitive.isBoolean) it.asBoolean
                else throw MenuFormatException("Meal $i \"eaten\" must be boolean")
            } ?: false
            Meal(name, cal, pro, eaten)
        }
    }

    private fun str(o: JsonObject, k: String, i: Int): String {
        val v = o.get(k)
        if (v == null || !v.isJsonPrimitive || !v.asJsonPrimitive.isString)
            throw MenuFormatException("Meal $i missing string \"$k\"")
        return v.asString
    }

    private fun int(o: JsonObject, k: String, i: Int): Int {
        val v = o.get(k)
        if (v == null || !v.isJsonPrimitive || !v.asJsonPrimitive.isNumber)
            throw MenuFormatException("Meal $i missing number \"$k\"")
        val d = v.asDouble
        if (d != Math.floor(d) || d > Int.MAX_VALUE) throw MenuFormatException("Meal $i \"$k\" must be an integer")
        return d.toInt()
    }
}
