package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "training_table")
data class Training(
    @PrimaryKey(autoGenerate = true)
    var trainingId: Long = 0L,

    @ColumnInfo(name = "target_time_milli")
    var targetTimeMilli: Long? = null,

    @ColumnInfo(name = "target_distance")
    var targetDistance: Int? = null,

    @ColumnInfo(name = "start_time_milli")
    val startTimeMilli: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "end_time_milli")
    var endTimeMilli: Long? = null,

    @ColumnInfo(name = "distance")
    var distance: Int = 0,
) : Parcelable