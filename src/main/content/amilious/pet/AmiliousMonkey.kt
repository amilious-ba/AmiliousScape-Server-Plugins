package content.amilious.pet


import content.amilious.pet.actions.BonesToBananasAction
import content.amilious.pet.actions.EatBananaAction
import content.amilious.pet.actions.FeedOwnerAction
import content.amilious.pet.actions.FollowIdleAction
import content.amilious.pet.actions.FollowIfFarAction
import content.amilious.pet.actions.LootAction
import content.amilious.pet.actions.PickBananaTreeAction
import content.amilious.pet.actions.ThrowDungAction
import content.amilious.pet.actions.WanderAction
import core.api.sendMessage
import core.game.component.CloseEvent
import core.game.component.Component
import core.game.container.Container
import core.game.container.access.InterfaceContainer
import core.game.interaction.MovementPulse
import core.game.interaction.Option
import core.game.node.entity.npc.NPC
import core.game.node.entity.player.Player
import core.game.node.item.GroundItem
import core.game.node.item.Item
import core.game.world.map.RegionManager
import core.game.world.repository.Repository

class AmiliousMonkey(val owner: Player) : NPC(MonkeyConfig.NPC_ID) {

    val bag = Container(MonkeyConfig.BOB_SIZE)
    var ownerIdleTicks = 0
        private set
    var outOfCombatTicks = 0
        private set
    private var lastOwnerX = Int.MIN_VALUE
    private var lastOwnerY = Int.MIN_VALUE
    private var lastOwnerHp = -1
    private var lastHudAction = ""

    private val brain = CompanionBrain(this)

    init {
        brain
            .addAction(FollowIfFarAction())
            .addAction(FeedOwnerAction())
            .addAction(PickBananaTreeAction())
            .addAction(EatBananaAction())
            .addAction(BonesToBananasAction())
            .addAction(LootAction())
            .addAction(ThrowDungAction())
            .addAction(WanderAction())
            .addAction(FollowIdleAction())
    }

    fun dungEnabled(): Boolean = owner.getAttribute(MonkeyConfig.ATTR_DUNG, true)
    fun eatEnabled(): Boolean = owner.getAttribute(MonkeyConfig.ATTR_EAT, true)
    fun b2bEnabled(): Boolean = owner.getAttribute(MonkeyConfig.ATTR_B2B, true)
    fun lootEnabled(): Boolean = owner.getAttribute(MonkeyConfig.ATTR_LOOT, true)
    fun feedEnabled(): Boolean = owner.getAttribute(MonkeyConfig.ATTR_FEED, true)

    fun ownerInCombat(): Boolean {
        val p = owner
        if (p.inCombat()) return true
        val pulse = p.properties.combatPulse
        return pulse != null && pulse.isAttacking
    }

    private fun tickCombatIdle() {
        if (ownerInCombat()) outOfCombatTicks = 0 else outOfCombatTicks++
    }

    fun hunger(): Int = owner.getAttribute(MonkeyConfig.ATTR_HUNGER, MonkeyConfig.HUNGER_MAX)

    fun setHunger(v: Int) {
        owner.setAttribute(MonkeyConfig.ATTR_HUNGER, v.coerceIn(0, MonkeyConfig.HUNGER_MAX))
    }

    fun addHunger(delta: Int): Int {
        setHunger(hunger() + delta)
        return hunger()
    }

    fun spawnAtOwner() {
        owner.getAttribute<AmiliousMonkey?>(MonkeyConfig.ATTR_ACTIVE, null)?.let { old ->
            if (old !== this) old.dismiss()
        }
        try {
            val locals = RegionManager.getLocalNpcs(owner, 64)
            for (npc in locals) {
                if (npc !== this && npc is AmiliousMonkey && npc.owner == owner) {
                    npc.dismiss()
                }
            }
        } catch (_: Exception) {
        }
        location = owner.location.transform(1, 0, 0)
        init()
        name = "Gigos"
        isWalks = true
        interaction.set(Option("Pack", 0))
        interaction.set(Option("Talk-to", 1))
        interaction.set(Option("Pet", 2))
        interaction.set(Option("Dismiss", 3))
        loadBag()
        owner.setAttribute(MonkeyConfig.ATTR_ACTIVE, this)
        refreshMenu()
        sendMessage(owner, "Gigos hops down beside you.")
        followOwner()
    }

    fun takeNonBananasToOwner(): Int {
        var moved = 0
        for (item in bag.toArray()) {
            if (item == null || isBananaItem(item)) continue
            var left = item.amount
            while (left > 0) {
                val n = left
                val copy = Item(item.id, n)
                if (owner.inventory.hasSpaceFor(copy)) {
                    if (bag.remove(copy) && owner.inventory.add(copy)) {
                        moved += n
                        left = 0
                    } else break
                } else if (n == 1) {
                    break
                } else {
                    left = n / 2
                }
            }
        }
        if (moved > 0) saveBag()
        return moved
    }

