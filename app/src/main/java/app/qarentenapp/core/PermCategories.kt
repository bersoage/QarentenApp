package app.qarentenapp.core

/** Categorías de permisos para la ficha de cada app y el filtro por permisos. */
enum class PermCategory(val label: String, val perms: Set<String>) {
    NETWORK("Acceso a internet", setOf(
        "android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE", "android.permission.ACCESS_WIFI_STATE",
        "android.permission.CHANGE_NETWORK_STATE", "android.permission.CHANGE_WIFI_STATE",
    )),
    LOCATION("Ubicación", setOf(
        "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
    )),
    MESSAGES("Enviar o leer mensajes", setOf(
        "android.permission.SEND_SMS", "android.permission.READ_SMS", "android.permission.RECEIVE_SMS",
        "android.permission.RECEIVE_MMS", "android.permission.RECEIVE_WAP_PUSH",
    )),
    PHONE_CALLS("Llamadas", setOf(
        "android.permission.CALL_PHONE", "android.permission.READ_PHONE_STATE", "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG", "android.permission.ANSWER_PHONE_CALLS", "android.permission.READ_PHONE_NUMBERS",
    )),
    COST_MONEY("Servicios que cuestan dinero", setOf(
        "android.permission.SEND_SMS", "android.permission.CALL_PHONE", "com.android.vending.BILLING",
    )),
    PERSONAL_INFO("Datos personales", setOf(
        "android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS", "android.permission.READ_CALENDAR",
        "android.permission.WRITE_CALENDAR", "android.permission.BODY_SENSORS", "android.permission.ACTIVITY_RECOGNITION",
    )),
    ACCOUNTS("Cuentas de usuario", setOf(
        "android.permission.GET_ACCOUNTS", "android.permission.MANAGE_ACCOUNTS", "android.permission.USE_CREDENTIALS",
        "android.permission.AUTHENTICATE_ACCOUNTS",
    )),
    STORAGE("Almacenamiento y archivos", setOf(
        "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.MANAGE_EXTERNAL_STORAGE", "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO", "android.permission.READ_MEDIA_AUDIO",
    )),
    HARDWARE_CONTROLS("Cámara, micrófono y hardware", setOf(
        "android.permission.CAMERA", "android.permission.RECORD_AUDIO", "android.permission.FLASHLIGHT",
        "android.permission.BLUETOOTH", "android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN",
        "android.permission.NFC", "android.permission.VIBRATE",
    )),
    SYSTEM_TOOLS("Herramientas del sistema", setOf(
        "android.permission.SYSTEM_ALERT_WINDOW", "android.permission.RECEIVE_BOOT_COMPLETED", "android.permission.WRITE_SETTINGS",
        "android.permission.REQUEST_INSTALL_PACKAGES", "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.BIND_ACCESSIBILITY_SERVICE", "android.permission.BIND_DEVICE_ADMIN",
        "android.permission.QUERY_ALL_PACKAGES", "android.permission.FOREGROUND_SERVICE",
    )),
    DEVELOPMENT_TOOLS("Herramientas de desarrollo", setOf(
        "android.permission.SET_DEBUG_APP", "android.permission.DUMP", "android.permission.READ_LOGS",
        "android.permission.WRITE_SECURE_SETTINGS",
    ));

    companion object {
        fun of(perms: Collection<String>): List<PermCategory> = entries.filter { c -> perms.any { it in c.perms } }
    }
}
