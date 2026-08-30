package content.amilious.pet.actions

import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.sendMessage
import core.game.interaction.MovementPulse
import core.game.world.map.Location
import core.game.world.map.RegionManager
import core.game.world.update.flag.context.Animation

class PickBananaTreeAction : CompanionAction<AmiliousMonkey> {

    private enum class Phase { WALK, PICK, HOLD }

    private var phase = Phase.WALK
    private var dest: Location? = null
    private var wait = 0
    private var walkTicks = 0
    private var cool = 0
    private var lastOwner = Location.create(0, 0, 0)
    private var idleTicks = 0
    private var picksOnThis = 0
    private val rest = HashMap<String, Int>()

    override fun name() = "pick-banana"

    override fun cooldown(actor: AmiliousMonkey) {
        if (cool > 0) cool--
        val here = actor.owner.location
        if (here == lastOwner) idleTicks++ else idleTicks = 0
        lastOwner = here
        val it = rest.iterator()
        while (it.hasNext()) {
            val e = it.next()
            e.setValue(e.value - 1)
            if (e.value <= 0) it.remove()
        }
    }

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (cool > 0) return false
        if (actor.location.getDistance(actor.owner.location) > MonkeyConfig.FOLLOW_DIST) return false
        return nearestTile(actor) != null
    }

    override fun start(actor: AmiliousMonkey) {
        dest = nearestTile(actor)
        phase = Phase.WALK
        wait = 0
        walkTicks = 0
        picksOnThis = 0
        sendMessage(actor.owner, "Gigos heads for a banana tree.")
        walkTo(actor)
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        val tile = dest ?: return done(actor)
        when (phase) {
            Phase.WALK -> {
                walkTicks++
                if (actor.location.getDistance(tile) <= 2.0) {
                    phase = Phase.PICK
                    walkTicks = 0
                    return true
                }
                if (walkTicks > 25) return done(actor)
                if (!actor.pulseManager.hasPulseRunning()) walkTo(actor)
                return true
            }
            Phase.PICK -> {
                if (actor.location.getDistance(tile) > 2.0) {
                    phase = Phase.WALK
                    walkTicks = 0
                    walkTo(actor)
                    return true
                }
                if (!actor.addBananasNoted(1)) {
                    sendMessage(actor.owner, "Gigos wants a banana but his pack is full.")
                    cool = 25
                    return done(actor)
                }
                actor.saveBag()
                GigosHudPacket.send(actor.owner, actor)
                sendMessage(actor.owner, "Gigos picks a banana.")
                actor.animate(Animation(827))
                picksOnThis++
                if (picksOnThis >= PICKS_PER_TREE) {
                    rest[key(tile)] = TREE_REST
                    dest = nearestTile(actor)
                    picksOnThis = 0
                    if (dest == null) {
                        cool = 4
                        return done(actor)
                    }
                    phase = Phase.WALK
                    walkTicks = 0
                    sendMessage(actor.owner, "Gigos moves to another tree.")
                    walkTo(actor)
                    return true
                }
                phase = Phase.HOLD
                wait = if (idleTicks >= 8) 2 else 5
                return true
            }
            Phase.HOLD -> {
                wait--
                if (wait > 0) return true
                phase = Phase.PICK
                return true
            }
        }
    }

    private fun done(actor: AmiliousMonkey): Boolean {
        actor.followOwner()
        return false
    }

    private fun walkTo(actor: AmiliousMonkey) {
        val tile = dest ?: return
        actor.pulseManager.clear()
        actor.pulseManager.run(object : MovementPulse(actor, tile) {
            override fun pulse(): Boolean = true
        })
    }

    private fun nearestTile(actor: AmiliousMonkey): Location? {
        val origin = actor.location
        val z = origin.z
        var best: Location? = null
        var bestDist = RANGE
        for (dx in -RANGE.toInt()..RANGE.toInt()) {
            for (dy in -RANGE.toInt()..RANGE.toInt()) {
                val loc = Location.create(origin.x + dx, origin.y + dy, z)
                if (rest.containsKey(key(loc))) continue
                val obj = RegionManager.getObject(z, loc.x, loc.y) ?: continue
                if (!isTree(obj.id, obj.name)) continue
                val d = origin.getDistance(obj.location)
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
        val n = name.lowercase()
        return n.contains("banana") && n.contains("tree")
    }

    private fun key(loc: Location) = "${loc.x},${loc.y},${loc.z}"

    companion object {
        private const val RANGE = 10.0
        private const val PICKS_PER_TREE = 3
        private const val TREE_REST = 40
        private val PICKABLE = intArrayOf(2073, 2074, 2075, 2076, 2077)
    }
}