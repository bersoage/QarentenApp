package app.qarentenapp.ui

/** Ícono de grupo: un glifo de la fuente Material Icons (incluida en la app). */
data class IconDef(val key: String, val label: String, val category: String, val glyph: String, val keywords: String)

/** Catálogo de íconos para los grupos (110), por categoría. Generado a partir de la fuente. */
val IconCatalog: List<IconDef> = listOf(
    IconDef("work", "Maletín", "Trabajo y estudio", "\ue8f9", "trabajo oficina laburo"),
    IconDef("business_center", "Oficina", "Trabajo y estudio", "\ueb3f", "oficina negocio empresa"),
    IconDef("computer", "Computadora", "Trabajo y estudio", "\ue30a", "pc compu notebook"),
    IconDef("description", "Documento", "Trabajo y estudio", "\ue873", "documento archivo"),
    IconDef("school", "Escuela", "Trabajo y estudio", "\ue80c", "escuela estudio facultad educacion curso"),
    IconDef("menu_book", "Libro", "Trabajo y estudio", "\uea19", "libro lectura"),
    IconDef("calculate", "Calculadora", "Trabajo y estudio", "\uea5f", "calculadora cuentas"),
    IconDef("event", "Calendario", "Trabajo y estudio", "\ue878", "calendario agenda"),
    IconDef("code", "Código", "Trabajo y estudio", "\ue86f", "programacion desarrollo dev codigo"),
    IconDef("science", "Ciencia", "Trabajo y estudio", "\uea4b", "ciencia laboratorio"),
    IconDef("translate", "Idiomas", "Trabajo y estudio", "\ue8e2", "idioma traductor ingles"),
    IconDef("chat", "Chat", "Comunicación", "\ue0b7", "chat mensajes mensajeria"),
    IconDef("forum", "Foro", "Comunicación", "\ue0bf", "foro comunidad"),
    IconDef("email", "Correo", "Comunicación", "\ue0be", "correo mail email"),
    IconDef("phone", "Teléfono", "Comunicación", "\ue0cd", "telefono llamadas"),
    IconDef("groups", "Personas", "Comunicación", "\uf233", "sociales social redes amigos personas"),
    IconDef("video_call", "Videollamada", "Comunicación", "\ue070", "videollamada reunion meet zoom"),
    IconDef("sms", "SMS", "Comunicación", "\ue625", "sms mensajes texto"),
    IconDef("photo_camera", "Cámara", "Multimedia", "\ue412", "camara fotos nikon canon"),
    IconDef("image", "Imagen", "Multimedia", "\ue3f4", "imagenes imagen fotos galeria"),
    IconDef("photo_library", "Galería", "Multimedia", "\ue413", "galeria album"),
    IconDef("movie", "Película", "Multimedia", "\ue02c", "video videos peliculas cine streaming"),
    IconDef("videocam", "Filmadora", "Multimedia", "\ue04b", "filmar grabar"),
    IconDef("music_note", "Música", "Multimedia", "\ue405", "musica canciones"),
    IconDef("headphones", "Auriculares", "Multimedia", "\uf01f", "audio auriculares sonido"),
    IconDef("mic", "Micrófono", "Multimedia", "\ue029", "microfono grabadora voz"),
    IconDef("podcasts", "Podcast", "Multimedia", "\uf048", "podcast"),
    IconDef("live_tv", "TV", "Multimedia", "\ue639", "tv television series"),
    IconDef("radio", "Radio", "Multimedia", "\ue03e", "radio"),
    IconDef("shopping_cart", "Carrito", "Compras y dinero", "\ue8cc", "shopping compras tienda carrito"),
    IconDef("shopping_bag", "Bolsa", "Compras y dinero", "\uf1cc", "bolsa ropa moda"),
    IconDef("local_grocery_store", "Súper", "Compras y dinero", "\ue547", "super supermercado almacen"),
    IconDef("storefront", "Tienda", "Compras y dinero", "\uea12", "tienda negocio local"),
    IconDef("account_balance", "Banco", "Compras y dinero", "\ue84f", "banco bancos finanzas"),
    IconDef("credit_card", "Tarjeta", "Compras y dinero", "\ue870", "tarjeta credito"),
    IconDef("wallet", "Billetera", "Compras y dinero", "\uf8ff", "billetera wallet"),
    IconDef("payments", "Pagos", "Compras y dinero", "\uef63", "pagos dinero plata"),
    IconDef("savings", "Alcancía", "Compras y dinero", "\ue2eb", "ahorro alcancia inversiones"),
    IconDef("currency_exchange", "Cambio", "Compras y dinero", "\ueb70", "cambio dolar cripto"),
    IconDef("receipt", "Ticket", "Compras y dinero", "\ue8b0", "ticket factura recibo"),
    IconDef("local_offer", "Oferta", "Compras y dinero", "\ue54e", "oferta descuento cupon"),
    IconDef("sell", "Venta", "Compras y dinero", "\uf05b", "venta vender"),
    IconDef("map", "Mapa", "Viajes y movilidad", "\ue55b", "gps mapa mapas ubicacion"),
    IconDef("navigation", "Navegación", "Viajes y movilidad", "\ue55d", "navegacion gps"),
    IconDef("flight", "Avión", "Viajes y movilidad", "\ue539", "viajes avion vuelo"),
    IconDef("directions_car", "Auto", "Viajes y movilidad", "\ue531", "auto coche"),
    IconDef("directions_bus", "Colectivo", "Viajes y movilidad", "\ue530", "colectivo bus transporte"),
    IconDef("train", "Tren", "Viajes y movilidad", "\ue570", "tren subte"),
    IconDef("local_taxi", "Taxi", "Viajes y movilidad", "\ue559", "taxi remis uber"),
    IconDef("two_wheeler", "Moto", "Viajes y movilidad", "\ue9f9", "moto"),
    IconDef("local_gas_station", "Nafta", "Viajes y movilidad", "\ue546", "nafta combustible"),
    IconDef("hotel", "Hotel", "Viajes y movilidad", "\ue53a", "hotel alojamiento"),
    IconDef("luggage", "Valija", "Viajes y movilidad", "\uf235", "valija equipaje turismo"),
    IconDef("public", "Mundo", "Viajes y movilidad", "\ue80b", "internet web mundo navegador"),
    IconDef("fitness_center", "Pesas", "Salud y deporte", "\ueb43", "gym gimnasio pesas fitness"),
    IconDef("directions_run", "Correr", "Salud y deporte", "\ue566", "correr running deporte"),
    IconDef("hiking", "Trekking", "Salud y deporte", "\ue50a", "trekking caminar"),
    IconDef("pool", "Natación", "Salud y deporte", "\ueb48", "natacion pileta"),
    IconDef("sports_soccer", "Fútbol", "Salud y deporte", "\uea2f", "futbol"),
    IconDef("sports_basketball", "Básquet", "Salud y deporte", "\uea26", "basquet"),
    IconDef("sports_tennis", "Tenis", "Salud y deporte", "\uea32", "tenis padel"),
    IconDef("favorite", "Corazón", "Salud y deporte", "\ue87d", "salud corazon favoritos"),
    IconDef("local_hospital", "Hospital", "Salud y deporte", "\ue548", "hospital medico salud"),
    IconDef("medication", "Remedios", "Salud y deporte", "\uf033", "remedios farmacia"),
    IconDef("spa", "Spa", "Salud y deporte", "\ueb4c", "spa belleza"),
    IconDef("self_improvement", "Meditación", "Salud y deporte", "\uea78", "meditacion yoga bienestar"),
    IconDef("psychology", "Mente", "Salud y deporte", "\uea4a", "mente psicologia"),
    IconDef("home", "Casa", "Hogar y vida diaria", "\ue88a", "casa hogar domotica"),
    IconDef("lightbulb", "Lamparita", "Hogar y vida diaria", "\ue0f0", "ideas luz"),
    IconDef("kitchen", "Cocina", "Hogar y vida diaria", "\ueb47", "cocina"),
    IconDef("restaurant", "Restaurante", "Hogar y vida diaria", "\ue56c", "comida restaurante delivery"),
    IconDef("delivery_dining", "Delivery", "Hogar y vida diaria", "\uea72", "delivery pedidos"),
    IconDef("local_shipping", "Envíos", "Hogar y vida diaria", "\ue558", "envios paquetes correo"),
    IconDef("local_cafe", "Café", "Hogar y vida diaria", "\ue541", "cafe"),
    IconDef("pets", "Mascotas", "Hogar y vida diaria", "\ue91d", "mascotas perro gato"),
    IconDef("child_care", "Bebé", "Hogar y vida diaria", "\ueb41", "bebe chicos hijos"),
    IconDef("yard", "Jardín", "Hogar y vida diaria", "\uf089", "jardin plantas"),
    IconDef("apartment", "Edificio", "Hogar y vida diaria", "\uea40", "edificio consorcio"),
    IconDef("church", "Iglesia", "Hogar y vida diaria", "\ueaae", "iglesia religion"),
    IconDef("build", "Llave", "Herramientas y sistema", "\ue869", "herramientas utilidades"),
    IconDef("settings", "Engranaje", "Herramientas y sistema", "\ue8b8", "ajustes sistema configuracion"),
    IconDef("security", "Escudo", "Herramientas y sistema", "\ue32a", "seguridad antivirus vpn"),
    IconDef("lock", "Candado", "Herramientas y sistema", "\ue897", "privado bloqueo"),
    IconDef("storage", "Almacenamiento", "Herramientas y sistema", "\ue1db", "almacenamiento archivos"),
    IconDef("cloud", "Nube", "Herramientas y sistema", "\ue2bd", "nube drive backup"),
    IconDef("wifi", "Wifi", "Herramientas y sistema", "\ue63e", "wifi red"),
    IconDef("bluetooth", "Bluetooth", "Herramientas y sistema", "\ue1a7", "bluetooth"),
    IconDef("devices", "Dispositivos", "Herramientas y sistema", "\ue1b1", "dispositivos"),
    IconDef("cast", "Transmitir", "Herramientas y sistema", "\ue307", "transmitir chromecast"),
    IconDef("settings_remote", "Control remoto", "Herramientas y sistema", "\ue8c7", "remoto control"),
    IconDef("terminal", "Terminal", "Herramientas y sistema", "\ueb8e", "terminal consola root"),
    IconDef("sports_esports", "Joystick", "Ocio", "\uea28", "juegos gaming juego"),
    IconDef("casino", "Dados", "Ocio", "\ueb40", "casino dados apuestas"),
    IconDef("extension", "Rompecabezas", "Ocio", "\ue87b", "puzzle rompecabezas"),
    IconDef("theater_comedy", "Teatro", "Ocio", "\uea66", "teatro entretenimiento"),
    IconDef("auto_stories", "Cuentos", "Ocio", "\ue666", "cuentos lectura comics"),
    IconDef("newspaper", "Diario", "Ocio", "\ueb81", "noticias diario"),
    IconDef("brush", "Pincel", "Creatividad", "\ue3ae", "adobe edicion pincel arte"),
    IconDef("palette", "Paleta", "Creatividad", "\ue40a", "diseño colores arte"),
    IconDef("design_services", "Diseño", "Creatividad", "\uf10a", "diseño"),
    IconDef("draw", "Lápiz", "Creatividad", "\ue746", "dibujo dibujar"),
    IconDef("folder", "Carpeta", "Otros", "\ue2c7", "carpeta"),
    IconDef("star", "Estrella", "Otros", "\ue838", "favoritos estrella"),
    IconDef("push_pin", "Chinche", "Otros", "\uf10d", "permanente fijo"),
    IconDef("bolt", "Rayo", "Otros", "\uea0b", "rapido next energia"),
    IconDef("auto_awesome", "Brillos", "Otros", "\ue65f", "ia magia"),
    IconDef("smart_toy", "Robot", "Otros", "\uf06c", "ai ia inteligencia artificial robot chatbot"),
    IconDef("hearing", "Oído", "Otros", "\ue023", "sordo audicion oido"),
    IconDef("sign_language", "Lengua de señas", "Otros", "\uebe5", "sordo señas"),
    IconDef("ac_unit", "Copo de nieve", "Otros", "\ueb3b", "frio congelado"),
)

