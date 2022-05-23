package my.com.codeplay.smartlightdemo.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Device(
    @ColumnInfo(name = "device_id") val devId: String,
    @ColumnInfo(name = "product_id") val prodId: String,
    val name: String,
    @PrimaryKey(autoGenerate = true) var did: Int = 0
)
