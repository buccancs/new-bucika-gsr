package com.topdon.commons.base

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import androidx.annotation.CallSuper
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

object AppHolder : Application.ActivityLifecycleCallbacks {

    private val runningActivities = CopyOnWriteArrayList<RunningActivity>()
    private var isCompleteExit = false
    private var application: Application? = null
    private var mainLooper: Looper? = null
    private var topActivity: RunningActivity? = null

    init {
        mainLooper = Looper.getMainLooper()
        application = tryGetApplication()
        application?.registerActivityLifecycleCallbacks(this)
    }

    private data class RunningActivity(
        val name: String,
        val weakActivity: WeakReference<Activity>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RunningActivity) return false
            return name == other.name
        }

        override fun hashCode(): Int = Objects.hash(name)
    }

    @SuppressLint("PrivateApi")
    @Nullable
    private fun tryGetApplication(): Application? {
        return try {
            val cls = Class.forName("android.app.ActivityThread")
            val catMethod = cls.getMethod("currentActivityThread")
            catMethod.isAccessible = true
            val aThread = catMethod.invoke(null)
            val method = aThread.javaClass.getMethod("getApplication")
            method.invoke(aThread) as Application
        } catch (e: Exception) {
            null
        }
    }

    @CallSuper
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        val runningActivity = RunningActivity(activity.javaClass.name, WeakReference(activity))
        if (!runningActivities.contains(runningActivity)) {
            runningActivities.add(runningActivity)
        }
        topActivity = runningActivity
    }

    @CallSuper
    override fun onActivityStarted(activity: Activity) {}

    @CallSuper
    override fun onActivityResumed(activity: Activity) {}

    @CallSuper
    override fun onActivityPaused(activity: Activity) {}

    @CallSuper
    override fun onActivityStopped(activity: Activity) {}

    @CallSuper
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    @CallSuper
    override fun onActivityDestroyed(activity: Activity) {
        if (runningActivities.isEmpty()) {
            topActivity = null
        }
        val runningActivity = RunningActivity(activity.javaClass.name, WeakReference(activity))
        runningActivities.remove(runningActivity)
        if (isCompleteExit && runningActivities.isEmpty()) {
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(0)
        }
    }

    fun initialize(@NonNull application: Application) {
        Objects.requireNonNull(application, "application is null")

        this.application?.let { oldApp ->
            if (oldApp != application) {
                oldApp.unregisterActivityLifecycleCallbacks(this)
                application.registerActivityLifecycleCallbacks(this)
            }
        }
        this.application = application
    }

    fun isMainThread(): Boolean = Looper.myLooper() == mainLooper

    @NonNull
    fun getMainLooper(): Looper {
        if (mainLooper == null) {
            mainLooper = Looper.getMainLooper()
        }
        return mainLooper!!
    }

    @NonNull
    fun getContext(): Context {
        return application ?: throw IllegalStateException(
            "The AppHolder has not been initialized, make sure to call AppHolder.initialize(app) first."
        )
    }

    @Nullable
    fun getPackageInfo(): PackageInfo? {
        return try {
            application?.let { app ->
                val pm = app.packageManager
                pm.getPackageInfo(app.packageName, 0)
            }
        } catch (ignore: Exception) {
            null
        }
    }

    fun isAppOnForeground(): Boolean {
        val am = application?.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val processes = am?.runningAppProcesses
        return processes?.any { process ->
            application?.packageName == process.processName &&
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND == process.importance
        } ?: false
    }

    private fun contains(array: Array<out Any>?, obj: Any): Boolean {
        return array?.any { it == obj } ?: false
    }

    fun finish(className: String, vararg classNames: String) {
        val list = runningActivities.reversed()
        list.forEach { runningActivity ->
            runningActivity.weakActivity.get()?.let { activity ->
                val name = activity.javaClass.name
                if (name == className || contains(classNames, name)) {
                    activity.finish()
                }
            }
        }
    }

    fun finishAllWithout(@Nullable className: String?, vararg classNames: String) {
        val list = runningActivities.reversed()
        list.forEach { runningActivity ->
            runningActivity.weakActivity.get()?.let { activity ->
                val name = activity.javaClass.name
                if (name != className && !contains(classNames, name)) {
                    activity.finish()
                }
            }
        }
    }

    fun finishAll() {
        finishAllWithout(null)
    }

    fun backTo(className: String) {
        val list = runningActivities.reversed()
        list.forEach { runningActivity ->
            runningActivity.weakActivity.get()?.let { activity ->
                val name = activity.javaClass.name
                if (name == className) {
                    activity.finish()
                    return
                }
            }
        }
    }

    @Nullable
    fun getActivity(className: String): Activity? {
        return runningActivities.find { it.name == className }?.weakActivity?.get()
    }

    fun isAllFinished(): Boolean = runningActivities.isEmpty()

    fun getAllActivities(): List<Activity> {
        return runningActivities.mapNotNull { it.weakActivity.get() }
    }

    fun completeExit() {
        isCompleteExit = true
        val list = runningActivities.reversed()
        list.forEach { runningActivity ->
            runningActivity.weakActivity.get()?.finish()
        }
    }

    fun getTopActivity(): Activity? = topActivity?.weakActivity?.get()
}