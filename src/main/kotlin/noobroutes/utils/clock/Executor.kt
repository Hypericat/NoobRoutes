package noobroutes.utils.clock

import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.utils.profile

/**
 * Class that allows repeating execution of code while being dynamic.
 * @author Stivais
 */
open class Executor(val delay: () -> Long, private val profileName: String = "Unspecified NoobRoutes executor", val shouldRun: () -> Boolean = { true }, val func: Executable) {

    constructor(delay: Long, profileName: String = "Unspecified NoobRoutes executor", shouldRun: () -> Boolean = { true }, func: Executable) : this({ delay }, profileName, shouldRun, func)

    internal val clock = Clock()
    internal var shouldFinish = false

    open fun run(): Boolean {
        if (shouldFinish) return true
        if (clock.hasTimePassed(delay(), true)) {
            profile(profileName) {
                runCatching {
                    func()
                }
            }
        }
        return false
    }

    /**
     * Starts an executor that ends after a certain number of times.
     */
    class LimitedExecutor(delay: Long, repeats: Int, profileName: String = "Unspecified NoobRoutes executor", shouldRun: () -> Boolean = { true }, func: Executable) : Executor(delay, profileName, shouldRun, func) {
        private val repeats = repeats - 1
        private var totalRepeats = 0

        override fun run(): Boolean {
            if (shouldFinish) return true
            if (clock.hasTimePassed(delay(), true)) {
                runCatching {
                    if (totalRepeats >= repeats) destroyExecutor()
                    totalRepeats++
                    func()
                }
            }
            return false
        }
    }

    /**
     * Allows stopping executing an executor permanently
     *
     * Returning [Nothing] allows for us to stop running the function without specifying
     * @author Stivais
     */
    fun Executor.destroyExecutor(): Nothing {
        shouldFinish = true
        throw Throwable()
    }

    companion object {
        private val executors = ArrayList<Executor>()

        fun Executor.register() {
            executors.add(this)
        }

        @SubscribeEvent
        fun onRender(event: RenderWorldLastEvent) {
            profile("Executors") {
                executors.removeAll {
                    if (!it.shouldRun()) return@removeAll false
                    else it.run()
                }
            }
        }
    }
}

/**
 * Here for more readability
 */
typealias Executable = Executor.() -> Unit