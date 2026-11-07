package app.qarentenapp.core

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import app.qarentenapp.data.AppEntry

/** Un grupo propuesto por «Auto agrupar». */
data class Proposal(val name: String, val icon: String, val color: Int, val packages: List<String>)

/**
 * «Auto agrupar», sin internet. Para cada app (de usuario y con ícono) busca el rubro en este orden:
 *  1. Una lista de apps conocidas incluida en Q'App.
 *  2. Palabras clave en el nombre y el paquete.
 *  3. La categoría que declara la propia app en Android.
 * Las que no encajan quedan afuera, para agregarlas a mano.
 */
object AutoGroup {

    private data class Cat(val name: String, val icon: String, val color: Int, val keywords: List<String>)

    private val cats = linkedMapOf(
        "bank" to Cat("Bancos y billeteras", "account_balance", 3, listOf("bank", "banco", "banca", "wallet", "billetera", "pay", "pago", "cuenta", "finanz", "credit", "tarjeta", "homebanking", "broker", "invest", "crypto", "cripto", "binance", "dolar")),
        "shopping" to Cat("Compras", "shopping_cart", 0, listOf("shop", "store", "tienda", "compra", "mercado", "market", "sale", "oferta", "outlet", "ecommerce")),
        "food" to Cat("Comida y delivery", "restaurant", 1, listOf("food", "comida", "delivery", "pedido", "eats", "rappi", "pedidosya", "burger", "pizza", "restaurant")),
        "social" to Cat("Sociales", "groups", 5, listOf("social", "facebook", "instagram", "twitter", "tiktok", "threads", "reddit", "snapchat", "pinterest", "linkedin")),
        "chat" to Cat("Mensajería", "chat", 0, listOf("whatsapp", "telegram", "messenger", "messag", "chat", "signal", "discord", "slack", "sms")),
        "games" to Cat("Juegos", "sports_esports", 2, listOf("game", "games", "juego", "play", "puzzle", "racing", "chess", "clash", "candy")),
        "music" to Cat("Música y audio", "headphones", 1, listOf("music", "musica", "spotify", "audio", "radio", "podcast", "sound", "mp3", "player")),
        "video" to Cat("Video y TV", "movie", 5, listOf("video", "tv", "netflix", "youtube", "disney", "prime", "hbo", "max", "stream", "flow", "star", "paramount", "twitch")),
        "photo" to Cat("Fotos e imágenes", "photo_camera", 4, listOf("photo", "foto", "camera", "camara", "gallery", "galeria", "image", "imagen", "snap", "lightroom", "picsart", "nikon", "canon")),
        "maps" to Cat("Mapas y viajes", "map", 6, listOf("map", "mapa", "gps", "navig", "waze", "travel", "viaje", "booking", "airbnb", "flight", "vuelo", "hotel", "trip")),
        "transport" to Cat("Transporte", "directions_car", 7, listOf("uber", "cabify", "didi", "taxi", "remis", "bus", "colectivo", "tren", "subte", "sube", "transit", "moovit", "parking", "estacionamiento")),
        "health" to Cat("Salud y deporte", "fitness_center", 6, listOf("fit", "gym", "health", "salud", "workout", "run", "strava", "yoga", "medit", "sleep", "step", "pasos", "doctor", "medic", "farmacia")),
        "work" to Cat("Oficina y trabajo", "work", 7, listOf("office", "oficina", "word", "excel", "docs", "sheets", "slides", "drive", "notion", "trello", "calendar", "mail", "outlook", "gmail", "zoom", "meet", "teams", "pdf", "scanner")),
        "ai" to Cat("Inteligencia artificial", "smart_toy", 3, listOf("chatgpt", "openai", "claude", "anthropic", "gemini", "copilot", "deepseek", "grok", "perplexity", "meta ai", "qwen", "mistral", "poe")),
        "news" to Cat("Noticias y lectura", "newspaper", 4, listOf("news", "noticia", "diario", "clarin", "infobae", "nacion", "kindle", "book", "libro", "read", "lector")),
        "edu" to Cat("Educación", "school", 2, listOf("learn", "duolingo", "school", "escuela", "edu", "curso", "course", "classroom", "academy", "aprend")),
        "tools" to Cat("Herramientas", "build", 7, listOf("tool", "util", "file", "archivo", "manager", "cleaner", "vpn", "launcher", "keyboard", "teclado", "flashlight", "linterna", "calculator", "calculadora", "clock", "reloj", "weather", "clima", "backup", "root", "shizuku", "magisk")),
    )

