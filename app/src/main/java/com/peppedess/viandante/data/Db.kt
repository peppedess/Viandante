package com.peppedess.viandante.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "visited")
data class VisitedPlace(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val region: String?,
    val imageUrl: String?,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

@Dao
interface VisitedDao {

    @Insert
    suspend fun insert(place: VisitedPlace)

    @Query("SELECT * FROM visited ORDER BY timestamp DESC")
    fun all(): Flow<List<VisitedPlace>>

    @Query("DELETE FROM visited")
    suspend fun clear()
}

@Database(entities = [VisitedPlace::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun visitedDao(): VisitedDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "viandante.db"
            ).build().also { instance = it }
        }
    }
}
