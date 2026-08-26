package com.sabbirsamol.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("sabbirs_amol", Context.MODE_PRIVATE) }
    private val amols = mutableListOf("ফজর নামাজ", "যোহর নামাজ", "আসর নামাজ", "মাগরিব নামাজ", "এশা নামাজ", "সকাল-সন্ধ্যার জিকির", "কুরআন তিলাওয়াত")
    private var count = 0
    private var target = 100
    private var locked = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        count = prefs.getInt("count", 0)
        target = prefs.getInt("target", 100)
        locked = prefs.getBoolean("locked", true)
        showTasbih()
    }

    private fun base(title: String): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        l.setPadding(32, 32, 32, 24)
        l.setBackgroundColor(Color.rgb(244, 247, 243))
        val h = TextView(this)
        h.text = "Sabbir's Amol\n$title"
        h.textSize = 24f
        h.setTextColor(Color.rgb(12, 77, 55))
        h.setPadding(0, 0, 0, 20)
        l.addView(h)
        return l
    }

    private fun nav(l: LinearLayout) {
        val row = LinearLayout(this)
        row.gravity = Gravity.CENTER
        listOf(
            "আমল" to { showToday() },
            "তাসবিহ" to { showTasbih() },
            "হিসাব" to { showReport() },
            "সেটিংস" to { showSettings() }
        ).forEach { p ->
            val b = Button(this)
            b.text = p.first
            b.setOnClickListener { p.second() }
            row.addView(b, LinearLayout.LayoutParams(0, -2, 1f))
        }
        l.addView(row)
    }

    private fun showToday() {
        val l = base("দৈনিক আমল")
        val key = "day_" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        amols.forEachIndexed { i, a ->
            val cb = CheckBox(this)
            cb.text = a
            cb.textSize = 18f
            cb.isChecked = prefs.getBoolean("${key}_$i", false)
            cb.setOnCheckedChangeListener { _, v -> prefs.edit().putBoolean("${key}_$i", v).apply() }
            l.addView(cb)
        }
        val add = Button(this)
        add.text = "+ নতুন আমল যোগ করুন"
        add.setOnClickListener {
            val e = EditText(this)
            e.hint = "আমলের নাম লিখুন"
            AlertDialog.Builder(this)
                .setTitle("নতুন আমল")
                .setView(e)
                .setPositiveButton("যোগ করুন") { _, _ ->
                    if (e.text.isNotBlank()) {
                        amols.add(e.text.toString())
                        showToday()
                    }
                }
                .setNegativeButton("বাতিল", null)
                .show()
        }
        l.addView(add)
        nav(l)
        setContentView(ScrollView(this).apply { addView(l) })
    }

    private fun showTasbih() {
        val l = base("ডিজিটাল তাসবিহ")
        val c = TextView(this)
        c.text = count.toString()
        c.textSize = 72f
        c.gravity = Gravity.CENTER
        c.setTextColor(Color.rgb(22, 115, 79))
        l.addView(c)

        val info = TextView(this)
        info.text = if (target > 0) "টার্গেট: $target | লক: ${if (locked) "চালু" else "বন্ধ"}" else "টার্গেট নেই"
        info.gravity = Gravity.CENTER
        l.addView(info)

        val plus = Button(this)
        plus.text = "গণনা করুন (+১)"
        plus.textSize = 22f
        plus.setOnClickListener {
            if (locked && target > 0 && count >= target) {
                Toast.makeText(this, "টার্গেট পূর্ণ হয়েছে!", Toast.LENGTH_SHORT).show()
            } else {
                count++
                prefs.edit().putInt("count", count).apply()
                showTasbih()
            }
        }
        l.addView(plus)

        val edit = Button(this)
        edit.text = "টার্গেট সেট করুন"
        edit.setOnClickListener {
            val e = EditText(this)
            e.inputType = 2
            e.setText(target.toString())
            AlertDialog.Builder(this)
                .setTitle("টার্গেট সংখ্যা")
                .setView(e)
                .setPositiveButton("সংরক্ষণ") { _, _ ->
                    target = e.text.toString().toIntOrNull() ?: 0
                    AlertDialog.Builder(this)
                        .setMessage("টার্গেট শেষে বাটন লক থাকবে?")
                        .setPositiveButton("হ্যাঁ") { _, _ ->
                            locked = true
                            saveT()
                            showTasbih()
                        }
                        .setNegativeButton("না") { _, _ ->
                            locked = false
                            saveT()
                            showTasbih()
                        }
                        .show()
                }
                .show()
        }
        l.addView(edit)

        val reset = Button(this)
        reset.text = "রিসেট"
        reset.setOnClickListener {
            count = 0
            prefs.edit().putInt("count", 0).apply()
            showTasbih()
        }
        l.addView(reset)

        nav(l)
        setContentView(l)
    }

    private fun saveT() {
        prefs.edit().putInt("target", target).putBoolean("locked", locked).apply()
    }

    private fun showReport() {
        val l = base("আমলের হিসাব")
        val t = TextView(this)
        t.text = "এখানে আপনার বিগত দিনের আমল ও তাসবিহের বিবরণ জমা থাকবে।"
        t.textSize = 18f
        l.addView(t)
        nav(l)
        setContentView(l)
    }

    private fun showSettings() {
        val l = base("সেটিংস")
        val t = TextView(this)
        t.text = "Sabbir's Amol v1.0\nআপনার দৈনন্দিন ইবাদতের সঙ্গী।"
        t.textSize = 17f
        l.addView(t)
        nav(l)
        setContentView(l)
    }
}
