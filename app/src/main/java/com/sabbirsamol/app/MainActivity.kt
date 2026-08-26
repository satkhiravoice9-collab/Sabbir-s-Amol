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
    private var selectedThana = "শ্যামনগর"
    private var selectedUnion = "ঈশ্বরীপুর"
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
        val mStr = String.format("%02d", minute % 60)
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
        selectedThana = prefs.getString("selected_thana", "শ্যামনগর") ?: "শ্যামনগর"
        selectedUnion = prefs.getString("selected_union", "ঈশ্বরীপুর") ?: "ঈশ্বরীপুর"
        selectedMadhab = prefs.getString("selected_madhab", "হানাফী") ?: "হানাফী"
        userEmail = prefs.getString("user_email", "") ?: ""
        activeZikrId = prefs.getString("active_zikr_id", "") ?: ""

        // জিকির লোড
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

        // নোটপ্যাড লোড
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
            "amal_folders" -> showAmalFoldersScreen()
            "masnun_pdf_screen" -> showMasnunPdfScreen()
            "manzil_pdf_screen" -> showManzilPdfScreen()
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
            Triple("আমল ও মানযিল", "amal_folders", "📂"),
            Triple("নোটপ্যাড", "notepad", "📝"),
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
                textSize = 10f
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

    // ১. হোম স্ক্রিন
    private fun showHomeScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getThemeBackground()
        }

        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(26, 26, 26, 26) }
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
            text = "আজকের নামাজের সময়সূচী ($selectedDistrict | $selectedMadhab)"
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

        // ডিজিটাল তাসবিহ ও ফোল্ডার বাটন
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

        val btnFolders = Button(this).apply {
            text = "📂 মাসনূন আমল ও মানযিল ফোল্ডার"
            setBackgroundColor(if (isWhiteTheme()) Color.parseColor("#0F766E") else Color.parseColor("#264536"))
            setTextColor(Color.WHITE)
            textSize = 14.5f
            typeface = Typeface.SERIF
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 12, 0, 0)
            layoutParams = lp
            setOnClickListener { openScreen("amal_folders") }
        }
        content.addView(btnFolders)

        scroll.addView(content)
        root.addView(scroll)
        root.addView(createNavBar("home"))
        setContentView(root)

        startLiveTimer()
    }

    private fun showLocationAndMadhabDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 16, 36, 16)
        }

        val divInput = EditText(this).apply { hint = "বিভাগ"; setText(selectedDivision); typeface = Typeface.SERIF }
        val distInput = EditText(this).apply { hint = "জেলা"; setText(selectedDistrict); typeface = Typeface.SERIF }
        val thanaInput = EditText(this).apply { hint = "থানা"; setText(selectedThana); typeface = Typeface.SERIF }
        val unionInput = EditText(this).apply { hint = "ইউনিয়ন"; setText(selectedUnion); typeface = Typeface.SERIF }

        val madhabSpinner = Spinner(this)
        val madhabs = arrayOf("হানাফী", "শাফেয়ী", "মালেকী", "হাম্বলী")
        madhabSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, madhabs)
        madhabSpinner.setSelection(madhabs.indexOf(selectedMadhab).coerceAtLeast(0))

        layout.addView(TextView(this).apply { text = "মাযহাব নির্বাচন করুন:"; typeface = Typeface.SERIF; setTextColor(getTextColor()) })
        layout.addView(madhabSpinner)
        layout.addView(TextView(this).apply { text = "\nবিস্তারিত লোকেশন দিন:"; typeface = Typeface.SERIF; setTextColor(getTextColor()) })
        layout.addView(divInput)
        layout.addView(distInput)
        layout.addView(thanaInput)
        layout.addView(unionInput)

        AlertDialog.Builder(this)
            .setTitle("লোকেশন ও মাযহাব সেটিংস")
            .setView(layout)
            .setPositiveButton("সংরক্ষণ করুন") { _, _ ->
                selectedDivision = divInput.text.toString().trim().ifEmpty { selectedDivision }
                selectedDistrict = distInput.text.toString().trim().ifEmpty { selectedDistrict }
                selectedThana = thanaInput.text.toString().trim().ifEmpty { selectedThana }
                selectedUnion = unionInput.text.toString().trim().ifEmpty { selectedUnion }
                selectedMadhab = madhabSpinner.selectedItem.toString()
                saveAllData()
                showHomeScreen()
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    // ২. ডিজিটাল তাসবিহ
    private fun showTasbihScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getThemeBackground()
        }

        val zikr = getActiveZikr()
        val accent = getAccentColor()

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 18, 24, 10)
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "ডিজিটাল তাসবিহ"
            textSize = 19f
            typeface = Typeface.SERIF
            setTextColor(accent)
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }

        val btnReset = Button(this).apply {
            text = "রিসেট (০)"
            textSize = 12f
            typeface = Typeface.SERIF
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
            textSize = 32f
            gravity = Gravity.CENTER
        }
        fullScreenTap.addView(centerIcon)

        val zikrNameText = TextView(this).apply {
            text = zikr.name
            textSize = 21f
            typeface = Typeface.SERIF
            setTextColor(if (isWhiteTheme()) Color.parseColor("#059669") else Color.parseColor("#9AE6B4"))
            gravity = Gravity.CENTER
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            setPadding(0, 4, 0, 8)
        }

        val countDisplay = TextView(this).apply {
            text = toBangla(zikr.count)
            textSize = 88f
            typeface = Typeface.SERIF
            setTextColor(getTextColor())
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        val targetInfo = TextView(this).apply {
            text = "টার্গেট: ${toBangla(zikr.target)} বার"
            textSize = 16f
            typeface = Typeface.SERIF
            setTextColor(accent)
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 18)
        }

        val tapGuide = TextView(this).apply {
            text = "👆 স্ক্রিনের যেকোনো জায়গায় ট্যাপ করে গণনা করুন"
            textSize = 12.5f
            typeface = Typeface.SERIF
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
                countDisplay.text = toBangla(zikr.count)

                if (zikr.count >= zikr.target) {
                    triggerVibration(500)
                    AlertDialog.Builder(this)
                        .setTitle("মাশাআল্লাহ!")
                        .setMessage("${zikr.name} নির্ধারিত টার্গেট (${toBangla(zikr.target)} বার) পূর্ণ হয়েছে।")
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
            setPadding(24, 6, 24, 14)
        }

        val btnToList = Button(this).apply {
            text = "📋 জিকির তালিকা ও কাস্টমাইজেশন"
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            typeface = Typeface.SERIF
            setBackgroundColor(if (isWhiteTheme()) Color.parseColor("#0F766E") else Color.parseColor("#264536"))
            setTextColor(Color.WHITE)
            setOnClickListener { openScreen("zikr_list") }
        }
        bottomBar.addView(btnToList)
        root.addView(bottomBar)

        root.addView(createNavBar("tasbih"))
        setContentView(root)
    }

    // ৩. জিকির তালিকা স্ক্রিন
    private fun showZikrListScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getThemeBackground()
        }

        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(26, 26, 26, 26) }
        val accent = getAccentColor()

        val title = TextView(this).apply {
            text = "জিকির তালিকা ও নতুন জিকির যোগ"
            textSize = 19f
            typeface = Typeface.SERIF
            setTextColor(accent)
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            setPadding(0, 0, 0, 14)
        }
        content.addView(title)

        val btnAddZikr = Button(this).apply {
            text = "➕ নতুন জিকির যুক্ত করুন"
            setBackgroundColor(accent)
            setTextColor(Color.BLACK)
            textSize = 14.5f
            typeface = Typeface.SERIF
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 0, 0, 18)
            layoutParams = lp
            setOnClickListener { showAddZikrDialog() }
        }
        content.addView(btnAddZikr)

        for (item in zikrList) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(18, 14, 18, 14)
                val bg = GradientDrawable()
                bg.setColor(getCardBgColor())
                bg.cornerRadius = 20f
                bg.setStroke(if (item.id == activeZikrId) 3 else 1, if (item.id == activeZikrId) accent else Color.parseColor("#CBD5E1"))
                background = bg
                val lp = LinearLayout.LayoutParams(-1, -2)
                lp.setMargins(0, 0, 0, 12)
                layoutParams = lp
            }

            val topInfo = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val countBadge = TextView(this).apply {
                text = toBangla(item.count)
                textSize = 17f
                typeface = Typeface.SERIF
                setTextColor(Color.WHITE)
                setTypeface(Typeface.SERIF, Typeface.BOLD)
                gravity = Gravity.CENTER
                val bg = GradientDrawable()
                bg.setColor(Color.parseColor("#0F766E"))
                bg.cornerRadius = 40f
                background = bg
                setPadding(18, 8, 18, 8)
            }

            val nameView = TextView(this).apply {
                text = item.name
                textSize = 15.5f
                typeface = Typeface.SERIF
                setTextColor(getTextColor())
                setTypeface(Typeface.SERIF, Typeface.BOLD)
                val lp = LinearLayout.LayoutParams(0, -2, 1f)
                lp.setMargins(14, 0, 8, 0)
                layoutParams = lp
            }

            topInfo.addView(countBadge)
            topInfo.addView(nameView)
            card.addView(topInfo)

            val targetText = TextView(this).apply {
                text = "টার্গেট: ${toBangla(item.target)} বার (গোণা হয়েছে: ${toBangla(item.count)} বার)"
                textSize = 12.5f
                typeface = Typeface.SERIF
                setTextColor(getSecondaryTextColor())
                setPadding(0, 6, 0, 10)
            }
            card.addView(targetText)

            val actionsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
            val btnContinue = Button(this).apply {
                text = "Continue"
                textSize = 12.5f
                typeface = Typeface.SERIF
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
                typeface = Typeface.SERIF
                val lp = LinearLayout.LayoutParams(-2, -2)
                lp.setMargins(0, 0, 8, 0)
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
        root.addView(createNavBar("tasbih"))
        setContentView(root)
    }

    private fun showAddZikrDialog() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 20) }
        val nameInput = EditText(this).apply { hint = "জিকিরের নাম লিখুন"; typeface = Typeface.SERIF }
        val targetInput = EditText(this).apply {
            hint = "টার্গেট সংখ্যা (যেমন: ৩৩)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("33")
            typeface = Typeface.SERIF
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

    // ৪. দুটি আলাদা ফোল্ডার মেনু (মাসনূন আমল ও মানযিল)
    private fun showAmalFoldersScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getThemeBackground()
        }

        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(26, 26, 26, 26) }
        val accent = getAccentColor()

        val h = TextView(this).apply {
            text = "📂 মাসনূন আমল ও মানযিল ভাণ্ডার"
            textSize = 20f
            typeface = Typeface.SERIF
            setTextColor(accent)
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            setPadding(0, 0, 0, 6)
        }
        val sub = TextView(this).apply {
            text = "সংকলনে: সাব্বির আহমাদ"
            textSize = 13.5f
            typeface = Typeface.SERIF
            setTextColor(getSecondaryTextColor())
            setPadding(0, 0, 0, 18)
        }
        content.addView(h)
        content.addView(sub)

        // ফোল্ডার ১: মাসনূন আমল কার্ড
        val folder1 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 20, 22, 20)
            val bg = GradientDrawable()
            bg.setColor(getCardBgColor())
            bg.cornerRadius = 24f
            bg.setStroke(2, accent)
            background = bg
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 0, 0, 16)
            layoutParams = lp
            setOnClickListener { openScreen("masnun_pdf_screen") }
        }

        val f1Title = TextView(this).apply {
            text = "📁 ফোল্ডার ১: মাসনূন আমল (সকাল-সন্ধ্যা ও শয়ন)"
            textSize = 16.5f
            typeface = Typeface.SERIF
            setTextColor(getTextColor())
            setTypeface(Typeface.SERIF, Typeface.BOLD)
        }
        val f1Desc = TextView(this).apply {
            text = "সকাল ও সন্ধ্যার নির্বাচিত মাসনূন দু'আ, সূরাসমূহ ও ঘুমানোর পূর্বের সুন্নাত আমলসমূহ।"
            textSize = 13f
            typeface = Typeface.SERIF
            setTextColor(getSecondaryTextColor())
            setPadding(0, 6, 0, 12)
        }
        val btnF1 = Button(this).apply {
            text = "📖 আমলসমূহ পড়ুন ➔"
            typeface = Typeface.SERIF
            setBackgroundColor(accent)
            setTextColor(Color.BLACK)
            setOnClickListener { openScreen("masnun_pdf_screen") }
        }
        folder1.addView(f1Title)
        folder1.addView(f1Desc)
        folder1.addView(btnF1)
        content.addView(folder1)

        // ফোল্ডার ২: মানযিল আয়াত কার্ড
        val folder2 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 20, 22, 20)
            val bg = GradientDrawable()
            bg.setColor(getCardBgColor())
            bg.cornerRadius = 24f
            bg.setStroke(2, Color.parseColor("#0F766E"))
            background = bg
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 0, 0, 16)
            layoutParams = lp
            setOnClickListener { openScreen("manzil_pdf_screen") }
        }

        val f2Title = TextView(this).apply {
            text = "📁 ফোল্ডার ২: মানযিল আয়াত (কুরআনী হিফাযত)"
            textSize = 16.5f
            typeface = Typeface.SERIF
            setTextColor(getTextColor())
            setTypeface(Typeface.SERIF, Typeface.BOLD)
        }
        val f2Desc = TextView(this).apply {
            text = "কুরআনুল কারীমের রোগ-বালাই ও সকল অনিষ্ট থেকে বাঁচার হিফাযতের বিশেষ আয়াতসমূহ।"
            textSize = 13f
            typeface = Typeface.SERIF
            setTextColor(getSecondaryTextColor())
            setPadding(0, 6, 0, 12)
        }
        val btnF2 = Button(this).apply {
            text = "📖 মানযিল তিলাওয়াত করুন ➔"
            typeface = Typeface.SERIF
            setBackgroundColor(if (isWhiteTheme()) Color.parseColor("#0F766E") else Color.parseColor("#264536"))
            setTextColor(Color.WHITE)
            setOnClickListener { openScreen("manzil_pdf_screen") }
        }
        folder2.addView(f2Title)
        folder2.addView(f2Desc)
        folder2.addView(btnF2)
        content.addView(folder2)

        scroll.addView(content)
        root.addView(scroll)
        root.addView(createNavBar("amal_folders"))
        setContentView(root)
    }

    // ফোল্ডার ১-এর বিস্তারিত রিডার: মাসনূন আমল
    private fun showMasnunPdfScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getThemeBackground()
        }

        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(26, 26, 26, 26) }
        val accent = getAccentColor()

        val h = TextView(this).apply {
            text = "মাসনূন আমল\nসংকলনে: সাব্বির আহমাদ"
            textSize = 19f
            typeface = Typeface.SERIF
            setTextColor(accent)
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        content.addView(h)

        val items = listOf(
            "সূরা আল-ফাতিহা (সকাল ও সন্ধ্যায় ৩ বার)" to "بِسْمِ اللهِ الرَّحْمَنِ الرَّحِيمِ (1) الْحَمْدُ لِلَّهِ رَبِّ الْعَلَمِينَ (٢) الرَّحْمَنِ الرَّحِيمِ (۳) مَلِكِ يَوْمِ الدِّينِ (4) إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ (٥) اِهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ (6) صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ (۷)",
            "আয়াতুল কুরসি (সকাল ও সন্ধ্যায় ৩ বার)" to "اللهُ لَا إِلَهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ، لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ لَهُ مَا فِي السَّمَاتِ وَمَا فِي الْأَرْضِ مَنْ ذَا الَّذِي يَشْفَعُ عِنْدَةً إِلَّا بِإِذْنِهِ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ وَسِعَ كُرْسِيُّهُ السَّمَوَاتِ وَالْأَرْضَ وَلَا يَئُودُهُ حِفْظُهُمَا وَهُوَ الْعَلِيُّ الْعَظِيمُ",
            "সূরা আল-কাফিরূন (সকাল ও সন্ধ্যায় ৩ বার)" to "بِسْمِ اللهِ الرَّحْمَنِ الرَّحِيمِ\nقُلْ يَأَيُّهَا الْكُفِرُونَ (١) لَا أَعْبُدُ مَا تَعْبُدُونَ (۲) وَلَا أَنْتُمْ عُبِدُونَ مَا أَعْبُدُ (٣) وَلَا أَنَا عَابِدٌ مَّا عَبَدْتُمْ (٤) وَلَا أَنْتُمْ عُبِدُونَ مَا أَعْبُدُ (٥) لَكُمْ دِينَكُمْ وَلِيَ دِينِ (٦)",
            "সূরা আল-ইখলাস (সকাল ও সন্ধ্যায় ৩ বার)" to "بِسْمِ اللهِ الرَّحْمَنِ الرَّحِيمِ\nقُلْ هُوَ اللهُ أَحَدٌ (۱) اللهُ الصَّمَدُ (۲) لَمْ يَلِدْ وَلَمْ يُولَدُ (۳) وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ (٤)",
            "সূরা আল-ফালাক (সকাল ও সন্ধ্যায় ৩ বার)" to "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ\nقُلْ أَعُوذُ بِرَبِّ الْفَلَقِ (1) مِنْ شَرِّ مَا خَلَقَ (۲) وَمِنْ شَرِّ غَاسِقٍ إِذَا وَقَبَ (۳) وَمِنْ شَرِّ النَّفْثَتِ فِي الْعُقَدِ (٤) وَمِنْ شَرِّ حَاسِدٍ إِذَا حَسَدَ (٥)",
            "সূরা আন-নাস (সকাল ও সন্ধ্যায় ৩ বার)" to "بِسْمِ اللهِ الرَّحْمَنِ الرَّحِيمِ\nقُلْ أَعُوذُ بِرَبِّ النَّاسِ (۱) مَلِكِ النَّاسِ (۲) إِلَهِ النَّاسِ (۳) مِنْ شَرِّ الْوَسْوَاسِ الْخَنَّاسِ (٤) الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ (٥) مِنَ الْجِنَّةِ وَالنَّاسِ (٦)",
            "সকাল ও সন্ধ্যায় ৩ বার" to "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ الَّتِي لَا يُجَاوِزُهُنَّ بَرَّ وَلَا فَاجِرٌ ، مِنْ شَرِّ مَا خَلَقَ وَ مَا خَلَقَ وَذَرَأَ وَبَرَأَ، وَمِنْ شَرِّ مَا يَنْزِلُ مِنَ السَّمَاءِ ، وَمِنْ شَرِّ مَا يَعْرُجُ فِيهَا ، وَمِنْ شَرِّ مَا ذَرَأَ فِي الْأَرْضِ، وَمِنْ شَرِّ مَا يَخْرُجُ مِنْهَا، وَمِنْ شَرِّ طَوَارِقِ اللَّيْلِ وَالنَّهَارِ، وَمِنْ شَرِّ كُلِّ طَارِقٍ إِلَّا طَارِقَا يَطْرُقُ بِخَيْرٍ يَا رَحْمَنُ.",
            "সকাল ও সন্ধ্যায় ১০ বার" to "سُبْحَانَ اللَّهِ ، وَالْحَمْدُ لِلَّهِ ، وَلَا إِلَهَ إِلَّا اللَّهُ ، وَاللَّهُ أَكْبَرُ ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ الْعَلِيِّ الْعَظِيمِ، هُوَ الْأَوَّلُ وَالْآخِرُ وَالظَّاهِرُ وَالْبَاطِنُ، بِيَدِهِ الْخَيْرُ ، يُحْيِي وَيُمِيتُ ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            "সকাল ও সন্ধ্যায় ১০০ বার" to "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ ، وَلَهُ الْحَمْدُ ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            "সকাল ও সন্ধ্যায় ৩ বার" to "أَعُوذُ بِكَلِمَاتِ اللهِ التَّامَّةِ مِنْ غَضَبِهِ وَعِقَابِهِ وَشَرِّ عِبَادِهِ وَمِنْ هَمَزَاتِ الشَّيَاطِينِ وَأَنْ يَحْضُرُونِ",
            "সকাল ও সন্ধ্যায় ৭ বার" to "حَسْبِيَ اللَّهُ لَا إِلَهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ ، وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ",
            "সকাল ও সন্ধ্যায় ৩ বার" to "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
            "সায়্যিদুল ইস্তিগফার (সকাল ও সন্ধ্যায় ১ বার)" to "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ لَكَ بِذَنْبِي فَاغْفِرْ لِي، فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
            "ঘুমানোর পূর্বের আমলসমূহ" to "১. ওযু করে ঘুমানো।\n২. শোয়ার পূর্বে বিস্মিল্লাহ পড়ে বিছানা ঝেড়ে শোয়া।\n৩. আয়াতুল কুরসি পড়া।\n৪. সূরা এখলাস, সূরা ফালাক এবং সূরা নাস পড়ে হাতের তালুতে ফু দিয়ে সমস্ত শরীরে হাত বুলিয়ে ঘুমানো।\n৫. ঘুমের দোয়া: (اللَّهُمَّ بِاسْمِكَ أَمُوتُ وَأَحْيَا)\n৬. ঘুম থেকে উঠে দোয়া: (الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ)"
        )

        for (item in items) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 16, 20, 16)
                val bg = GradientDrawable()
                bg.setColor(getCardBgColor())
                bg.cornerRadius = 20f
                bg.setStroke(1, Color.parseColor("#CBD5E1"))
                background = bg
                val lp = LinearLayout.LayoutParams(-1, -2)
                lp.setMargins(0, 0, 0, 14)
                layoutParams = lp
            }

            val t = TextView(this).apply {
                text = item.first
                textSize = 15.5f
                typeface = Typeface.SERIF
                setTextColor(accent)
                setTypeface(Typeface.SERIF, Typeface.BOLD)
                setPadding(0, 0, 0, 6)
            }
            val b = TextView(this).apply {
                text = item.second
                textSize = 17f
                typeface = Typeface.SERIF
                setTextColor(getTextColor())
                setLineSpacing(10f, 1.2f)
            }

            card.addView(t)
            card.addView(b)
            content.addView(card)
        }

        val btnBack = Button(this).apply {
            text = "⬅ ফোল্ডার তালিকায় ফিরে যান"
            typeface = Typeface.SERIF
            setBackgroundColor(if (isWhiteTheme()) Color.parseColor("#0F766E") else Color.parseColor("#264536"))
            setTextColor(Color.WHITE)
            setOnClickListener { openScreen("amal_folders") }
        }
        content.addView(btnBack)

        scroll.addView(content)
        root.addView(scroll)
        root.addView(createNavBar("amal_folders"))
        setContentView(root)
    }

    // ফোল্ডার ২-এর বিস্তারিত রিডার: মানযিল আয়াত
    private fun showManzilPdfScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getThemeBackground()
        }

        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(26, 26, 26, 26) }
        val accent = getAccentColor()

        val h = TextView(this).apply {
            text = "মানযিল (কুরআনী হিফাযত)"
            textSize = 20f
            typeface = Typeface.SERIF
            setTextColor(accent)
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        content.addView(h)

        val manzilAyats = listOf(
            "১. সূরা আল-ইনশিকাক (১-২৫)" to "بِسْمِ اللهِ الرَّحْمَنِ الرَّحِيمِ\nإِذَا السَّمَاءُ انْشَقَّتْ ، وَأَذِنَتْ لِرَبِّهَا وَحُقَّتْ ، وَإِذَا الْأَرْضُ مُدَّتْ وَأَلْقَتْ مَا فِيهَا وَتَخَلَّتْ ، وَأَذِنَتْ لِرَبِّهَا وَحُقَّتْ ، يَأَيُّهَا الْإِنْسَانُ إِنَّكَ كَادِحٌ إِلَى رَبِّكَ كَدْحًا فَمُلَقِيهِ ، فَأَمَّا مَنْ أُوتِيَ كِتَبَهُ بِيَمِينِهِ ، فَسَوْفَ يُحَاسَبُ حِسَابًا يَسِيرًا ، وَيَنْقَلِبُ إِلَى أَهْلِهِ مَسْرُورًا ، وَأَمَّا مَنْ أُوتِيَ كِتَبَهُ وَرَاءَ ظَهْرِهِ ، فَسَوْفَ يَدْعُوا ثُبُورًا ، وَيَصْلَى سَعِيرًا ، إِنَّهُ كَانَ فِي أَهْلِهِ مَسْرُورًا ، إِنَّهُ ظَنَّ أَنْ لَنْ يَحُورَ ، بَلَى إِنَّ رَبَّهُ كَانَ بِهِ بَصِيرًا ، فَلَا أُقْسِمُ بِالشَّفَقِ ، وَالَّيْلِ وَمَا وَسَقَ ، وَالْقَمَرِ إِذَا اتَّسَقَ ، لَتَرْكَبُنَّ طَبَقًا عَنْ طَبَقٍ ، فَمَا لَهُمْ لَا يُؤْمِنُونَ ، وَإِذَا قُرِئَ عَلَيْهِمُ الْقُرْآنُ لَا يَسْجُدُونَ ، بَلِ الَّذِينَ كَفَرُوا يُكَذِّبُونَ ، وَاللَّهُ أَعْلَمُ بِمَا يُوعُونَ ، فَبَشِّرْهُمْ بِعَذَابٍ أَلِيمٍ ، إِلَّا الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَتِ لَهُمْ أَجْرٌ غَيْرُ مَمْنُونٍ",
            "২. সূরা আল-ফাতিহা" to "بِسْمِ اللهِ الرَّحْمَنِ الرَّحِيمِ\nالْحَمْدُ لِلَّهِ رَبِّ الْعَلَمِينَ ، الرَّحْمَنِ الرَّحِيمِ ، مَلِكِ يَوْمِ الدِّينِ ، إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ ، اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ ، صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ",
            "৩. সূরা আল-বাকারাহ (১-৫)" to "بِسْمِ اللهِ الرَّحْمَنِ الرَّحِيمِ\nالم ، ذَلِكَ الْكِتَبُ لَا رَيْبَ فِيهِ هُدًى لِلْمُتَّقِينَ ، الَّذِينَ يُؤْمِنُونَ بِالْغَيْبِ وَيُقِيمُونَ الصَّلوةَ وَمِمَّا رَزَقْنَاهُمْ يُنْفِقُونَ ، وَالَّذِينَ يُؤْمِنُونَ بِمَا أُنْزِلَ إِلَيْكَ وَمَا أُنْزِلَ مِنْ قَبْلِكَ وَبِالْآخِرَةِ هُمْ يُوقِنُونَ ، أُولَئِكَ عَلَى هُدًى مِنْ رَبِّهِمْ وَأُولَئِكَ هُمُ الْمُفْلِحُونَ",
            "৪. আয়াতুল কুরসি ও বাকারাহ শেষ রুকু" to "وَإِلَهُكُمْ إِلَهٌ وَاحِدٌ لَا إِلَهَ إِلَّا هُوَ الرَّحْمَنُ الرَّحِيمُ\n\nاللهُ لَا إِلَهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ لَهُ مَا فِي السَّمَوَاتِ وَمَا فِي الْأَرْضِ مَنْ ذَا الَّذِي يَشْفَعُ عِنْدَهُ إِلَّا بِإِذْنِهِ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ وَلَا يُحِيطُونَ بِشَيْءٍ مِنْ عِلْمِهِ إِلَّا بِمَا شَاءَ وَسِعَ كُرْسِيُّهُ السَّمَوَاتِ وَالْأَرْضَ وَلَا يَئُودُهُ حِفْظُهُمَا وَهُوَ الْعَلِيُّ الْعَظِيمُ\n\nآمَنَ الرَّسُولُ بِمَا أُنْزِلَ إِلَيْهِ مِنْ رَّبِّهِ وَالْمُؤْمِنُونَ كُلٌّ آمَنَ بِاللَّهِ وَمَلَئِكَتِهِ وَكُتُبِهِ وَرُسُلِهِ لَا نُفَرِّقُ بَيْنَ أَحَدٍ مِنْ رُسُلِهِ وَقَالُوا سَمِعْنَا وَأَطَعْنَا غُفْرَانَكَ رَبَّنَا وَإِلَيْكَ الْمَصِيرُ ، لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا لَهَا مَا كَسَبَتْ وَعَلَيْهَا مَا اكْتَسَبَتْ رَبَّنَا لَا تُؤَاخِذْنَا إِنْ نَسِينَا أَوْ أَخْطَأْنَا رَبَّنَا وَلَا تَحْمِلْ عَلَيْنَا إِصْرًا كَمَا حَمَلْتَهُ عَلَى الَّذِينَ مِنْ قَبْلِنَا رَبَّنَا وَلَا تُحَمِّلْنَا مَا لَا طَاقَةَ لَنَا بِهِ وَاعْفُ عَنَّا وَاغْفِرْ لَنَا وَارْحَمْنَا أَنْتَ مَوْلَانَا فَانْصُرْنَا عَلَى الْقَوْمِ الْكَفِرِينَ",
            "৫. সূরা আলে ইমরান (১৮, ২৬-২৭)" to "شَهِدَ اللَّهُ أَنَّهُ لَا إِلَهَ إِلَّا هُوَ وَالْمَلَئِكَةُ وَأُولُوا الْعِلْمِ قَائِمًا بِالْقِسْطِ لَا إِلَهَ إِلَّا هُوَ الْعَزِيزُ الْحَكِيمُ\n\nقُلِ اللَّهُمَّ مَلِكَ الْمُلْكِ تُؤْتِي الْمُلْكَ مَنْ تَشَاءُ وَتَنْزِعُ الْمُلْكَ مِمَّنْ تَشَاءُ وَتُعِزُّ مَنْ تَشَاءُ وَتُذِلُّ مَنْ تَشَاءُ بِيَدِكَ الْخَيْرُ إِنَّكَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ ، تُولِجُ الَّيْلَ فِي النَّهَارِ وَتُولِجُ النَّهَارَ فِي الَّيْلِ وَتُخْرِجُ الْحَيَّ مِنَ الْمَيِّتِ وَتُخْرِجُ الْمَيِّتَ مِنَ الْحَيِّ وَتَرْزُقُ مَنْ تَشَاءُ بِغَيْرِ حِسَابٍ",
            "৬. সূরা আল-আ'রাফ (৫৪-৫৬)" to "إِنَّ رَبَّكُمُ اللَّهُ الَّذِي خَلَقَ السَّمَوَاتِ وَالْأَرْضَ فِي سِتَّةِ أَيَّامٍ ثُمَّ اسْتَوَى عَلَى الْعَرْشِ يُغْشِي الَّيْلَ النَّهَارَ يَطْلُبُهُ حَثِيثًا وَالشَّمْسَ وَالْقَمَرَ وَالنُّجُومَ مُسَخَّرَاتٍ بِأَمْرِهِ أَلَا لَهُ الْخَلْقُ وَالْأَمْرُ تَبَارَكَ اللَّهُ رَبُّ الْعَلَمِينَ ، ادْعُوا رَبَّكُمْ تَضَرُّعًا وَخُفْيَةً إِنَّهُ لَا يُحِبُّ الْمُعْتَدِينَ ، وَلَا تُفْسِدُوا فِي الْأَرْضِ بَعْدَ إِصْلَاحِهَا وَادْعُوهُ خَوْفًا وَطَمَعًا إِنَّ رَحْمَتَ اللَّهِ قَرِيبٌ مِّنَ الْمُحْسِنِينَ",
            "৭. সূরা আল-ইসরা (১১০-১১১) ও আল-মুমিনূন (১১৫-১১৮)" to "قُلِ ادْعُوا اللَّهَ أَوِ ادْعُوا الرَّحْمَنَ أَيًّا مَّا تَدْعُوا فَلَهُ الْأَسْمَاءُ الْحُسْنَى وَلَا تَجْهَرْ بِصَلَاتِكَ وَلَا تُخَافِتْ بِهَا وَابْتَغِ بَيْنَ ذَلِكَ سَبِيلًا ، وَقُلِ الْحَمْدُ لِلَّهِ الَّذِي لَمْ يَتَّخِذْ وَلَدًا وَلَمْ يَكُنْ لَهُ شَرِيكٌ فِي الْمُلْكِ وَلَمْ يَكُنْ لَهُ وَلِيٌّ مِنَ الذُّلِّ وَكَبِّرْهُ تَكْبِيرًا\n\nأَفَحَسِبْتُمْ أَنَّمَا خَلَقْنَاكُمْ عَبَثًا وَأَنَّكُمْ إِلَيْنَا لَا تُرْجَعُونَ ، فَتَعَالَى اللَّهُ الْمَلِكُ الْحَقُّ لَا إِلَهَ إِلَّا هُوَ رَبُّ الْعَرْشِ الْكَرِيمِ ، وَمَنْ يَدْعُ مَعَ اللَّهِ إِلَهًا آخَرَ لَا بُرْهَانَ لَهُ بِهِ فَإِنَّمَا حِسَابُهُ عِنْدَ رَبِّهِ إِنَّهُ لَا يُفْلِحُ الْكَفِرُونَ ، وَقُلْ رَبِّ اغْفِرْ وَارْحَمْ وَأَنْتَ خَيْرُ الرَّاحِمِينَ",
            "৮. সূরা আস-সাফফাত (১-১১)" to "بِسْمِ اللهِ الرَّحْمَنِ الرَّحِيمِ\nوَالصَّافَّاتِ صَفًّا ، فَالزَّاجِرَاتِ زَجْرًا ، فَالتَّالِيَاتِ ذِكْرًا ، إِنَّ إِلَهَكُمْ لَوَاحِدٌ ، رَبُّ السَّمَوَاتِ وَالْأَرْضِ وَمَا بَيْنَهُمَا وَرَبُّ الْمَشَارِقِ ، إِنَّا زَيَّنَّا السَّمَاءَ الدُّنْيَا بِزِينَةٍ الْكَوَاكِبِ ، وَحِفْظًا مِنْ كُلِّ شَيْطَانٍ مَارِدٍ ، لَا يَسَّمَّعُونَ إِلَى الْمَلَإِ الْأَعْلَى وَيُقْذَفُونَ مِنْ كُلِّ جَانِبٍ ، دُحُورًا وَلَهُمْ عَذَابٌ وَاصِبٌ ، إِلَّا مَنْ خَطِفَ الْخَطْفَةَ فَأَتْبَعَهُ شِهَابٌ ثَاقِبٌ ، فَاسْتَفْتِهِمْ أَهُمْ أَشَدُّ خَلْقًا أَمْ مَّنْ خَلَقْنَا إِنَّا خَلَقْنَاهُمْ مِنْ طِينٍ لَازِبٍ",
            "৯. সূরা আর-রহমান (৩৩-৩৭) ও আল-হাশর (২১-২৪)" to "يَا مَعْشَرَ الْجِنِّ وَالْإِنْسِ إِنِ اسْتَطَعْتُمْ أَنْ تَنْفُذُوا مِنْ أَقْطَارِ السَّمَوَاتِ وَالْأَرْضِ فَانْفُذُوا لَا تَنْفُذُونَ إِلَّا بِسُلْطَانٍ ، فَبِأَيِّ آلَاءِ رَبِّكُمَا تُكَذِّبَانِ ، يُرْسَلُ عَلَيْكُمَا شُوَاظٌ مِنْ نَارٍ وَنُحَاسٌ فَلَا تَنْتَصِرَانِ ، فَبِأَيِّ آلَاءِ رَبِّكُمَا تُكَذِّبَانِ ، فَإِذَا انْشَقَّتِ السَّمَاءُ فَكَانَتْ وَرْدَةً كَالدِّهَانِ ، فَبِأَيِّ آلَاءِ رَبِّكُمَا تُكَذِّبَانِ\n\nلَوْ أَنْزَلْنَا هَذَا الْقُرْآنَ عَلَى جَبَلٍ لَرَأَيْتَهُ خَاشِعًا مُتَصَدِّعًا مِنْ خَشْيَةِ اللَّهِ وَتِلْكَ الْأَمْثَالُ نَضْرِبُهَا لِلنَّاسِ لَعَلَّهُمْ يَتَفَكَّরُونَ ، هُوَ اللَّهُ الَّذِي لَا إِلَهَ إِلَّا هُوَ عَالِمُ الْغَيْبِ وَالشَّهَادَةِ هُوَ الرَّحْمَنُ الرَّحِيمُ ، هُوَ اللَّهُ الَّذِي لَا إِلَهَ إِلَّا هُوَ الْمَلِكُ الْقُدُّوسُ السَّلَامُ الْمُؤْمِنُ الْمُهَيْمِنُ الْعَزِيزُ الْجَبَّارُ الْمُتَكَبِّرُ سُبْحَانَ اللَّهِ عَمَّا يُشْرِكُونَ ، هُوَ اللَّهُ الْخَالِقُ الْبَارِئُ الْمُصَوِّرُ لَهُ الْأَسْمَاءُ الْحُسْنَى يُسَبِّحُ لَهُ مَا فِي السَّمَوَاتِ وَالْأَرْضِ وَهُوَ الْعَزِيزُ الْحَكِيمُ",
            "১০. সূরা আল-জিন (১-৪) ও শেষ ৪ কুল" to "بِسْمِ اللهِ الرَّحْمَنِ الرَّحِيمِ\nقُلْ أُوحِيَ إِلَيَّ أَنَّهُ اسْتَمَعَ نَفَرٌ مِنَ الْجِنِّ فَقَالُوا إِنَّا سَمِعْنَا قُرْآنًا عَجَبًا ، يَهْدِي إِلَى الرُّشْدِ فَآمَنَّا بِهِ وَلَنْ نُشْرِكَ بِرَبِّنَا أَحَدًا ، وَأَنَّهُ تَعَالَى جَدُّ رَبِّنَا مَا اتَّخَذَ صَاحِبَةً وَلَا وَلَدًا ، وَأَنَّهُ كَانَ يَقُولُ سَفِيهُنَا عَلَى اللَّهِ شَطَطًا\n\n(সূরা আল-কাফিরূন, সূরা আল-ইখলাস, সূরা আল-ফালাক, সূরা আন-নাস)"
        )

        for (item in manzilAyats) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 16, 20, 16)
                val bg = GradientDrawable()
                bg.setColor(getCardBgColor())
                bg.cornerRadius = 20f
                bg.setStroke(1, Color.parseColor("#CBD5E1"))
                background = bg
                val lp = LinearLayout.LayoutParams(-1, -2)
                lp.setMargins(0, 0, 0, 14)
                layoutParams = lp
            }

            val t = TextView(this).apply {
                text = item.first
                textSize = 15.5f
                typeface = Typeface.SERIF
                setTextColor(accent)
                setTypeface(Typeface.SERIF, Typeface.BOLD)
                setPadding(0, 0, 0, 6)
            }
            val b = TextView(this).apply {
                text = item.second
                textSize = 17f
                typeface = Typeface.SERIF
                setTextColor(getTextColor())
                setLineSpacing(10f, 1.2f)
            }

            card.addView(t)
            card.addView(b)
            content.addView(card)
        }

        val btnBack = Button(this).apply {
            text = "⬅ ফোল্ডার তালিকায় ফিরে যান"
            typeface = Typeface.SERIF
            setBackgroundColor(if (isWhiteTheme()) Color.parseColor("#0F766E") else Color.parseColor("#264536"))
            setTextColor(Color.WHITE)
            setOnClickListener { openScreen("amal_folders") }
        }
        content.addView(btnBack)

        scroll.addView(content)
        root.addView(scroll)
        root.addView(createNavBar("amal_folders"))
        setContentView(root)
    }

    // ৫. সিহাহ সিত্তাহ হাদীস কিতাব ভাণ্ডার
    private fun showHadithScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getThemeBackground()
        }

        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(26, 26, 26, 26) }
        val accent = getAccentColor()

        val h = TextView(this).apply {
            text = "📚 সিহাহ সিত্তাহ (বিশুদ্ধ হাদীস কিতাব)"
            textSize = 19f
            typeface = Typeface.SERIF
            setTextColor(accent)
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            setPadding(0, 0, 0, 8)
        }
        val desc = TextView(this).apply {
            text = "আরবি মূল মতন ও পূর্ণাঙ্গ বাংলা অনুবাদসহ সরাসরি অধ্যয়ন করুন:"
            textSize = 13f
            typeface = Typeface.SERIF
            setTextColor(getSecondaryTextColor())
            setPadding(0, 0, 0, 16)
        }
        content.addView(h)
        content.addView(desc)

        val hadithBooks = listOf(
            Triple("১. সহীহ বুখারী (Sahih al-Bukhari)", "সর্বমোট হাদিস: ৭৫৬৩ টি", "https://sunnah.com/bukhari"),
            Triple("২. সহীহ মুসলিম (Sahih Muslim)", "সর্বমোট হাদিস: ৭৫০০ টি", "https://sunnah.com/muslim"),
            Triple("৩. জামে আত-তিরমিযী (Jami` at-Tirmidhi)", "সর্বমোট হাদিস: ৩৯৫৬ টি", "https://sunnah.com/tirmidhi"),
            Triple("৪. সুনান আবু দাউদ (Sunan Abi Dawud)", "সর্বমোট হাদিস: ৫২৭৪ টি", "https://sunnah.com/abudawud"),
            Triple("৫. সুনান আন-নাসায়ী (Sunan an-Nasa'i)", "সর্বমোট হাদিস: ৫৭৫৮ টি", "https://sunnah.com/nasai"),
            Triple("৬. সুনান ইবনে মাজাহ (Sunan Ibn Majah)", "সর্বমোট হাদিস: ৪৩৪১ টি", "https://sunnah.com/ibnmajah"),
            Triple("📖 আল-কুরআনুল কারীম (অনলাইন তাফসির)", "সম্পূর্ণ ১১৪ সুরা আরবি ও বাংলা", "https://quran.com/bn")
        )

        for (book in hadithBooks) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 16, 20, 16)
                val bg = GradientDrawable()
                bg.setColor(getCardBgColor())
                bg.cornerRadius = 20f
                bg.setStroke(1, Color.parseColor("#CBD5E1"))
                background = bg
                val lp = LinearLayout.LayoutParams(-1, -2)
                lp.setMargins(0, 0, 0, 14)
                layoutParams = lp
            }

            val bName = TextView(this).apply {
                text = book.first
                textSize = 15.5f
                typeface = Typeface.SERIF
                setTextColor(getTextColor())
                setTypeface(Typeface.SERIF, Typeface.BOLD)
            }
            val bInfo = TextView(this).apply {
                text = book.second
                textSize = 12.5f
                typeface = Typeface.SERIF
                setTextColor(getSecondaryTextColor())
                setPadding(0, 4, 0, 10)
            }

            val btnRead = Button(this).apply {
                text = "📖 হাদীস পড়ুন (আরবি ও বাংলা)"
                textSize = 12.5f
                typeface = Typeface.SERIF
                setBackgroundColor(if (isWhiteTheme()) Color.parseColor("#0F766E") else Color.parseColor("#264536"))
                setTextColor(Color.WHITE)
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(book.third))
                    startActivity(intent)
                }
            }

            card.addView(bName)
            card.addView(bInfo)
            card.addView(btnRead)
            content.addView(card)
        }

        scroll.addView(content)
        root.addView(scroll)
        root.addView(createNavBar("hadith"))
        setContentView(root)
    }

    // ৬. ব্যক্তিগত ইসলামিক নোটপ্যাড
    private fun showNotepadScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getThemeBackground()
        }

        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(26, 26, 26, 26) }
        val accent = getAccentColor()

        val h = TextView(this).apply {
            text = "📝 ইসলামিক নোটপ্যাড"
            textSize = 19f
            typeface = Typeface.SERIF
            setTextColor(accent)
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            setPadding(0, 0, 0, 14)
        }
        content.addView(h)

        val btnAddNote = Button(this).apply {
            text = "➕ নতুন নোট যুক্ত করুন"
            setBackgroundColor(accent)
            setTextColor(Color.BLACK)
            textSize = 14.5f
            typeface = Typeface.SERIF
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 0, 0, 18)
            layoutParams = lp
            setOnClickListener { showNoteEditDialog(null) }
        }
        content.addView(btnAddNote)

        if (noteList.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "কোনো নোট সংরক্ষিত নেই। উপরে বাটনে চাপ দিয়ে যেকোনো আমল বা প্রয়োজনীয় তথ্য লিখে রাখুন।"
                textSize = 14f
                typeface = Typeface.SERIF
                setTextColor(getSecondaryTextColor())
                gravity = Gravity.CENTER
                setPadding(20, 60, 20, 40)
            }
            content.addView(emptyText)
        } else {
            for (note in noteList) {
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(20, 16, 20, 16)
                    val bg = GradientDrawable()
                    bg.setColor(getCardBgColor())
                    bg.cornerRadius = 20f
                    bg.setStroke(1, Color.parseColor("#CBD5E1"))
                    background = bg
                    val lp = LinearLayout.LayoutParams(-1, -2)
                    lp.setMargins(0, 0, 0, 14)
                    layoutParams = lp
                }

                val nTitle = TextView(this).apply {
                    text = note.title
                    textSize = 16f
                    typeface = Typeface.SERIF
                    setTextColor(getTextColor())
                    setTypeface(Typeface.SERIF, Typeface.BOLD)
                }
                val nDate = TextView(this).apply {
                    text = "তারিখ: ${note.date}"
                    textSize = 11.5f
                    typeface = Typeface.SERIF
                    setTextColor(getSecondaryTextColor())
                    setPadding(0, 2, 0, 8)
                }
                val nContent = TextView(this).apply {
                    text = note.content
                    textSize = 14f
                    typeface = Typeface.SERIF
                    setTextColor(getTextColor())
                    setPadding(0, 0, 0, 12)
                }

                val rowAction = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
                val btnEdit = Button(this).apply {
                    text = "এডিট"
                    textSize = 12f
                    typeface = Typeface.SERIF
                    setOnClickListener { showNoteEditDialog(note) }
                }
                val btnDel = Button(this).apply {
                    text = "ডিলিট"
                    textSize = 12f
                    typeface = Typeface.SERIF
                    setBackgroundColor(Color.parseColor("#DC2626"))
                    setTextColor(Color.WHITE)
                    val lp = LinearLayout.LayoutParams(-2, -2)
                    lp.setMargins(10, 0, 0, 0)
                    layoutParams = lp
                    setOnClickListener {
                        noteList.remove(note)
                        saveAllData()
                        showNotepadScreen()
                    }
                }

                rowAction.addView(btnEdit)
                rowAction.addView(btnDel)

                card.addView(nTitle)
                card.addView(nDate)
                card.addView(nContent)
                card.addView(rowAction)
                content.addView(card)
            }
        }

        scroll.addView(content)
        root.addView(scroll)
        root.addView(createNavBar("notepad"))
        setContentView(root)
    }

    private fun showNoteEditDialog(existingNote: NoteItem?) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 16, 36, 16)
        }

        val titleInput = EditText(this).apply {
            hint = "নোটের টাইটেল"
            setText(existingNote?.title ?: "")
            typeface = Typeface.SERIF
        }

        val contentInput = EditText(this).apply {
            hint = "নোটের বিবরণ..."
            setText(existingNote?.content ?: "")
            typeface = Typeface.SERIF
            minLines = 4
            gravity = Gravity.TOP
        }

        layout.addView(titleInput)
        layout.addView(contentInput)

        AlertDialog.Builder(this)
            .setTitle(if (existingNote == null) "নতুন নোট সংরক্ষণ" else "নোট এডিট করুন")
            .setView(layout)
            .setPositiveButton("সেভ করুন") { _, _ ->
                val t = titleInput.text.toString().trim()
                val c = contentInput.text.toString().trim()
                if (t.isNotEmpty() || c.isNotEmpty()) {
                    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("bn", "BD")).format(Date())
                    if (existingNote == null) {
                        noteList.add(0, NoteItem(UUID.randomUUID().toString(), t.ifEmpty { "শিরোনামহীন নোট" }, c, toBangla(dateStr)))
                    } else {
                        existingNote.title = t.ifEmpty { "শিরোনামহীন নোট" }
                        existingNote.content = c
                    }
                    saveAllData()
                    showNotepadScreen()
                }
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    // ৭. প্রোফাইল ও ডেভেলপার সাব্বির আহমাদ পরিচিতি
    private fun showSettingsScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = getThemeBackground()
        }

        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(26, 26, 26, 26) }
        val accent = getAccentColor()

        val h = TextView(this).apply {
            text = "প্রোফাইল, ব্যাকআপ ও সেটিংস"
            textSize = 20f
            typeface = Typeface.SERIF
            setTextColor(accent)
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            setPadding(0, 0, 0, 18)
        }
        content.addView(h)

        // ডেভেলপার ও উদ্যোক্তা পরিচিতি
        val devCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 18, 22, 18)
            val bg = GradientDrawable()
            bg.setColor(if (isWhiteTheme()) Color.parseColor("#FEF3C7") else Color.parseColor("#223329"))
            bg.cornerRadius = 24f
            bg.setStroke(2, accent)
            background = bg
        }

        val devTitle = TextView(this).apply {
            text = "🌟 অ্যাপ উদ্যোক্তা ও পরিচালক"
            textSize = 16.5f
            typeface = Typeface.SERIF
            setTextColor(getTextColor())
            setTypeface(Typeface.SERIF, Typeface.BOLD)
        }
        val devName = TextView(this).apply {
            text = "নাম: সাব্বির আহমাদ"
            textSize = 15f
            typeface = Typeface.SERIF
            setTextColor(getTextColor())
            setPadding(0, 6, 0, 2)
        }
        val devPhone = TextView(this).apply {
            text = "মোবাইল: ০১৭২৫-২২৮৬২২"
            textSize = 15f
            typeface = Typeface.SERIF
            setTextColor(if (isWhiteTheme()) Color.parseColor("#059669") else Color.parseColor("#86EFAC"))
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            setPadding(0, 0, 0, 8)
        }
        val btnCall = Button(this).apply {
            text = "📞 সরাসরি কল করুন"
            textSize = 13f
            typeface = Typeface.SERIF
            setBackgroundColor(Color.parseColor("#059669"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:01725228622"))
                startActivity(intent)
            }
        }

        devCard.addView(devTitle)
        devCard.addView(devName)
        devCard.addView(devPhone)
        devCard.addView(btnCall)
        content.addView(devCard)

        // জিমেইল ক্লাউড ব্যাকআপ
        val backupCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
            val bg = GradientDrawable()
            bg.setColor(getCardBgColor())
            bg.cornerRadius = 20f
            bg.setStroke(1, Color.parseColor("#CBD5E1"))
            background = bg
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 18, 0, 0)
            layoutParams = lp
        }

        val bTitle = TextView(this).apply {
            text = "📧 জিমেইল ক্লাউড ব্যাকআপ (মাল্টি ডিভাইস)"
            textSize = 15.5f
            typeface = Typeface.SERIF
            setTextColor(getTextColor())
            setTypeface(Typeface.SERIF, Typeface.BOLD)
        }
        val bDesc = TextView(this).apply {
            text = if (userEmail.isEmpty()) "কোনো জিমেইল সংযুক্ত নেই। অন্য ফোনে সব নোট ও জিকির পেতে আপনার Gmail আইডি যুক্ত করুন।" else "সংযুক্ত ক্লাউড জিমেইল: $userEmail\n(সব ডাটা স্বয়ংক্রিয়ভাবে সংরক্ষিত হচ্ছে)"
            textSize = 12.5f
            typeface = Typeface.SERIF
            setTextColor(getSecondaryTextColor())
            setPadding(0, 4, 0, 12)
        }
        val btnLogin = Button(this).apply {
            text = if (userEmail.isEmpty()) "জিমেইল আইডি যুক্ত করুন" else "জিমেইল পরিবর্তন / সিঙ্ক করুন"
            typeface = Typeface.SERIF
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
            text = "\n🎨 অ্যাপ থিম নির্বাচন করুন:"
            textSize = 15f
            typeface = Typeface.SERIF
            setTextColor(getTextColor())
            setPadding(0, 10, 0, 10)
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
                typeface = Typeface.SERIF
                setBackgroundColor(Color.parseColor(th.second))
                setTextColor(if (th.first.contains("সাদা")) Color.BLACK else Color.WHITE)
                textSize = 14.5f
                val lp = LinearLayout.LayoutParams(-1, -2)
                lp.setMargins(0, 6, 0, 6)
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
            hint = "আপনার Gmail লিখুন (যেমন: sabbir@gmail.com)"
            setText(userEmail)
            typeface = Typeface.SERIF
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
