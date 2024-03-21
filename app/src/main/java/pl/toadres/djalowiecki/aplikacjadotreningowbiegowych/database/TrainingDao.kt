package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TrainingDao {
    @Insert
    suspend fun insert(training: Training)

    @Update
    suspend fun update(training: Training)

    @Query("SELECT * FROM training_table WHERE trainingId = :id")
    suspend fun get(id: Long): Training?

    @Query("DELETE FROM training_table")
    suspend fun clear()

    @Query("SELECT * FROM training_table ORDER BY trainingId DESC")
    fun getAllTrainings(): LiveData<List<Training>>

    @Query("SELECT * FROM training_table ORDER BY trainingId DESC LIMIT 1")
    suspend fun getLatestTraining(): Training?

    @Query("SELECT * FROM training_table WHERE trainingId = :id")
    fun getTrainingWithId(id: Long): LiveData<Training>

    @Query("UPDATE training_table SET distance = distance + :distance WHERE trainingId = :id")
    fun updateTrainingDistance(id: Long, distance: Int)

    @Query("UPDATE training_table SET end_time_milli = :endTimeMilli WHERE trainingId = :id")
    fun finishTraining(id: Long, endTimeMilli: Long)
}