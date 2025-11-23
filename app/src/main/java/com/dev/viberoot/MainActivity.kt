package com.dev.viberoot

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import org.json.JSONObject

// ⚠️ PASTE YOUR API KEY HERE
const val MY_API_KEY = "AIzaSyDoozKSFTQz1bW7zr9uuejpxTLB99H_xpE"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // VibeRoot Theme: Deep Earthy Greens & Neon Accents
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF1B5E20), // Dark Forest
                    secondary = Color(0xFFC0CA33), // Lime Green
                    background = Color(0xFFF1F8E9) // Light Mist
                )
            ) {
                VibeRootApp()
            }
        }
    }
}

@Composable
fun VibeRootApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val db = Firebase.firestore

    val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = MY_API_KEY
    )

    var vibeInput by remember { mutableStateOf("") }
    var plantName by remember { mutableStateOf("") }
    var vibeReason by remember { mutableStateOf("") }
    var imageKeyword by remember { mutableStateOf("jungle") }
    var isLoading by remember { mutableStateOf(false) }
    var hasResult by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Branding
        Text("✨ VibeRoot", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        Text("Plant matching for your soul.", fontSize = 16.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(30.dp))

        // Input Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("What's your vibe?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = vibeInput,
                    onValueChange = { vibeInput = it },
                    label = { Text("e.g. Chaotic energy, Zen master, Forgetful") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (vibeInput.isBlank()) return@Button
                        scope.launch {
                            isLoading = true
                            hasResult = false
                            try {
                                // 🌟 THE VIBE PROMPT 🌟
                                val prompt = """
                                    Match a houseplant to this personality/vibe: '$vibeInput'.
                                    Return ONLY JSON:
                                    {
                                      "plant": "Common Name",
                                      "vibe_match": "One sentence explaining why it matches the vibe.",
                                      "img_keyword": "SingleWordForSearch"
                                    }
                                """.trimIndent()

                                val response = generativeModel.generateContent(prompt)
                                val cleanJson = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: "{}"
                                val jsonObject = JSONObject(cleanJson)

                                plantName = jsonObject.getString("plant")
                                vibeReason = jsonObject.getString("vibe_match")
                                imageKeyword = jsonObject.getString("img_keyword")
                                hasResult = true
                            } catch (e: Exception) {
                                Toast.makeText(context, "AI Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White)
                    else Text("Find My Plant Match")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Result Card
        if (hasResult) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box {
                        AsyncImage(
                            model = "https://loremflickr.com/500/400/$imageKeyword,plant",
                            contentDescription = "Plant",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(220.dp)
                        )
                        // Badge
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(bottomEnd = 16.dp),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Text(
                                "PERFECT MATCH",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(plantName, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("✨ $vibeReason", fontSize = 16.sp, lineHeight = 24.sp)

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val data = hashMapOf(
                                    "plant" to plantName,
                                    "vibe_input" to vibeInput,
                                    "timestamp" to System.currentTimeMillis()
                                )
                                db.collection("vibe_garden").add(data)
                                    .addOnSuccessListener { Toast.makeText(context, "Planted in Cloud! 🌱", Toast.LENGTH_SHORT).show() }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save to My Garden", color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}