    /** Apps conocidas → rubro (paquete exacto o prefijo). */
    private val known = mapOf(
        // Bancos y billeteras (Argentina y generales)
        "com.mercadopago.wallet" to "bank", "ar.com.bbva.net" to "bank", "com.bancogalicia" to "bank", "com.santander" to "bank",
        "ar.com.santander" to "bank", "com.brubank" to "bank", "com.uala" to "bank", "ar.com.uala" to "bank", "com.naranjax" to "bank",
        "com.macro" to "bank", "ar.com.bancomacro" to "bank", "ar.com.hsbc" to "bank", "ar.com.icbc" to "bank", "com.icbc" to "bank",
        "ar.com.bancopatagonia" to "bank", "ar.bapro" to "bank", "ar.com.bna" to "bank", "ar.gob.bna" to "bank", "com.bnacion" to "bank",
        "ar.com.supervielle" to "bank", "com.personalpay" to "bank", "com.lemoncash" to "bank", "com.belo" to "bank", "com.binance" to "bank",
        "com.paypal" to "bank", "com.google.android.apps.walletnfcrel" to "bank", "com.revolut" to "bank", "com.wise" to "bank",
        "ar.com.modo" to "bank", "com.modo" to "bank", "com.prex" to "bank", "ar.com.bancociudad" to "bank", "com.bancocorrientes" to "bank",
        // Compras
        "com.mercadolibre" to "shopping", "com.amazon.mShop" to "shopping", "com.einnovation.temu" to "shopping", "com.zzkko" to "shopping",
        "com.alibaba.aliexpresshd" to "shopping", "com.ebay" to "shopping", "com.fravega" to "shopping", "com.garbarino" to "shopping",
        "com.shein" to "shopping", "com.tiendanube" to "shopping", "com.carrefour" to "shopping", "com.coto" to "shopping",
        // Comida
        "com.pedidosya" to "food", "com.grability.rappi" to "food", "com.ubercab.eats" to "food", "com.mcdonalds" to "food",
        // Sociales
        "com.facebook.katana" to "social", "com.instagram.android" to "social", "com.twitter.android" to "social", "com.zhiliaoapp.musically" to "social",
        "com.ss.android.ugc.trill" to "social", "com.instagram.barcelona" to "social", "com.reddit.frontpage" to "social", "com.snapchat.android" to "social",
        "com.pinterest" to "social", "com.linkedin.android" to "social", "com.bereal" to "social", "com.facebook.lite" to "social",
        // Mensajería
        "com.whatsapp" to "chat", "com.whatsapp.w4b" to "chat", "org.telegram" to "chat", "com.facebook.orca" to "chat", "org.thoughtcrime.securesms" to "chat",
        "com.discord" to "chat", "com.Slack" to "chat", "com.google.android.apps.messaging" to "chat", "jp.naver.line" to "chat", "com.viber" to "chat",
        // Música
        "com.spotify.music" to "music", "com.google.android.apps.youtube.music" to "music", "deezer.android" to "music", "com.shazam" to "music",
        "com.soundcloud" to "music", "com.amazon.mp3" to "music", "com.apple.android.music" to "music",
        // Video
        "com.netflix.mediaclient" to "video", "com.google.android.youtube" to "video", "com.disney" to "video", "com.amazon.avod" to "video",
        "com.wbd.stream" to "video", "com.hbo" to "video", "com.twitch" to "video", "tv.twitch" to "video", "com.paramount" to "video",
        "ar.com.flow" to "video", "com.mubi" to "video", "com.plexapp" to "video", "org.videolan.vlc" to "video", "com.stremio" to "video",
        // Fotos
        "com.google.android.apps.photos" to "photo", "com.adobe.lrmobile" to "photo", "com.adobe.psmobile" to "photo", "com.picsart" to "photo",
        "com.niksoftware.snapseed" to "photo", "com.vsco" to "photo", "com.canva" to "photo", "com.nikon" to "photo", "com.canon" to "photo",
        "com.lemon.lvoverseas" to "photo", "com.adobe" to "photo",
        // Mapas y viajes
        "com.google.android.apps.maps" to "maps", "com.waze" to "maps", "com.booking" to "maps", "com.airbnb.android" to "maps",
        "com.tripadvisor" to "maps", "com.despegar" to "maps", "com.aerolineas" to "maps", "com.flybondi" to "maps",
        // Transporte
        "com.ubercab" to "transport", "com.cabify" to "transport", "com.didiglobal" to "transport", "com.tranzmate" to "transport",
        "ar.gob.sube" to "transport", "com.sube" to "transport",
        // Salud y deporte
        "com.strava" to "health", "com.google.android.apps.fitness" to "health", "com.sec.android.app.shealth" to "health",
        "com.mi.health" to "health", "com.xiaomi.wearable" to "health", "com.huami" to "health", "com.nike" to "health",
        "com.myfitnesspal" to "health", "com.calm" to "health", "com.headspace" to "health", "com.samsung.android.wear" to "health",
        // Oficina
        "com.microsoft.office" to "work", "com.microsoft.teams" to "work", "com.microsoft.office.outlook" to "work",
        "com.google.android.gm" to "work", "com.google.android.apps.docs" to "work", "com.google.android.calendar" to "work",
        "notion.id" to "work", "com.trello" to "work", "us.zoom.videomeetings" to "work", "com.google.android.apps.meetings" to "work",
        "com.adobe.reader" to "work", "com.dropbox.android" to "work", "com.microsoft.skydrive" to "work", "com.evernote" to "work",
        "com.google.android.keep" to "work", "com.todoist" to "work", "com.anydesk" to "work", "com.teamviewer" to "work",
        // IA
        "com.openai.chatgpt" to "ai", "com.anthropic.claude" to "ai", "com.google.android.apps.bard" to "ai", "com.microsoft.copilot" to "ai",
        "com.deepseek.chat" to "ai", "ai.x.grok" to "ai", "ai.perplexity.app.android" to "ai", "com.facebook.stella" to "ai",
        "com.quora.poe" to "ai", "ai.chat.gpt" to "ai", "com.aliyun.tongyi" to "ai", "ai.mistral" to "ai",
        // Noticias y lectura
        "com.google.android.apps.magazines" to "news", "com.amazon.kindle" to "news", "com.clarin" to "news", "com.infobae" to "news",
        "com.lanacion" to "news", "com.google.android.apps.books" to "news",
        // Educación
        "com.duolingo" to "edu", "com.google.android.apps.classroom" to "edu", "com.coursera" to "edu", "org.coursera" to "edu", "com.udemy" to "edu",
        // Herramientas
        "com.topjohnwu.magisk" to "tools", "moe.shizuku.privileged.api" to "tools", "com.google.android.apps.nbu.files" to "tools",
        "com.mixplorer" to "tools", "com.ghisler" to "tools", "com.cloudflare.onedotonedotonedotone" to "tools", "com.nordvpn" to "tools",
        "com.teslacoilsw.launcher" to "tools", "com.google.android.inputmethod.latin" to "tools", "com.joaomgcd" to "tools",
        "net.dinglisch.android.taskerm" to "tools",
    )

