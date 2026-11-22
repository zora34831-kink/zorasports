
package com.zora.sports

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.rgb(7, 19, 33)
        window.navigationBarColor = android.graphics.Color.rgb(7, 19, 33)

        setContent {
            ZoraSportsApp()
        }
    }
}

@Composable
fun ZoraSportsApp() {
    val navy = Color(0xFF071321)
    val panel = Color(0xFF112438)
    val cyan = Color(0xFF45D9F5)

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = cyan,
            background = navy,
            surface = panel
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = navy
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "ZORA",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    "SPORTS",
                    color = cyan,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 7.sp
                )

                Spacer(Modifier.height(35.dp))

                Surface(
                    color = panel,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(25.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "WELCOME TO ZORASPORTS",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "Your world of football.",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )

                        Spacer(Modifier.height(20.dp))

                        Text(
                            "SYSTEM READY",
                            color = cyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