    fun refreshMenu() {
        val lootOn = owner.getAttribute(MonkeyConfig.ATTR_LOOT, true)
        NpcMenuPacket.send(
            owner, this, "Gigos",
            0 to "Pack",
            1 to "Talk-to",
            2 to "Pet",
            3 to "Dismiss"
        )
        GigosHudPacket.send(owner, this)
    }

    fun dismiss() {
        brain.interrupt()
        saveBag()
        NpcMenuPacket.clear(owner, this)
        owner.removeAttribute(MonkeyConfig.ATTR_ACTIVE)
        GigosHudPacket.hide(owner)
        clear()
        sendMessage(owner, "Gigos scurries off. His pack is safe. ::monkey to call him back.")
    }

    fun noteOwnerIdle() {
        val loc = owner.location
        val hp = owner.skills.lifepoints
        val busy = owner.properties.combatPulse.isAttacking
        if (busy || loc.x != lastOwnerX || loc.y != lastOwnerY || hp != lastOwnerHp) {
            ownerIdleTicks = 0
            lastOwnerX = loc.x
            lastOwnerY = loc.y
            lastOwnerHp = hp
            return
        }
        ownerIdleTicks++
    }

    fun tickCompanion() {
        val gone = !owner.isActive
                || owner.session == null
                || Repository.getPlayerByName(owner.name) == null
        if (gone) {
            dismiss()
            return
        }
        noteOwnerIdle()
        tickCombatIdle()
        brain.tick()
        val name = brain.currentName()
        if (name != lastHudAction) {
            lastHudAction = name
            GigosHudPacket.send(owner, this)
        }
    }

    fun saveBag() {
        val encoded = bag.toArray()
            .filterNotNull()
            .joinToString(";") { "${it.id}:${it.amount}" }
        owner.setAttribute(MonkeyConfig.ATTR_BAG, encoded)
    }

    fun loadBag() {
        bag.clear()
        val raw = owner.getAttribute(MonkeyConfig.ATTR_BAG, "") ?: return
        if (raw.isBlank()) return
        for (part in raw.split(";")) {
            val bits = part.split(":")
            if (bits.size != 2) continue
            val id = bits[0].toIntOrNull() ?: continue
            val amt = bits[1].toIntOrNull() ?: continue
            if (id > 0 && amt > 0) bag.add(Item(id, amt))
        }
    }

    fun openBagUi() {
        owner.interfaceManager.open(Component(671)).setCloseEvent(CloseEvent { player, _ ->
            saveBag()
            player.interfaceManager.closeSingleTab()
            true
        })
        bag.shift()
        owner.interfaceManager.openSingleTab(Component(665))
        InterfaceContainer.generateItems(
            owner, owner.inventory.toArray(),
            arrayOf("Examine", "Store-X", "Store-All", "Store-10", "Store-5", "Store-1"),
            665, 0, 7, 4, 93
        )
        InterfaceContainer.generateItems(
            owner, bag.toArray(),
            arrayOf("Examine", "Withdraw-X", "Withdraw-All", "Withdraw-10", "Withdraw-5", "Withdraw-1"),
            671, 27, 5, 6, 30
        )
    }

    fun followOwner() {
        if (pulseManager.hasPulseRunning()) return
        pulseManager.run(object : MovementPulse(this, owner) {
            override fun pulse(): Boolean = false
        })
    }

    fun isOwnerDrop(gi: GroundItem): Boolean {
        if (gi.isRemoved) return false
        if (gi.droppedBy(owner)) return true
        if (gi.dropper == owner) return true
        return false
    }

    fun canTake(gi: GroundItem): Boolean {
        if (gi.isRemoved) return false
        if (gi.id == MonkeyConfig.BANANA_ID) return true
        return isOwnerDrop(gi)
    }

    fun isBone(item: Item): Boolean {
        val n = item.name.lowercase()
        return n == "bones" || n.endsWith(" bones") || n == "bone"
    }

    fun bananaNoteId(): Int {
        val nid = Item(MonkeyConfig.BANANA_ID).definition?.noteId ?: -1
        return if (nid > 0) nid else MonkeyConfig.BANANA_NOTE_ID
    }

    fun isBananaItem(item: Item): Boolean =
        item.id == MonkeyConfig.BANANA_ID || item.id == bananaNoteId()

    fun addBananasNoted(amount: Int): Boolean {
        if (amount <= 0) return false
        val note = Item(bananaNoteId(), amount)
        if (bag.hasSpaceFor(note) && bag.add(note)) return true
        val raw = Item(MonkeyConfig.BANANA_ID, amount)
        return bag.hasSpaceFor(raw) && bag.add(raw)
    }

    fun takeOneBanana(): Boolean {
        if (bag.remove(Item(MonkeyConfig.BANANA_ID, 1))) return true
        return bag.remove(Item(bananaNoteId(), 1))
    }

    fun hasBanana(): Boolean = bag.toArray().any { it != null && isBananaItem(it) }

    fun bananaCount(): Int {
        var n = 0
        for (it in bag.toArray()) {
            if (it != null && isBananaItem(it)) n += it.amount
        }
        return n
    }

    fun brainActionName(): String = brain.currentName()

}