    private fun norm(s: String) = java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

    private fun knownCat(pkg: String): String? =
        known[pkg] ?: known.entries.firstOrNull { pkg.startsWith(it.key + ".") || pkg.startsWith(it.key) && it.key.count { c -> c == '.' } >= 2 }?.value

    private fun keywordCat(label: String, pkg: String): String? {
        val text = norm("$label ${pkg.replace('.', ' ')}")
        val words = text.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
        var best: String? = null; var score = 0
        for ((key, c) in cats) {
            val s = c.keywords.sumOf { k ->
                when {
                    k.contains(' ') -> if (text.contains(k)) 2 else 0
                    words.any { it == k } -> 2
                    k.length >= 4 && words.any { it.startsWith(k) } -> 1
                    else -> 0
                }.toInt()
            }
            if (s > score) { score = s; best = key }
        }
        return best
    }

    private fun androidCat(pm: PackageManager, pkg: String): String? {
        if (Build.VERSION.SDK_INT < 26) return null
        val ai = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull() ?: return null
        return when (ai.category) {
            ApplicationInfo.CATEGORY_GAME -> "games"
            ApplicationInfo.CATEGORY_AUDIO -> "music"
            ApplicationInfo.CATEGORY_VIDEO -> "video"
            ApplicationInfo.CATEGORY_IMAGE -> "photo"
            ApplicationInfo.CATEGORY_SOCIAL -> "social"
            ApplicationInfo.CATEGORY_NEWS -> "news"
            ApplicationInfo.CATEGORY_MAPS -> "maps"
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> "work"
            else -> null
        }
    }

    /** Arma la propuesta. Solo apps de usuario con ícono, que no estén ya en algún grupo. */
    fun propose(apps: List<AppEntry>, alreadyGrouped: Set<String>): List<Proposal> {
        val pm = Repo.ctx.packageManager
        val buckets = linkedMapOf<String, MutableList<String>>()
        apps.filter { !it.isSystem && it.hasLauncher && !it.protected && it.pkg !in alreadyGrouped && it.pkg != Repo.ctx.packageName }
            .forEach { a ->
                val c = knownCat(a.pkg) ?: keywordCat(a.label, a.pkg) ?: androidCat(pm, a.pkg)
                if (c != null) buckets.getOrPut(c) { mutableListOf() } += a.pkg
            }
        return cats.mapNotNull { (key, c) ->
            val list = buckets[key] ?: return@mapNotNull null
            Proposal(c.name, c.icon, c.color, list.sortedBy { Repo.labelOf(it).lowercase() })
        }
    }
}
