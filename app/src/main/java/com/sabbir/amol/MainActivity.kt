package com.sabbir.amol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TwentySixAmolSimpleScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwentySixAmolSimpleScreen() {
    val amolList = listOf(
        "১. সূরা আল-ফাতিহা" to "রোগমুক্তি ও মনের শান্তি লাভের আমল",
        "২. আয়াতুল কুরসি" to "শয়তানের অনিষ্ট ও বিপদ থেকে বাঁচার আমল",
        "৩. সূরা ইয়াসিন" to "মাগফিরাত ও মনের নেক আশা পূরণের আমল",
        "৪. সূরা আল-ওয়াকিয়া" to "দারিদ্র্য দূরীকরণ ও রিজিক বৃদ্ধির আমল",
        "৫. সূরা আল-মুলক" to "কবরের আজাব থেকে মুক্তির আমল",
        "৬. সূরা আর-রহমান" to "আল্লাহর নিয়ামতের শুকরিয়া ও অন্তরের প্রশান্তি",
        "৭. সূরা আল-কাহফ" to "দাজ্জালের ফিতনা থেকে হিফাজতের আমল",
        "৮. সূরা আল-ইখলাস" to "এক-তৃতীয়াংশ কুরআন তিলাওয়াতের সওয়াব",
        "৯. সূরা আল-ফালাক" to "জাদু-টোনা ও হিংসুকদের থেকে বাঁচার আমল",
        "১০. সূরা আন-নাস" to "শয়তানের কুমন্ত্রণা ও অনিষ্ট থেকে মুক্তির আমল",
        "১১. সাইয়্যিদুল ইস্তিগফার" to "জান্নাত লাভের সর্বশ্রেষ্ঠ তওবা ও দোয়া",
        "১২. দরূদে ইব্রাহিম" to "আল্লাহর রহমত ও শাফায়াত পাওয়ার শ্রেষ্ঠ মাধ্যম",
        "১৩. তাহাজ্জুদ সালাত" to "আল্লাহর নৈকট্য ও দোয়া কবুলের সেরা সময়",
        "১৪. সালাতুত তাসবীহ" to "জীবনের সকল সগিরা গুনাহ মাফের বিশেষ নামাজ",
        "১৫. ইশরাকের নামাজ" to "একটি কবুল হজ ও উমরার পূর্ণ সওয়াব",
        "১৬. সূরা বাকারার শেষ দুই আয়াত" to "রাতের সকল প্রকার বালা-মুসিবত থেকে রক্ষা",
        "১৭. সূরা আদ-দুহা" to "হতাশা ও মানসিক বিষণ্ণতা দূর করার আমল",
        "১৮. সূরা আল-ইনশিরাহ" to "কঠিন কাজ সহজ করা ও মনের ভার লাঘব",
        "১৯. লা হাওলা ওয়ালা কুওয়াতা" to "জান্নাতের গুপ্তধন ও ৯৯টি বিপদের শেফা",
        "২০. সুবহানাল্লাহি ওয়া বিহামদিহি" to "প্রতিদিন ১০০ বার পড়লে সব গুনাহ মাফ",
        "২১. খাবার খাওয়ার দোয়া" to "খাবারে বরকত ও শয়তানের অংশ রোধ",
        "২২. ঘর থেকে বের হওয়ার দোয়া" to "সারাদিন আল্লাহর হেফাজতে থাকার আমল",
        "২৩. ঘুমানোর পূর্বের আমল" to "আয়াতুল কুরসি ও তিন কুল পড়ে শরীরে ফুঁক দেওয়া",
        "২৪. পিতা-মাতার জন্য দোয়া" to "রব্বির হামহুমা কামা রব্বায়ানি সগিরা",
        "২৫. ঋণ মুক্তির দোয়া" to "আল্লাহুম্মাকফিনী বিহালালিকা আন হারামিক",
        "২৬. জান্নাত ওয়াজিব হওয়ার দোয়া" to "রদিতু বিল্লাহি রব্বা ওয়াবিল ইসলামি দ্বীনা"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("২৬ আমল ও প্রয়োজনীয় সূরা", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(amolList) { (title, subtitle) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
