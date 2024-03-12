package pl.toadres.djalowiecki.aplikacjadotreningowbiegowych.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Training::class], version = 1, exportSchema = false)
abstract class RunnerAppDatabase : RoomDatabase() {
    abstract val trainingDao: TrainingDao

    companion object {
        @Volatile
        private var INSTANCE: RunnerAppDatabase? = null

        fun getInstance(context: Context): RunnerAppDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        RunnerAppDatabase::class.java,
                        "runner_app_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()

                    INSTANCE = instance
                }

                return instance
            }
        }
    }
}