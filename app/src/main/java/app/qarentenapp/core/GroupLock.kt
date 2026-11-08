package app.qarentenapp.core

import app.qarentenapp.data.Group

/**
 * Protección por grupo: qué grupos están desbloqueados ahora (solo en memoria).
 * lockMode 1 = todo el grupo (una sola autenticación al entrar); 2 = solo las acciones de «todas» y los toques de un paso.
 */
object GroupLock {
    private val until = HashMap<String, Long>()

    private fun minutes() = Repo.config.value.settings.unlockMinutes

    @Synchronized fun unlocked(id: String): Boolean {
        val t = until[id] ?: return false
        if (System.currentTimeMillis() < t) return true
        until.remove(id); return false
    }

    /** Entraste al grupo (autenticado): queda abierto hasta que salgas, o por el tiempo elegido. */
    @Synchronized fun enter(id: String) {
        val m = minutes()
        until[id] = if (m > 0) System.currentTimeMillis() + m * 60_000L else Long.MAX_VALUE
    }

    /** Saliste del grupo: si no hay tiempo de gracia, se vuelve a bloquear. */
    @Synchronized fun leave(id: String) {
        if (minutes() == 0) until.remove(id)
    }

    /** Una acción puntual autenticada: solo deja abierto si hay tiempo de gracia configurado. */
    @Synchronized fun grant(id: String) {
        val m = minutes()
        if (m > 0) until[id] = System.currentTimeMillis() + m * 60_000L
    }

    @Synchronized fun lockAll() = until.clear()

    private fun groups() = Repo.config.value.groups

    /** Grupos «todo el grupo» que contienen la app y siguen bloqueados. */
    fun blockingForApp(pkg: String): List<Group> =
        groups().filter { it.lockMode == 1 && pkg in it.packages && !unlocked(it.id) }

    fun needsForApps(pkgs: Collection<String>): Boolean = pkgs.any { blockingForApp(it).isNotEmpty() }

    /** Acciones sobre el grupo entero (todas / un toque) piden autenticación en cualquier modo protegido. */
    fun needsForGroup(g: Group): Boolean = g.lockMode != 0 && !unlocked(g.id)

    fun needsEntry(g: Group): Boolean = g.lockMode == 1 && !unlocked(g.id)

    /** Apps que un «congelar todo» general no debe tocar (están en grupos protegidos bloqueados). */
    fun blockedApps(pkgs: Collection<String>): Set<String> = pkgs.filter { blockingForApp(it).isNotEmpty() }.toSet()
}
