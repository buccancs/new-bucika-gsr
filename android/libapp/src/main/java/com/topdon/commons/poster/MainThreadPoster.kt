package com.topdon.commons.poster

import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.annotation.NonNull
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

internal class MainThreadPoster : Handler(Looper.getMainLooper()), Poster {
    private val queue: Queue<Runnable> = ConcurrentLinkedQueue()
    private var handlerActive = false
    
    override fun enqueue(@NonNull runnable: Runnable) {
        Objects.requireNonNull(runnable, "runnable is null, cannot be enqueued")
        synchronized(this) {
            queue.add(runnable)
            if (!handlerActive) {
                handlerActive = true
                if (!sendMessage(obtainMessage())) {
                    throw RuntimeException("Could not send handler message")
                }
            }
        }
    }
    
    override fun clear() {
        synchronized(this) {
            queue.clear()
        }
    }
    
    override fun handleMessage(msg: Message) {
        try {
            while (true) {
                var runnable = queue.poll()
                if (runnable == null) {
                    synchronized(this) {
                        runnable = queue.poll()
                        if (runnable == null) {
                            handlerActive = false
                            return
                        }
                    }
                }
                runnable.run()
            }
        } finally {
            handlerActive = false
        }
    }
}