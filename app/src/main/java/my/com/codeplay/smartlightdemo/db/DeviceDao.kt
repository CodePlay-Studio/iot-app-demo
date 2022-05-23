package my.com.codeplay.smartlightdemo.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM device")
    fun getAllDevice(): Flow<List<Device>>

    @Insert
    fun insert(vararg device: Device): LongArray

    @Delete
    fun delete(vararg device: Device): Int
}