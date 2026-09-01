package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val createdAt: Long,
    val lastRunAt: Long,
    val runCount: Int,
    val isScheduled: Boolean,
    val scheduleTimeMinutes: Int,
    val scheduleDaysCsv: String,
    val isRepeatDaily: Boolean,
    val batteryModeName: String,
    val humanizeEnabled: Boolean,
    val jitterRadiusPx: Float,
    val timeVariancePercentage: Float,
    val naturalBezierCurves: Boolean,
    val antiBotScore: Int,
    val stepsJson: String,
    val tagsCsv: String
)

@Entity(tableName = "backup_snapshots")
data class BackupEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val scriptCount: Int,
    val totalRuns: Int,
    val jsonContent: String,
    val note: String
)
