package my.com.codeplay.smartlightdemo.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity

@Suppress("DEPRECATION")
fun AppCompatActivity.isWiFiConnected(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val isConnected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    } else {
        val networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        networkInfo?.isConnected
    }
    return isConnected == true
}