package com.sabbir.amol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SabbirsAmolApp()
        }
    }
}

@Composable
fun SabbirsAmolApp() {
    var currentScreen by remember { mutableStateOf("home") }

    when (currentScreen) {
        "home" -> HomeScreen(
            onOpen = { screen -> currentScreen = screen }
        )

        "quran" -> SimpleListScreen(
            title = "📖 আল-কুরআন",
            items = listOf(
                "সম্পূর্ণ কুরআন শরীফ",
                "সূরার তালিকা (শীঘ্রই যোগ করা হবে)"
            ),
            onBack = { currentScreen = "home" }
        )

        "special" -> SimpleListScreen(
            title = "⭐ বিশেষ সূরা",
            items = listOf(
                "সূরা আল-বাকারা",
                "সূরা আদ-দুখান",
                "সূরা ইয়াসিন",
                "সূরা আস-সাফফাত",
                "সূরা আল-জিন",
                "সূরা আর-রহমান",
                "সূরা আল-ওয়াকিয়া"
            ),
            onBack = { currentScreen = "home" }
        )

        "prayer" -> SimpleListScreen(
            title = "🕌 নামাজের দোয়া",
            items = listOf(
                "সানা",
                "রুকুর তাসবিহ",
                "রুকু থেকে ওঠার দোয়া",
                "সিজদার তাসবিহ",
                "দুই সিজদার মাঝের দোয়া",
                "তাশাহহুদ",
                "দরুদে ইবরাহিম",
                "দোয়া মাসুরা"
            ),
            onBack = { currentScreen = "home" }
        )

        "daily" -> SimpleListScreen(
            title = "🤲 দৈনন্দিন মাসনূন দোয়া",
            items = listOf(
                "খাবার শুরু করার দোয়া",
                "খাবার শেষে দোয়া",
                "বাথরুমে প্রবেশের দোয়া",
                "বাথরুম থেকে বের হওয়ার দোয়া",
                "ভ্রমণের দোয়া",
                "স্ত্রী সহবাসের পূর্বের দোয়া",
                "জানাজার দোয়া"
            ),
            onBack = { currentScreen = "home" }
        )

        "tasbih" -> TasbihScreen(
            onBack = { currentScreen = "home" }
        )

        "tracker" -> SimpleListScreen(
            title = "✅ দৈনিক আমল ট্র্যাকার",
            items = listOf(
                "ফজরের নামাজ",
                "যিকির",
                "কুরআন তিলাওয়াত",
                "সকাল-সন্ধ্যার আজকার"
            ),
            onBack = { currentScreen = "home" }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpen: (String) -> Unit) {
    val menuItems = listOf(
        "📖 আল-কুরআন" to "quran",
        "⭐ বিশেষ সূরা" to "special",
        "🕌 নামাজের দোয়া" to "prayer",
        "🤲 দৈনন্দিন মাসনূন দোয়া" to "daily",
        "📿 তাসবিহ কাউন্টার" to "tasbih",
        "✅ দৈনিক আমল ট্র্যাকার" to "tracker"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🕌 Sabbir's Amol") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(menuItems) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(item.second) }
                ) {
                    Text(
                        text = item.first,
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleListScreen(
    title: String,
    items: List<String>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← ফিরে যান")
                    }
                }
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
            items(items) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = item,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(onBack: () -> Unit) {
    var count by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📿 তাসবিহ কাউন্টার") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← ফিরে যান")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { count++ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("গণনা করুন")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { count = 0 },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("রিসেট করুন")
            }
        }
    }
}
