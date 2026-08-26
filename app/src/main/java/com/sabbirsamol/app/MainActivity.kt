package com.sabbirsamol.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("sabbir_amol_pref", Context.MODE_PRIVATE) }
    private val vibrator by lazy { getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

    private val screenStack = Stack<String>()

    private val zikrList = mutableListOf(
        "সুবহানাল্লাহ (SubhanAllah)",
        "আলহামদুলিল্লাহ (Alhamdulillah)",
        "লা ইলাহা ইল্লাল্লাহ (La Ilaha Illallah)",
        "আল্লাহু আকবার (Allahu Akbar)",
        "আস্তাগফিরুল্লাহ (Astaghfirullah)",
        "সাল্লাল্লাহু আলাইহি ওয়াসাল্লাম (Darood)"
    )

    private var currentZikr = "সুবহানাল্লাহ (SubhanAllah)"
    private var tasbihCount = 0
    private var tasbihTarget = 33
    private var currentTheme = "কাবা থিম" // "কাবা থিম" অথবা "মদিনা থিম"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadData()
        openScreen("home")
    }

    private fun loadData() {
        tasbihCount = prefs.getInt("tasbih_count", 0)
        tasbihTarget = prefs.getInt("tasbih_target", 33)
        currentZikr = prefs.getString("current_zikr", zikrList[0]) ?: zikrList[0]
        currentTheme = prefs.getString("current_theme", "কাবা থিম") ?: "কাবা থিম"
    }

    private fun saveData() {
        prefs.edit()
            .putInt("tasbih_count", tasbihCount)
            .putInt("tasbih_target", tasbihTarget)
            .putString("current_zikr", currentZikr)
            .putString("current_theme", currentTheme)
            .apply()
    }

    // ব্যাক বাটন হ্যান্ডলিং: অ্যাপ থেকে বের না হয়ে আগের স্ক্রিনে যাবে
    override fun onBackPressed() {
        if (screenStack.size > 1) {
            screenStack.pop() // বর্তমান স্ক্রিন রিমুভ
            renderScreen(screenStack.peek(), false)
        } else {
            AlertDialog.Builder(this)
                .setTitle("অ্যাপ বন্ধ করতে চান?")
                .setMessage("আপনি কি অ্যাপ থেকে বের হয়ে যেতে চান?")
                .setPositiveButton("হ্যাঁ") { _, _ -> finish() }
                .setNegativeButton("না", null)
                .show()
        }
    }

    private fun openScreen(name: String) {
        if (screenStack.isEmpty() || screenStack.peek() != name) {
            screenStack.push(name)
        }
        renderScreen(name, true)
    }

    private fun renderScreen(name: String, isNew: Boolean) {
        when (name) {
            "home" -> showHomeScreen()
            "tasbih" -> showTasbihScreen()
            "amal" -> showAmalScreen()
            "quran" -> showQuranScreen()
            "settings" -> showSettingsScreen()
        }
    }

    // ভাইব্রেশন মেথড
    private fun triggerVibration(duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    // থিম ব্যাকগ্রাউন্ড কালার
    private fun getThemeBackground(): GradientDrawable {
        return if (currentTheme == "কাবা থিম") {
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#1B2A22"), Color.parseColor("#0C1510"), Color.parseColor("#050A08"))
            )
        } else {
            // মদিনা থিম (হালকা সোনালী ও সবুজ গ্রেডিয়েন্ট)
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#0D4E3A"), Color.parseColor("#1A7A5E"), Color.parseColor("#083829"))
            )
        }
    }

    // বটম নেভিগেশন বার
    private fun createNavBar(current: String): LinearLayout {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#121F18"))
            setPadding(10, 16, 10, 20)
            gravity = Gravity.CENTER
        }

        val items = listOf(
            Triple("হোম", "home", "🏠"),
            Triple("তাসবিহ", "tasbih", "📿"),
            Triple("আমল", "amal", "📋"),
            Triple("কুরআন", "quran", "📖"),
            Triple("থিম/সেটিংস", "settings", "⚙️")
        )

        for (item in items) {
            val btn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                setOnClickListener { openScreen(item.second) }
            }

            val icon = TextView(this).apply {
                text = item.third
                textSize = 18f
                gravity = Gravity.CENTER
            }

            val text = TextView(this).apply {
                text = item.first
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(if (current == item.second) Color.parseColor("#E0A938") else Color.parseColor("#A0B8AD"))
                setTypeface(null, if (current == item.second) Typeface.BOLD else Typeface.NORMAL)
            }

            btn.addView(icon)
            btn.addView(text)
            nav.addView(btn)
        }
        return nav
    }

    // ১. হোম স্ক্রিন (নামাজের সময়সূচী ও ব্যানার)
    private fun showHomeScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getThemeBackground()
        }

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 32)
        }

        // টপ হেডার
        val title = TextView(this).apply {
            text = "Sabbir's Amol"
            textSize = 26f
            setTextColor(Color.parseColor("#F5C358"))
            setTypeface(null, Typeface.BOLD)
        }
        val dateText = TextView(this).apply {
            val banglaDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("bn", "BD")).format(Date())
            text = "📍 সাতক্ষীরা | $banglaDate"
            textSize = 14f
            setTextColor(Color.parseColor("#E0EAE4"))
            setPadding(0, 4, 0, 24)
        }
        content.addView(title)
        content.addView(dateText)

        // নামাজের সময়সূচী কার্ড (ইসলামিক ফাউন্ডেশন ভিত্তিক)
        val prayerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 24, 30, 24)
            val bg = GradientDrawable()
            bg.setColor(Color.parseColor("#22382C"))
            bg.cornerRadius = 24f
            bg.setStroke(2, Color.parseColor("#345846"))
            background = bg
        }

        val pTitle = TextView(this).apply {
            text = "আজকের নামাজের সময়সূচী (সাতক্ষীরা)"
            textSize = 18f
            setTextColor(Color.parseColor("#F5C358"))
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        prayerCard.addView(pTitle)

        val times = listOf(
            "ফজর" to "০৪:২৫ - ০৫:৪৩ মি.",
            "যোহর" to "১২:০৮ - ০৪:৩৬ মি.",
            "আসর" to "০৪:৩৭ - ০৬:২৬ মি.",
            "মাগরিব (ইফতার)" to "০৬:২৭ - ০৭:৪৩ মি.",
            "এশা" to "০৭:৪৪ - ০৪:২৪ মি.",
            "সাহরির শেষ সময়" to "০৪:১৯ মি."
        )

        for (t in times) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }
            val name = TextView(this).apply {
                text = t.first
                setTextColor(Color.WHITE)
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }
            val time = TextView(this).apply {
                text = t.second
                setTextColor(Color.parseColor("#9AE6B4"))
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
            }
            row.addView(name)
            row.addView(time)
            prayerCard.addView(row)
        }
        content.addView(prayerCard)

        // দ্রুত অ্যাক্সেস বাটন
        val quickTasbih = Button(this).apply {
            text = "📿 ডিজিটাল তাসবিহ শুরু করুন"
            setBackgroundColor(Color.parseColor("#E0A938"))
            setTextColor(Color.BLACK)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 32, 0, 0)
            layoutParams = lp
            setOnClickListener { openScreen("tasbih") }
        }
        content.addView(quickTasbih)

        scroll.addView(content)
        root.addView(scroll)
        root.addView(createNavBar("home"))
        setContentView(root)
    }

    // ২. ফুল স্ক্রিন ডিজিটাল তাসবিহ স্ক্রিন (যেকোনো জায়গায় ট্যাপ করে গণনা)
    private fun showTasbihScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getThemeBackground()
        }

        // টপ বার (হেডার এবং রিসেট/টার্গেট বাটন)
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 24, 24, 16)
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "ডিজিটাল তাসবিহ"
            textSize = 20f
            setTextColor(Color.parseColor("#F5C358"))
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }

        val btnReset = Button(this).apply {
            text = "রিসেট"
            textSize = 12f
            setOnClickListener {
                tasbihCount = 0
                saveData()
                showTasbihScreen()
            }
        }
        topBar.addView(title)
        topBar.addView(btnReset)
        root.addView(topBar)

        // ফুল স্ক্রিন ট্যাপ এরিয়া
        val tapArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            gravity = Gravity.CENTER
            setPadding(40, 20, 40, 20)
        }

        val zikrNameView = TextView(this).apply {
            text = currentZikr
            textSize = 22f
            setTextColor(Color.parseColor("#9AE6B4"))
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 20)
        }

        val countView = TextView(this).apply {
            text = "$tasbihCount"
            textSize = 85f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        val targetView = TextView(this).apply {
            text = "টার্গেট: $tasbihTarget বার"
            textSize = 16f
            setTextColor(Color.parseColor("#E0EAE4"))
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 30)
        }

        val hint = TextView(this).apply {
            text = "👆 স্ক্রিনের যেকোনো জায়গায় ট্যাপ করুন"
            textSize = 14f
            setTextColor(Color.parseColor("#718096"))
            gravity = Gravity.CENTER
        }

        tapArea.addView(zikrNameView)
        tapArea.addView(countView)
        tapArea.addView(targetView)
        tapArea.addView(hint)

        // পুরো স্ক্রিনে ট্যাপ লজিক
        tapArea.setOnClickListener {
            if (tasbihCount < tasbihTarget) {
                tasbihCount++
                triggerVibration(40) // হালকা ভাইব্রেশন প্রতি ট্যাপে
                saveData()
                countView.text = "$tasbihCount"

                if (tasbihCount >= tasbihTarget) {
                    triggerVibration(400) // টার্গেট পূরণে জোরালো ভাইব্রেশন
                    AlertDialog.Builder(this)
                        .setTitle("মাশাআল্লাহ!")
                        .setMessage("আপনার ধার্যকৃত টার্গেট ($tasbihTarget বার) সফলভাবে পূর্ণ হয়েছে।")
                        .setPositiveButton("নতুন করে শুরু") { _, _ ->
                            tasbihCount = 0
                            saveData()
                            showTasbihScreen()
                        }
                        .setNegativeButton("বন্ধ রাখুন", null)
                        .show()
                }
            } else {
                Toast.makeText(this, "টার্গেট পূর্ণ হয়েছে! পুনরায় রিসেট করুন।", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(tapArea)

        // কাস্টমাইজেশন বাটন বার (জিকির নির্বাচন ও টার্গেট সেট)
        val controlBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 10, 24, 20)
            gravity = Gravity.CENTER
        }

        val btnChangeZikr = Button(this).apply {
            text = "জিকির পরিবর্তন"
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("জিকির নির্বাচন করুন")
                    .setItems(zikrList.toTypedArray()) { _, which ->
                        currentZikr = zikrList[which]
                        tasbihCount = 0
                        saveData()
                        showTasbihScreen()
                    }
                    .show()
            }
        }

        val btnSetTarget = Button(this).apply {
            text = "টার্গেট সংখ্যা"
            val lp = LinearLayout.LayoutParams(0, -2, 1f)
            lp.setMargins(16, 0, 0, 0)
            layoutParams = lp
            setOnClickListener {
                val input = EditText(this@MainActivity).apply {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    setText("$tasbihTarget")
                }
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("টার্গেট সংখ্যা নির্ধারণ করুন")
                    .setView(input)
                    .setPositiveButton("সংরক্ষণ") { _, _ ->
                        val v = input.text.toString().toIntOrNull() ?: 33
                        tasbihTarget = if (v > 0) v else 33
                        tasbihCount = 0
                        saveData()
                        showTasbihScreen()
                    }
                    .setNegativeButton("বাতিল", null)
                    .show()
            }
        }

        controlBar.addView(btnChangeZikr)
        controlBar.addView(btnSetTarget)
        root.addView(controlBar)

        root.addView(createNavBar("tasbih"))
        setContentView(root)
    }

    // ৩. দৈনিক আমল ও সকাল-সন্ধ্যার জিকির স্ক্রিন
    private fun showAmalScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getThemeBackground()
        }

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val h = TextView(this).apply {
            text = "দৈনিক আমল ও সকাল-সন্ধ্যার জিকির"
            textSize = 20f
            setTextColor(Color.parseColor("#F5C358"))
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 20)
        }
        content.addView(h)

        val amols = listOf(
            "সকালের মাসনুন দোয়া ও আয়াতুল কুরসি",
            "ফজর নামাজ আদায়",
            "ইশরাক নামাজ আদায়",
            "যোহর নামাজ আদায়",
            "আসর নামাজ আদায়",
            "সন্ধ্যার মাসনুন জিকির ও ৩ কুল",
            "মাগরিব নামাজ আদায়",
            "এশা ও বিতর নামাজ আদায়",
            "সুরা মুলক তিলাওয়াত (ঘুমানোর আগে)"
        )

        val dayKey = "day_" + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        for ((idx, am) in amols.withIndex()) {
            val cb = CheckBox(this).apply {
                text = am
                textSize = 16f
                setTextColor(Color.WHITE)
                isChecked = prefs.getBoolean("${dayKey}_$idx", false)
                setOnCheckedChangeListener { _, isChecked ->
                    prefs.edit().putBoolean("${dayKey}_$idx", isChecked).apply()
                }
            }
            content.addView(cb)
        }

        scroll.addView(content)
        root.addView(scroll)
        root.addView(createNavBar("amal"))
        setContentView(root)
    }

    // ৪. কুরআন ও মাসনুন আমলের রিসোর্স স্ক্রিন
    private fun showQuranScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getThemeBackground()
        }

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val h = TextView(this).apply {
            text = "আল-কুরআন ও ইসলামিক রিসোর্স"
            textSize = 22f
            setTextColor(Color.parseColor("#F5C358"))
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 20)
        }
        content.addView(h)

        val links = listOf(
            "📖 সম্পূর্ণ আল-কুরআন (অনলাইন পড়ুন ও অডিও)" to "https://quran.com/bn",
            "🤲 সকাল ও সন্ধ্যার মাসনুন দোয়া সমূহ" to "https://sunnah.com/hisn",
            "🕌 ইসলামিক ফাউন্ডেশন বাংলাদেশ" to "http://www.islamicfoundation.gov.bd"
        )

        for (item in links) {
            val btn = Button(this).apply {
                text = item.first
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#22382C"))
                textSize = 15f
                val lp = LinearLayout.LayoutParams(-1, -2)
                lp.setMargins(0, 12, 0, 12)
                layoutParams = lp
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.second))
                    startActivity(intent)
                }
            }
            content.addView(btn)
        }

        scroll.addView(content)
        root.addView(scroll)
        root.addView(createNavBar("quran"))
        setContentView(root)
    }

    // ৫. থিম ও সেটিংস স্ক্রিন (কাবা ও মদিনা থিম সুইচ)
    private fun showSettingsScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getThemeBackground()
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }

        val h = TextView(this).apply {
            text = "অ্যাপ থিম ও সেটিংস"
            textSize = 22f
            setTextColor(Color.parseColor("#F5C358"))
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        }
        content.addView(h)

        val themeTitle = TextView(this).apply {
            text = "থিম পরিবর্তন করুন (বর্তমান: $currentTheme):"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 16)
        }
        content.addView(themeTitle)

        val btnKaba = Button(this).apply {
            text = "🕋 কাবা শরীফ থিম (ডার্ক গ্রিন ও ব্ল্যাক)"
            setBackgroundColor(Color.parseColor("#1B2A22"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                currentTheme = "কাবা থিম"
                saveData()
                showSettingsScreen()
            }
        }
        content.addView(btnKaba)

        val btnMadina = Button(this).apply {
            text = "🕌 মদিনা শরীফ থিম (এমেরাল্ড গ্রিন)"
            setBackgroundColor(Color.parseColor("#0D4E3A"))
            setTextColor(Color.WHITE)
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 16, 0, 0)
            layoutParams = lp
            setOnClickListener {
                currentTheme = "মদিনা থিম"
                saveData()
                showSettingsScreen()
            }
        }
        content.addView(btnMadina)

        root.addView(content)
        root.addView(createNavBar("settings"))
        setContentView(root)
    }
}
