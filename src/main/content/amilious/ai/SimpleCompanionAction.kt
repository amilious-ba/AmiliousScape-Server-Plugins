package content.amilious.ai


/**
 * Represents an abstract class providing a basic implementation for actions with cooldowns and priorities.
 *
 * This class defines the structure and fundamental behaviors of a companion action, such as managing
 * cooldown states, identifying the action by name or priority, and supporting functionalities like
 * multi-phase execution. Derived classes are expected to implement specific behaviors for the action.
 *
 * @param T The type of the actor or entity associated with this action.
 * @param actionName The name identifying the action.
 * @param rank The priority rank associated with the action. Default value is 0.
 */
abstract class SimpleCompanionAction<T>(private val actionName: String, private val rank: Int = 0) :
    ICompanionAction<T> {

    //#region Properties ###############################################################################################

    /**
     * Tracks the cooldown period in ticks for the action.
     *
     * This property is used to manage the delay or cooldown state of the action. A positive value indicates
     * that the action is in a cooldown period and cannot be executed. The `cool` value decreases over time or
     * through specific implementation logic until it reaches zero, indicating that the action is ready to be performed again.
     *
     * Usage within the containing class includes:
     * - Setting a cooldown period (`rest` or `delay` methods).
     * - Checking if the action is ready (`ready` method).
     * - Resetting the cooldown to make the action immediately executable (`resetCoolDown` method).
     */
    protected var cool = 0

    protected var hold = 0

    //#endregion #######################################################################################################


    //#region Override Methods #########################################################################################

    /**
     * Returns the name of the action.
     *
     * This method provides the identifier or name associated with the action.
     * It is typically used to distinguish between different actions or to display
     * the action's name in user-facing contexts.
     *
     * @return The action name as a string.
     */
    override fun name() = actionName

    override fun start(actor: T) {
        hold = 0
    }

    /**
     * Retrieves the priority of this action.
     *
     * This method returns a numeric value representing the priority level of the action.
     * Higher values may indicate higher priority in contexts where actions are compared
     * or organized based on their importance or execution order.
     *
     * @return The priority rank as an integer.
     */
    override fun priority() = rank

    /**
     * Retrieves the number of phases associated with this action.
     *
     * This method returns a fixed value representing the count of distinct phases
     * or stages involved in the execution or lifecycle of the action.
     *
     * @return The number of phases as an integer.
     */
    override fun getNumberPhases(): Int = 1

    /**
     * Retrieves the current phase of the action.
     *
     * This method provides the phase or stage in the lifecycle of the action.
     * It is typically used to determine the current operational status or progression
     * within a multi-phase action.
     *
     * @return The current phase as an integer.
     */
    override fun getPhase(): Int = 0

    /**
     * Retrieves the name of the current phase for this action.
     *
     * This method provides the descriptive or identifying name
     * associated with the current phase of the action. It is useful
     * for understanding or displaying the phase in contexts where
     * the phase's name plays a meaningful role.
     *
     * @return The name of the current phase as a string.
     */
    override fun getPhaseName(): String = actionName

    /**
     * Reduces the cooldown of the action if it is greater than zero.
     *
     * This method decreases the `cool` value by one for each invocation, stopping at zero.
     * It is used to manage the cooldown state of an action, ensuring that the action
     * cannot be executed until the cooldown duration has elapsed.
     *
     * @param actor The actor or entity associated with this action.
     * This is typically the entity performing the action whose cooldown is being managed.
     */
    override fun cooldown(actor: T) {
        if (cool > 0) cool--
    }

    override fun priorityWeight(): Int = 1

    //#endregion #######################################################################################################


    //#region Protected Methods ########################################################################################

    protected fun holdFor(ticks: Int) {
        hold = ticks
    }

    protected fun holding(): Boolean {
        if (hold > 0) {
            hold--
            return hold > 0
        }
        return false
    }

    /**
     * Sets the cooldown for the action.
     *
     * This method updates the cooldown duration of the action by assigning the specified
     * number of ticks, effectively delaying the action's availability until the cooldown
     * period has elapsed.
     *
     * @param ticks The number of ticks to set as the cooldown duration. Must be a non-negative integer.
     */
    protected fun rest(ticks: Int) {
        cool = ticks
    }

    /**
     * Delays the action by a specified number of ticks.
     *
     * This method increases the current cooldown (`cool`) value by the specified
     * number of ticks. It is used to postpone the availability of the action
     * until the cooldown reaches the desired duration.
     *
     * @param ticks The number of ticks to add to the current cooldown. Must be a non-negative integer.
     */
    protected fun delay(ticks: Int) {
        cool += ticks
    }

    /**
     * Sets the cooldown for the action based on the specified duration in seconds.
     *
     * This method calculates the equivalent number of ticks for the given duration in seconds
     * and applies the corresponding cooldown to the action. It is used to manage the action's
     * availability in time-based scenarios.
     *
     * @param seconds The duration in seconds for which the action should remain on cooldown.
     * Must be a non-negative double value.
     */
    protected fun restSeconds(seconds: Double) {
        rest(secondsToTicks(seconds))
    }

    /**
     * Delays the action by a specified duration in seconds.
     *
     * This method computes the equivalent number of ticks for the given duration
     * in seconds and applies a delay to the action, postponing its availability
     * until the calculated ticks have elapsed.
     *
     * @param seconds The duration in seconds for which the action should be delayed.
     * Must be a non-negative double value.
     */
    protected fun delaySeconds(seconds: Double) {
        delay(secondsToTicks(seconds))
    }

    /**
     * Resets the cooldown of the action.
     *
     * This method sets the `cool` value to zero, effectively removing any active cooldown
     * on the action. It is typically used to immediately make the action available
     * for execution without waiting for the cooldown to naturally expire.
     */
    protected fun resetCoolDown() {
        cool = 0
    }

    /**
     * Checks if the action is ready to be executed.
     *
     * This method determines whether the action is available to be performed
     * by evaluating its current cooldown state. An action is considered ready
     * if its cooldown value (`cool`) is less than or equal to zero.
     *
     * @return `true` if the action is ready, `false` otherwise.
     */
    protected fun ready(): Boolean = cool <= 0

    //#endregion #######################################################################################################


    //#region Private Methods ##########################################################################################

    /**
     * Converts a duration from seconds to the corresponding number of ticks.
     *
     * A tick is defined as 0.6 seconds in this context. The method divides the given
     * duration in seconds by 0.6, rounds the result, and returns it as an integer.
     * If the input is less than or equal to zero, the result is 0. The minimum
     * return value for a positive input is 1, ensuring that a non-zero duration
     * always corresponds to at least one tick.
     *
     * @param seconds The duration in seconds to be converted to ticks. Must be a non-negative double.
     * @return The equivalent number of ticks as a positive integer, or 0 if the input is less than or equal to zero.
     */
    private fun secondsToTicks(seconds: Double): Int {
        if (seconds <= 0.0) return 0
        return kotlin.math.round(seconds / 0.6).toInt().coerceAtLeast(1)
    }

    //#endregion #######################################################################################################

}