
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

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF45D9F5),
                    background = Color(0xFF071321),
                    surface = Color(0xFF112438)
                )
            ) {
                ZoraTestScreen()
            }
        }
    }
}

@Composable
fun ZoraTestScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF071321)
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
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                "SPORTS",
                fontSize = 17.sp,
                letterSpacing = 6.sp,
                color = Color(0xFF45D9F5)
            )

            Spacer(Modifier.height(35.dp))

            Surface(
                color = Color(0xFF112438),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "ZORASPORTS READY",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Aplikasi berhasil dijalankan.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
