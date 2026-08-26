package com.sabbirsamol.app

import android.accounts.AccountManager
import android.app.*
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.widget.*
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.*
import kotlin.concurrent.thread

data class ZikrItem(
    val id: String,
    var name: String,
    var count: Int,
    var target: Int,
    var scheduleHour: Int = 7,
    var scheduleMinute: Int = 0,
    var isAlarmEnabled: Boolean = false
)

data class NoteItem(val id: String, var title: String, var content: String, val date: String)

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "জিকির ও আমলের সময় হয়েছে"
        val message = intent.getStringExtra("message") ?: "আপনার নির্ধারিত জিকিরটি আদায় করে নিন।"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "prayer_alarm_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "ইসলামিক অ্যালার্ম", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "নামাজ ও জিকিরের অ্যালার্ম নোটিফিকেশন"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(alarmSound)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

class MainActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("sabbirs_amol_db", Context.MODE_PRIVATE) }
    private val vibrator by lazy { getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

    private val screenStack = Stack<String>()
    private val zikrList = mutableListOf<ZikrItem>()
    private val noteList = mutableListOf<NoteItem>()
    
    private var freeCounterCount = 0
    private var freeCounterTarget = 100
    private var freeCounterTitle = "মুক্ত জিকির গণনা"

    private var activeZikrId: String = ""
    private var currentTheme = "সাদা থিম (লাইট)"
    private var selectedDivision = "খুলনা"
    private var selectedDistrict = "সাতক্ষীরা"
    private var selectedThana = "শ্যামনগর"
    private var selectedUnion = "ঈশ্বরীপুর"
    private var selectedMadhab = "হানাফী"
    private var userEmail = ""

    private val FB_PAGE_URL = "https://www.facebook.com/share/1cJFJYHujX/"
    private val GOOGLE_ACCOUNT_PICKER_REQUEST = 1001
    private val FIREBASE_DB_URL = "https://sabbirs-amol-default-rtdb.firebaseio.com"

    private val timerHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var liveClockTextView: TextView? = null
    private var liveDateTextView: TextView? = null
    private var liveTimerTextView: TextView? = null
    private var liveWaqtTextView: TextView? = null

    private fun toBangla(input: Any): String {
        val str = input.toString()
        val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder()
        for (ch in str) {
            if (ch in '0'..'9') sb.append(banglaDigits[ch - '0']) else sb.append(ch)
        }
        return sb.toString()
    }

    private fun formatBanglaTime(hour: Int, minute: Int): String {
        var h = hour
        var m = minute
        while (m >= 60) { h += 1; m -= 60 }
        while (m < 0) { h -= 1; m += 60 }
        val amPm = if (h < 12) "AM" else "PM"
        if (h > 12) h -= 12
        if (h == 0) h = 12
        return "${toBangla(String.format("%02d", h))}:${toBangla(String.format("%02d", m))} $amPm"
    }

    private fun checkAndResetTasbihCount() {
        val now = Calendar.getInstance()
        val yyyyMMdd = SimpleDateFormat("yyyyMMdd", Locale.US).format(now.time)
        val hour24 = now.get(Calendar.HOUR_OF_DAY)
        val slot = if (hour24 < 12) "night" else "noon"
        val currentSlotKey = "${yyyyMMdd}_$slot"

        val lastResetSlot = prefs.getString("last_tasbih_reset_slot", "") ?: ""
        if (lastResetSlot != currentSlotKey) {
            for (item in zikrList) {
                item.count = 0
            }
            freeCounterCount = 0
            prefs.edit()
                .putString("last_tasbih_reset_slot", currentSlotKey)
                .putInt("free_counter_count", 0)
                .apply()
            saveAllData()
        }
    }

    private fun getCombinedIslamicDate(): Triple<String, String, String> {
        val now = Calendar.getInstance()
        val hour24 = now.get(Calendar.HOUR_OF_DAY)
        val min = now.get(Calendar.MINUTE)
        val currentMins = hour24 * 60 + min

        val offset = if (selectedDistrict.contains("সাতক্ষীরা")) 4 else if (selectedDistrict.contains("ঢাকা")) 0 else 2
        val magribStartMins = 18 * 60 + 27 + offset

        val isAfterSunset = currentMins >= magribStartMins

        val calForHijri = Calendar.getInstance()
        if (isAfterSunset) {
            calForHijri.add(Calendar.DATE, 0)
        } else {
            calForHijri.add(Calendar.DATE, -1)
        }

        val engFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH)
        val engDate = engFormat.format(now.time)

        val arabicMonths = arrayOf("মুহাররম", "সফর", "রবিউল আউয়াল", "রবিউস সানি", "জমাদিউল আউয়াল", "জমাদিউস সানি", "রজব", "শাবান", "রমজান", "শাওয়াল", "জ্বিলকদ", "জ্বিলহজ্জ")
        
        var hDay = 12
        var hMonth = 3
        var hYear = 1448

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val hijrah = HijrahDate.from(calForHijri.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate())
                hDay = hijrah.get(ChronoField.DAY_OF_MONTH)
                hMonth = hijrah.get(ChronoField.MONTH_OF_YEAR)
                hYear = hijrah.get(ChronoField.YEAR_OF_ERA)
            } catch (e: Exception) {}
        }
        val hDate = "${toBangla(hDay)} ${arabicMonths[(hMonth - 1).coerceIn(0, 11)]}, ${toBangla(hYear)} হিজরি"

        val bnMonthNames = arrayOf("বৈশাখ", "জ্যৈষ্ঠ", "আষাঢ়", "শ্রাবণ", "ভাদ্র", "আশ্বিন", "কার্তিক", "অগ্রহায়ণ", "পৌষ", "মাঘ", "ফাল্গুন", "চৈত্র")
        val monthDays = intArrayOf(31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29, 30)
        
        val gYear = now.get(Calendar.YEAR)
        var bYear = gYear - 593
        val isLeapYear = (gYear % 4 == 0 && gYear % 100 != 0) || (gYear % 400 == 0)
        if (isLeapYear) monthDays[10] = 30

        val startDayOfBengaliYear = Calendar.getInstance().apply { set(gYear, Calendar.APRIL, 14, 0, 0, 0) }
        var dayDiff = ((now.timeInMillis - startDayOfBengaliYear.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
        
        var bMonthIdx = 0
        var bDay = 1
        if (dayDiff < 0) {
            bYear -= 1
            dayDiff += if (isLeapYear) 366 else 365
        }
        for (i in 0 until 12) {
            if (dayDiff < monthDays[i]) {
                bMonthIdx = i
                bDay = dayDiff + 1
                break
            }
            dayDiff -= monthDays[i]
        }
        val bnDate = "${toBangla(bDay)} ${bnMonthNames[bMonthIdx]}, ${toBangla(bYear)} বঙ্গাব্দ"

        return Triple(hDate, bnDate, engDate)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadAllData()
        checkAndResetTasbihCount()
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
        
        freeCounterCount = prefs.getInt("free_counter_count", 0)
        freeCounterTarget = prefs.getInt("free_counter_target", 100)
        freeCounterTitle = prefs.getString("free_counter_title", "মুক্ত জিকির গণনা") ?: "মুক্ত জিকির গণনা"

        val savedZikr = prefs.getString("zikr_items_json", null)
        zikrList.clear()
        if (savedZikr != null) {
            try {
                val array = JSONArray(savedZikr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    zikrList.add(
                        ZikrItem(
                            obj.getString("id"),
                            obj.getString("name"),
                            obj.getInt("count"),
                            obj.getInt("target"),
                            obj.optInt("scheduleHour", 7),
                            obj.optInt("scheduleMinute", 0),
                            obj.optBoolean("isAlarmEnabled", false)
                        )
                    )
                }
            } catch (e: Exception) { initDefaultZikr() }
        } else { initDefaultZikr() }

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
        zikrList.add(ZikrItem("1", "সুবহানাল্লাহ (SubhanAllah)", 0, 33, 7, 0, false))
        zikrList.add(ZikrItem("2", "আলহামদুলিল্লাহ (Alhamdulillah)", 0, 33, 13, 0, false))
        zikrList.add(ZikrItem("3", "আল্লাহু আকবার (Allahu Akbar)", 0, 34, 17, 0, false))
        zikrList.add(ZikrItem("4", "আস্তাগফিরুল্লাহ (Astaghfirullah)", 0, 100, 19, 0, false))
        zikrList.add(ZikrItem("5", "আয়াতুল কুরসি (Ayatul Kursi)", 0, 7, 21, 0, false))
        saveAllData(uploadToCloud = false)
    }

    private fun saveAllData(uploadToCloud: Boolean = true) {
        val zikrArray = JSONArray()
        for (item in zikrList) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("count", item.count)
                put("target", item.target)
                put("scheduleHour", item.scheduleHour)
                put("scheduleMinute", item.scheduleMinute)
                put("isAlarmEnabled", item.isAlarmEnabled)
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
            .putInt("free_counter_count", freeCounterCount)
            .putInt("free_counter_target", freeCounterTarget)
            .putString("free_counter_title", freeCounterTitle)
            .apply()

        if (uploadToCloud && userEmail.isNotEmpty()) {
            syncDataToCloud(userEmail, zikrArray.toString(), noteArray.toString())
        }
    }

    private fun syncDataToCloud(email: String, zikrJson: String, noteJson: String) {
        val cleanKey = email.replace(".", "_").replace("@", "_at_")
        thread {
            try {
                val url = URL("$FIREBASE_DB_URL/users/$cleanKey.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "PUT"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")

                val payload = JSONObject().apply {
                    put("email", email)
                    put("zikr_data", zikrJson)
                    put("note_data", noteJson)
                    put("last_updated", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                }

                val os = OutputStreamWriter(conn.outputStream)
                os.write(payload.toString())
                os.flush()
                os.close()
                conn.responseCode
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun restoreDataFromCloud(email: String, onComplete: (Boolean) -> Unit) {
        val cleanKey = email.replace(".", "_").replace("@", "_at_")
        thread {
            try {
                val url = URL("$FIREBASE_DB_URL/users/$cleanKey.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = reader.readText()
                    reader.close()

                    if (response != "null" && response.isNotEmpty()) {
                        val obj = JSONObject(response)
                        val zikrStr = obj.optString("zikr_data", "")
                        val noteStr = obj.optString("note_data", "")

                        runOnUiThread {
                            if (zikrStr.isNotEmpty()) {
                                val zArray = JSONArray(zikrStr)
                                zikrList.clear()
                                for (i in 0 until zArray.length()) {
                                    val zObj = zArray.getJSONObject(i)
                                    zikrList.add(
                                        ZikrItem(
                                            zObj.getString("id"),
                                            zObj.getString("name"),
                                            zObj.getInt("count"),
                                            zObj.getInt("target"),
                                            zObj.optInt("scheduleHour", 7),
                                            zObj.optInt("scheduleMinute", 0),
                                            zObj.optBoolean("isAlarmEnabled", false)
                                        )
                                    )
                                }
                            }
                            if (noteStr.isNotEmpty()) {
                                val nArray = JSONArray(noteStr)
                                noteList.clear()
                                for (i in 0 until nArray.length()) {
                                    val nObj = nArray.getJSONObject(i)
                                    noteList.add(NoteItem(nObj.getString("id"), nObj.getString("title"), nObj.getString("content"), nObj.getString("date")))
                                }
                            }
                            saveAllData(uploadToCloud = false)
                            onComplete(true)
                        }
                        return@thread
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            runOnUiThread { onComplete(false) }
        }
    }

    private fun setCustomAlarm(reqCode: Int, hour: Int, minute: Int, title: String, message: String, isEnable: Boolean) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, PrayerAlarmReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
        }
        val pendingIntent = PendingIntent.getBroadcast(this, reqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        if (isEnable) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            }
        } else {
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun promptGoogleAccountPicker() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), null, null, null, null)
            } else {
                @Suppress("DEPRECATION")
                AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), false, null, null, null, null)
            }
            startActivityForResult(intent, GOOGLE_ACCOUNT_PICKER_REQUEST)
        } catch (e: ActivityNotFoundException) {
            showManualEmailDialog()
        } catch (e: Exception) {
            showManualEmailDialog()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_ACCOUNT_PICKER_REQUEST && resultCode == RESULT_OK && data != null) {
            val accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrEmpty()) handleSuccessfulLogin(accountName)
        }
    }

    private fun handleSuccessfulLogin(email: String) {
        userEmail = email
        Toast.makeText(this, "গুগল অ্যাকাউন্ট সিঙ্ক হচ্ছে: $email", Toast.LENGTH_SHORT).show()
        restoreDataFromCloud(email) { success ->
            if (success) {
                Toast.makeText(this, "আলহামদুলিল্লাহ! ক্লাউড থেকে সমস্ত ডাটা রিস্টোর হয়েছে।", Toast.LENGTH_LONG).show()
            } else {
                saveAllData(uploadToCloud = true)
                Toast.makeText(this, "নতুন অ্যাকাউন্ট সিঙ্ক ও ব্যাকআপ সক্রিয় হয়েছে!", Toast.LENGTH_LONG).show()
            }
            showSettingsScreen()
        }
    }

    private fun showManualEmailDialog() {
        val input = EditText(this).apply { hint = "আপনার Gmail লিখুন (e.g. sabbir@gmail.com)"; setText(userEmail); typeface = Typeface.SERIF }
        AlertDialog.Builder(this)
            .setTitle("জিমেইল অ্যাকাউন্ট")
            .setView(input)
            .setPositiveButton("সংরক্ষণ ও সিঙ্ক") { _, _ ->
                val em = input.text.toString().trim()
                if (em.isNotEmpty()) handleSuccessfulLogin(em)
            }
            .setNegativeButton("বাতিল", null)
            .show()
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
        if (screenStack.isEmpty() || screenStack.peek() != name) screenStack.push(name)
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
            GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.parseColor("#FFFDF5"), Color.parseColor("#F8FAFC"), Color.parseColor("#FFFFFF")))
        } else {
            when (currentTheme) {
                "মদিনা থিম (এমারেল্ড গ্রিন)" -> GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.parseColor("#0E4D3A"), Color.parseColor("#146B52"), Color.parseColor("#072C21")))
                "সুবহ-সাদিক থিম (রয়্যাল গোল্ড)" -> GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.parseColor("#422A0A"), Color.parseColor("#261704"), Color.parseColor("#120A01")))
                "লাইলাতুল কদর (নাইট ব্লু)" -> GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.parseColor("#0F2027"), Color.parseColor("#203A43"), Color.parseColor("#2C5364")))
                else -> GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.parseColor("#1B2A22"), Color.parseColor("#0B1410"), Color.parseColor("#040706")))
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
            val icon = TextView(this).apply { text = item.third; textSize = 17f; gravity = Gravity.CENTER }
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
        val hour24 = now.get(Calendar.HOUR_OF_DAY)
        val min = now.get(Calendar.MINUTE)
        val sec = now.get(Calendar.SECOND)
        val currentMinutes = hour24 * 60 + min

        if ((hour24 == 0 && min == 0 && sec <= 2) || (hour24 == 12 && min == 0 && sec <= 2)) {
            checkAndResetTasbihCount()
        }

        var h12 = hour24 % 12
        if (h12 == 0) h12 = 12
        val amPm = if (hour24 < 12) "পূর্বাহ্ন (AM)" else "অপরাহ্ন (PM)"
        val liveClockStr = String.format("%02d:%02d:%02d %s", h12, min, sec, amPm)
        liveClockTextView?.text = "🕒 বর্তমান সময়: " + toBangla(liveClockStr)

        val (hijri, bangla, english) = getCombinedIslamicDate()
        liveDateTextView?.text = "🌙 $hijri\n📅 $bangla  |  $english"

        val offset = if (selectedDistrict.contains("সাতক্ষীরা")) 4 else if (selectedDistrict.contains("ঢাকা")) 0 else 2

        val sehriEnd = 4 * 60 + 19 + offset
        val fojrStart = 4 * 60 + 25 + offset
        val fojrEnd = 5 * 60 + 43 + offset
        val sunriseHaramEnd = 6 * 60 + 3 + offset
        val ishraqEnd = 6 * 60 + 30 + offset
        val chashtEnd = 11 * 60 + 45 + offset
        val middayHaramEnd = 12 * 60 + 8 + offset
        val zohrStart = 12 * 60 + 8 + offset
        
        val asrStartHour = if (selectedMadhab == "হানাফী") 16 else 15
        val asrStartMin = if (selectedMadhab == "হানাফী") 37 else 52
        val asrStartTotalMin = asrStartHour * 60 + asrStartMin + offset

        val asrEnd = 18 * 60 + 10 + offset
        val sunsetHaramEnd = 18 * 60 + 27 + offset
        val magribStart = 18 * 60 + 27 + offset
        val magribEnd = 19 * 60 + 44 + offset

        val (waqtName, targetMin) = when {
            currentMinutes < fojrStart -> "ফজর ওয়াক্ত শুরু হতে বাকি" to fojrStart
            currentMinutes < fojrEnd -> "ফজর শেষ হতে বাকি" to fojrEnd
            currentMinutes < sunriseHaramEnd -> "🚫 সূর্যোদয় (নামাজ নিষিদ্ধ সময়)" to sunriseHaramEnd
            currentMinutes < ishraqEnd -> "ইশরাক শেষ হতে বাকি" to ishraqEnd
            currentMinutes < chashtEnd -> "চাশত / দুহা শেষ হতে বাকি" to chashtEnd
            currentMinutes < middayHaramEnd -> "🚫 দ্বিপ্রহর (নামাজ নিষিদ্ধ সময়)" to middayHaramEnd
            currentMinutes < asrStartTotalMin -> "যোহর শেষ হতে বাকি" to asrStartTotalMin
            currentMinutes < asrEnd -> "আসর ($selectedMadhab) শেষ হতে বাকি" to asrEnd
            currentMinutes < sunsetHaramEnd -> "🚫 সূর্যাস্ত (নামাজ নিষিদ্ধ সময়)" to sunsetHaramEnd
            currentMinutes < magribEnd -> "মাগরিব / ইফতারের ওয়াক্ত শেষ হতে বাকি" to magribEnd
            else -> "এশা শেষ হতে বাকি" to (24 * 60 + fojrStart)
        }

        var diffSec = (targetMin * 60) - (currentMinutes * 60 + sec)
        if (diffSec < 0) diffSec += 24 * 3600

        val h = diffSec / 3600
        val m = (diffSec % 3600) / 60
        val s = diffSec % 60

        val timeString = String.format("%02d:%02d:%02d", h, m, s)
        liveWaqtTextView?.text = waqtName
        liveTimerTextView?.text = toBangla(timeString)
    }

    private fun showHomeScreen() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = getThemeBackground() }
        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 24) }
        val accent = getAccentColor()

        val clockCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(18, 12, 18, 12)
            val bg = GradientDrawable()
            bg.setColor(if (isWhiteTheme()) Color.parseColor("#EFF6FF") else Color.parseColor("#172554"))
            bg.cornerRadius = 20f
            bg.setStroke(2, Color.parseColor("#3B82F6"))
            background = bg
        }

        liveClockTextView = TextView(this).apply {
            text = "🕒 বর্তমান সময়: লোড হচ্ছে..."
            textSize = 16.5f
            typeface = Typeface.SERIF
            setTextColor(if (isWhiteTheme()) Color.parseColor("#1D4ED8") else Color.parseColor("#93C5FD"))
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        clockCard.addView(liveClockTextView)
        content.addView(clockCard)

        val dateCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 14)
            val bg = GradientDrawable()
            bg.setColor(if (isWhiteTheme()) Color.parseColor("#FEF3C7") else Color.parseColor("#1B2E24"))
            bg.cornerRadius = 20f
            bg.setStroke(1, accent)
            background = bg
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 12, 0, 0)
            layoutParams = lp
        }

        val (hijri, bangla, english) = getCombinedIslamicDate()

        liveDateTextView = TextView(this).apply {
            text = "🌙 $hijri\n📅 $bangla  |  $english"
            textSize = 14.5f
            typeface = Typeface.SERIF
            setTextColor(getTextColor())
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
            setLineSpacing(6f, 1.2f)
        }
        dateCard.addView(liveDateTextView)
        content.addView(dateCard)

        val timerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(26, 20, 26, 20)
            val bg = GradientDrawable()
            bg.setColor(if (isWhiteTheme()) Color.parseColor("#FFFBEB") else Color.parseColor("#1B2E24"))
            bg.cornerRadius = 30f
            bg.setStroke(3, accent)
            background = bg
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 12, 0, 0)
            layoutParams = lp
        }

        liveWaqtTextView = TextView(this).apply {
            text = "ওয়াক্ত শেষ হতে বাকি"
            textSize = 15f
            typeface = Typeface.SERIF
            setTextColor(getTextColor())
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        liveTimerTextView = TextView(this).apply {
            text = "০০:০০:০০"
            textSize = 40f
            typeface = Typeface.SERIF
            setTextColor(accent)
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 8)
        }

        val locationBox = TextView(this).apply {
            text = "📍 $selectedUnion, $selectedThana, $selectedDistrict, $selectedDivision"
            textSize = 12f
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

        val offset = if (selectedDistrict.contains("সাতক্ষীরা")) 4 else if (selectedDistrict.contains("ঢাকা")) 0 else 2

        val specialCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
            val bg = GradientDrawable()
            bg.setColor(if (isWhiteTheme()) Color.parseColor("#FEF3C7") else Color.parseColor("#1E3A2F"))
            bg.cornerRadius = 24f
            bg.setStroke(2, accent)
            background = bg
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 14, 0, 0)
            layoutParams = lp
        }

        val spTitle = TextView(this).apply {
            text = "🌙 সাহরি, ইফতার ও তাহাজ্জুদ সময়সূচী"
            textSize = 15.5f
            typeface = Typeface.SERIF
            setTextColor(getTextColor())
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            setPadding(0, 0, 0, 8)
        }
        specialCard.addView(spTitle)

        val spTimes = listOf(
            "সাহরির শেষ সময়" to "${formatBanglaTime(4, 19 + offset)}",
            "ইফতারের সময়" to "${formatBanglaTime(6, 27 + offset)}",
            "তাহাজ্জুদের উত্তম সময়" to "রাত ০১:৩০ হতে সাহরির পূর্ব পর্যন্ত"
        )
        for (st in spTimes) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 4, 0, 4) }
            val name = TextView(this).apply { text = st.first; setTextColor(getTextColor()); textSize = 13.5f; typeface = Typeface.SERIF; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
            val time = TextView(this).apply { text = st.second; setTextColor(if (isWhiteTheme()) Color.parseColor("#B45309") else Color.parseColor("#FDE047")); textSize = 13.5f; typeface = Typeface.SERIF; setTypeface(Typeface.SERIF, Typeface.BOLD) }
            row.addView(name)
            row.addView(time)
            specialCard.addView(row)
        }
        content.addView(specialCard)

        val prayerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
            val bg = GradientDrawable()
            bg.setColor(getCardBgColor())
            bg.cornerRadius = 24f
            bg.setStroke(1, Color.parseColor("#CBD5E1"))
            background = bg
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 14, 0, 0)
            layoutParams = lp
        }

        val pTitle = TextView(this).apply {
            text = "🕌 ৫ ওয়াক্ত নামাজ ও অ্যালার্ম ($selectedMadhab)"
            textSize = 15.5f
            typeface = Typeface.SERIF
            setTextColor(accent)
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            setPadding(0, 0, 0, 8)
        }
        prayerCard.addView(pTitle)

        val asrStartHourDisplay = if (selectedMadhab == "হানাফী") 4 else 3
        val asrStartMinDisplay = if (selectedMadhab == "হানাফী") 37 else 52

        val prayerAlarmList = listOf(
            Triple("ফজর", "${formatBanglaTime(4, 25 + offset)}", Pair(4, 25 + offset)),
            Triple("যোহর", "${formatBanglaTime(12, 8 + offset)}", Pair(12, 8 + offset)),
            Triple("আসর", "${formatBanglaTime(asrStartHourDisplay, asrStartMinDisplay + offset)}", Pair(if (asrStartHourDisplay < 12) asrStartHourDisplay + 12 else asrStartHourDisplay, asrStartMinDisplay + offset)),
            Triple("মাগরিব", "${formatBanglaTime(6, 27 + offset)}", Pair(18, 27 + offset)),
            Triple("এশা", "${formatBanglaTime(7, 44 + offset)}", Pair(19, 44 + offset))
        )

        for ((idx, p) in prayerAlarmList.withIndex()) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 4, 0, 4)
            }
            val name = TextView(this).apply { text = p.first; setTextColor(getTextColor()); textSize = 14f; typeface = Typeface.SERIF; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
            val time = TextView(this).apply { text = p.second; setTextColor(if (isWhiteTheme()) Color.parseColor("#059669") else Color.parseColor("#86EFAC")); textSize = 14f; typeface = Typeface.SERIF; setTypeface(Typeface.SERIF, Typeface.BOLD); setPadding(0, 0, 16, 0) }

            val alarmSwitch = Switch(this).apply {
                val alarmKey = "alarm_${p.first}"
                isChecked = prefs.getBoolean(alarmKey, false)
                setOnCheckedChangeListener { _, isChecked ->
                    prefs.edit().putBoolean(alarmKey, isChecked).apply()
                    setCustomAlarm(100 + idx, p.third.first, p.third.second, "${p.first} নামাজের ওয়াক্ত হয়েছে", "নামাজের জন্য প্রস্তুতি নিয়ে মসজিদে চলুন।", isChecked)
                    Toast.makeText(this@MainActivity, "${p.first} অ্যালার্ম " + (if (isChecked) "চালু হয়েছে" else "বন্ধ হয়েছে"), Toast.LENGTH_SHORT).show()
                }
            }

            row.addView(name)
            row.addView(time)
            row.addView(alarmSwitch)
            prayerCard.addView(row)
        }
        content.addView(prayerCard)

        val haramCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
            val bg = GradientDrawable()
            bg.setColor(if (isWhiteTheme()) Color.parseColor("#FEF2F2") else Color.parseColor("#331818"))
            bg.cornerRadius = 24f
            bg.setStroke(1, Color.parseColor("#EF4444"))
            background = bg
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, 14, 0, 0)
            layoutParams = lp
        }

        val hTitle = TextView(this).apply {
            text = "🚫 নামাজ নিষিদ্ধ ৩টি হারাম ওয়াক্ত"
            textSize = 15f
            typeface = Typeface.SERIF
            setTextColor(Color.parseColor("#EF4444"))
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            setPadding(0, 0, 0, 6)
        }
        haramCard.addView(hTitle)

        val haramTimes = listOf(
            "১. সূর্যোদয়কালীন সময়" to "${formatBanglaTime(5, 44 + offset)} - ${formatBanglaTime(6, 4 + offset)}",
            "২. ঠিক দ্বিপ্রহরের সময়" to "${formatBanglaTime(11, 48 + offset)} - ${formatBanglaTime(12, 7 + offset)}",
            "৩. সূর্যাস্তকালীন সময়" to "${formatBanglaTime(6, 11 + offset)} - ${formatBanglaTime(6, 26 + offset)}"
        )

        for (ht in haramTimes) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 4, 0, 4) }
            val name = TextView(this).apply { text = ht.first; setTextColor(getTextColor()); textSize = 13f; typeface = Typeface.SERIF; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
            val time = TextView(this).apply { text = ht.second; setTextColor(Color.parseColor("#EF4444")); textSize = 13f; typeface = Typeface.SERIF; setTypeface(Typeface.SERIF, Typeface.BOLD) }
            row.addView(name)
            row.addView(time)
            haramCard.addView(row)
        }
        content.addView(haramCard)

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
            lp.setMargins(0, 10, 0, 0)
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
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(36, 16, 36, 16) }
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

    private fun showTasbihScreen() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = getThemeBackground() }
        val accent = getAccentColor()

        val topBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(20, 16, 20, 8); gravity = Gravity.CENTER_VERTICAL }
        
        val btnTitleEdit = Button(this).apply {
            text = "✏️ $freeCounterTitle"
            textSize = 13.5f
            typeface = Typeface.SERIF
            setTextColor(getTextColor())
            setBackgroundColor(if (isWhiteTheme()) Color.parseColor("#F1F5F9") else Color.parseColor("#1E293B"))
            val lp = LinearLayout.LayoutParams(0, -2, 1f)
            layoutParams = lp
            setOnClickListener { showEditFreeCounterTitleDialog() }
        }

        val btnReset = Button(this).apply {
            text = "রিসেট (০)"
            textSize = 12f
            typeface = Typeface.SERIF
            val lp = LinearLayout.LayoutParams(-2, -2)
            lp.setMargins(8, 0, 0, 0)
            layoutParams = lp
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("রিসেট করবেন?")
                    .setMessage("গণনা শূন্য (০) করতে চান?")
                    .setPositiveButton("হ্যাঁ") { _, _ ->
                        freeCounterCount = 0
                        saveAllData()
                        showTasbihScreen()
                    }
                    .setNegativeButton("না", null).show()
            }
        }
        topBar.addView(btnTitleEdit)
        topBar.addView(btnReset)
        root.addView(topBar)

        val fullScreenTap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(-1, 0, 1f); gravity = Gravity.CENTER; setPadding(32, 10, 32, 10) }
        val centerIcon = TextView(this).apply { text = "📿"; textSize = 38f; gravity = Gravity.CENTER }
        fullScreenTap.addView(centerIcon)

        val countDisplay = TextView(this).apply { 
            text = toBangla(freeCounterCount)
            textSize = 96f
            typeface = Typeface.SERIF
            setTextColor(getTextColor())
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER 
        }

        val targetInfo = TextView(this).apply { 
            text = "টার্গেট: ${toBangla(freeCounterTarget)} বার (পরিবর্তন করতে ট্যাপ করুন)"
            textSize = 14f
            typeface = Typeface.SERIF
            setTextColor(accent)
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 18)
            setOnClickListener { showEditFreeCounterTargetDialog() }
        }

        val tapGuide = TextView(this).apply { 
            text = "👆 স্ক্রিনের যেকোনো জায়গায় ট্যাপ করে আপনার ইচ্ছামতো যে কোনো জিকির গণনা করুন"
            textSize = 13f
            typeface = Typeface.SERIF
            setTextColor(getSecondaryTextColor())
            gravity = Gravity.CENTER 
        }

        fullScreenTap.addView(countDisplay)
        fullScreenTap.addView(targetInfo)
        fullScreenTap.addView(tapGuide)

        fullScreenTap.setOnClickListener {
            if (freeCounterCount < freeCounterTarget) {
                freeCounterCount++
                triggerVibration(40)
                saveAllData()
                countDisplay.text = toBangla(freeCounterCount)

                if (freeCounterCount >= freeCounterTarget) {
                    triggerVibration(500)
                    AlertDialog.Builder(this)
                        .setTitle("মাশাআল্লাহ!")
                        .setMessage("নির্ধারিত টার্গেট (${toBangla(freeCounterTarget)} বার) পূর্ণ হয়েছে।")
                        .setPositiveButton("আবার শুরু") { _, _ ->
                            freeCounterCount = 0
                            saveAllData()
                            showTasbihScreen()
                        }
                        .setNegativeButton("ঠিক আছে", null).show()
                }
            } else {
                Toast.makeText(this, "টার্গেট পূর্ণ হয়েছে! রিসেট বাটন চাপুন।", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(fullScreenTap)

        val bottomBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(20, 6, 20, 14) }
        val btnToList = Button(this).apply {
            text = "📋 সংরক্ষিত জিকির তালিকা ও শিডিউল"
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

    private fun showEditFreeCounterTitleDialog() {
        val input = EditText(this).apply {
            hint = "জিকিরের নাম লিখুন (যেমন: সুবহানাল্লাহ)"
            setText(freeCounterTitle)
            typeface = Typeface.SERIF
        }
        AlertDialog.Builder(this)
            .setTitle("জিকিরের নাম নির্ধারণ")
            .setView(input)
            .setPositiveButton("সংরক্ষণ") { _, _ ->
                val str = input.text.toString().trim()
                if (str.isNotEmpty()) {
                    freeCounterTitle = str
                    saveAllData()
                    showTasbihScreen()
                }
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    private fun showEditFreeCounterTargetDialog() {
        val input = EditText(this).apply {
            hint = "টার্গেট সংখ্যা লিখুন (যেমন: ১০০, ৫০০)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("$freeCounterTarget")
            typeface = Typeface.SERIF
        }
        AlertDialog.Builder(this)
            .setTitle("কাউন্টার টার্গেট নির্ধারণ")
            .setView(input)
            .setPositiveButton("সেট করুন") { _, _ ->
                val num = input.text.toString().toIntOrNull() ?: 100
                if (num > 0) {
                    freeCounterTarget = num
                    saveAllData()
                    showTasbihScreen()
                }
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    private fun showZikrListScreen() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = getThemeBackground() }
        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 24) }
        val accent = getAccentColor()

        val title = TextView(this).apply { text = "জিকির তালিকা ও অ্যালার্ম শিডিউল"; textSize = 19f; typeface = Typeface.SERIF; setTextColor(accent); setTypeface(Typeface.SERIF, Typeface.BOLD); setPadding(0, 0, 0, 14) }
        content.addView(title)

        val btnAddZikr = Button(this).apply {
            text = "➕ নতুন জিকির যুক্ত করুন"; setBackgroundColor(accent); setTextColor(Color.BLACK); textSize = 14.5f; typeface = Typeface.SERIF; setTypeface(Typeface.SERIF, Typeface.BOLD)
            val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 0, 0, 16); layoutParams = lp
            setOnClickListener { showAddZikrDialog() }
        }
        content.addView(btnAddZikr)

        for ((idx, item) in zikrList.withIndex()) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(18, 14, 18, 14)
                val bg = GradientDrawable(); bg.setColor(getCardBgColor()); bg.cornerRadius = 20f; bg.setStroke(if (item.id == activeZikrId) 3 else 1, if (item.id == activeZikrId) accent else Color.parseColor("#CBD5E1")); background = bg
                val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 0, 0, 14); layoutParams = lp
            }

            val topInfo = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val countBadge = TextView(this).apply {
                text = toBangla(item.count); textSize = 17f; typeface = Typeface.SERIF; setTextColor(Color.WHITE); setTypeface(Typeface.SERIF, Typeface.BOLD); gravity = Gravity.CENTER
                val bg = GradientDrawable(); bg.setColor(Color.parseColor("#0F766E")); bg.cornerRadius = 40f; background = bg; setPadding(18, 8, 18, 8)
            }
            val nameView = TextView(this).apply { text = item.name; textSize = 15.5f; typeface = Typeface.SERIF; setTextColor(getTextColor()); setTypeface(Typeface.SERIF, Typeface.BOLD); val lp = LinearLayout.LayoutParams(0, -2, 1f); lp.setMargins(14, 0, 8, 0); layoutParams = lp }
            topInfo.addView(countBadge); topInfo.addView(nameView); card.addView(topInfo)

            val targetText = TextView(this).apply { text = "টার্গেট: ${toBangla(item.target)} বার (গোণা হয়েছে: ${toBangla(item.count)} বার)"; textSize = 12.5f; typeface = Typeface.SERIF; setTextColor(getSecondaryTextColor()); setPadding(0, 6, 0, 6) }
            card.addView(targetText)

            val scheduleRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 4, 0, 10)
            }

            val btnSetTime = Button(this).apply {
                text = "⏰ শিডিউল: ${formatBanglaTime(item.scheduleHour, item.scheduleMinute)}"
                textSize = 11.5f
                typeface = Typeface.SERIF
                setBackgroundColor(if (isWhiteTheme()) Color.parseColor("#E0F2FE") else Color.parseColor("#1E293B"))
                setTextColor(if (isWhiteTheme()) Color.parseColor("#0369A1") else Color.parseColor("#38BDF8"))
                val lp = LinearLayout.LayoutParams(0, -2, 1f)
                layoutParams = lp
                setOnClickListener {
                    TimePickerDialog(this@MainActivity, { _, h, m ->
                        item.scheduleHour = h
                        item.scheduleMinute = m
                        if (item.isAlarmEnabled) {
                            setCustomAlarm(500 + idx, item.scheduleHour, item.scheduleMinute, "জিকিরের সময় হয়েছে", "${item.name} আদায়ের সময় হয়েছে।", true)
                        }
                        saveAllData()
                        showZikrListScreen()
                    }, item.scheduleHour, item.scheduleMinute, false).show()
                }
            }

            val alarmSwitch = Switch(this).apply {
                isChecked = item.isAlarmEnabled
                val lp = LinearLayout.LayoutParams(-2, -2); lp.setMargins(10, 0, 0, 0); layoutParams = lp
                setOnCheckedChangeListener { _, isChecked ->
                    item.isAlarmEnabled = isChecked
                    setCustomAlarm(500 + idx, item.scheduleHour, item.scheduleMinute, "জিকিরের সময় হয়েছে", "${item.name} আদায়ের সময় হয়েছে।", isChecked)
                    saveAllData()
                    Toast.makeText(this@MainActivity, "${item.name} এর অ্যালার্ম " + (if (isChecked) "চালু হয়েছে" else "বন্ধ হয়েছে"), Toast.LENGTH_SHORT).show()
                }
            }

            scheduleRow.addView(btnSetTime)
            scheduleRow.addView(alarmSwitch)
            card.addView(scheduleRow)

            val actionsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
            
            val btnEdit = Button(this).apply {
                text = "✏️ Edit"
                textSize = 12f
                typeface = Typeface.SERIF
                val lp = LinearLayout.LayoutParams(-2, -2); lp.setMargins(0, 0, 8, 0); layoutParams = lp
                setBackgroundColor(if (isWhiteTheme()) Color.parseColor("#D97706") else Color.parseColor("#F59E0B"))
                setTextColor(Color.BLACK)
                setOnClickListener { showEditZikrDialog(item, idx) }
            }

            val btnDelete = Button(this).apply {
                text = "Delete"; textSize = 12f; typeface = Typeface.SERIF; val lp = LinearLayout.LayoutParams(-2, -2); lp.setMargins(0, 0, 8, 0); layoutParams = lp
                setBackgroundColor(Color.parseColor("#DC2626")); setTextColor(Color.WHITE)
                setOnClickListener {
                    if (zikrList.size <= 1) {
                        Toast.makeText(this@MainActivity, "কমপক্ষে একটি জিকির থাকতে হবে!", Toast.LENGTH_SHORT).show()
                    } else {
                        setCustomAlarm(500 + idx, item.scheduleHour, item.scheduleMinute, "", "", false)
                        zikrList.remove(item)
                        if (activeZikrId == item.id) activeZikrId = zikrList.first().id
                        saveAllData()
                        showZikrListScreen()
                    }
                }
            }

            val btnContinue = Button(this).apply {
                text = "কাউন্টারে আনুন"; textSize = 12.5f; typeface = Typeface.SERIF; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE)
                setOnClickListener {
                    freeCounterTitle = item.name
                    freeCounterTarget = item.target
                    freeCounterCount = item.count
                    saveAllData()
                    openScreen("tasbih")
                }
            }

            actionsRow.addView(btnEdit)
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

    private fun showEditZikrDialog(item: ZikrItem, idx: Int) {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 20) }

        val labelName = TextView(this).apply { text = "জিকিরের নাম:"; typeface = Typeface.SERIF; setTextColor(getTextColor()) }
        val nameInput = EditText(this).apply { setText(item.name); typeface = Typeface.SERIF }

        val labelTarget = TextView(this).apply { text = "\nটার্গেট সংখ্যা:"; typeface = Typeface.SERIF; setTextColor(getTextColor()) }
        val targetInput = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("${item.target}")
            typeface = Typeface.SERIF
        }

        val labelCount = TextView(this).apply { text = "\nবর্তমান গোণার সংখ্যা:"; typeface = Typeface.SERIF; setTextColor(getTextColor()) }
        val countInput = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("${item.count}")
            typeface = Typeface.SERIF
        }

        layout.addView(labelName)
        layout.addView(nameInput)
        layout.addView(labelTarget)
        layout.addView(targetInput)
        layout.addView(labelCount)
        layout.addView(countInput)

        AlertDialog.Builder(this)
            .setTitle("জিকির কাস্টমাইজ করুন")
            .setView(layout)
            .setPositiveButton("আপডেট করুন") { _, _ ->
                val newName = nameInput.text.toString().trim()
                val newTarget = targetInput.text.toString().toIntOrNull() ?: item.target
                val newCount = countInput.text.toString().toIntOrNull() ?: item.count
                if (newName.isNotEmpty()) {
                    item.name = newName
                    item.target = if (newTarget > 0) newTarget else 33
                    item.count = if (newCount >= 0) newCount else 0
                    if (item.isAlarmEnabled) {
                        setCustomAlarm(500 + idx, item.scheduleHour, item.scheduleMinute, "জিকিরের সময় হয়েছে", "${item.name} আদায়ের সময় হয়েছে।", true)
                    }
                    saveAllData()
                    showZikrListScreen()
                    Toast.makeText(this, "সফলভাবে আপডেট হয়েছে!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("বাতিল", null)
            .show()
    }

    private fun showAddZikrDialog() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 20) }
        val nameInput = EditText(this).apply { hint = "জিকিরের নাম লিখুন"; typeface = Typeface.SERIF }
        val targetInput = EditText(this).apply { hint = "টার্গেট সংখ্যা (যেমন: ৩৩)"; inputType = android.text.InputType.TYPE_CLASS_NUMBER; setText("33"); typeface = Typeface.SERIF }
        layout.addView(nameInput)
        layout.addView(targetInput)

        AlertDialog.Builder(this)
            .setTitle("নতুন জিকির যোগ করুন")
            .setView(layout)
            .setPositiveButton("সংরক্ষণ") { _, _ ->
                val name = nameInput.text.toString().trim()
                val target = targetInput.text.toString().toIntOrNull() ?: 33
                if (name.isNotEmpty()) {
                    val newItem = ZikrItem(UUID.randomUUID().toString(), name, 0, if (target > 0) target else 33, 7, 0, false)
                    zikrList.add(newItem)
                    activeZikrId = newItem.id
                    saveAllData()
                    showZikrListScreen()
                }
            }
            .setNegativeButton("বাতিল", null).show()
    }

    private fun showAmalFoldersScreen() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = getThemeBackground() }
        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(26, 26, 26, 26) }
        val accent = getAccentColor()

        val h = TextView(this).apply { text = "📂 মাসনূন আমল ও মানযিল ভাণ্ডার"; textSize = 20f; typeface = Typeface.SERIF; setTextColor(accent); setTypeface(Typeface.SERIF, Typeface.BOLD); setPadding(0, 0, 0, 6) }
        val sub = TextView(this).apply { text = "সংকলনে: সাব্বির আহমাদ"; textSize = 13.5f; typeface = Typeface.SERIF; setTextColor(getSecondaryTextColor()); setPadding(0, 0, 0, 18) }
        content.addView(h)
        content.addView(sub)

        val folder1 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(22, 20, 22, 20)
            val bg = GradientDrawable(); bg.setColor(getCardBgColor()); bg.cornerRadius = 24f; bg.setStroke(2, accent); background = bg
            val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 0, 0, 16); layoutParams = lp
            setOnClickListener { openScreen("masnun_pdf_screen") }
        }
        val f1Title = TextView(this).apply { text = "📁 ফোল্ডার ১: মাসনূন আমল (সকাল-সন্ধ্যা ও শয়ন)"; textSize = 16.5f; typeface = Typeface.SERIF; setTextColor(getTextColor()); setTypeface(Typeface.SERIF, Typeface.BOLD) }
        val f1Desc = TextView(this).apply { text = "সকাল ও সন্ধ্যার নির্বাচিত মাসনূন দু'আ, সূরাসমূহ ও ঘুমানোর পূর্বের সুন্নাত আমলসমূহ।"; textSize = 13f; typeface = Typeface.SERIF; setTextColor(getSecondaryTextColor()); setPadding(0, 6, 0, 12) }
        val btnF1 = Button(this).apply { text = "📖 আমলসমূহ পড়ুন ➔"; typeface = Typeface.SERIF; setBackgroundColor(accent); setTextColor(Color.BLACK); setOnClickListener { openScreen("masnun_pdf_screen") } }
        folder1.addView(f1Title); folder1.addView(f1Desc); folder1.addView(btnF1); content.addView(folder1)

        val folder2 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(22, 20, 22, 20)
            val bg = GradientDrawable(); bg.setColor(getCardBgColor()); bg.cornerRadius = 24f; bg.setStroke(2, Color.parseColor("#0F766E")); background = bg
            val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 0, 0, 16); layoutParams = lp
            setOnClickListener { openScreen("manzil_pdf_screen") }
        }
        val f2Title = TextView(this).apply { text = "📁 ফোল্ডার ২: মানযিল আয়াত (কুরআনী হিফাযত)"; textSize = 16.5f; typeface = Typeface.SERIF; setTextColor(getTextColor()); setTypeface(Typeface.SERIF, Typeface.BOLD) }
        val f2Desc = TextView(this).apply { text = "কুরআনুল কারীমের রোগ-বালাই ও সকল অনিষ্ট থেকে বাঁচার হিফাযতের বিশেষ আয়াতসমূহ।"; textSize = 13f; typeface = Typeface.SERIF; setTextColor(getSecondaryTextColor()); setPadding(0, 6, 0, 12) }
        val btnF2 = Button(this).apply { text = "📖 মানযিল তিলাওয়াত করুন ➔"; typeface = Typeface.SERIF; setBackgroundColor(if (isWhiteTheme()) Color.parseColor("#0F766E") else Color.parseColor("#264536")); setTextColor(Color.WHITE); setOnClickListener { openScreen("manzil_pdf_screen") } }
        folder2.addView(f2Title); folder2.addView(f2Desc); folder2.addView(btnF2); content.addView(folder2)

        scroll.addView(content)
        root.addView(scroll)
        root.addView(createNavBar("amal_folders"))
        setContentView(root)
    }

    private fun showMasnunPdfScreen() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = getThemeBackground() }
        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(26, 26, 26, 26) }
        val accent = getAccentColor()

        val h = TextView(this).apply { text = "মাসনূন আমল\nসংকলনে: সাব্বির আহমাদ"; textSize = 19f; typeface = Typeface.SERIF; setTextColor(accent); setTypeface(Typeface.SERIF, Typeface.BOLD); gravity = Gravity.CENTER; setPadding(0, 0, 0, 16) }
        content.addView(h)

        val items = listOf(
            "সূরা আল-ফাতিহা (৩ বার)" to "بِسْمِ اللهِ الرَّحْمَنِ الرَّحِيمِ (1) الْحَمْدُ لِلَّهِ رَبِّ الْعَلَمِينَ (٢) الرَّحْمَنِ الرَّحِيمِ (۳) مَلِكِ يَوْمِ الدِّينِ (4) إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ (٥) اِهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ (6) صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ (۷)[span_0](start_span)"[span_0](end_span),
            "আয়াতুল কুরসি (৩ বার)" to "اللهُ لَا إِلَهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ، لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ لَهُ مَا فِي السَّمَاتِ وَمَا فِي الْأَرْضِ مَنْ ذَا الَّذِي يَشْفَعُ عِنْدَةً إِلَّا بِإِذْنِهِ يَعْلَمُ مَا بَيْنَ أَيْদِيهِمْ وَمَا خَلْفَهُمْ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ وَسِعَ كُرْسِيُّهُ السَّمَوَاتِ وَالْأَرْضَ وَلَا يَئُودُهُ حِفْظُهُمَا وَهُوَ الْعَلِيُّ الْعَظِيمُ[span_1](start_span)"[span_1](end_span),
            "৪ কুল ও বিশেষ দু'আসমূহ" to "সূরা আল-কাফিরূন, সূরা আল-ইখলাস, সূরা আল-ফালাক, সূরা আন-নাস (প্রত্যেকটি ৩ বার করে)[span_2](start_span)"[span_2](end_span),
            "সকাল ও সন্ধ্যার তাসবিহ" to "সুবহানাল্লাহ (১০ বার), আলহামদুলিল্লাহ (১০ বার), আল্লাহু আকবার (১০ বার)\n\nলা ইলাহা ইল্লাল্লাহু ওয়াহদাহু লা শারীকা লাহু... (১০০ বার)[span_3](start_span)"[span_3](end_span),
            "সায়্যিদুল ইস্তিগফার (১ বার)" to "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ لَكَ بِذَنْبِي فَاغْفِرْ লِي، فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ[span_4](start_span)"[span_4](end_span),
            "ঘুমানোর পূর্বের আমল" to "১. ওযু করে ঘুমানো।\n২. বিছানা ঝেড়ে শোয়া।\n৩. আয়াতুল কুরসি পড়া।\n৪. ৩ কুল পড়ে শরীরে ফু দেওয়া।\n৫. ঘুমের দোয়া: (اللَّهُمَّ بِاسْمِكَ أَمُوتُ وَأَحْيَا)\n৬. ঘুম থেকে উঠে দোয়া পড়া।[span_5](start_span)"[span_5](end_span)
        )

        for (item in items) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(20, 16, 20, 16)
                val bg = GradientDrawable(); bg.setColor(getCardBgColor()); bg.cornerRadius = 20f; bg.setStroke(1, Color.parseColor("#CBD5E1")); background = bg
                val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 0, 0, 14); layoutParams = lp
            }
            val t = TextView(this).apply { text = item.first; textSize = 15.5f; typeface = Typeface.SERIF; setTextColor(accent); setTypeface(Typeface.SERIF, Typeface.BOLD); setPadding(0, 0, 0, 6) }
            val b = TextView(this).apply { text = item.second; textSize = 16.5f; typeface = Typeface.SERIF; setTextColor(getTextColor()); setLineSpacing(8f, 1.2f) }
            card.addView(t); card.addView(b); content.addView(card)
        }

        val btnBack = Button(this).apply {
            text = "⬅ ফোল্ডার তালিকায় ফিরে যান"; typeface = Typeface.SERIF
            setBackgroundColor(if (isWhiteTheme()) Color.parseColor("#0F766E") else Color.parseColor("#264536")); setTextColor(Color.WHITE)
            setOnClickListener { openScreen("amal_folders") }
        }
        content.addView(btnBack)

        scroll.addView(content)
        root.addView(scroll)
        root.addView(createNavBar("amal_folders"))
        setContentView(root)
    }

    private fun showManzilPdfScreen() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = getThemeBackground() }
        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(26, 26, 26, 26) }
        val accent = getAccentColor()

        val h = TextView(this).apply { text = "মানযিল (কুরআনী হিফাযত)"; textSize = 20f; typeface = Typeface.SERIF; setTextColor(accent); setTypeface(Typeface.SERIF, Typeface.BOLD); gravity = Gravity.CENTER; setPadding(0, 0, 0, 16) }
        content.addView(h)

        val manzilAyats = listOf(
            "১. সূরা আল-ইনশিকাক (১-২৫)" to "بِسْمِ اللهِ الرَّحْمَنِ الرَّحِيمِ\nإِذَا السَّمَاءُ انْشَقَّتْ ، وَأَذِنَتْ لِرَبِّهَا وَحُقَّتْ ، وَإِذَا الْأَرْضُ مُদَّتْ وَأَلْقَتْ مَا فِيهَا وَتَخَلَّتْ ، وَأَذِنَتْ لِرَبِّهَا وَحُقَّتْ ، يَأَيُّهَا الْإِنْسَانُ إِنَّكَ كَادِحٌ إِلَى رَبِّكَ كَدْحًا فَمُلَقِيهِ...[span_6](start_span)"[span_6](end_span),
            "২. সূরা আল-ফাতিহা ও আল-বাকারাহ (১-৫)" to "بِسْمِ اللهِ الرَّحْمَنِ الرَّحِيمِ\nالْحَمْدُ لِلَّهِ رَبِّ الْعَلَمِينَ ، الرَّحْمَنِ الرَّحِيمِ ، مَلِكِ يَوْمِ الدِّينِ...\n\nالم ، ذَلِكَ الْكِتَبُ لَا رَيْبَ فِيهِ هُদًى لِلْمُتَّقِينَ...[span_7](start_span)"[span_7](end_span),
            "৩. আয়াতুল কুরসি ও আমানার রাসুল" to "اللهُ لَا إِلَهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ...\n\nآمَنَ الرَّسُولُ بِمَا أُنْزِلَ إِلَيْهِ مِنْ رَّبِّهِ وَالْمُؤْمِنُونَ...[span_8](start_span)"[span_8](end_span),
            "৪. সূরা আলে ইমরান, আল-আ'রাফ ও আল-ইসরা" to "شَهِدَ اللَّهُ أَنَّهُ لَا إِلَهَ إِلَّا هُوَ وَالْمَلَئِكَةُ...\n\nإِنَّ رَبَّكُمُ اللَّهُ الَّذِي خَلَقَ السَّمَوَاتِ وَالْأَرْضَ...[span_9](start_span)"[span_9](end_span),
            "৫. সূরা আস-সাফফাত, আল-হাশর ও আল-জিন" to "وَالصَّافَّاتِ صَفًّا ، فَالزَّاجِرَاتِ زَجْرًا...\n\nلَوْ أَنْزَلْنَا هَذَا الْقُرْآنَ عَلَى جَبَلٍ لَرَأَيْتَهُ خَاشِعًا...\n\nقُلْ أُوحِيَ إِلَيَّ أَنَّهُ اسْتَمَعَ نَفَرٌ مِنَ الْجِنِّ...[span_10](start_span)"[span_10](end_span)
        )

        for (item in manzilAyats) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(20, 16, 20, 16)
                val bg = GradientDrawable(); bg.setColor(getCardBgColor()); bg.cornerRadius = 20f; bg.setStroke(1, Color.parseColor("#CBD5E1")); background = bg
                val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 0, 0, 14); layoutParams = lp
            }
            val t = TextView(this).apply { text = item.first; textSize = 15.5f; typeface = Typeface.SERIF; setTextColor(accent); setTypeface(Typeface.SERIF, Typeface.BOLD); setPadding(0, 0, 0, 6) }
            val b = TextView(this).apply { text = item.second; textSize = 16.5f; typeface = Typeface.SERIF; setTextColor(getTextColor()); setLineSpacing(8f, 1.2f) }
            card.addView(t); card.addView(b); content.addView(card)
        }

        val btnBack = Button(this).apply {
            text = "⬅ ফোল্ডার তালিকায় ফিরে যান"; typeface = Typeface.SERIF
            setBackgroundColor(if (isWhiteTheme()) Color.parseColor("#0F766E") else Color.parseColor("#264536")); setTextColor(Color.WHITE)
            setOnClickListener { openScreen("amal_folders") }
        }
        content.addView(btnBack)

        scroll.addView(content)
        root.addView(scroll)
        root.addView(createNavBar("amal_folders"))
        setContentView(root)
    }

    private fun showHadithScreen() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = getThemeBackground() }
        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(26, 26, 26, 26) }
        val accent = getAccentColor()

        val h = TextView(this).apply { text = "📚 সিহাহ সিত্তাহ (বিশুদ্ধ হাদীস কিতাব)"; textSize = 19f; typeface = Typeface.SERIF; setTextColor(accent); setTypeface(Typeface.SERIF, Typeface.BOLD); setPadding(0, 0, 0, 8) }
        val desc = TextView(this).apply { text = "আরবি মূল মতন ও পূর্ণাঙ্গ বাংলা অনুবাদসহ সরাসরি অধ্যয়ন করুন:"; textSize = 13f; typeface = Typeface.SERIF; setTextColor(getSecondaryTextColor()); setPadding(0, 0, 0, 16) }
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
                orientation = LinearLayout.VERTICAL; setPadding(20, 16, 20, 16)
                val bg = GradientDrawable(); bg.setColor(getCardBgColor()); bg.cornerRadius = 20f; bg.setStroke(1, Color.parseColor("#CBD5E1")); background = bg
                val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 0, 0, 14); layoutParams = lp
            }
            val bName = TextView(this).apply { text = book.first; textSize = 15.5f; typeface = Typeface.SERIF; setTextColor(getTextColor()); setTypeface(Typeface.SERIF, Typeface.BOLD) }
            val bInfo = TextView(this).apply { text = book.second; textSize = 12.5f; typeface = Typeface.SERIF; setTextColor(getSecondaryTextColor()); setPadding(0, 4, 0, 10) }
            val btnRead = Button(this).apply {
                text = "📖 হাদীস পড়ুন (আরবি ও বাংলা)"; textSize = 12.5f; typeface = Typeface.SERIF
                setBackgroundColor(if (isWhiteTheme()) Color.parseColor("#0F766E") else Color.parseColor("#264536")); setTextColor(Color.WHITE)
                setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(book.third))) }
            }
            card.addView(bName); card.addView(bInfo); card.addView(btnRead); content.addView(card)
        }

        scroll.addView(content)
        root.addView(scroll)
        root.addView(createNavBar("hadith"))
        setContentView(root)
    }

    private fun showNotepadScreen() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = getThemeBackground() }
        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(26, 26, 26, 26) }
        val accent = getAccentColor()

        val h = TextView(this).apply { text = "📝 ইসলামিক নোটপ্যাড (ক্লাউড স্টোরেজ)"; textSize = 19f; typeface = Typeface.SERIF; setTextColor(accent); setTypeface(Typeface.SERIF, Typeface.BOLD); setPadding(0, 0, 0, 14) }
        content.addView(h)

        val btnAddNote = Button(this).apply {
            text = "➕ নতুন নোট যুক্ত করুন"; setBackgroundColor(accent); setTextColor(Color.BLACK); textSize = 14.5f; typeface = Typeface.SERIF; setTypeface(Typeface.SERIF, Typeface.BOLD)
            val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 0, 0, 18); layoutParams = lp
            setOnClickListener { showNoteEditDialog(null) }
        }
        content.addView(btnAddNote)

        if (noteList.isEmpty()) {
            val emptyText = TextView(this).apply { text = "কোনো নোট সংরক্ষিত নেই। উপরে বাটনে চাপ দিয়ে যেকোনো আমল বা প্রয়োজনীয় তথ্য লিখে রাখুন।"; textSize = 14f; typeface = Typeface.SERIF; setTextColor(getSecondaryTextColor()); gravity = Gravity.CENTER; setPadding(20, 60, 20, 40) }
            content.addView(emptyText)
        } else {
            for (note in noteList) {
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL; setPadding(20, 16, 20, 16)
                    val bg = GradientDrawable(); bg.setColor(getCardBgColor()); bg.cornerRadius = 20f; bg.setStroke(1, Color.parseColor("#CBD5E1")); background = bg
                    val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 0, 0, 14); layoutParams = lp
                }
                val nTitle = TextView(this).apply { text = note.title; textSize = 16f; typeface = Typeface.SERIF; setTextColor(getTextColor()); setTypeface(Typeface.SERIF, Typeface.BOLD) }
                val nDate = TextView(this).apply { text = "তারিখ: ${note.date}"; textSize = 11.5f; typeface = Typeface.SERIF; setTextColor(getSecondaryTextColor()); setPadding(0, 2, 0, 8) }
                val nContent = TextView(this).apply { text = note.content; textSize = 14f; typeface = Typeface.SERIF; setTextColor(getTextColor()); setPadding(0, 0, 0, 12) }

                val rowAction = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
                val btnEdit = Button(this).apply { text = "এডিট"; textSize = 12f; typeface = Typeface.SERIF; setOnClickListener { showNoteEditDialog(note) } }
                val btnDel = Button(this).apply {
                    text = "ডিলিট"; textSize = 12f; typeface = Typeface.SERIF; setBackgroundColor(Color.parseColor("#DC2626")); setTextColor(Color.WHITE)
                    val lp = LinearLayout.LayoutParams(-2, -2); lp.setMargins(10, 0, 0, 0); layoutParams = lp
                    setOnClickListener {
                        noteList.remove(note)
                        saveAllData()
                        showNotepadScreen()
                    }
                }
                rowAction.addView(btnEdit); rowAction.addView(btnDel)
                card.addView(nTitle); card.addView(nDate); card.addView(nContent); card.addView(rowAction); content.addView(card)
            }
        }

        scroll.addView(content)
        root.addView(scroll)
        root.addView(createNavBar("notepad"))
        setContentView(root)
    }

    private fun showNoteEditDialog(existingNote: NoteItem?) {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(36, 16, 36, 16) }
        val titleInput = EditText(this).apply { hint = "নোটের টাইটেল"; setText(existingNote?.title ?: ""); typeface = Typeface.SERIF }
        val contentInput = EditText(this).apply { hint = "নোটের বিবরণ..."; setText(existingNote?.content ?: ""); typeface = Typeface.SERIF; minLines = 4; gravity = Gravity.TOP }
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
            .setNegativeButton("বাতিল", null).show()
    }

    private fun showSettingsScreen() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = getThemeBackground() }
        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 0, 1f) }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(26, 26, 26, 26) }
        val accent = getAccentColor()

        val h = TextView(this).apply { text = "প্রোফাইল, ব্যাকআপ ও সেটিংস"; textSize = 20f; typeface = Typeface.SERIF; setTextColor(accent); setTypeface(Typeface.SERIF, Typeface.BOLD); setPadding(0, 0, 0, 18) }
        content.addView(h)

        val devCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(22, 18, 22, 18)
            val bg = GradientDrawable(); bg.setColor(if (isWhiteTheme()) Color.parseColor("#FEF3C7") else Color.parseColor("#223329")); bg.cornerRadius = 24f; bg.setStroke(2, accent); background = bg
        }
        val devTitle = TextView(this).apply { text = "🌟 অ্যাপ উদ্যোক্তা ও পরিচালক"; textSize = 16.5f; typeface = Typeface.SERIF; setTextColor(getTextColor()); setTypeface(Typeface.SERIF, Typeface.BOLD) }
        val devName = TextView(this).apply { text = "নাম: সাব্বির আহমাদ"; textSize = 15f; typeface = Typeface.SERIF; setTextColor(getTextColor()); setPadding(0, 6, 0, 2) }
        val devPhone = TextView(this).apply { text = "মোবাইল: ০১৭২৫-২২৮৬২২"; textSize = 15f; typeface = Typeface.SERIF; setTextColor(if (isWhiteTheme()) Color.parseColor("#059669") else Color.parseColor("#86EFAC")); setTypeface(Typeface.SERIF, Typeface.BOLD); setPadding(0, 0, 0, 8) }
        
        val btnCall = Button(this).apply {
            text = "📞 সরাসরি কল করুন"; textSize = 13f; typeface = Typeface.SERIF; setBackgroundColor(Color.parseColor("#059669")); setTextColor(Color.WHITE)
            setOnClickListener { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:01725228622"))) }
        }

        val btnFbPage = Button(this).apply {
            text = "📘 আমাদের ইসলামিক ফেসবুক পেজ"; textSize = 13f; typeface = Typeface.SERIF; setBackgroundColor(Color.parseColor("#1877F2")); setTextColor(Color.WHITE)
            val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 8, 0, 0); layoutParams = lp
            setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(FB_PAGE_URL))
                startActivity(intent)
            }
        }

        devCard.addView(devTitle)
        devCard.addView(devName)
        devCard.addView(devPhone)
        devCard.addView(btnCall)
        devCard.addView(btnFbPage)
        content.addView(devCard)

        val backupCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(20, 16, 20, 16)
            val bg = GradientDrawable(); bg.setColor(getCardBgColor()); bg.cornerRadius = 20f; bg.setStroke(1, Color.parseColor("#CBD5E1")); background = bg
            val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 18, 0, 0); layoutParams = lp
        }
        val bTitle = TextView(this).apply { text = "☁️ গুগল ক্লাউড সাইন ইন ও সিঙ্ক"; textSize = 15.5f; typeface = Typeface.SERIF; setTextColor(getTextColor()); setTypeface(Typeface.SERIF, Typeface.BOLD) }
        val bDesc = TextView(this).apply {
            text = if (userEmail.isEmpty()) "কোনো জিমেইল সংযুক্ত নেই। নিচের বাটনে চাপ দিয়ে সরাসরি আপনার ফোনের Google Account সিলেক্ট করুন।" else "সংযুক্ত ক্লাউড জিমেইল:\n$userEmail\n(১০০% গুগল ক্লাউডে স্টোর হচ্ছে)"
            textSize = 12.5f; typeface = Typeface.SERIF; setTextColor(getSecondaryTextColor()); setPadding(0, 4, 0, 12)
        }
        val btnLogin = Button(this).apply {
            text = if (userEmail.isEmpty()) "🟢 গুগল দিয়ে সরাসরি সাইন ইন" else "🔄 গুগল অ্যাকাউন্ট পরিবর্তন / সিঙ্ক"
            typeface = Typeface.SERIF; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE)
            setOnClickListener { promptGoogleAccountPicker() }
        }
        backupCard.addView(bTitle); backupCard.addView(bDesc); backupCard.addView(btnLogin); content.addView(backupCard)

        val themeInfo = TextView(this).apply { text = "\n🎨 অ্যাপ থিম নির্বাচন করুন:"; textSize = 15f; typeface = Typeface.SERIF; setTextColor(getTextColor()); setPadding(0, 10, 0, 10) }
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
                text = th.first; typeface = Typeface.SERIF; setBackgroundColor(Color.parseColor(th.second))
                setTextColor(if (th.first.contains("সাদা")) Color.BLACK else Color.WHITE); textSize = 14.5f
                val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 6, 0, 6); layoutParams = lp
                setOnClickListener { currentTheme = th.first; saveAllData(); showSettingsScreen() }
            }
            content.addView(btn)
        }

        scroll.addView(content)
        root.addView(scroll)
        root.addView(createNavBar("settings"))
        setContentView(root)
    }
}
