package com.sabbir.amol

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this)
        textView.text = "Sabbir's Amol চালু হয়েছে"
        textView.textSize = 24f

        setContentView(textView)
    }
}
