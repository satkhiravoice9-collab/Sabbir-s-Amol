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
            else -> GradientDrawable(
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
            Triple("থিম", "settings", "🎨")
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
                bg.setStroke(if (item.id == activeZikrId) 3 else 1, if (item.id == activeZikrId) accent else Color.parseColor("#2F503F"))
                background = bg
                val lp = LinearLayout.LayoutParams(-1, -2)
                lp.setMargins(0, 0, 0, 16)
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
                bg.setColor(Color.parseColor("#3B6852"))
                bg.cornerRadius = 40f
                background = bg
                setPadding(24, 12, 24, 12)
            }

            val nameView = TextView(this).apply {
                text = item.name
                textSize = 17f
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                val lp = LinearLayout.LayoutParams(0, -2, 1f)
                lp.setMargins(20, 0, 10, 0)
                layoutParams = lp
            }

            topInfo.addView(countBadge)
            topInfo.addView(nameView)
            card.addView(topInfo)

            val targetText = TextView(this).apply {
                text = "টার্গেট: ${item.target} বার (গোণা হয়েছে: ${item.count} বার)"
                textSize = 13f
                setTextColor(Color.parseColor("#A0BDB0"))
                setPadding(0, 10, 0, 14)
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

            val btnEdit = Button(this).apply {
                text = "✏️ এডিট"
                textSize = 12f
                val lp = LinearLayout.LayoutParams(-2, -2)
                lp.setMargins(12, 0, 12, 0)
                layoutParams = lp
                setOnClickListener { showEditZikrDialog(item) }
            }

            val btnDelete = Button(this).apply {
                text = "Delete"
                textSize = 12f
                setBackgroundColor(Color.parseColor("#991B1B"))
                setTextColor(Color.WHITE)
                setOnClickListener {
                    if (zikrList.size <= 1) {
                        Toast.makeText(this@MainActivity, "কমপক্ষে একটি জিকির থাকতে হবে!", Toast.LENGTH_SHORT).show()
                    } else {
                        zikrList.remove(item)
                        if (activeZikrId == item.id) {
                            activeZikrId = zikrList.first().id
                        }
                        saveAllData()
                        showZikrListScreen()
                    }
                }
            }

            actionsRow.addView(btnDelete)
            actionsRow.addView(btnEdit)
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

        val nameInput = EditText(this).apply {
            hint = "জিকিরের নাম লিখুন"
        }
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
                    val newItem = ZikrItem(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        count = 0,
                        target = if (target > 0) target else 33
                    )
                    zikrList.add(newItem)
                    activeZikrId = newItem.id
                    saveAllData()
                    showZikrListScreen()
                }
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    private fun showEditZikrDialog(item: ZikrItem) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val nameInput = EditText(this).apply {
            setText(item.name)
        }
        val targetInput = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("${item.target}")
        }

        layout.addView(nameInput)
        layout.addView(targetInput)

        AlertDialog.Builder(this)
            .setTitle("জিকির এডিট করুন")
            .setView(layout)
            .setPositiveButton("আপডেট") { _, _ ->
                val n = nameInput.text.toString().trim()
                val t = targetInput.text.toString().toIntOrNull() ?: item.target
                if (n.isNotEmpty()) {
                    item.name = n
                    item.target = if (t > 0) t else 33
                    saveAllData()
                    showZikrListScreen()
                }
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

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

        val accent = getAccentColor()

        val h = TextView(this).apply {
            text = "৪ কালার থিম সেটিংস"
            textSize = 22f
            setTextColor(accent)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        }
        content.addView(h)

        val themeInfo = TextView(this).apply {
            text = "পছন্দের থিম নির্বাচন করুন (বর্তমান: $currentTheme):"
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 16)
        }
        content.addView(themeInfo)

        val themes = listOf(
            "🕋 কাবা থিম (ডার্ক গোল্ড)" to "#1B2A22",
            "🕌 মদিনা থিম (এমারেল্ড গ্রিন)" to "#0E4D3A",
            "🌅 সুবহ-সাদিক থিম (রয়্যাল গোল্ড)" to "#422A0A",
            "🌌 লাইলাতুল কদর (নাইট ব্লু)" to "#0F2027"
        )

        for (th in themes) {
            val btn = Button(this).apply {
                text = th.first
                setBackgroundColor(Color.parseColor(th.second))
                setTextColor(Color.WHITE)
                textSize = 15f
                val lp = LinearLayout.LayoutParams(-1, -2)
                lp.setMargins(0, 12, 0, 12)
                layoutParams = lp
                setOnClickListener {
                    currentTheme = th.first
                    saveAllData()
                    showSettingsScreen()
                }
            }
            content.addView(btn)
        }

        root.addView(content)
        root.addView(createNavBar("settings"))
        setContentView(root)
    }
}
