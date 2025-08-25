package com.topdon.lib.core.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment

object ModernRouter {

    private val routes = mutableMapOf<String, Class<out Activity>>()
    
    fun register(path: String, activityClass: Class<out Activity>) {
        routes[path] = activityClass
    }
    
    fun build(path: String): NavigationBuilder {
        return NavigationBuilder(path, routes[path])
    }
    
    class NavigationBuilder(
        private val path: String,
        private val activityClass: Class<out Activity>?
    ) {
        private val extras = mutableMapOf<String, Any>()
        
        fun withString(key: String, value: String): NavigationBuilder {
            extras[key] = value
            return this
        }
        
        fun withInt(key: String, value: Int): NavigationBuilder {
            extras[key] = value
            return this
        }
        
        fun withBoolean(key: String, value: Boolean): NavigationBuilder {
            extras[key] = value
            return this
        }
        
        fun withSerializable(key: String, value: java.io.Serializable): NavigationBuilder {
            extras[key] = value
            return this
        }
        
        fun navigation(activity: Activity) {
            activityClass?.let { clazz ->
                val intent = Intent(activity, clazz)
                for (key in extras.keys) {
                    val value = extras[key]
                    when (value) {
                        is String -> intent.putExtra(key, value)
                        is Int -> intent.putExtra(key, value)
                        is Boolean -> intent.putExtra(key, value)
                        is java.io.Serializable -> intent.putExtra(key, value)
                    }
                }
                activity.startActivity(intent)
            }
        }
        
        fun navigation(fragment: Fragment) {
            fragment.context?.let { context ->
                navigation(context)
            }
        }
        
        fun navigation(context: Context) {
            activityClass?.let { clazz ->
                val intent = Intent(context, clazz)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                for (key in extras.keys) {
                    val value = extras[key]
                    when (value) {
                        is String -> intent.putExtra(key, value)
                        is Int -> intent.putExtra(key, value) 
                        is Boolean -> intent.putExtra(key, value)
                        is java.io.Serializable -> intent.putExtra(key, value)
                    }
                }
                context.startActivity(intent)
            }
        }
    }
