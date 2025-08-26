package com.topdon.commons.poster

import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService

internal class BackgroundPoster(private val executorService: ExecutorService) : Runnable, Poster {
    private val queue: Queue<Runnable> = ConcurrentLinkedQueue()
    @Volatile
    private var executorRunning = false

    override fun enqueue(runnable: Runnable) {
        Objects.requireNonNull(runnable, "runnable is null, cannot be enqueued")
        synchronized(this) {
            queue.add(runnable)
            if (!executorRunning) {
                executorRunning = true
                executorService.execute(this)
            }
        }
    }

    override fun clear() {
        synchronized(this) {
            queue.clear()
        }
    }

    override fun run() {
        try {
            while (true) {
                var runnable = queue.poll()
                if (runnable == null) {
                    synchronized(this) {
                        runnable = queue.poll()
                        if (runnable == null) {
                            executorRunning = false
                            return
                        }
                    }
                }
                runnable.run()
            }
        } finally {
            executorRunning = false
        }
    }
}