package com.example.history

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.migration.Migration

/**
 * One row per successfully processed image (single or from a batch). Stores only what's
 * needed to show a useful history list - no file contents, no unnecessary personal data.
 * All fields are primitives/String, so no Room TypeConverter is required, which keeps this
 * schema simple to verify by inspection.
 */
@Entity(tableName = "compression_history")
data class CompressionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val originalFileName: String,
    val originalSizeBytes: Long,
    val finalSizeBytes: Long,
    val originalWidth: Int,
    val originalHeight: Int,
    val finalWidth: Int,
    val finalHeight: Int,
    val inputFormat: String,
    val outputFormat: String,
    val compressionMode: String,
    val settingValue: String = "",
    val targetReached: Boolean? = null,
    // String, not Uri - Uri isn't a Room-supported column type without a TypeConverter, and
    // the output lives in MediaStore/shared gallery storage anyway, so a plain string
    // reference is both simpler and doesn't imply the app manages private storage for it.
    val outputUriString: String?
) {
    val bytesSaved: Long get() = (originalSizeBytes - finalSizeBytes).coerceAtLeast(0)
    val percentSaved: Int get() = if (originalSizeBytes > 0) {
        ((bytesSaved.toDouble() / originalSizeBytes.toDouble()) * 100).toInt().coerceIn(0, 100)
    } else 0
}

@Dao
interface CompressionHistoryDao {
    @Insert
    suspend fun insert(entry: CompressionHistoryEntity): Long

    @Query("SELECT * FROM compression_history ORDER BY timestampMillis DESC")
    suspend fun getAll(): List<CompressionHistoryEntity>

    @Delete
    suspend fun delete(entry: CompressionHistoryEntity)

    @Query("DELETE FROM compression_history")
    suspend fun clearAll(): Int

    @Query("SELECT COUNT(*) FROM compression_history")
    suspend fun count(): Int

    @Query(
        "SELECT COALESCE(SUM(CASE WHEN finalSizeBytes < originalSizeBytes " +
            "THEN originalSizeBytes - finalSizeBytes ELSE 0 END), 0) FROM compression_history"
    )
    suspend fun totalBytesSaved(): Long
}

@Database(entities = [CompressionHistoryEntity::class], version = 2, exportSchema = false)
abstract class CompressionHistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): CompressionHistoryDao

    companion object {
        @Volatile private var instance: CompressionHistoryDatabase? = null

        fun getInstance(context: Context): CompressionHistoryDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CompressionHistoryDatabase::class.java,
                    "compression_history.db"
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE compression_history ADD COLUMN settingValue TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE compression_history ADD COLUMN targetReached INTEGER DEFAULT NULL"
                )
                db.execSQL(
                    "UPDATE compression_history SET compressionMode = 'target_size' " +
                        "WHERE compressionMode NOT IN ('quality', 'target_size')"
                )
            }
        }
    }
}
