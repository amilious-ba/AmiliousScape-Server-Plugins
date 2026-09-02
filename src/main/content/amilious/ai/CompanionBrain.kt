package content.amilious.ai

import core.game.node.entity.npc.NPC

class CompanionBrain<T>(private val actor: T) {

    val path = CompanionPath()

    private val actions = ArrayList<ICompanionAction<T>>()
    private var current: ICompanionAction<T>? = null
    private var stopWhenAble = false
    private var stuckTicks = 0

    fun getCurrentActionName(): String = current?.name() ?: "idle"
    fun getCurrentAction(): ICompanionAction<T>? = current
    fun getCurrentPhase(): Int = current?.getPhase() ?: 0
    fun getCurrentActionPhases(): Int = current?.getNumberPhases() ?: 0
    fun getCurrentActionPhaseName(): String = current?.getPhaseName() ?: "no phase"
    fun getNumberOfActions(): Int = actions.size
    fun busy(): Boolean = current != null

    fun addAction(action: ICompanionAction<T>): CompanionBrain<T> {
        val i = actions.indexOfFirst { it.priority() < action.priority() }
        if (i < 0) actions.add(action) else actions.add(i, action)
        return this
    }

    fun removeAction(action: ICompanionAction<T>): Boolean {
        if (!actions.contains(action)) return false
        if (current === action) interrupt()
        actions.remove(action)
        return true
    }

    fun tick() {
        tickCooldowns()
        tickStuck()
        if (tickCurrent()) return
        startNext()
    }

    fun requestStop() {
        stopWhenAble = true
    }

    fun interrupt() {
        finishCurrent()
    }

    fun debugLines(): List<String> {
        val lines = ArrayList<String>()
        lines.add(
            "current=${current?.name() ?: "idle"} phase=${current?.getPhaseName() ?: "-"} busy=${busy()}"
        )
        for (a in actions) {
            lines.add("  p=${a.priority()} ${a.name()} phase=${a.getPhaseName()}")
        }
        return lines
    }

    private fun tickCooldowns() {
        actions.forEach { it.cooldown(actor) }
    }

    private fun tickStuck() {
        val npc = actor as? NPC ?: return
        path.noteMove(npc)
        val moving = npc.walkingQueue.isMoving || npc.pulseManager.hasPulseRunning()
        val walking = path.hasTarget()
        if (!moving && walking && current != null) {
            stuckTicks++
        } else {
            stuckTicks = 0
        }
        if (stuckTicks > 10) {
            finishCurrent()
        }
    }

    /** @return true if an action is still running and we should not pick another */
    private fun tickCurrent(): Boolean {
        val running = current ?: return false
        val keep = running.tick(actor)
        if (!keep || stopWhenAble) {
            finishCurrent()
            return false
        }
        return true
    }

    private fun startNext() {
        val next = pickNext() ?: return
        clearMotion()
        current = next
        next.start(actor)
        if (!next.tick(actor)) {
            finishCurrent()
        }
    }

    private fun finishCurrent() {
        val running = current
        current = null
        stopWhenAble = false
        stuckTicks = 0
        running?.stop(actor)
        clearMotion()
    }

    private fun clearMotion() {
        val npc = actor as? NPC ?: return
        path.stop(npc)
        npc.pulseManager.clear()
        npc.walkingQueue.reset()
        path.resetStuck()
        stuckTicks = 0
    }

    private fun pickNext(): ICompanionAction<T>? {
        val ready = actions.filter { it.canStart(actor) }
        if (ready.isEmpty()) return null

        val bands = ready.groupBy { it.priority() }
        val bestPri = bands.keys.maxOrNull() ?: return null
        val pool = bands[bestPri] ?: return null

        val total = pool.sumOf { it.priorityWeight().coerceAtLeast(1) }
        var roll = kotlin.random.Random.nextInt(total)
        for (a in pool) {
            roll -= a.priorityWeight().coerceAtLeast(1)
            if (roll < 0) return a
        }
        return pool.last()
    }
}