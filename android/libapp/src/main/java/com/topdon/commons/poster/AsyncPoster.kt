package com.topdon.commons.poster

import androidx.annotation.NonNull
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService

internal class AsyncPoster(@NonNull private val executorService: ExecutorService) : Runnable, Poster {
    private val queue: Queue<Runnable> = ConcurrentLinkedQueue()
    
    override fun enqueue(@NonNull runnable: Runnable) {
        requireNotNull(runnable) { "runnable is null, cannot be enqueued" }
        queue.add(runnable)
        executorService.execute(this)
    }
    
    override fun clear() {
        synchronized(this) {
            queue.clear()
        }
    }
    
    override fun run() {
        queue.poll()?.run()
    }
}
