package com.recipetracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/*
 * IMPORTANT for future versions: this database holds the user's recipe history.
 * When the schema changes, bump the version and add a Migration (or an
 * AutoMigration, using the exported schemas in app/schemas). Never fall back to a
 * destructive migration.
 */
@Database(
    entities = [RecipeEntity::class, IterationEntity::class, PhotoEntity::class, RatingEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "recipes.db").build()

        fun inMemory(context: Context): AppDatabase =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }
}
