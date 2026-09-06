package content.amilious.pet.actions

import content.amilious.ai.PhasedCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.MonkeyConfig
import core.api.sendMessage
import core.game.node.entity.combat.graves.GraveController

class BankRunAction(rank: Int = 75) :
    PhasedCompanionAction<AmiliousMonkey, BankRunAction.Phase>(
        "bank", rank, Phase::class
    ) {

    enum class Phase { LEAVE, GONE, COUNTER, BACK }

    private var goneTicks = 0

    override fun canStart(actor: AmiliousMonkey): Boolean {
        if (!ready()) return false
        if (!actor.bankEnabled()) return false
        if (graveWaiting(actor)) return false
        if (!actor.bagIsFull()) return false
        return actor.hasBankableItems()
    }

    override fun start(actor: AmiliousMonkey) {
        super.start(actor)
        goneTicks = 0
        actor.brain.path.stop(actor)
        actor.pulseManager.clear()
        actor.walkingQueue.reset()
        actor.poofHere()
        actor.isInvisible = true
        sendMessage(actor.owner, "Gigos teleports to the bank.")
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        if (graveWaiting(actor)) {
            revealBeside(actor)
            rest(4)
            return false
        }
        when (phase) {
            Phase.LEAVE -> {
                actor.isInvisible = true
                goneTicks = 0
                nextPhase()
                return true
            }
            Phase.GONE -> {
                goneTicks++
                actor.isInvisible = true
                if (goneTicks >= MonkeyConfig.BANK_GONE_TICKS) {
                    nextPhase()
                }
                return true
            }
            Phase.COUNTER -> {
                val moved = actor.depositBagToBank(keepBananas = true)
                actor.addHunger(-MonkeyConfig.HUNGER_BANK)
                if (moved > 0) {
                    sendMessage(
                        actor.owner,
                        "Gigos banks $moved item${if (moved == 1) "" else "s"}. Bananas stay."
                    )
                } else {
                    sendMessage(actor.owner, "Gigos finds nothing to bank. Bananas stay with him.")
                }
                nextPhase()
                return true
            }
            Phase.BACK -> {
                revealBeside(actor)
                actor.poofHere()
                sendMessage(actor.owner, "Gigos teleports back from the bank.")
                rest(MonkeyConfig.BANK_AUTO_REST)
                return false
            }
        }
    }

    override fun stop(actor: AmiliousMonkey) {
        if (actor.isInvisible) {
            revealBeside(actor)
        }
    }

    private fun revealBeside(actor: AmiliousMonkey) {
        val land = actor.owner.location.transform(1, 0, 0)
        actor.isInvisible = false
        actor.location = land
        actor.properties.teleportLocation = land
        actor.refreshPose()
    }

    private fun graveWaiting(actor: AmiliousMonkey): Boolean {
        if (!actor.graveEnabled()) return false
        val grave = GraveController.activeGraves[actor.owner.details.uid] ?: return false
        return grave.getItems().any { !it.isRemoved }
    }
}