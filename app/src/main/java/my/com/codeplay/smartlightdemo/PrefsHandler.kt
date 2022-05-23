package my.com.codeplay.smartlightdemo

import android.content.Context

class PrefsHandler(context: Context) {
    var username: String
        get() = homePrefs.getString(usernameKey, "User")!!
        set(value) = homePrefs.edit().putString(usernameKey, value).apply()

    var homeId: Long
        get() = homePrefs.getLong(homeIdKey, 0)
        set(value) = homePrefs.edit().putLong(homeIdKey, value).apply()

    private val homePrefsFile = "HomePrefs"
    private val homePrefs =
        context.applicationContext.getSharedPreferences(homePrefsFile, Context.MODE_PRIVATE)
    private val usernameKey = "key_username"
    private val homeIdKey = "key_home_id"
}