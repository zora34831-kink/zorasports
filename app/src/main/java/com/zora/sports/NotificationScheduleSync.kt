
package com.zora.sports

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.database.ValueEventListener

@Composable
fun NotificationScheduleSync() {
    val context = LocalContext.current.applicationContext

    DisposableEffect(context) {
        val listener: ValueEventListener = StreamRepository.listen(
            onData = { streams ->
                StreamNotificationScheduler.sync(
                    context = context,
                    streams = streams
                )
            },
            onError = {
                // Sinkronisasi akan dicoba kembali
                // ketika Firebase mengirim perubahan berikutnya.
            }
        )

        onDispose {
            StreamRepository.removeListener(listener)
        }
    }
}
