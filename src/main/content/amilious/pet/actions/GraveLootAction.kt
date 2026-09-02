package content.amilious.pet.actions

import content.amilious.ai.PhasedCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.GigosHudPacket
import core.api.sendMessage
import core.game.node.entity.combat.graves.GraveController
import core.game.node.item.GroundItemManager
import core.game.node.item.Item
import core.game.world.map.Location

class GraveLootAction(rank: Int = 110) :
    PhasedCompanionAction<AmiliousMonkey, GraveLootAction.Phase>(
        "grave", rank, Phase::class
    ) {

    enum class Phase { WALK, SCOOP }

    private var dest: Location? = null
    private var walkTicks = 0

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!actor.graveEnabled()) return false
        if (!ready()) return false
        val grave = GraveController.activeGraves[actor.owner.details.uid] ?: return false
        return grave.getItems().any { !it.isRemoved }
    }

    override fun start(actor: AmiliousMonkey) {
        super.start(actor)
        dest = GraveController.activeGraves[actor.owner.details.uid]?.location
        walkTicks = 0
        val tile = dest ?: return
        actor.brain.path.walk(actor, tile)
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        val path = actor.brain.path
        val tile = dest ?: return abort(actor, 8)
        when (phase) {
            Phase.WALK -> {
                walkTicks++
                if (path.arrived(actor, tile, 1.5)) {
                    path.stop(actor)
                    nextPhase()
                    return true
                }
                if (path.reallyStuck(actor, tile) || path.stuck(walkTicks, 40)) {
                    path.stop(actor)
                    actor.properties.teleportLocation = tile
                    nextPhase()
                    return true
                }
                path.walk(actor, tile)
                return true
            }
            Phase.SCOOP -> {
                val grave = GraveController.activeGraves[actor.owner.details.uid]
                var any = false
                if (grave != null) {
                    for (gi in grave.getItems()) {
                        if (gi.isRemoved) continue
                        val copy = Item(gi.id, gi.amount)
                        if (actor.isBananaItem(copy)) {
                            if (actor.addBananasNoted(copy.amount)) {
                                GroundItemManager.destroy(gi)
                                any = true
                            }
                            continue
                        }
                        if (!actor.bag.hasSpaceFor(copy)) continue
                        if (actor.bag.add(copy)) {
                            GroundItemManager.destroy(gi)
                            any = true
                        }
                    }
                }
                if (any) {
                    actor.saveBag()
                    GigosHudPacket.send(actor.owner, actor)
                    sendMessage(actor.owner, "Gigos looted your grave.")
                }
                rest(5)
                return false
            }
        }
    }

    private fun abort(actor: AmiliousMonkey, restTicks: Int): Boolean {
        actor.brain.path.stop(actor)
        rest(restTicks)
        return false
    }
}