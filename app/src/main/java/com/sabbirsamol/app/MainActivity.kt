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
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class ZikrItem(
    val id: String,
    var name: String,
    var count: Int,
    var target: Int
)

class MainActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("sabbirs_amol_db", Context.MODE_PRIVATE) }
    private val vibrator by lazy { getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

    private val screenStack = Stack<String>()
    private val zikrList = mutableListOf<ZikrItem>()
    private var activeZikrId: String = ""
    
    // ৪ টি প্রিমিয়াম থিম
    private var currentTheme = "কাবা থিম (ডার্ক গোল্ড)"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadAllData()
        openScreen("home")
    }

    private fun loadAllData() {
        currentTheme = prefs.getString("current_theme", "কাবা থিম (ডার্ক গোল্ড)") ?: "কাবা থিম (ডার্ক গোল্ড)"
        activeZikrId = prefs.getString("active_zikr_id", "") ?: ""

        val savedJson = prefs.getString("zikr_items_json", null)
        zikrList.clear()
        if (savedJson != null) {
            try {
                val array = JSONArray(savedJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    zikrList.add(
                        ZikrItem(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            count = obj.getInt("count"),
                            target = obj.getInt("target")
                        )
                    )
                }
            } catch (e: Exception) {
                initDefaultZikr()
            }
        } else {
            initDefaultZikr()
        }

        if (activeZikrId.isEmpty() || zikrList.none { it.id == activeZikrId }) {
            activeZikrId = zikrList.firstOrNull()?.id ?: "1"
        }
    }

    private fun initDefaultZikr() {
        zikrList.add(ZikrItem("1", "সুবহানাল্লাহ (SubhanAllah)", 0, 33))
        zikrList.add(ZikrItem("2", "আলহামদুলিল্লাহ (Alhamdulillah)", 0, 33))
        zikrList.add(ZikrItem("3", "আল্লাহু আকবার (Allahu Akbar)", 0, 34))
        zikrList.add(ZikrItem("4", "আস্তাগফিরুল্লাহ (Astaghfirullah)", 0, 100))
        zikrList.add(ZikrItem("5", "আয়াতুল কুরসি (Ayatul Kursi)", 0, 7))
        saveAllData()
    }

    private fun saveAllData() {
        val array = JSONArray()
        for (item in zikrList) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("count", item.count)
                put("target", item.target)
            }
            array.put(obj)
        }
        prefs.edit()
            .putString("zikr_items_json", array.toString())
            .putString("active_zikr_id", activeZikrId)
            .putString("current_theme", currentTheme)
            .apply()
    }

    private fun getActiveZikr(): ZikrItem {
        return zikrList.find { it.id == activeZikrId } ?: zikrList[0]
    }

    override fun onBackPressed() {
        if (screenStack.size > 1) {
            screenStack.pop()
            renderScreen(screenStack.peek())
        } else {
            AlertDialog.Builder(this)
                .setTitle("অ্যাপ বন্ধ করবেন?")
                .setMessage("আপনি কি Sabbir's Amol অ্যাপ থেকে বের হতে চান?")
                .setPositiveButton("হ্যাঁ") { _, _ -> finish() }
                .setNegativeButton("না", null)
                .show()
        }
    }

    private fun openScreen(name: String) {
        if (screenStack.isEmpty() || screenStack.peek() != name) {
            screenStack.push(name)
        }
        renderScreen(name)
    }

    private fun renderScreen(name: String) {
        when (name) {
            "home" -> showHomeScreen()
            "tasbih" -> showTasbihScreen()
            "zikr_list" -> showZikrListScreen()
            "amal" -> showAmalScreen()
            "quran" -> showQuranScreen()
            "settings" -> showSettingsScreen()
        }
    }

    private fun triggerVibration(duration: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    // ৪ কালারের ব্যাকগ্রাউন্ড
    private fun getThemeBackground(): GradientDrawable {
        return when (currentTheme) {
            "মদিনা থিম (এমারেল্ড গ্রিন)" -> GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#0E4D3A"), Color.parseColor("#146B52"), Color.parseColor("#072C21"))
            )
            "সুবহ-সাদিক থিম (রয়্যাল গোল্ড)" -> GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#422A0A"), Color.parseColor("#261704"), Color.parseColor("#120A01"))
            )
            "লাইলাতুল কদর (নাইট ব্লু)" -> GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#0F2027"), Color.parseColor("#203A43"), Color.parseColor("#2C5364"))
            )
            else -> GradientDrawable( // কাবা থিম (ডার্ক গোল্ড)
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#1B2A22"), Color.parseColor("#0B1410"), Color.parseColor("#040706"))
            )
        }
    }

    private fun getAccentColor(): Int {
        return when (currentTheme) {
            "মদিনা থিম (এমারেল্ড গ্রিন)" -> Color.parseColor("#10B981")
            "সুবহ-সাদিক থিম (রয়্যাল গোল্ড)" -> Color.parseColor("#F59E0B")
            "লাইলাতুল কদর (নাইট ব্লু)" -> Color.parseColor("#38BDF8")
            else -> Color.parseColor("#F5C358")
        }
    }

    private fun createNavBar(current: String): LinearLayout {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#0D1712"))
            setPadding(10, 16, 10, 20)
            gravity = Gravity.CENTER
        }

        val items = listOf(
            Triple("হোম", "home", "🏠"),
            Triple("তাসবিহ", "tasbih", "📿"),
            Triple("জিকির", "zikr_list", "📋"),
            Triple("আমল", "amal", "✅"),
            Triple("কুরআন", "quran", "📖"),
            Triple("থিম (৪)", "settings", "🎨")
        )

        val accent = getAccentColor()

        for (item in items) {
            val btn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                setOnClickListener { openScreen(item.second) }
            }

            val icon = TextView(this).apply {
                text = item.third
                textSize = 17f
                gravity = Gravity.CENTER
            }

            val text = TextView(this).apply {
                text = item.first
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(if (current == item.second) accent else Color.parseColor("#9CB7AA"))
                setTypeface(null, if (current == item.second) Typeface.BOLD else Typeface.NORMAL)
            }

            btn.addView(icon)
            btn.addView(text)
            nav.addView(btn)
        }
        return nav
    }

    // ১. হোম স্ক্রিন (কাবা ও সবুজ গম্বুজ ব্যাজসহ)
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
            setPadding(32, 36, 32, 32)
        }

        val accent = getAccentColor()

        // টপ আইকন হেডার
        val iconHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 16)
        }

        val kabaBadge = TextView(this).apply {
            text = "🕋"
            textSize = 36f
            gravity = Gravity.CENTER
        }

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(0, -2, 1f)
            lp.setMargins(14, 0, 14, 0)
            layoutParams = lp
        }

        val title = TextView(this).apply {
            text = "Sabbir's Amol"
            textSize = 24f
            setTextColor(accent)
            setTypeface(null, Typeface.BOLD)
        }

        val dateText = TextView(this).apply {
            val banglaDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("bn", "BD")).format(Date())
            text = "📍 সাতক্ষীরা\n$banglaDate"
            textSize = 13f
            setTextColor(Color.parseColor("#D0E2D8"))
            setPadding(0, 2, 0, 0)
        }
        titleBox.addView(title)
        titleBox.addView(dateText)

        val domeBadge = TextView(this).apply {
            text = "🕌"
            textSize = 36f
            gravity = Gravity.CENTER
        }

        iconHeader.addView(kabaBadge)
        iconHeader.addView(titleBox)
        iconHeader.addView(domeBadge)
        content.addView(iconHeader)

        // নামাজের সময় কার্ড
        val prayerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
            val bg = GradientDrawable()
            bg.setColor(Color.parseColor("#1B2E24"))
            bg.cornerRadius = 24f
            bg.setStroke(2, accent)
            background = bg
        }

        val pTitle = TextView(this).apply {
            text = "আজকের নামাজের সময়সূচী"
            textSize = 18f
            setTextColor(accent)
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
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }
            val time = TextView(this).apply {
                text = t.second
                setTextColor(Color.parseColor("#86EFAC"))
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
            }
            row.addView(name)
            row.addView(time)
            prayerCard.addView(row)
        }
        content.addView(prayerCard)

        val btnGoTasbih = Button(this).apply {
            text = "📿 ডিজিটাল তাসবিহ শুরু করুন"
            setBackgroundColor(accent)
            setTextColor(Color.BLACK)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 24, 0, 0)
            layoutParams = lp
            setOnClickListener { openScreen("tasbih") }
        }
        content.addView(btnGoTasbih)

        val btnGoList = Button(this).apply {
            text = "📋 জিকির তালিকা ও কাস্টম জিকির যোগ"
            setBackgroundColor(Color.parseColor("#264536"))
            setTextColor(Color.WHITE)
            textSize = 15f
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 16, 0, 0)
            layoutParams = lp
            setOnClickListener { openScreen("zikr_list") }
        }
        content.addView(btnGoList)

        scroll.addView(content)
        root.addView(scroll)
        root.addView(createNavBar("home"))
        setContentView(root)
    }

    // ২. সম্পূর্ণ ফুল-স্ক্রিন ট্যাপ তাসবিহ
    private fun showTasbihScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getThemeBackground()
        }

        val zikr = getActiveZikr()
        val accent = getAccentColor()

        // টপ বার
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 20, 24, 12)
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "ডিজিটাল তাসবিহ"
            textSize = 20f
            setTextColor(accent)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }

        val btnReset = Button(this).apply {
            text = "রিসেট (০)"
            textSize = 12f
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("রিসেট করবেন?")
                    .setMessage("${zikr.name} এর বর্তমান গণনা শূন্য করতে চান?")
                    .setPositiveButton("হ্যাঁ") { _, _ ->
                        zikr.count = 0
                        saveAllData()
                        showTasbihScreen()
                    }
                    .setNegativeButton("না", null)
                    .show()
            }
        }
        topBar.addView(title)
        topBar.addView(btnReset)
        root.addView(topBar)

        // ফুল স্ক্রিন ট্যাপ এরিয়া (সম্পূর্ণ স্ক্রিন তাসবিহ)
        val fullScreenTap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            gravity = Gravity.CENTER
            setPadding(32, 10, 32, 10)
        }

        // সেন্টার ইসলামিক আইকন ব্যাজ
        val centerIcon = TextView(this).apply {
            text = if (currentTheme.contains("মদিনা")) "🕌\n(মদিনা শরীফ)" else "🕋\n(কাবা শরীফ)"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(accent)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 10)
        }
        fullScreenTap.addView(centerIcon)

        val zikrNameText = TextView(this).apply {
            text = zikr.name
            textSize = 22f
            setTextColor(Color.parseColor("#9AE6B4"))
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 10)
        }

        val countDisplay = TextView(this).apply {
            text = "${zikr.count}"
            textSize = 85f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        val targetInfo = TextView(this).apply {
            text = "টার্গেট: ${zikr.target} বার"
            textSize = 17f
            setTextColor(accent)
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 20)
        }

        val tapGuide = TextView(this).apply {
            text = "👆 স্ক্রিনের যেকোনো জায়গায় ট্যাপ করে গণনা করুন"
            textSize = 13f
            setTextColor(Color.parseColor("#718096"))
            gravity = Gravity.CENTER
        }

        fullScreenTap.addView(zikrNameText)
        fullScreenTap.addView(countDisplay)
        fullScreenTap.addView(targetInfo)
        fullScreenTap.addView(tapGuide)

        // ফুল স্ক্রিন ট্যাপ লজিক
        fullScreenTap.setOnClickListener {
            if (zikr.count < zikr.target) {
                zikr.count++
                triggerVibration(40)
                saveAllData()
                countDisplay.text = "${zikr.count}"

                if (zikr.count >= zikr.target) {
                    triggerVibration(500)
                    AlertDialog.Builder(this)
                        .setTitle("মাশাআল্লাহ!")
                        .setMessage("${zikr.name} নির্ধারিত টার্গেট (${zikr.target} বার) সফলভাবে পূর্ণ হয়েছে।")
                        .setPositiveButton("আবার শুরু") { _, _ ->
                            zikr.count = 0
                            saveAllData()
                            showTasbihScreen()
                        }
                        .setNegativeButton("ঠিক আছে", null)
                        .show()
                }
            } else {
                Toast.makeText(this, "টার্গেট পূর্ণ হয়েছে! রিসেট বাটন চাপুন।", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(fullScreenTap)

        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 6, 24, 16)
        }

        val btnToList = Button(this).apply {
            text = "📋 জিকির পরিবর্তন / তালিকা"
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            setBackgroundColor(Color.parseColor("#264536"))
            setTextColor(Color.WHITE)
            setOnClickListener { openScreen("zikr_list") }
        }
        bottomBar.addView(btnToList)
        root.addView(bottomBar)

        root.addView(createNavBar("tasbih"))
        setContentView(root)
    }

    // ৩. জিকির তালিকা
    private fun showZikrListScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getThemeBackground()
        }

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
        }

        val accent = getAccentColor()

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 20)
        }

        val title = TextView(this).apply {
            text = "জিকির তালিকা ও কাস্টমাইজেশন"
            textSize = 20f
            setTextColor(accent)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        headerRow.addView(title)
        content.addView(headerRow)

        val btnAddZikr = Button(this).apply {
            text = "➕ নতুন জিকির যোগ করুন (Add Zikr)"
            setBackgroundColor(accent)
            setTextColor(Color.BLACK)
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 0, 0, 24)
            layoutParams = lp
            setOnClickListener { showAddZikrDialog() }
        }
        content.addView(btnAddZikr)

        for (item in zikrList) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24, 20, 24, 20)
                val bg = GradientDrawable()
                bg.setColor(if (item.id == activeZikrId) Color.parseColor("#264536") else Color.parseColor("#1B2E24"))
                bg.cornerRadius = 20f
                bg.setStroke(if (item.id == activeZikrId) 3 else 1, if (item.id == activeZikrId) accent else Color.parse
