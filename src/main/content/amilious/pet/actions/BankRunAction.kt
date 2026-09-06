package content.amilious.pet.actions

import content.amilious.ai.PhasedCompanionAction
import content.amilious.pet.AmiliousMonkey
import content.amilious.pet.MonkeyConfig
import core.api.playAudio
import core.api.sendMessage
import core.game.node.entity.combat.graves.GraveController
import core.game.world.map.Location
import kotlin.random.Random

class BankRunAction(rank: Int = 75) :
    PhasedCompanionAction<AmiliousMonkey, BankRunAction.Phase>(
        "bank", rank, Phase::class
    ) {

    enum class Phase { LEAVE, GONE, COUNTER, BACK }

    private var leaveTile: Location? = null
    private var backTile: Location? = null
    private var walkTicks = 0
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
        walkTicks = 0
        goneTicks = 0
        leaveTile = pickAway(actor, 8, 12)
        backTile = null
        val tile = leaveTile
        if (tile != null) {
            actor.brain.path.walk(actor, tile)
        }
        sendMessage(actor.owner, "Gigos scampers off toward the bank.")
        playAudio(actor.owner, MonkeyConfig.SFX_PLAYFUL)
    }

    override fun tick(actor: AmiliousMonkey): Boolean {
        if (graveWaiting(actor)) {
            revealBeside(actor)
            rest(4)
            return false
        }
        when (phase) {
            Phase.LEAVE -> {
                walkTicks++
                val tile = leaveTile
                val path = actor.brain.path
                val arrived = tile != null && path.arrived(actor, tile, 1.5)
                val tired = walkTicks >= 12 || tile == null ||
                        path.reallyStuck(actor, tile) || path.stuck(walkTicks, 12)
                if (arrived || tired) {
                    path.stop(actor)
                    actor.pulseManager.clear()
                    actor.walkingQueue.reset()
                    actor.isInvisible = true
                    goneTicks = 0
                    nextPhase()
                    return true
                }
                if (tile != null) {
                    path.walk(actor, tile)
                }
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
                    sendMessage(actor.owner, "Gigos banks $moved item${if (moved == 1) "" else "s"}. Bananas stay.")
                } else {
                    sendMessage(actor.owner, "Gigos finds nothing to bank. Bananas stay with him.")
                }
                backTile = pickAway(actor, 5, 8) ?: actor.owner.location.transform(1, 0, 0)
                nextPhase()
                return true
            }
            Phase.BACK -> {
                if (actor.isInvisible) {
                    val land = backTile ?: actor.owner.location.transform(1, 0, 0)
                    actor.isInvisible = false
                    actor.location = land
                    actor.properties.teleportLocation = land
                    actor.refreshPose()
                    walkTicks = 0
                    actor.brain.path.walk(actor, actor.owner.location)
                    playAudio(actor.owner, MonkeyConfig.SFX_PLAYFUL)
                    sendMessage(actor.owner, "Gigos jogs back from the bank.")
                    return true
                }
                walkTicks++
                val path = actor.brain.path
                val dest = actor.owner.location
                if (path.arrived(actor, dest, 1.5) || walkTicks >= 16) {
                    path.stop(actor)
                    rest(MonkeyConfig.BANK_AUTO_REST)
                    return false
                }
                if (path.reallyStuck(actor, dest) || path.stuck(walkTicks, 16)) {
                    actor.location = dest.transform(1, 0, 0)
                    actor.properties.teleportLocation = actor.location
                    actor.refreshPose()
                    rest(MonkeyConfig.BANK_AUTO_REST)
                    return false
                }
                path.walk(actor, dest)
                return true
            }
        }
    }

    override fun stop(actor: AmiliousMonkey) {
        if (actor.isInvisible) {
            revealBeside(actor)
        }
    }

    private fun revealBeside(actor: AmiliousMonkey) {
        actor.isInvisible = false
        val land = actor.owner.location.transform(1, 0, 0)
        actor.location = land
        actor.properties.teleportLocation = land
        actor.refreshPose()
    }

    private fun graveWaiting(actor: AmiliousMonkey): Boolean {
        if (!actor.graveEnabled()) return false
        val grave = GraveController.activeGraves[actor.owner.details.uid] ?: return false
        return grave.getItems().any { !it.isRemoved }
    }

    private fun pickAway(actor: AmiliousMonkey, min: Int, max: Int): Location? {
        val path = actor.brain.path
        val origin = actor.owner.location
        val ax = actor.location.x
        val ay = actor.location.y
        val awayX = if (ax >= origin.x) 1 else -1
        val awayY = if (ay >= origin.y) 1 else -1
        for (i in 0 until 16) {
            val dist = Random.nextInt(min, max + 1)
            val dx = awayX * dist + Random.nextInt(-2, 3)
            val dy = awayY * dist + Random.nextInt(-2, 3)
            val tile = Location.create(origin.x + dx, origin.y + dy, origin.z)
            if (tile.getDistance(origin) < min) continue
            if (path.canReach(actor, tile)) return tile
        }
        return null
    }
}