package content.amilious.ai

import core.game.global.action.DoorActionHandler
import core.game.node.entity.npc.NPC
import core.game.node.scenery.Scenery
import core.game.world.map.Location
import core.game.world.map.Point
import core.game.world.map.RegionManager
import core.game.world.map.path.Path
import core.game.world.map.path.Pathfinder

class CompanionPath {

    private val GATE_SCAN = 8
    private var lastX = Int.MIN_VALUE
    private var lastY = Int.MIN_VALUE
    private var stillTicks = 0

    data class Route(val path: Path, val end: Location) {
        fun exact(dest: Location) = end.getDistance(dest) <= 1.0
    }

    fun arrived(actor: NPC, dest: Location, dist: Double = 1.5): Boolean =
        actor.location.getDistance(dest) <= dist

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
        return findGate(actor, dest) != null
    }

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

        val gate = findGate(actor, dest)
        if (gate != null) {
            val through = throughTile(actor, gate, dest)
            if (isClosed(gate)) {
                if (arrived(actor, gate.location, 1.5)) {
                    if (!openGate(actor, gate)) return false
                    return walkTo(actor, through)
                }
                return walkTo(actor, gate.location)
            }
            // already open — never toggle it, just go through
            return walkTo(actor, through)
        }

        if (path.isSuccessful || path.isMoveNear) {
            actor.walkingQueue.reset()
            path.walk(actor)
            return true
        }
        return false
    }

    fun stuck(walkTicks: Int, limit: Int = 24): Boolean = walkTicks > limit

    fun noteMove(actor: NPC) {
        val x = actor.location.x
        val y = actor.location.y
        if (x == lastX && y == lastY) stillTicks++ else stillTicks = 0
        lastX = x
        lastY = y
    }

    /** Not moving, no pulse, not at dest. Do not retry the same tile. */
    fun reallyStuck(actor: NPC, dest: Location, dist: Double = 1.5): Boolean {
        noteMove(actor)
        if (actor.walkingQueue.isMoving) return false
        if (actor.pulseManager.hasPulseRunning()) return false
        if (stillTicks < 6) return false
        return !arrived(actor, dest, dist)
    }

    fun findGate(actor: NPC, dest: Location): Scenery? {
        var best: Scenery? = null
        var bestScore = Double.MAX_VALUE
        val z = actor.location.z
        val ax = actor.location.x
        val ay = actor.location.y
        for (dx in -GATE_SCAN..GATE_SCAN) {
            for (dy in -GATE_SCAN..GATE_SCAN) {
                val loc = Location.create(ax + dx, ay + dy, z)
                val obj = RegionManager.getObject(loc) ?: continue
                if (!isGate(obj)) continue
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

    private fun walkTo(actor: NPC, dest: Location): Boolean {
        val p = Pathfinder.find(actor, dest, true, Pathfinder.SMART)
        if (!p.isSuccessful && !p.isMoveNear) return false
        actor.walkingQueue.reset()
        p.walk(actor)
        return true
    }

    fun resetStuck() {
        stillTicks = 0
        lastX = Int.MIN_VALUE
        lastY = Int.MIN_VALUE
    }

    /** One tile on the dest side of the gate, not the gate tile itself. */
    private fun throughTile(actor: NPC, gate: Scenery, dest: Location): Location {
        val gx = gate.location.x
        val gy = gate.location.y
        val gz = gate.location.z
        val dx = when {
            dest.x > gx -> 1
            dest.x < gx -> -1
            else -> 0
        }
        val dy = when {
            dest.y > gy -> 1
            dest.y < gy -> -1
            else -> 0
        }
        if (dx == 0 && dy == 0) {
            val ax = if (actor.location.x <= gx) 1 else -1
            return Location.create(gx + ax, gy, gz)
        }
        return Location.create(gx + dx, gy + dy, gz)
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

    private fun isGate(obj: Scenery): Boolean {
        val name = obj.name.lowercase()
        if (name.contains("locked")) return false
        val named = name.contains("gate") || name.contains("door") || name.contains("fence")
        val opts = obj.definition.options ?: return false
        val canToggle = opts.any {
            it != null && (it.equals("open", true) || it.equals("close", true))
        }
        return named && canToggle
    }

    private fun isClosed(obj: Scenery): Boolean {
        val opts = obj.definition.options ?: return false
        return opts.any { it != null && it.equals("open", true) }
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