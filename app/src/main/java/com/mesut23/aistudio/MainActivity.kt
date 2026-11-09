package com.mesut23.aistudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val Night = Color(0xFF0A0E1F)
private val Violet = Color(0xFF8B5CF6)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StudioApp() }
    }
}

@Composable
private fun StudioApp() {
    var prompt by remember { mutableStateOf("") }
    var active by remember { mutableIntStateOf(0) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Violet, background = Night, surface = Color(0xFF151B32))) {
        Scaffold(containerColor = Night, bottomBar = {
            NavigationBar(containerColor = Color(0xFF151B32)) {
                listOf("الرئيسية", "إنشاء", "تعديل", "أعمالي", "الإعدادات").forEachIndexed { index, label ->
                    NavigationBarItem(selected = active == index, onClick = { active = index }, icon = {}, label = { Text(label) })
                }
            }
        }) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("MESUT AI STUDIO", style = MaterialTheme.typography.labelLarge, color = Violet)
                Text(
                    when (active) {
                        1 -> "حوّل فكرتك إلى صورة"
                        2 -> "تعديل الصور بالذكاء الاصطناعي"
                        3 -> "معرض إبداعاتك"
                        4 -> "الإعدادات"
                        else -> "مرحباً بك في عالم الإبداع"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                if (active == 0) {
                    Card {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Mesut AI Studio · الإصدار 1.0.0", color = Violet)
                            Text("أنشئ صوراً جديدة وعدّل أفكارك من مكان واحد.")
                        }
                    }
                    Button(onClick = { active = 1 }, modifier = Modifier.fillMaxWidth()) { Text("ابدأ الإنشاء") }
                    OutlinedButton(onClick = { active = 2 }, modifier = Modifier.fillMaxWidth()) { Text("تعديل صورة") }
                } else if (active == 1 || active == 2) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        label = { Text(if (active == 1) "اكتب وصف الصورة" else "صف التعديلات المطلوبة") }
                    )
                    Button(onClick = { }, enabled = prompt.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                        Text(if (active == 1) "إنشاء الصورة" else "تطبيق التعديل")
                    }
                    Text("سيتم تفعيل التوليد بعد إعداد عنوان خادم الذكاء الاصطناعي.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (active == 3) {
                    Text("ستظهر الصور التي تنشئها هنا بعد تفعيل خدمة الإنشاء.")
                } else {
                    Text("اللغة: العربية")
                    Text("الإصدار: 1.0.0")
                }
            }
        }
    }
}
