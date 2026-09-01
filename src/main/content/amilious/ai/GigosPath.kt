package content.amilious.ai

import core.game.global.action.DoorActionHandler
import core.game.node.entity.npc.NPC
import core.game.node.scenery.Scenery
import core.game.world.map.Location
import core.game.world.map.Point
import core.game.world.map.RegionManager
import core.game.world.map.path.Path
import core.game.world.map.path.Pathfinder

object GigosPath {

    private const val GATE_SCAN = 8

    data class Route(val path: Path, val end: Location) {
        fun exact(dest: Location) = end.getDistance(dest) <= 1.0
    }

    fun arrived(actor: NPC, dest: Location, dist: Double = 1.5): Boolean =
        actor.location.getDistance(dest) <= dist

    /** Only clear the walk queue. Do not clear pulses — that kills the companion action. */
    fun stop(actor: NPC) {
        actor.walkingQueue.reset()
    }

    fun route(actor: NPC, dest: Location): Route? {
        val path = Pathfinder.find(actor, dest, true, Pathfinder.SMART)
        val last = lastPoint(path) ?: return null
        return Route(path, Location.create(last.x, last.y, actor.location.z))
    }

    fun canReachExact(actor: NPC, dest: Location): Boolean {
        val r = route(actor, dest) ?: return false
        return r.path.isSuccessful && r.exact(dest)
    }

    fun canReach(actor: NPC, dest: Location): Boolean {
        if (canReachExact(actor, dest)) return true
        val path = Pathfinder.find(actor, dest, true, Pathfinder.SMART)
        if (path.isSuccessful || path.isMoveNear) return true
        return findClosedGate(actor, dest) != null
    }

    /** Call once when an action takes over from follow. */
    fun takeOver(actor: NPC) {
        actor.pulseManager.clear()
        actor.walkingQueue.reset()
    }

    fun stopPath(actor: NPC) {
        actor.walkingQueue.reset()
    }

    fun walk(actor: NPC, dest: Location, dist: Double = 1.5): Boolean {
        if (arrived(actor, dest, dist)) return true

        val path = Pathfinder.find(actor, dest, true, Pathfinder.SMART)
        val exact = path.isSuccessful && lastPoint(path)?.let {
            Location.create(it.x, it.y, actor.location.z).getDistance(dest) <= dist
        } == true

        if (exact) {
            actor.walkingQueue.reset()
            path.walk(actor)
            return true
        }

        val gate = findClosedGate(actor, dest)
        if (gate != null) {
            if (arrived(actor, gate.location, 1.5)) {
                return openGate(actor, gate)
            }
            val toGate = Pathfinder.find(actor, gate.location, true, Pathfinder.SMART)
            if (toGate.isSuccessful || toGate.isMoveNear) {
                actor.walkingQueue.reset()
                toGate.walk(actor)
                return true
            }
        }

        if (path.isSuccessful || path.isMoveNear) {
            actor.walkingQueue.reset()
            path.walk(actor)
            return true
        }
        return false
    }

    fun stuck(walkTicks: Int, limit: Int = 24): Boolean = walkTicks > limit

    fun findClosedGate(actor: NPC, dest: Location): Scenery? {
        var best: Scenery? = null
        var bestScore = Double.MAX_VALUE
        val z = actor.location.z
        val ax = actor.location.x
        val ay = actor.location.y
        for (dx in -GATE_SCAN..GATE_SCAN) {
            for (dy in -GATE_SCAN..GATE_SCAN) {
                val loc = Location.create(ax + dx, ay + dy, z)
                val obj = RegionManager.getObject(loc) ?: continue
                if (!isOpenableGate(obj)) continue
                val score = obj.location.getDistance(actor.location) +
                        obj.location.getDistance(dest) * 0.25
                if (score < bestScore) {
                    bestScore = score
                    best = obj
                }
            }
        }
        return best
    }

    private fun lastPoint(path: Path): Point? {
        val pts = path.points ?: return null
        if (pts.isEmpty()) return null
        return try {
            pts.last()
        } catch (_: Exception) {
            var last: Point? = null
            for (p in pts) last = p
            last
        }
    }

    private fun isOpenableGate(obj: Scenery): Boolean {
        val name = obj.name.lowercase()
        if (name.contains("locked")) return false
        val named = name.contains("gate") || name.contains("door") || name.contains("fence")
        val opts = obj.definition.options ?: return false
        val canOpen = opts.any { it != null && it.equals("open", ignoreCase = true) }
        return named && canOpen
    }

    private fun openGate(actor: NPC, gate: Scenery): Boolean {
        return try {
            DoorActionHandler.handleAutowalkDoor(actor, gate)
            true
        } catch (_: Exception) {
            false
        }
    }

}