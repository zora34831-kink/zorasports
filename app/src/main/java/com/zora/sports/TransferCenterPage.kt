package com.zora.sports

import android.text.Html
import android.util.Xml
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL

private data class TransferArticle(val title: String, val link: String, val summary: String, val date: String)

private object TransferCenterRepository {
    private const val FEED = "https://feeds.bbci.co.uk/sport/football/rss.xml"
    suspend fun load(): List<TransferArticle> = withContext(Dispatchers.IO) {
        val connection = URL(FEED).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 12000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", "ZORASPORTS Android")
            if (connection.responseCode !in 200..299) error("Sumber berita transfer tidak tersedia (HTTP ${connection.responseCode}).")
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            connection.inputStream.use { parser.setInput(it, null) }
            val found = mutableListOf<TransferArticle>()
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT && found.size < 60) {
                if (event == XmlPullParser.START_TAG && parser.name.equals("item", true)) {
                    var title = ""
                    var link = ""
                    var summary = ""
                    var date = ""
                    var inside = true
                    while (inside) {
                        event = parser.next()
                        when (event) {
                            XmlPullParser.START_TAG -> when (parser.name.lowercase()) {
                                "title" -> title = parser.nextText().trim()
                                "link" -> link = parser.nextText().trim()
                                "description" -> summary = parser.nextText().trim()
                                "pubdate" -> date = parser.nextText().trim()
                            }
                            XmlPullParser.END_TAG -> if (parser.name.equals("item", true)) inside = false
                            XmlPullParser.END_DOCUMENT -> inside = false
                        }
                    }
                    val cleanTitle = Html.fromHtml(title, Html.FROM_HTML_MODE_COMPACT).toString()
                    val cleanSummary = Html.fromHtml(summary, Html.FROM_HTML_MODE_COMPACT).toString().replace(Regex("\\s+"), " ").trim()
                    val text = (cleanTitle + " " + cleanSummary).lowercase()
                    val terms = listOf("transfer", "transfers", "signing", "signs", "loan", "bid", "deal", "rumour", "rumor", "contract", "move to", "joins")
                    if (cleanTitle.isNotBlank() && link.startsWith("https://") && terms.any { text.contains(it) }) {
                        found.add(TransferArticle(cleanTitle, link, cleanSummary, date))
                    }
                } else event = parser.next()
            }
            found
        } finally { connection.disconnect() }
    }
}

@Composable
fun TransferCenterPage(modifier: Modifier = Modifier) {
    val uri = LocalUriHandler.current
    var articles by remember { mutableStateOf<List<TransferArticle>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(refresh) {
        loading = true
        error = null
        try {
            articles = withTimeout(22000L) { TransferCenterRepository.load() }
            if (articles.isEmpty()) error = "Belum ada artikel transfer yang terdeteksi dari sumber berita."
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "Gagal memuat kabar transfer." }
        finally { loading = false }
    }
    Column(Modifier.fillMaxSize().background(ZoraThemeColors.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ZORA SPORTS / TRANSFER DESK", color = ZoraThemeColors.accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text("Transfer Center", color = ZoraThemeColors.text, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("Kabar perpindahan pemain dan aktivitas bursa", color = ZoraThemeColors.muted, fontSize = 11.sp)
            }
            IconButton(onClick = { refresh++ }, enabled = !loading) { Icon(Icons.Default.Refresh, null, tint = ZoraThemeColors.accent) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(RoundedCornerShape(14.dp)).background(ZoraThemeColors.panel).border(1.dp, Color(0xFF294258), RoundedCornerShape(14.dp)).padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.SwapHoriz, null, tint = ZoraThemeColors.accent, modifier = Modifier.size(25.dp))
            Column {
                Text("TRANSFER WATCH", color = ZoraThemeColors.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Artikel disaring dari feed BBC Sport. Rumor bukan konfirmasi resmi.", color = ZoraThemeColors.muted, fontSize = 9.sp)
            }
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ZoraThemeColors.accent) }
            error != null -> Box(Modifier.fillMaxSize().padding(22.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(error.orEmpty(), color = ZoraThemeColors.text, fontSize = 13.sp)
                    TextButton(onClick = { refresh++ }) { Text("COBA LAGI", color = ZoraThemeColors.accent) }
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                item { Text("${articles.size} ARTIKEL TERKAIT TRANSFER", color = ZoraThemeColors.muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                items(articles) { article ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(ZoraThemeColors.panel).border(1.dp, Color(0xFF294258), RoundedCornerShape(17.dp)).clickable { runCatching { uri.openUri(article.link) } }.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("TRANSFER NEWS  •  BBC SPORT", color = ZoraThemeColors.accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(article.title, color = ZoraThemeColors.text, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        if (article.summary.isNotBlank()) Text(article.summary, color = ZoraThemeColors.muted, fontSize = 10.sp, lineHeight = 15.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(article.date, color = ZoraThemeColors.muted, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}
