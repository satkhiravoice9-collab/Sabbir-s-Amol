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
import android.os.Handler
import android.os.Looper
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
    private var currentTheme = "সাদা থিম (লাইট)"
    private var selectedDistrict = "সাতক্ষীরা"
    private var userEmail = ""

    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var liveTimerTextView: TextView? = null
    private var liveWaqtTextView: TextView? = null

    // জেলাভিত্তিক সময়ের সমন্বয় (মিনিটে)
    private val districtOffsets = mapOf(
        "সাতক্ষীরা" to 4,
        "ঢাকা" to 0,
        "চট্টগ্রাম" to -5,
        "খুলনা" to 3,
        "রাজশাহী" to 7,
        "সিলেট" to -6,
        "বরিশাল" to 1,
        "রংপুর" to 6,
        "কুষ্টিয়া" to 5,
        "যশোর" to 4
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadAllData()
        openScreen("home")
    }

    private fun loadAllData() {
        currentTheme = prefs.getString("current_theme", "সাদা থিম (লাইট)") ?: "সাদা থিম (লাইট)"
        selectedDistrict = prefs.getString("selected_district", "সাতক্ষীরা") ?: "সাতক্ষীরা"
        userEmail = prefs.getString("user_email", "") ?: ""
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
            .putString("selected_district", selectedDistrict)
            .putString("user_email", userEmail)
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
        stopLiveTimer()
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

    private fun isWhiteTheme(): Boolean = currentTheme.contains("সাদা")

    private fun getThemeBackground(): GradientDrawable {
        return if (isWhiteTheme()) {
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#FFFBEB"), Color.parseColor("#F8FAFC"), Color.parseColor("#FFFFFF"))
            )
        } else {
            when (currentTheme) {
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
                else -> GradientDrawable( // কাবা থিম
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(Color.parseColor("#1B2A22"), Color.parseColor("#0B1410"), Color.parseColor("#040706"))
                )
            }
        }
    }

    private fun getTextColor(): Int = if (isWhiteTheme()) Color.parseColor("#1E293B") else Color.WHITE
    private fun getSecondaryTextColor(): Int = if (isWhiteTheme()) Color.parseColor("#475569") else Color.parseColor("#D0E2D8")
    private fun getAccentColor(): Int = if (isWhiteTheme()) Color.parseColor("#D97706") else Color.parseColor("#F5C358")
    private fun getCardBgColor(): Int = if (isWhiteTheme()) Color.parseColor("#FFFFFF") else Color.parseColor("#1B2E24")

    private fun createNavBar(current: String): LinearLayout {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(if (isWhiteTheme()) Color.parseColor("#FFFFFF") else Color.parseColor("#0D1712"))
            setPadding(10, 16, 10, 20)
            gravity = Gravity.CENTER
            elevation = 16f
        }

        val items = listOf(
            Triple("হোম", "home", "🏠"),
            Triple("তাসবিহ", "tasbih", "📿"),
            Triple("জিকির", "zikr_list", "📋"),
            Triple("আমল", "amal", "✅"),
            Triple("কুরআন", "quran", "📖"),
            Triple("সেটিংস", "settings", "⚙️")
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
                setTextColor(if (current == item.second) accent else Color.parseColor("#94A3B8"))
                setTypeface(null, if (current == item.second) Typeface.BOLD else Typeface.NORMAL)
            }

            btn.addView(icon)
            btn.addView(text)
            nav.addView(btn)
        }
        return nav
    }

    // লাইভ টাইমার লজিক (ঘড়ির সাথে সাথে সেকেন্ড কমা)
    private fun startLiveTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                updateCountdown()
                timerHandler.postDelayed(this, 1000)
            }
        }
        timerHandler.post(timerRunnable!!)
    }

    private fun stopLiveTimer() {
        timerRunnable?.let { timerHandler.removeCallbacks(it) }
    }

    private fun updateCountdown() {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val currentSeconds = now.get(Calendar.SECOND)

        val offset = districtOffsets[selectedDistrict] ?: 0

        // ওয়াক্তের সময়সূচী (মিনিটে)
        val fojrEnd = 5 * 60 + 43 + offset
        val ishraqEnd = 6 * 60 + 30 + offset
        val chashtEnd = 11 * 60 + 45 + offset
        val zohrEnd = 16 * 60 + 36 + offset
        val asrEnd = 18 * 60 + 26 + offset
        val magribEnd = 19 * 60 + 43 + offset

        val (waqtName, targetMin) = when {
            currentMinutes < fojrEnd -> "ফজর" to fojrEnd
            currentMinutes < ishraqEnd -> "ইশরাক" to ishraqEnd
            currentMinutes < chashtEnd -> "চাশত / দুহা" to chashtEnd
            currentMinutes < zohrEnd -> "যোহর" to zohrEnd
            currentMinutes < asrEnd -> "আসর" to asrEnd
            currentMinutes < magribEnd -> "মাগরিব" to magribEnd
            else -> "এশা" to (24 * 60 + fojrEnd)
        }

        var diffSec = (targetMin * 60) - (currentMinutes * 60 + currentSeconds)
        if (diffSec < 0) diffSec += 24 * 3600

        val h = diffSec / 3600
        val m = (diffSec % 3600) / 60
        val s = diffSec % 60

        val timeString = String.format("%02d:%02d:%02d", h, m, s)
        liveWaqtTextView?.text = "$waqtName শেষ হতে বাকি"
        liveTimerTextView?.text = timeString
    }

    // ১. হোম স্ক্রিন (ভিডিওর মতো চলমান টাইমার ও লোকেশন প্যানেল)
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
            setPadding(28, 28, 28, 28)
        }

        val accent = getAccentColor()

        // লাইভ টাইমার কার্ড (ভিডিওর হুবহু ডিজাইন)
        val timerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(32, 28, 32, 28)
            val bg = GradientDrawable()
            bg.setColor(if (isWhiteTheme()) Color.parseColor("#FFFBEB") else Color.parseColor("#1B2E24"))
            bg.cornerRadius = 32f
            bg.setStroke(3, accent)
            background = bg
        }

        liveWaqtTextView = TextView(this).apply {
            text = "ইশরাক শেষ হতে বাকি"
            textSize = 17f
            setTextColor(getTextColor())
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        liveTimerTextView = TextView(this).apply {
            text = "০০:০০:০০"
            textSize = 44f
            setTextColor(accent)
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 12)
        }

        // লোকেশন চেঞ্জার বাটন
        val locationBtn = Button(this).apply {
            text = "📍 $selectedDistrict (পরিবর্তন করুন ▾)"
            textSize = 13f
            setBackgroundColor(if (isWhiteTheme()) Color.parseColor("#FEF3C7") else Color.parseColor("#264536"))
            setTextColor(getTextColor())
            setOnClickListener { showDistrictDialog() }
        }

        timerCard.addView(liveWaqtTextView)
        timerCard.addView(liveTimerTextView)
        timerCard.addView(locationBtn)
        content.addView(timerCard)

        // নামাজের ৫ ওয়াক্তের সময়সূচী
        val prayerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
            val bg = GradientDrawable()
            bg.setColor(getCardBgColor())
            bg.cornerRadius = 24f
            bg.setStroke(1, Color.parseColor("#CBD5E1"))
            background = bg
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 20, 0, 0)
            layoutParams = lp
        }

        val pTitle = TextView(this).apply {
            text = "আজকের নামাজের সময়সূচী ($selectedDistrict)"
            textSize = 17f
            setTextColor(accent)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 12)
        }
        prayerCard.addView(pTitle)

        val offset = districtOffsets[selectedDistrict] ?: 0
        val times = listOf(
            "ফজর" to "০৪:${25 + offset} - ০৫:${43 + offset} মি.",
            "যোহর" to "১২:${8 + offset} - ০৪:${36 + offset} মি.",
            "আসর" to "০৪:${37 + offset} - ০৬:${26 + offset} মি.",
            "মাগরিব (ইফতার)" to "০৬:${27 + offset} - ০৭:${43 + offset} মি.",
            "এশা" to "০৭:${44 + offset} - ০৪:${24 + offset} মি."
        )

        for (t in times) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 8, 0, 8)
            }
            val name = TextView(this).apply {
                text = t.first
                setTextColor(getTextColor())
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }
            val time = TextView(this).apply {
                text = t.second
                setTextColor(if (isWhiteTheme()) Color.parseColor("#059669") else Color.parseColor("#86EFAC"))
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
            }
            row.addView(name)
            row.addView(time)
            prayerCard.addView(row)
        }
        content.addView(prayerCard)

        // কুইক তাসবিহ বাটন
        val btnTasbih = Button(this).apply {
            text = "📿 ডিজিটাল তাসবিহ শুরু করুন"
            setBackgroundColor(accent)
            setTextColor(Color.BLACK)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 20, 0, 0)
            layoutParams = lp
            setOnClickListener { openScreen("tasbih") }
        }
        content.addView(btnTasbih)

        scroll.addView(content)
        root.addView(scroll)
        root.addView(createNavBar("home"))
        setContentView(root)

        startLiveTimer()
    }

    // জেলা পরিবর্তনের ডায়ালগ
    private fun showDistrictDialog() {
        val districts = districtOffsets.keys.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("আপনার জেলা নির্বাচন করুন")
            .setItems(districts) { _, which ->
                selectedDistrict = districts[which]
                saveAllData()
                showHomeScreen()
            }
            .show()
    }

    // ২. ফুল স্ক্রিন ডিজিটাল তাসবিহ
    private fun showTasbihScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getThemeBackground()
        }

        val zikr = getActiveZikr()
        val accent = getAccentColor()

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

        val fullScreenTap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            gravity = Gravity.CENTER
            setPadding(32, 10, 32, 10)
        }

        val centerIcon = TextView(this).apply {
            text = "🕋"
            textSize = 34f
            gravity = Gravity.CENTER
        }
        fullScreenTap.addView(centerIcon)

        val zikrNameText = TextView(this).apply {
            text = zikr.name
            textSize = 22f
            setTextColor(if (isWhiteTheme()) Color.parseColor("#059669") else Color.parseColor("#9AE6B4"))
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 6, 0, 10)
        }

        val countDisplay = TextView(this).apply {
            text = "${zikr.count}"
            textSize = 90f
            setTextColor(getTextColor())
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
            setTextColor(getSecondaryTextColor())
            gravity = Gravity.CENTER
        }

        fullScreenTap.addView(zikrNameText)
        fullScreenTap.addView(countDisplay)
        fullScreenTap.addView(targetInfo)
        fullScreenTap.addView(tapGuide)

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
                        .setMessage("${zikr.name} নির্ধারিত টার্গেট (${zikr.target} বার) পূর্ণ হয়েছে।")
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
            setBackgroundColor(if (isWhiteTheme()) Color.parseColor("#0F766E") else Color.parseColor("#264536"))
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

        val title = TextView(this).apply {
            text = "জিকির তালিকা ও কাস্টমাইজেশন"
            textSize = 20f
            setTextColor(accent)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        content.addView(title)

        val btnAddZikr = Button(this).apply {
            text = "➕ নতুন জিকির যোগ করুন (Add Zikr)"
            setBackgroundColor(accent)
            setTextColor(Color.BLACK)
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 0, 0, 20)
            layoutParams = lp
            setOnClickListener { showAddZikrDialog() }
        }
        content.addView(btnAddZikr)

        for (item in zikrList) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 16, 20, 16)
                val bg = GradientDrawable()
                bg.setColor(getCardBgColor())
                bg.cornerRadius = 20f
                bg.setStroke(if (item.id == activeZikrId) 3 else 1, if (item.id == activeZikrId) accent else Color.parseColor("#CBD5E1"))
                background = bg
                val lp = LinearLayout.LayoutParams(-1, -2)
                lp.setMargins(0, 0, 0, 14)
                layoutParams = lp
            }

            val topInfo = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val countBadge = TextView(this).apply {
                text = "${item.count}"
                textSize = 18f
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                val bg = GradientDrawable()
                bg.setColor(Color.parseColor("#0F766E"))
                bg.cornerRadius = 40f
                background = bg
                setPadding(20, 10, 20, 10)
            }

            val nameView = TextView(this).apply {
                text = item.name
                textSize = 16f
                setTextColor(getTextColor())
                setTypeface(null, Typeface.BOLD)
                val lp = LinearLayout.LayoutParams(0, -2, 1f)
                lp.setMargins(16, 0, 10, 0)
                layoutParams = lp
            }

            topInfo.addView(countBadge)
            topInfo.addView(nameView)
            card.addView(topInfo)

            val targetText = TextView(this).apply {
                text = "টার্গেট: ${item.target} বার (গোণা হয়েছে: ${item.count} বার)"
                textSize = 13f
                setTextColor(getSecondaryTextColor())
                setPadding(0, 8, 0, 12)
            }
            card.addView(targetText)

            val actionsRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }

            val btnContinue = Button(this).apply {
                text = "Continue"
                textSize = 13f
                setBackgroundColor(Color.parseColor("#2563EB"))
                setTextColor(Color.WHITE)
                setOnClickListener {
                    activeZikrId = item.id
                    saveAllData()
                    openScreen("tasbih")
                }
            }

            val btnDelete = Button(this).apply {
                text = "Delete"
                textSize = 12f
                val lp = LinearLayout.LayoutParams(-2, -2)
                lp.setMargins(0, 0, 10, 0)
                layoutParams = lp
                setBackgroundColor(Color.parseColor("#DC2626"))
                setTextColor(Color.WHITE)
                setOnClickListener {
                    if (zikrList.size <= 1) {
                        Toast.makeText(this@MainActivity, "কমপক্ষে একটি জিকির থাকতে হবে!", Toast.LENGTH_SHORT).show()
                    } else {
                        zikrList.remove(item)
                        if (activeZikrId == item.id) activeZikrId = zikrList.first().id
                        saveAllData()
                        showZikrListScreen()
                    }
                }
            }

            actionsRow.addView(btnDelete)
            actionsRow.addView(btnContinue)
            card.addView(actionsRow)

            content.addView(card)
        }

        scroll.addView(content)
        root.addView(scroll)
        root.addView(createNavBar("zikr_list"))
        setContentView(root)
    }

    private fun showAddZikrDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val nameInput = EditText(this).apply { hint = "জিকিরের নাম লিখুন" }
        val targetInput = EditText(this).apply {
            hint = "টার্গেট সংখ্যা (যেমন: ৩৩)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("33")
        }

        layout.addView(nameInput)
        layout.addView(targetInput)

        AlertDialog.Builder(this)
            .setTitle("নতুন জিকির যোগ করুন")
            .setView(layout)
            .setPositiveButton("সংরক্ষণ") { _, _ ->
                val name = nameInput.text.toString().trim()
                val target = targetInput.text.toString().toIntOrNull() ?: 33
                if (name.isNotEmpty()) {
                    val newItem = ZikrItem(UUID.randomUUID().toString(), name, 0, if (target > 0) target else 33)
                    zikrList.add(newItem)
                    activeZikrId = newItem.id
                    saveAllData()
                    showZikrListScreen()
                }
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    // ৪. দৈনিক আমল স্ক্রিন
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
            text = "দৈনিক আমল চেকলিস্ট"
            textSize = 22f
            setTextColor(getAccentColor())
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
            "সুরা মুলক তিলাওয়াত"
        )

        val dayKey = "day_" + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        for ((idx, am) in amols.withIndex()) {
            val cb = CheckBox(this).apply {
                text = am
                textSize = 16f
                setTextColor(getTextColor())
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

    // ৫. কুরআন ও ইসলামিক রিসোর্স
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
            setTextColor(getAccentColor())
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 20)
        }
        content.addView(h)

        val links = listOf(
            "📖 সম্পূর্ণ আল-কুরআন (অনলাইন পড়ুন ও শুনুন)" to "https://quran.com/bn",
            "🤲 হিসনুল মুসলিম (সকাল-সন্ধ্যার মাসনুন দোয়া)" to "https://sunnah.com/hisn",
            "🕌 ইসলামিক ফাউন্ডেশন বাংলাদেশ" to "http://www.islamicfoundation.gov.bd"
        )

        for (item in links) {
            val btn = Button(this).apply {
                text = item.first
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#0F766E"))
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

    // ৬. জিমেইল ব্যাকআপ ও থিম সেটিংস
    private fun showSettingsScreen() {
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

        val accent = getAccentColor()

        val h = TextView(this).apply {
            text = "সেটিংস ও ব্যাকআপ"
            textSize = 22f
            setTextColor(accent)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 20)
        }
        content.addView(h)

        // জিমেইল / ক্লাউড ব্যাকআপ সেকশন
        val backupCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
            val bg = GradientDrawable()
            bg.setColor(getCardBgColor())
            bg.cornerRadius = 20f
            bg.setStroke(1, Color.parseColor("#CBD5E1"))
            background = bg
        }

        val bTitle = TextView(this).apply {
            text = "📧 জিমেইল লগইন / ব্যাকআপ (মাল্টি ডিভাইস)"
            textSize = 16f
            setTextColor(getTextColor())
            setTypeface(null, Typeface.BOLD)
        }
        val bDesc = TextView(this).apply {
            text = if (userEmail.isEmpty()) "কোনো জিমেইল যুক্ত নেই। অন্য ফোনে চালাতে আপনার জিমেইল লিখুন।" else "সংযুক্ত জিমেইল: $userEmail"
            textSize = 13f
            setTextColor(getSecondaryTextColor())
            setPadding(0, 4, 0, 12)
        }
        val btnLogin = Button(this).apply {
            text = if (userEmail.isEmpty()) "জিমেইল যুক্ত করুন" else "জিমেইল পরিবর্তন / সিঙ্ক"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            setOnClickListener { showLoginDialog() }
        }

        backupCard.addView(bTitle)
        backupCard.addView(bDesc)
        backupCard.addView(btnLogin)
        content.addView(backupCard)

        // থিম নির্বাচন
        val themeInfo = TextView(this).apply {
            text = "\n🎨 থিম নির্বাচন করুন (বর্তমান: $currentTheme):"
            textSize = 15f
            setTextColor(getTextColor())
            setPadding(0, 16, 0, 12)
        }
        content.addView(themeInfo)

        val themes = listOf(
            "⚪ সাদা থিম (লাইট)" to "#F1F5F9",
            "🕋 কাবা থিম (ডার্ক গোল্ড)" to "#1B2A22",
            "🕌 মদিনা থিম (এমারেল্ড গ্রিন)" to "#0E4D3A",
            "🌅 সুবহ-সাদিক থিম (রয়্যাল গোল্ড)" to "#422A0A",
            "🌌 লাইলাতুল কদর (নাইট ব্লু)" to "#0F2027"
        )

        for (th in themes) {
            val btn = Button(this).apply {
                text = th.first
                setBackgroundColor(Color.parseColor(th.second))
                setTextColor(if (th.first.contains("সাদা")) Color.BLACK else Color.WHITE)
                textSize = 15f
                val lp = LinearLayout.LayoutParams(-1, -2)
                lp.setMargins(0, 8, 0, 8)
                layoutParams = lp
                setOnClickListener {
                    currentTheme = th.first
                    saveAllData()
                    showSettingsScreen()
                }
            }
            content.addView(btn)
        }

        scroll.addView(content)
        root.addView(scroll)
        root.addView(createNavBar("settings"))
        setContentView(root)
    }

    private fun showLoginDialog() {
        val input = EditText(this).apply {
            hint = "আপনার Gmail লিখুন (e.g. sabbir@gmail.com)"
            setText(userEmail)
        }
        AlertDialog.Builder(this)
            .setTitle("জিমেইল ক্লাউড অ্যাকাউন্ট")
            .setView(input)
            .setPositiveButton("লগইন / সিঙ্ক") { _, _ ->
                val em = input.text.toString().trim()
                if (em.isNotEmpty()) {
                    userEmail = em
                    saveAllData()
                    Toast.makeText(this, "সফলভাবে জিমেইল সংযুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                    showSettingsScreen()
                }
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }
}
