package com.example.mealmenu

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Meal(
    val name: String,
    val calories: Int,
    val protein: Int,
    var eaten: Boolean = false
)

class MainActivity : AppCompatActivity() {

    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val today = dateFormat.format(Date())
    private val stateFile: File by lazy { File(filesDir, "eaten_state.json") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        val scroll = android.widget.ScrollView(this).apply {
            addView(root)
        }
        setContentView(scroll)

        val header = TextView(this).apply {
            text = "Today's Menu — $today"
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        }
        root.addView(header)

        val meals = loadMeals()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(container)

        meals.forEachIndexed { index, meal ->
            val card = CardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }
                radius = 16f
                cardElevation = 4f
            }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 24, 24, 24)
            }
            val title = TextView(this).apply {
                text = meal.name
                textSize = 18f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val macros = TextView(this).apply {
                text = "${meal.calories} cal · ${meal.protein}g protein"
                textSize = 14f
            }
            val box = CheckBox(this).apply {
                text = "Eaten"
                isChecked = meal.eaten
                setOnCheckedChangeListener { _, checked ->
                    meals[index].eaten = checked
                    saveState(meals)
                    title.paint.isStrikeThruText = checked
                }
                title.paint.isStrikeThruText = meal.eaten
            }
            row.addView(title)
            row.addView(macros)
            row.addView(box)
            card.addView(row)
            container.addView(card)
        }

        if (loadSavedDate() != today) {
            meals.forEach { it.eaten = false }
            saveState(meals)
        }
    }

    private fun loadMeals(): List<Meal> {
        val file = File(filesDir, "today_menu.json")
        return if (file.exists()) {
            val type = object : TypeToken<List<Meal>>() {}.type
            gson.fromJson(file.readText(), type)
        } else {
            listOf(
                Meal("Breakfast: Greek yogurt, berries, granola", 420, 30),
                Meal("Lunch: Grilled chicken salad", 550, 45),
                Meal("Snack: Protein shake", 200, 25),
                Meal("Dinner: Salmon, rice, broccoli", 650, 40)
            )
        }
    }

    private fun loadSavedDate(): String? {
        val f = File(filesDir, "state_date.txt")
        return if (f.exists()) f.readText().trim() else null
    }

    private fun saveState(meals: List<Meal>) {
        stateFile.writeText(gson.toJson(meals.map { it.eaten }))
        File(filesDir, "state_date.txt").writeText(today)
    }
}
