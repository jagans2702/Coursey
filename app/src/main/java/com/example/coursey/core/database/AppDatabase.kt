package com.example.coursey.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppDatabase(context: Context) : SQLiteOpenHelper(context.applicationContext, NAME, null, VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CourseTable.CREATE)
        db.execSQL(LessonTable.CREATE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS ${LessonTable.NAME}")
        db.execSQL("DROP TABLE IF EXISTS ${CourseTable.NAME}")
        onCreate(db)
    }

    private companion object {
        const val NAME = "learning_dashboard.db"
        const val VERSION = 1
    }
}
