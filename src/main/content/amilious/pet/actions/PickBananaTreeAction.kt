package content.amilious.pet.actions

import content.amilious.ai.GigosPath
import content.amilious.ai.PhasedCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.playAudio
import core.api.sendMessage
import core.game.world.map.Location
import core.game.world.map.RegionManager
import kotlin.math.abs

class PickBananaTreeAction(rank: Int = 40) :
    PhasedCompanionAction<AmiliousMonkey, PickBananaTreeAction.Phase>(
        "pick-banana", rank, Phase::class
    ) {

    enum class Phase { WALK, PICK, HOLD }

    private var tree: Location? = null
    private var dest: Location? = null
    private var wait = 0
    private var walkTicks = 0
    private var picksOnThis = 0
    private val treeCd = HashMap<String, Int>()

    override fun cooldown(actor: AmiliousMonkey) {
        super.cooldown(actor)
        val it = treeCd.iterator()
        while (it.hasNext()) {
            val e = it.next()
            e.setValue(e.value - 1)
            if (e.value <= 0) it.remove()
        }
    }

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!ready()) return false
        if (!actor.pickEnabled()) return false
        if (actor.ownerIdleTicks < IDLE_NEED) return false
        if (actor.hunger() < MonkeyConfig.HUNGER_PICK) return false
        if (actor.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST) return false
        return nearestTree(actor) != null
    }

    override fun start(actor: AmiliousMonkey) {
        super.start(actor)
        tree = nearestTree(actor)
        dest = standTile(actor, tree)
        wait = 0
        walkTicks = 0
        picksOnThis = 0
        goToPhase(Phase.WALK)
        val tile = dest ?: tree
        if (tile == null || !GigosPath.walk(actor, tile)) {
            tree?.let { treeCd[key(it)] = TREE_REST }
            rest(6)
        }
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        if (actor.ownerIdleTicks < 2) {
            GigosPath.stop(actor)
            rest(4)
            return false
        }
        val stand = dest
        val target = tree ?: return abort(actor, 6)
        when (phase) {
            Phase.WALK -> {
                walkTicks++
                if (atTree(actor, target)) {
                    goToPhase(Phase.PICK)
                    return true
                }
                val stand = dest
                if (stand != null && GigosPath.arrived(actor, stand, 0.6)) {
                    val next = otherStand(actor, target, stand)
                    if (next == null) {
                        treeCd[key(target)] = TREE_REST
                        return abort(actor, 6)
                    }
                    dest = next
                    walkTicks = 0
                    if (!GigosPath.walk(actor, next)) {
                        treeCd[key(target)] = TREE_REST
                        return abort(actor, 6)
                    }
                    return true
                }
                if (GigosPath.stuck(walkTicks, 20)) {
                    val next = otherStand(actor, target, dest)
                    if (next == null) {
                        treeCd[key(target)] = TREE_REST
                        return abort(actor, 6)
                    }
                    dest = next
                    walkTicks = 0
                    if (!GigosPath.walk(actor, next)) {
                        treeCd[key(target)] = TREE_REST
                        return abort(actor, 6)
                    }
                    return true
                }
                if (!actor.walkingQueue.isMoving) {
                    val tile = dest ?: target
                    if (!GigosPath.arrived(actor, tile, 0.6)) {
                        if (!GigosPath.walk(actor, tile)) {
                            val next = otherStand(actor, target, dest)
                            if (next == null || !GigosPath.walk(actor, next)) {
                                treeCd[key(target)] = TREE_REST
                                return abort(actor, 6)
                            }
                            dest = next
                        }
                    }
                }
                return true
            }
            Phase.PICK -> {
                if (!atTree(actor, target)) {
                    goToPhase(Phase.WALK)
                    walkTicks = 0
                    dest = standTile(actor, target)
                    dest?.let { GigosPath.walk(actor, it) }
                    return true
                }
                if (actor.hunger() < MonkeyConfig.HUNGER_PICK) {
                    return abort(actor, 8)
                }
                if (!actor.addBananasNoted(1)) {
                    sendMessage(actor.owner, "Gigos wants a banana but his pack is full.")
                    return abort(actor, 25)
                }
                actor.addHunger(-MonkeyConfig.HUNGER_PICK)
                playAudio(actor.owner, MonkeyConfig.SFX_OOK)
                actor.saveBag()
                GigosHudPacket.send(actor.owner, actor)
                sendMessage(actor.owner, "Gigos picks a banana.")
                picksOnThis++
                if (picksOnThis >= PICKS_PER_TREE) {
                    treeCd[key(target)] = TREE_REST
                    return abort(actor, 6)
                }
                goToPhase(Phase.HOLD)
                wait = 3
                return true
            }
            Phase.HOLD -> {
                wait--
                if (wait > 0) return true
                goToPhase(Phase.PICK)
                return true
            }
        }
    }

    private fun abort(actor: AmiliousMonkey, restTicks: Int): Boolean {
        GigosPath.stop(actor)
        rest(restTicks)
        return false
    }

    private fun atTree(actor: AmiliousMonkey, tree: Location): Boolean {
        val dx = abs(actor.location.x - tree.x)
        val dy = abs(actor.location.y - tree.y)
        return actor.location.z == tree.z && dx <= 1 && dy <= 1 && (dx + dy) > 0
    }

    private fun otherStand(actor: AmiliousMonkey, tree: Location, used: Location?): Location? {
        val spots = arrayOf(
            tree.transform(1, 0, 0),
            tree.transform(-1, 0, 0),
            tree.transform(0, 1, 0),
            tree.transform(0, -1, 0)
        )
        return spots
            .filter { used == null || !sameTile(it, used) }
            .filter { !tileBlocked(it) }
            .filter { GigosPath.canReach(actor, it) }
            .minByOrNull { actor.location.getDistance(it) }
    }

    private fun standTile(actor: AmiliousMonkey, tree: Location?): Location? {
        if (tree == null) return null
        val spots = arrayOf(
            tree.transform(1, 0, 0),
            tree.transform(-1, 0, 0),
            tree.transform(0, 1, 0),
            tree.transform(0, -1, 0)
        )
        return spots
            .filter { !tileBlocked(it) }
            .filter { GigosPath.canReach(actor, it) }
            .minByOrNull { actor.location.getDistance(it) }
            ?: spots.filter { GigosPath.canReach(actor, it) }
                .minByOrNull { actor.location.getDistance(it) }
    }

    private fun nearestTree(actor: AmiliousMonkey): Location? {
        val origin = actor.owner.location
        val z = origin.z
        var best: Location? = null
        var bestDist = RANGE
        for (dx in -RANGE.toInt()..RANGE.toInt()) {
            for (dy in -RANGE.toInt()..RANGE.toInt()) {
                val loc = Location.create(origin.x + dx, origin.y + dy, z)
                if (treeCd.containsKey(key(loc))) continue
                val obj = RegionManager.getObject(loc) ?: continue
                if (!isTree(obj.id, obj.name)) continue
                val stand = standTile(actor, obj.location) ?: continue
                if (!GigosPath.canReach(actor, stand)) continue
                val d = actor.location.getDistance(obj.location)
                if (d < bestDist) {
                    bestDist = d
                    best = obj.location
                }
            }
        }
        return best
    }

    private fun isTree(id: Int, name: String): Boolean {
        if (id == 2078) return false
        if (id in PICKABLE) return true
        return name.lowercase().contains("banana")
    }

    private fun tileBlocked(loc: Location): Boolean {
        val obj = RegionManager.getObject(loc) ?: return false
        val opts = obj.definition.options
        if (opts != null && opts.any { it != null && it.equals("open", ignoreCase = true) }) {
            return false
        }
        return obj.definition.sizeX > 0
    }

    private fun sameTile(a: Location, b: Location): Boolean =
        a.x == b.x && a.y == b.y && a.z == b.z

    private fun key(loc: Location) = "${loc.x},${loc.y},${loc.z}"

    companion object {
        private const val RANGE = 10.0
        private const val PICKS_PER_TREE = 3
        private const val TREE_REST = 40
        private const val IDLE_NEED = 8
        private val PICKABLE = intArrayOf(2073, 2074, 2075, 2076, 2077)
    }
}