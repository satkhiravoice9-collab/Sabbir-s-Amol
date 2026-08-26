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
import android.view.View
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

data class NoteItem(
    val id: String,
    var title: String,
    var content: String,
    val date: String
)

class MainActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("sabbirs_amol_db", Context.MODE_PRIVATE) }
    private val vibrator by lazy { getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

    private val screenStack = Stack<String>()
    private val zikrList = mutableListOf<ZikrItem>()
    private val noteList = mutableListOf<NoteItem>()
    
    private var activeZikrId: String = ""
    private var currentTheme = "সাদা থিম (লাইট)"
    private var selectedDivision = "খুলনা"
    private var selectedDistrict = "সাতক্ষীরা"
    private var selectedThana = "সাতক্ষীরা সদর"
    private var selectedUnion = "ঈশ্বরীপুর / পোস্ট অফিস"
    private var selectedMadhab = "হানাফী"
    private var userEmail = ""

    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var liveTimerTextView: TextView? = null
    private var liveWaqtTextView: TextView? = null

    // ইংরেজি সংখ্যাকে ১০০% খাঁটি বাংলা সংখ্যায় রূপান্তর
    private fun toBangla(input: Any): String {
        val str = input.toString()
        val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder()
        for (ch in str) {
            if (ch in '0'..'9') {
                sb.append(banglaDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun formatBanglaTime(hour: Int, minute: Int): String {
        var h = hour
        var m = minute
        while (m >= 60) {
            h += 1
            m -= 60
        }
        while (m < 0) {
            h -= 1
            m += 60
        }
        if (h > 12) h -= 12
        if (h == 0) h = 12
        val hStr = String.format("%02d", h)
        val mStr = String.format("%02d", m)
        return "${toBangla(hStr)}:${toBangla(mStr)}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadAllData()
        openScreen("home")
    }

    private fun loadAllData() {
        currentTheme = prefs.getString("current_theme", "সাদা থিম (লাইট)") ?: "সাদা থিম (লাইট)"
        selectedDivision = prefs.getString("selected_division", "খুলনা") ?: "খুলনা"
        selectedDistrict = prefs.getString("selected_district", "সাতক্ষীরা") ?: "সাতক্ষীরা"
        selectedThana = prefs.getString("selected_thana", "সাতক্ষীরা সদর") ?: "সাতক্ষীরা সদর"
        selectedUnion = prefs.getString("selected_union", "ঈশ্বরীপুর / পোস্ট অফিস") ?: "ঈশ্বরীপুর / পোস্ট অফিস"
        selectedMadhab = prefs.getString("selected_madhab", "হানাফী") ?: "হানাফী"
        userEmail = prefs.getString("user_email", "") ?: ""
        activeZikrId = prefs.getString("active_zikr_id", "") ?: ""

        // জিকির ডাটা লোড
        val savedZikr = prefs.getString("zikr_items_json", null)
        zikrList.clear()
        if (savedZikr != null) {
            try {
                val array = JSONArray(savedZikr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    zikrList.add(ZikrItem(obj.getString("id"), obj.getString("name"), obj.getInt("count"), obj.getInt("target")))
                }
            } catch (e: Exception) { initDefaultZikr() }
        } else { initDefaultZikr() }

        if (activeZikrId.isEmpty() || zikrList.none { it.id == activeZikrId }) {
            activeZikrId = zikrList.firstOrNull()?.id ?: "1"
        }

        // নোটপ্যাড ডাটা লোড
        val savedNotes = prefs.getString("note_items_json", null)
        noteList.clear()
        if (savedNotes != null) {
            try {
                val array = JSONArray(savedNotes)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    noteList.add(NoteItem(obj.getString("id"), obj.getString("title"), obj.getString("content"), obj.getString("date")))
                }
            } catch (e: Exception) {}
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
        val zikrArray = JSONArray()
        for (item in zikrList) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("count", item.count)
                put("target", item.target)
            }
            zikrArray.put(obj)
        }

        val noteArray = JSONArray()
        for (item in noteList) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("content", item.content)
                put("date", item.date)
            }
            noteArray.put(obj)
        }

        prefs.edit()
            .putString("zikr_items_json", zikrArray.toString())
            .putString("note_items_json", noteArray.toString())
            .putString("active_zikr_id", activeZikrId)
            .putString("current_theme", currentTheme)
            .putString("selected_division", selectedDivision)
            .putString("selected_district", selectedDistrict)
            .putString("selected_thana", selectedThana)
            .putString("selected_union", selectedUnion)
            .putString("selected_madhab", selectedMadhab)
            .putString("user_email", userEmail)
            .apply()
    }

    private fun getActiveZikr(): ZikrItem = zikrList.find { it.id == activeZikrId } ?: zikrList[0]

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
            "hadith" -> showHadithScreen()
            "notepad" -> showNotepadScreen()
            "amal" -> showAmalScreen()
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
                intArrayOf(Color.parseColor("#FFFDF5"), Color.parseColor("#F8FAFC"), Color.parseColor("#FFFFFF"))
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
            setPadding(8, 14, 8, 18)
            gravity = Gravity.CENTER
            elevation = 16f
        }

        val items = listOf(
            Triple("হোম", "home", "🏠"),
            Triple("তাসবিহ", "tasbih", "📿"),
            Triple("হাদীস", "hadith", "📚"),
            Triple("নোটপ্যাড", "notepad", "📝"),
            Triple("আমল", "amal", "✅"),
            Triple("প্রোফাইল", "settings", "👤")
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
                textSize = 10.5f
                gravity = Gravity.CENTER
                typeface = Typeface.SERIF
                setTextColor(if (current == item.second) accent else Color.parseColor("#94A3B8"))
                setTypeface(Typeface.SERIF, if (current == item.second) Typeface.BOLD else Typeface.NORMAL)
            }

            btn.addView(icon)
            btn.addView(text)
            nav.addView(btn)
        }
        return nav
    }

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

        val offset = if (selectedDistrict.contains("সাতক্ষীরা")) 4 else if (selectedDistrict.contains("ঢাকা")) 0 else 2
        val asrExtra = if (selectedMadhab == "হানাফী") 45 else 0

        val fojrEnd = 5 * 60 + 43 + offset
        val ishraqEnd = 6 * 60 + 30 + offset
        val chashtEnd = 11 * 60 + 45 + offset
        val zohrEnd = 16 * 60 + 36 + offset + asrExtra
        val asrEnd = 18 * 60 + 26 + offset
        val magribEnd = 19 * 60 + 43 + offset

        val (waqtName, targetMin) = when {
            currentMinutes < fojrEnd -> "ফজর" to fojrEnd
            currentMinutes < ishraqEnd -> "ইশরাক" to ishraqEnd
            currentMinutes < chashtEnd -> "চাশত / দুহা" to chashtEnd
            currentMinutes < zohrEnd -> "যোহর" to zohrEnd
            currentMinutes < asrEnd -> "আসর ($selectedMadhab)" to asrEnd
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
        liveTimerTextView?.text = toBangla(timeString)
    }

    // ১. হোম স্ক্রিন (বিস্তারিত লাইভ লোকেশন, মাযহাব ও কাউন্টডাউন)
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
            setPadding(26, 26, 26, 26)
        }

        val accent = getAccentColor()

        // লাইভ টাইমার কার্ড
        val timerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 24, 28, 24)
            val bg = GradientDrawable()
            bg.setColor(if (isWhiteTheme()) Color.parseColor("#FFFBEB") else Color.parseColor("#1B2E24"))
            bg.cornerRadius = 30f
            bg.setStroke(3, accent)
            background = bg
        }

        liveWaqtTextView = TextView(this).apply {
            text = "ওয়াক্ত শেষ হতে বাকি"
            textSize = 16f
            typeface = Typeface.SERIF
            setTextColor(getTextColor())
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        liveTimerTextView = TextView(this).apply {
            text = "০০:০০:০০"
            textSize = 44f
            typeface = Typeface.SERIF
            setTextColor(accent)
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 10)
        }

        // লাইভ লোকেশন বিস্তারিত বক্স
        val locationBox = TextView(this).apply {
            text = "📍 $selectedUnion, $selectedThana, $selectedDistrict, $selectedDivision"
            textSize = 12.5f
            typeface = Typeface.SERIF
            setTextColor(getTextColor())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        }

        val btnChangeLoc = Button(this).apply {
            text = "লোকেশন ও মাযহাব পরিবর্তন ▾"
            textSize = 12f
            typeface = Typeface.SERIF
            setBackgroundColor(if (isWhiteTheme()) Color.parseColor("#FEF3C7") else Color.parseColor("#264536"))
            setTextColor(getTextColor())
            setOnClickListener { showLocationAndMadhabDialog() }
        }

        timerCard.addView(liveWaqtTextView)
        timerCard.addView(liveTimerTextView)
        timerCard.addView(locationBox)
        timerCard.addView(btnChangeLoc)
        content.addView(timerCard)

        // নামাজের ৫ ওয়াক্তের সময়সূচী
        val prayerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 18, 22, 18)
            val bg = GradientDrawable()
            bg.setColor(getCardBgColor())
            bg.cornerRadius = 24f
            bg.setStroke(1, Color.parseColor("#CBD5E1"))
            background = bg
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 18, 0, 0)
            layoutParams = lp
        }

        val pTitle = TextView(this).apply {
            text = "আজকের নামাজের সময়সূচী ($selectedDistrict | $selectedMadhab মাযহাব)"
            textSize = 16f
            typeface = Typeface.SERIF
            setTextColor(accent)
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            setPadding(0, 0, 0, 10)
        }
        prayerCard.addView(pTitle)

        val asrStartHour = if (selectedMadhab == "হানাফী") 4 else 3
        val asrStartMin = if (selectedMadhab == "হানাফী") 37 else 52

        val times = listOf(
            "ফজর" to "${formatBanglaTime(4, 25)} - ${formatBanglaTime(5, 43)} মি.",
            "যোহর" to "${formatBanglaTime(12, 8)} - ${formatBanglaTime(asrStartHour, asrStartMin - 1)} মি.",
            "আসর ($selectedMadhab)" to "${formatBanglaTime(asrStartHour, asrStartMin)} - ${formatBanglaTime(6, 26)} মি.",
            "মাগরিব (ইফতার)" to "${formatBanglaTime(6, 27)} - ${formatBanglaTime(7, 43)} মি.",
            "এশা ও বিতর" to "${formatBanglaTime(7, 44)} - ${formatBanglaTime(4, 24)} মি."
        )

        for (t in times) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 7, 0, 7)
            }
            val name = TextView(this).apply {
                text = t.first
                setTextColor(getTextColor())
                textSize = 14.5f
                typeface = Typeface.SERIF
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }
            val time = TextView(this).apply {
                text = t.second
                setTextColor(if (isWhiteTheme()) Color.parseColor("#059669") else Color.parseColor("#86EFAC"))
                textSize = 14.5f
                typeface = Typeface.SERIF
                setTypeface(Typeface.SERIF, Typeface.BOLD)
            }
            row.addView(name)
            row.addView(time)
            prayerCard.addView(row)
        }
        content.addView(prayerCard)

        // ডিজিটাল তাসবিহ কুইক বাটন
        val btnTasbih = Button(this).apply {
            text = "📿 ডিজিটাল তাসবিহ চালু করুন"
            setBackgroundColor(accent)
            setTextColor(Color.BLACK)
            textSize = 15f
            typeface = Typeface.SERIF
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 18, 0, 0)
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

    // লোকেশন ও মাযহাব সেট করার ডায়ালগ
    private fun showLocationAndMadhabDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 16, 36, 16)
        }

        val divInput = EditText(this).apply { hint = "বিভাগ (যেমন: খুলনা)"; setText(selec
