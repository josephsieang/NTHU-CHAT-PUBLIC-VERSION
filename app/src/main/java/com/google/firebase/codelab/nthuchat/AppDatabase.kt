package com.google.firebase.codelab.nthuchat

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context

@Database(entities = arrayOf(User::class), version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {

        private var INSTANCE: AppDatabase? = null

        fun getAppDatabase(context: Context?): AppDatabase? {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context!!.applicationContext, AppDatabase::class.java, "user-database")
                        // allow queries on the main thread.
                        // Don't do this on a real app! See PersistenceBasicSample for an example.
                        .allowMainThreadQueries()
                        .build()
            }
            return INSTANCE
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}