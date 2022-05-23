package my.com.codeplay.smartlightdemo

import android.app.Application
import com.tuya.smart.home.sdk.TuyaHomeSdk

class CPSmartLightDemo : Application() {
    override fun onCreate() {
        super.onCreate()

        TuyaHomeSdk.init(this)
        TuyaHomeSdk.setDebugMode(BuildConfig.DEBUG)
        /* do we needs this?
        TuyaHomeSdk.setOnNeedLoginListener {

        }*/
    }
}