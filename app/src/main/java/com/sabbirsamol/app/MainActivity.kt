package com.sabbirsamol.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
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

class MainActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("sabbirs_amol_db", Context.MODE_PRIVATE) }
    private val vibrator by lazy { getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

    private val screenStack = Stack<String>()
    private val zikrList = mutableListOf<ZikrItem>()
    private var activeZikrId: String = ""
    
    // ৪ টি স্বতন্ত্র থিম মোড
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

    // কাবা শরীফের কাস্টম আর্ট আইকন তৈরি
    private fun createKabaBitmap(): Bitmap {
        val size = 200
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // কাবা বেস বডি (কালো কিউব)
        paint.color = Color.parseColor("#151515")
        canvas.drawRoundRect(RectF(40f, 40f, 160f, 160f), 12f, 12f, paint)

        // কাবার গিলাফের সোনালী বেল্ট (কিসওয়াহ স্ট্রিপ)
        paint.color = Color.parseColor("#F5C358")
        canvas.drawRect(40f, 65f, 160f, 82f, paint)

        // কাবার দরজা (সোনালী গেট)
        paint.color = Color.parseColor("#D4AF37")
        canvas.drawRoundRect(RectF(110f, 100f, 140f, 160f), 6f, 6f, paint)

        return bitmap
    }

    // মদিনা শরীফের সবুজ গম্বুজ আইকন তৈরি
    private fun createMadinaDomeBitmap(): Bitmap {
        val size = 200
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // গম্বুজ বেস পিলার
        paint.color = Color.parseColor("#FFFFFF")
        canvas.drawRect(50f, 110f, 150f, 160f, paint)

        // সবুজ গম্বুজ (Green Dome)
        paint.color = Color.parseColor("#0E7A4A")
        val path = Path()
        path.moveTo(50f, 110f)
        path.quadTo(100f, 20f, 150f, 110f)
        path.close()
        canvas.drawPath(path, paint)

        // গম্বুজের সোনালী মিনার ক্রেসেন্ট শীর্ষ
        paint.color = Color.parseColor("#F5C358")
        paint.strokeWidth = 6f
        canvas.drawLine(100f, 20f, 100f, 5f, paint)
        canvas.drawCircle(100f, 5f, 6f, paint)

        return bitmap
    }

    // ৪ কালার থিম গ্রেডিয়েন্ট ব্যাকগ্রাউন্ড
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

    // ১. হোম স্ক্রিন (কাবা ও সবুজ গম্বুজ আইকনসহ)
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

        // আইকন হেডার রো (কাবা ও মদিনা গম্বুজ আইকন আর্ট)
        val iconHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 16)
        }

        val kabaView = ImageView(this).apply {
            setImageBitmap(createKabaBitmap())
            layoutParams = LinearLayout.LayoutParams(110, 110)
        }

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(0, -2, 1f)
            lp.setMargins(16, 0, 16, 0)
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

        val domeView = ImageView(this).apply {
            setImageBitmap(createMadinaDomeBitmap())
            layoutParams = LinearLayout.LayoutParams(110, 110)
        }

        iconHeader.addView(kabaView)
        iconHeader.addView(titleBox)
        iconHeader.addView(domeView)
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

    // ২. সম্পূর্ণ ফুল-স্ক্রিন ট্যাপ তাসবিহ (আইকন ব্যাকগ্রাউন্ডসহ)
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

        // ফুল স্ক্রিন ট্যাপ এরিয়া
        val fullScreenTap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
            gravity = Gravity.CENTER
            setPadding(32, 10, 32, 10)
        }

        // সেন্টার ইসলামিক থিম আইকন
        val centerIcon = ImageView(this).apply {
            val bmp = if (currentTheme.contains("মদিনা")) createMadinaDomeBitmap() else createKabaBitmap()
            setImageBitmap(bmp)
            val lp = LinearLayout.LayoutParams(160, 160)
            lp.setMargins(0, 0, 0, 16)
            layoutParams = lp
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
     