/** Claves de la beta anterior que cambiaron de nombre. */
private val legacy = mapOf("shopping" to "shopping_bag", "bank" to "account_balance", "camera" to "photo_camera", "music" to "music_note", "games" to "sports_esports", "travel" to "flight")

fun iconDef(key: String): IconDef {
    val k = legacy[key] ?: key
    return IconCatalog.firstOrNull { it.key == k } ?: IconCatalog.first { it.key == "folder" }
}

private fun norm(s: String) = java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD)
    .replace(Regex("\\p{M}"), "")

/** Sugiere un ícono según el nombre del grupo ("Juegos" → joystick). */
fun suggestIcon(name: String): String? {
    val words = norm(name).split(Regex("[^a-z0-9]+")).filter { it.length >= 2 }
    if (words.isEmpty()) return null
    var best: IconDef? = null
    var score = 0
    for (d in IconCatalog) {
        val kws = norm(d.keywords + " " + d.label).split(" ")
        val s = words.sumOf { w -> kws.count { k -> k == w || (w.length >= 4 && k.startsWith(w)) || (k.length >= 4 && w.startsWith(k)) } }
        if (s > score) { score = s; best = d }
    }
    return best?.key
}

fun searchIcons(q: String): List<IconDef> {
    val n = norm(q.trim())
    if (n.isEmpty()) return IconCatalog
    return IconCatalog.filter { norm(it.label + " " + it.keywords + " " + it.category).contains(n) }
}
