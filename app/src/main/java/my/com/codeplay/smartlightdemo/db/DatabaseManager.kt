package my.com.codeplay.smartlightdemo.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Device::class], version = 1)
abstract class DatabaseManager: RoomDatabase() {
    abstract fun deviceDao(): DeviceDao

    companion object {
        @Volatile private var instance: DatabaseManager? = null

        fun getInstance(context: Context): DatabaseManager {
            synchronized(this) {
                var localInstance: DatabaseManager? = instance

                if (localInstance == null) {
                    localInstance = Room.databaseBuilder(
                        context.applicationContext,
                        DatabaseManager::class.java,
                        "app-database.db"
                    ).build()

                    instance = localInstance
                }
                return localInstance
            }
        }
    }
}