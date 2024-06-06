package eu.tortitas.stash

import java.time.DayOfWeek
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs

fun calculateDaysTillNextRun(dayOfWeek: DayOfWeek, nowDayOfWeek: DayOfWeek): Int {
    val dayOfWeekNextWeek = DayOfWeek.of(dayOfWeek.ordinal + 7)

    val daysTillNextRun = abs(dayOfWeekNextWeek.ordinal - nowDayOfWeek.ordinal)
    return daysTillNextRun + 1
}

class Scheduler(private val task: Runnable) {
    private val executor = Executors.newScheduledThreadPool(1)!!

    fun scheduleExecution(every: Every) {
        val taskWrapper = Runnable {
            task.run()
        }

        executor.scheduleWithFixedDelay(taskWrapper, every.n, every.n, every.unit)
    }

    fun scheduleExecution(everyDayOfWeek: EveryDayOfWeek) {
        val now = ZonedDateTime.now(ZoneId.of("Europe/Madrid"))
        var nextRun = now
            .withHour(everyDayOfWeek.hour)
            .withMinute(0)
            .withSecond(0);

        if (everyDayOfWeek.dayOfWeek != null) {
            val nowDayOfWeek = now.dayOfWeek

            val daysTillNextRun = calculateDaysTillNextRun(everyDayOfWeek.dayOfWeek, nowDayOfWeek)
            nextRun = nextRun.plusDays(daysTillNextRun.toLong())
        } else {
            nextRun = nextRun.plusDays(1)
        }

        val duration: Duration = Duration.between(now, nextRun)
        val initialDelay: Long = duration.getSeconds()

        val taskWrapper = Runnable {
            task.run()
        }

        executor.scheduleWithFixedDelay(taskWrapper, initialDelay, 7, TimeUnit.DAYS)
    }


    fun stop() {
        executor.shutdown()

        try {
            executor.awaitTermination(1, TimeUnit.HOURS)
        } catch (e: InterruptedException) {
        }

    }
}

data class Every(val n: Long, val unit: TimeUnit)
data class EveryDayOfWeek(val dayOfWeek: DayOfWeek?, val hour: Int)
