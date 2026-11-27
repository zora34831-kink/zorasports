package com.zora.sports

import android.text.Html
import android.util.Xml
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL

private data class FootballNewsItem(
    val title: String,
    val link: String,
    val summary: String,
    val publishedAt: String,
    val imageUrl: String?
)

private object FootballNewsRepository {
    private const val BBC_FEED = "https://feeds.bbci.co.uk/sport/football/rss.xml"

    suspend fun load(): List<FootballNewsItem> = withContext(Dispatchers.IO) {
        val connection = URL(BBC_FEED).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "ZORASPORTS Android")
            connection.connectTimeout = 12000
            connection.readTimeout = 15000
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Sumber berita sedang tidak bisa diakses (HTTP " + connection.responseCode + ").")
            }

            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            connection.inputStream.use { parser.setInput(it, null) }
            val results = mutableListOf<FootballNewsItem>()
            var event = parser.eventType

            while (event != XmlPullParser.END_DOCUMENT && results.size < 30) {
                if (event == XmlPullParser.START_TAG && parser.name.equals("item", true)) {
                    var title = ""
                    var link = ""
                    var description = ""
                    var date = ""
                    var image: String? = null
                    var inside = true

                    while (inside) {
                        event = parser.next()
                        when (event) {
                            XmlPullParser.START_TAG -> when (parser.name.lowercase()) {
                                "title" -> title = parser.nextText().trim()
                                "link" -> link = parser.nextText().trim()
                                "description" -> description = parser.nextText().trim()
                                "pubdate" -> date = parser.nextText().trim()
                                "enclosure" -> image = parser.getAttributeValue(null, "url")
                                "content" -> image = parser.getAttributeValue(
                                    "http://search.yahoo.com/mrss/", "url"
                                ) ?: image
                            }
                            XmlPullParser.END_TAG -> if (parser.name.equals("item", true)) inside = false
                            XmlPullParser.END_DOCUMENT -> inside = false
                        }
                    }

                    if (title.isNotBlank() && link.startsWith("https://")) {
                        val cleanSummary = Html.fromHtml(
                            description, Html.FROM_HTML_MODE_COMPACT
                        ).toString().replace(Regex("\\s+"), " ").trim()
                        results.add(
                            FootballNewsItem(
                                title = Html.fromHtml(title, Html.FROM_HTML_MODE_COMPACT).toString(),
                                link = link,
                                summary = cleanSummary,
                                publishedAt = date,
                                imageUrl = image
                            )
                        )
                    }
                } else {
                    event = parser.next()
                }
            }
            results
        } finally {
            connection.disconnect()
        }
    }
}

@Composable
fun FootballNewsPage(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    var news by remember { mutableStateOf<List<FootballNewsItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh) {
        loading = true
        error = null
        try {
            news = withTimeout(22000L) { FootballNewsRepository.load() }
            if (news.isEmpty()) error = "Belum ada berita yang tersedia."
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Gagal memuat berita sepak bola."
        } finally {
            loading = false
        }
    }

    Column(modifier = modifier.fillMaxSize().background(ZoraThemeColors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ZORA SPORTS / NEWSROOM", color = ZoraThemeColors.accent,
                    fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                Text("Football News", color = ZoraThemeColors.text,
                    fontSize = 23.sp, fontWeight = FontWeight.Black)
                Text("Berita sepak bola dari BBC Sport", color = ZoraThemeColors.muted, fontSize = 11.sp)
            }
            IconButton(onClick = { refresh++ }, enabled = !loading) {
                Icon(Icons.Default.Refresh, contentDescription = "Muat ulang berita",
                    tint = ZoraThemeColors.accent)
            }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = ZoraThemeColors.accent)
                    Text("Mengambil berita terbaru...", color = ZoraThemeColors.muted, fontSize = 12.sp)
                }
            }
            error != null -> Box(Modifier.fillMaxSize().padding(22.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Newspaper, null, tint = ZoraThemeColors.muted,
                        modifier = Modifier.size(35.dp))
                    Text(error.orEmpty(), color = ZoraThemeColors.text, fontSize = 13.sp)
                    TextButton(onClick = { refresh++ }) {
                        Text("COBA LAGI", color = ZoraThemeColors.accent)
                    }
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                item {
                    Text(news.size.toString() + " ARTIKEL TERBARU",
                        color = ZoraThemeColors.muted, fontSize = 9.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                }
                items(news) { item ->
                    Surface(
                        onClick = { runCatching { uriHandler.openUri(item.link) } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = ZoraThemeColors.panel,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ZoraThemeColors.border)
                    ) {
                        Column {
                            if (!item.imageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxWidth().height(178.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text("BBC SPORT  •  FOOTBALL", color = ZoraThemeColors.accent,
                                    fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text(item.title, color = ZoraThemeColors.text,
                                    fontSize = 15.sp, lineHeight = 20.sp,
                                    fontWeight = FontWeight.Bold, maxLines = 3,
                                    overflow = TextOverflow.Ellipsis)
                                if (item.summary.isNotBlank()) {
                                    Text(item.summary, color = ZoraThemeColors.muted,
                                        fontSize = 11.sp, lineHeight = 16.sp,
                                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.publishedAt, color = ZoraThemeColors.muted,
                                        fontSize = 9.sp, maxLines = 1, modifier = Modifier.weight(1f),
                                        overflow = TextOverflow.Ellipsis)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("BACA BERITA", color = ZoraThemeColors.accent,
                                            fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(5.dp))
                                        Icon(Icons.Default.OpenInNew, null, tint = ZoraThemeColors.accent,
                                            modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
