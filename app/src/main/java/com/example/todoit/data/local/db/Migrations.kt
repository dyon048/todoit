package com.example.todoit.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add status column with default value PENDING
        db.execSQL(
            "ALTER TABLE todo_items ADD COLUMN status TEXT NOT NULL DEFAULT 'PENDING'"
        )
        // Add index for efficient status filtering
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_todo_items_status ON todo_items (status)"
        )
    }
}

