package com.zora.sports

import android.text.Html
import android.util.Xml
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            connection.instanceFollowRedirects = true
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) ZORASPORTS/1.5")
            connection.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, */*")
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("BBC Sport tidak merespons (HTTP ${connection.responseCode}).")
            }

            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            connection.inputStream.buffered().use { parser.setInput(it, "UTF-8") }
            val found = linkedMapOf<String, TransferArticle>()
            var event = parser.eventType

            while (event != XmlPullParser.END_DOCUMENT && found.size < 80) {
                if (event == XmlPullParser.START_TAG && parser.name.substringAfter(':').equals("item", true)) {
                    var title = ""
                    var link = ""
                    var summary = ""
                    var date = ""
                    var inside = true
                    while (inside) {
                        event = parser.next()
                        when (event) {
                            XmlPullParser.START_TAG -> when (parser.name.substringAfter(':').lowercase()) {
                                "title" -> title = parser.nextText().trim()
                                "link" -> link = parser.nextText().trim()
                                "description" -> summary = parser.nextText().trim()
                                "pubdate" -> date = parser.nextText().trim()
                            }
                            XmlPullParser.END_TAG -> if (parser.name.substringAfter(':').equals("item", true)) inside = false
                            XmlPullParser.END_DOCUMENT -> inside = false
                        }
                    }
                    val cleanTitle = Html.fromHtml(title, Html.FROM_HTML_MODE_COMPACT).toString().trim()
                    val cleanSummary = Html.fromHtml(summary, Html.FROM_HTML_MODE_COMPACT).toString()
                        .replace(Regex("\\s+"), " ").trim()
                    val text = (cleanTitle + " " + cleanSummary).lowercase()
                    val terms = listOf(
                        "transfer", "transfers", "signing", "signs", "signed", "loan", "bid", "deal",
                        "rumour", "rumor", "gossip", "contract", "move to", "joins", "joined",
                        "interested", "linked with", "target", "departure", "exit", "wants", "agreed terms"
                    )
                    if (cleanTitle.isNotBlank() && link.startsWith("https://") &&
                        terms.any { text.contains(it) }) {
                        found.putIfAbsent(link, TransferArticle(cleanTitle, link, cleanSummary, date))
                    }
                } else {
                    event = parser.next()
                }
            }
            found.values.toList()
        } finally {
            connection.disconnect()
        }
    }
}

@Composable
fun TransferCenterPage(modifier: Modifier = Modifier) {
    var selectedSection by remember { mutableStateOf("TRANSFER") }
    Column(modifier.fillMaxSize().background(ZoraThemeColors.background)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("ZORA SPORTS / MARKET", color = ZoraThemeColors.accent,
                fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text("Market", color = ZoraThemeColors.text, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Pusat kabar transfer dan berita sepak bola", color = ZoraThemeColors.muted, fontSize = 11.sp)
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            listOf("TRANSFER" to "Transfer Pemain", "NEWS" to "Football News").forEach { (key, label) ->
                val active = selectedSection == key
                Column(
                    Modifier.clickable { selectedSection = key }.padding(vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(label, color = if (active) ZoraThemeColors.text else ZoraThemeColors.muted,
                        fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                    Spacer(Modifier.fillMaxWidth().height(2.dp)
                        .background(if (active) ZoraThemeColors.accent else ZoraThemeColors.background))
                }
            }
        }
        HorizontalDivider(color = ZoraThemeColors.border)

        if (selectedSection == "TRANSFER") {
            TransferArticlesContent(Modifier.weight(1f))
        } else {
            FootballNewsPage(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TransferArticlesContent(modifier: Modifier = Modifier) {
    val uri = LocalUriHandler.current
    var articles by remember { mutableStateOf<List<TransferArticle>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh) {
        loading = true
        error = null
        try {
            articles = withTimeout(25000L) { TransferCenterRepository.load() }
            if (articles.isEmpty()) error = "Belum ada kabar transfer terbaru dari BBC Sport."
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Gagal memuat kabar transfer."
        } finally {
            loading = false
        }
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Icon(Icons.Default.SwapHoriz, null, tint = ZoraThemeColors.accent, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text("TRANSFER WATCH", color = ZoraThemeColors.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Rumor transfer bukan konfirmasi resmi.", color = ZoraThemeColors.muted, fontSize = 9.sp)
            }
            IconButton(onClick = { refresh++ }, enabled = !loading) {
                Icon(Icons.Default.Refresh, "Muat ulang", tint = ZoraThemeColors.accent)
            }
        }
        when {
            loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ZoraThemeColors.accent)
            }
            error != null -> Box(Modifier.weight(1f).fillMaxWidth().padding(22.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(error.orEmpty(), color = ZoraThemeColors.text, fontSize = 13.sp)
                    TextButton(onClick = { refresh++ }) { Text("COBA LAGI", color = ZoraThemeColors.accent) }
                }
            }
            else -> LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
            ) {
                item {
                    Text("${articles.size} KABAR TRANSFER", color = ZoraThemeColors.muted,
                        fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                }
                items(articles, key = { it.link }) { article ->
                    Column(
                        Modifier.fillMaxWidth().clickable { runCatching { uri.openUri(article.link) } }
                            .padding(vertical = 13.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("TRANSFER NEWS  /  BBC SPORT", color = ZoraThemeColors.accent,
                            fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(article.title, color = ZoraThemeColors.text, fontSize = 14.sp,
                            lineHeight = 19.sp, fontWeight = FontWeight.Bold, maxLines = 3,
                            overflow = TextOverflow.Ellipsis)
                        if (article.summary.isNotBlank()) Text(article.summary, color = ZoraThemeColors.muted,
                            fontSize = 10.sp, lineHeight = 15.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(article.date, color = ZoraThemeColors.muted, fontSize = 9.sp)
                    }
                    HorizontalDivider(color = ZoraThemeColors.border)
                }
            }
        }
    }
}
