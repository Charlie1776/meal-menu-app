package com.example.mealmenu

import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.gson.Gson
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

private data class EatenState(val date: String, val menuDate: String?, val eaten: List<Boolean>)

class MainActivity : AppCompatActivity() {

    private val gson = Gson()
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val menuFile by lazy { File(filesDir, "today_menu.json") }
    private val stateFile by lazy { File(filesDir, "eaten_state.json") }
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }

    private lateinit var root: LinearLayout
    private lateinit var status: TextView

    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        buttons.addView(Button(this).apply { text = "Refresh"; setOnClickListener { refresh() } })
        buttons.addView(Button(this).apply { text = "Menu URL"; setOnClickListener { editUrl() } })
        status = TextView(this).apply { textSize = 12f; setPadding(0, 8, 0, 16) }
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        outer.addView(buttons)
        outer.addView(status)
        outer.addView(root)
        setContentView(ScrollView(this).apply { addView(outer) })
    }

    override fun onResume() {
        super.onResume()
        render()      // show cached copy immediately (offline-safe)
        refresh()     // then try the cloud
    }

    private fun editUrl() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            setText(prefs.getString("menu_url", ""))
            hint = "https://…/today_menu.json"
        }
        AlertDialog.Builder(this).setTitle("Menu URL (HTTPS)").setView(input)
            .setPositiveButton("Save") { _, _ ->
                val u = input.text.toString().trim()
                if (u.isEmpty() || u.startsWith("https://")) {
                    prefs.edit().putString("menu_url", u).apply(); refresh()
                } else status.text = "URL must start with https://"
            }.setNegativeButton("Cancel", null).show()
    }

    private fun refresh() {
        val url = prefs.getString("menu_url", "").orEmpty()
        if (url.isEmpty()) { status.text = "No menu URL set — showing local file"; return }
        status.text = "Checking for new menu…"
        io.execute {
            val msg = try {
                val sep = if (url.contains('?')) '&' else '?'
                val c = URL("$url${sep}t=${System.currentTimeMillis()}").openConnection() as HttpURLConnection
                c.connectTimeout = 10000; c.readTimeout = 10000
                c.setRequestProperty("Cache-Control", "no-cache")
                val code = c.responseCode
                if (code != 200) throw Exception("HTTP $code")
                val body = c.inputStream.bufferedReader().use { it.readText() }
                if (body.length > 200_000) throw Exception("Response too large")
                MenuParser.parse(body)  // validate before replacing cache
                val tmp = File(filesDir, "today_menu.json.tmp")
                tmp.writeText(body)
                if (!tmp.renameTo(menuFile)) { menuFile.writeText(body); tmp.delete() }
                "Updated from cloud"
            } catch (e: MenuFormatException) {
                "Cloud menu invalid (${e.message}) — kept cached menu"
            } catch (e: Exception) {
                "Offline or fetch failed (${e.message}) — showing cached menu"
            }
            main.post { status.text = msg; render() }
        }
    }

    private fun loadMenu(): Pair<Menu, String?> {
        if (!menuFile.exists()) return sample() to "No menu file yet — sample menu"
        return try {
            MenuParser.parse(menuFile.readText()) to null
        } catch (e: Exception) {
            sample() to "Saved menu unreadable (${e.message}) — sample menu"
        }
    }

    private fun sample() = Menu(null, listOf(
        Meal("Breakfast: Greek yogurt, berries, granola", 420, 30),
        Meal("Lunch: Grilled chicken salad", 550, 45),
        Meal("Snack: Protein shake", 200, 25),
        Meal("Dinner: Salmon, rice, broccoli", 650, 40)
    ))

    private fun render() {
        val today = today()
        val (menu, warn) = loadMenu()
        val meals = menu.meals.map { it.copy() }
        // Eaten state resets on a new day or when the menu changes.
        val state = try { gson.fromJson(stateFile.readText(), EatenState::class.java) } catch (e: Exception) { null }
        if (state != null && state.date == today && state.menuDate == menu.date && state.eaten.size == meals.size) {
            meals.forEachIndexed { i, m -> m.eaten = state.eaten[i] }
        } else {
            meals.forEach { it.eaten = false }
            save(today, menu.date, meals)
        }

        root.removeAllViews()
        val label = when {
            menu.date == null || menu.date == today -> "Today's Menu — $today"
            menu.date > today -> "Upcoming Menu — ${menu.date}"
            else -> "Menu from ${menu.date} (no newer menu yet)"
        }
        root.addView(TextView(this).apply {
            text = label; textSize = 22f; setTypeface(null, Typeface.BOLD); setPadding(0, 0, 0, 8)
        })
        root.addView(TextView(this).apply {
            text = "Total: ${meals.sumOf { it.calories }} cal · ${meals.sumOf { it.protein }}g protein" +
                (warn?.let { "\n$it" } ?: "")
            textSize = 14f; setPadding(0, 0, 0, 24)
        })
        meals.forEachIndexed { index, meal ->
            val card = CardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16 }
                radius = 16f; cardElevation = 4f
            }
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 24) }
            val title = TextView(this).apply { text = meal.name; textSize = 18f; setTypeface(null, Typeface.BOLD) }
            title.paint.isStrikeThruText = meal.eaten
            row.addView(title)
            row.addView(TextView(this).apply { text = "${meal.calories} cal · ${meal.protein}g protein"; textSize = 14f })
            row.addView(CheckBox(this).apply {
                text = "Eaten"; isChecked = meal.eaten
                setOnCheckedChangeListener { _, checked ->
                    meals[index].eaten = checked
                    save(today(), menu.date, meals)
                    title.paint.isStrikeThruText = checked
                    title.invalidate()
                }
            })
            card.addView(row)
            root.addView(card)
        }
    }

    private fun save(today: String, menuDate: String?, meals: List<Meal>) {
        stateFile.writeText(gson.toJson(EatenState(today, menuDate, meals.map { it.eaten })))
    }
}
