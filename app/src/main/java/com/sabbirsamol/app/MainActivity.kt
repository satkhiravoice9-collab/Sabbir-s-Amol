package com.sabbirsamol.app

import android.accounts.AccountManager
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
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
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Stack
import java.util.UUID
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

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        val notification = builder
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
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
        return "${toBangla(String.format(Locale.US, "%02d", h))}:${toBangla(String.format(Locale.US, "%02d", m))} $amPm"
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

        if (activeZikrId.isEmpty() || zikrList.none { it.id == activeZikrId }) {
            activeZikrId = zikrList.firstOrNull()?.id ?: "1"
        }

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
        } catch (e: ActivityNotFoundE
