package com.multisensor.recording.ui.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.multisensor.recording.R

object NavigationUtils {

    fun navigateToFragment(fragment: Fragment, destinationId: Int) {
        try {
            fragment.findNavController().navigate(destinationId)
        } catch (e: Exception) {

        }
    }

    fun launchActivity(context: Context, activityClass: Class<*>) {
        val intent = Intent(context, activityClass)
        context.startActivity(intent)
    }

    fun launchActivity(context: Context, activityClass: Class<*>, extras: Bundle) {
        val intent = Intent(context, activityClass)
        intent.putExtras(extras)
        context.startActivity(intent)
    }

    fun handleDrawerNavigation(navController: NavController, itemId: Int): Boolean {
        return try {
            when (itemId) {
                R.id.nav_recording, R.id.nav_devices, R.id.nav_calibration, R.id.nav_files -> {
                    navController.navigate(itemId)
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getCurrentDestinationName(navController: NavController): String {
        return when (navController.currentDestination?.id) {
            R.id.nav_recording -> "Recording"
            R.id.nav_devices -> "Devices"
            R.id.nav_calibration -> "Calibration"
            R.id.nav_files -> "Files"
            else -> "Unknown"
        }
    }

    fun canNavigateToDestination(navController: NavController, destinationId: Int): Boolean {
        return try {
            navController.graph.findNode(destinationId) != null
        } catch (e: Exception) {
            false
        }
    }
}
