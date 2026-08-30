package content.amilious.pet.actions

import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import content.amilious.pet.MonkeyConfig
import core.api.sendMessage
import core.game.interaction.MovementPulse
import core.game.node.scenery.Scenery
import core.game.world.map.Location
import core.game.world.map.RegionManager
import core.game.world.update.flag.context.Animation

class PickBananaTreeAction : CompanionAction<AmiliousMonkey> {

    private enum class Phase { WALK, PICK, HOLD }

    private var phase = Phase.WALK
    private var dest: Location? = null
    private var wait = 0
    private var walking = false
    private var cool = 0
    private var lastOwner = Location.create(0, 0, 0)
    private var idleTicks = 0
    private var picksOnThis = 0
    private val rest = HashMap<Location, Int>()

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
        return nearest(actor) != null
    }

    override fun start(actor: AmiliousMonkey) {
        dest = nearest(actor)?.location
        phase = Phase.WALK
        wait = 0
        walking = false
        picksOnThis = 0
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        val tile = dest ?: return false
        when (phase) {
            Phase.WALK -> {
                if (actor.location.getDistance(tile) <= 1.5) {
                    phase = Phase.PICK
                    return true
                }
                if (!walking) {
                    walking = true
                    actor.pulseManager.run(object : MovementPulse(actor, tile) {
                        override fun pulse(): Boolean {
                            walking = false
                            return true
                        }
                    })
                }
                if (walking && !actor.pulseManager.hasPulseRunning()) walking = false
                return true
            }
            Phase.PICK -> {
                if (actor.location.getDistance(tile) > 1.5) {
                    phase = Phase.WALK
                    return true
                }
                if (!actor.addBananasNoted(1)) {
                    sendMessage(actor.owner, "Gigos wants a banana but his pack is full.")
                    cool = 25
                    return false
                }
                actor.saveBag()
                GigosHudPacket.send(actor.owner, actor)
                sendMessage(actor.owner, "Gigos picks a banana.")
                actor.animate(Animation(827))
                picksOnThis++
                walking = false
                if (picksOnThis >= PICKS_PER_TREE) {
                    rest[tile] = TREE_REST
                    dest = nearest(actor)?.location
                    picksOnThis = 0
                    if (dest == null) {
                        cool = 8
                        return false
                    }
                    phase = Phase.WALK
                    return true
                }
                phase = Phase.HOLD
                wait = if (idleTicks >= 8) 2 else 5
                cool = if (idleTicks >= 8) 1 else 6
                return true
            }
            Phase.HOLD -> {
                wait--
                return wait > 0
            }
        }
    }

    private fun nearest(actor: AmiliousMonkey): Scenery? {
        val origin = actor.location
        var best: Scenery? = null
        var bestDist = RANGE
        for (dx in -RANGE.toInt()..RANGE.toInt()) {
            for (dy in -RANGE.toInt()..RANGE.toInt()) {
                val loc = origin.transform(dx, dy, 0)
                if (rest.containsKey(loc)) continue
                val obj = RegionManager.getObject(loc) ?: continue
                if (obj.id !in PICKABLE) continue
                val d = origin.getDistance(loc)
                if (d < bestDist) {
                    bestDist = d
                    best = obj
                }
            }
        }
        return best
    }

    companion object {
        private const val RANGE = 8.0
        private const val PICKS_PER_TREE = 3
        private const val TREE_REST = 40
        private val PICKABLE = intArrayOf(2073, 2074, 2075, 2076, 2077)
    }
}