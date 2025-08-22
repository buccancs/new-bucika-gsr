@file:Suppress("DEPRECATION")

package com.multisensor.recording.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log

object NetworkUtils {

    private const val TAG = "NetworkUtils"

    fun isNetworkConnected(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network connectivity: ${e.message}")
            false
        }
    }

    fun getNetworkType(context: Context): String {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork == null) return "Disconnected"

            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            if (networkCapabilities == null) return "Not Connected"

            when {
                !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> "Not Connected"
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    getCellularNetworkType(context)
                }

                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Other"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting network type: ${e.message}")
            "Unknown"
        }
    }

    private fun getCellularNetworkType(context: Context): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                when (telephonyManager.dataNetworkType) {
                    TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
                    TelephonyManager.NETWORK_TYPE_HSDPA,
                    TelephonyManager.NETWORK_TYPE_HSUPA,
                    TelephonyManager.NETWORK_TYPE_HSPA -> "3G"

                    TelephonyManager.NETWORK_TYPE_EDGE,
                    TelephonyManager.NETWORK_TYPE_GPRS -> "2G"

                    else -> "Mobile"
                }
            } else {
                "Mobile"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cellular network type: ${e.message}")
            "Mobile"
        }
    }
}
