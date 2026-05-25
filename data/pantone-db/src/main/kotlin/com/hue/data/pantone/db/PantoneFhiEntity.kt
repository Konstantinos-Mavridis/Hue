package com.hue.data.pantone.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hue.core.color.model.ColorTemperature
import com.hue.core.color.model.LabColor
import com.hue.core.color.model.Season
import com.hue.domain.model.PantoneFhiColor

@Entity(
    tableName = "pantone_fhi",
    indices = [Index("code", unique = true), Index("name")]
)
data class PantoneFhiEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val labL: Double,
    val labA: Double,
    val labB: Double,
    val hex: String,
    val temperature: String,       // "WARM" | "COOL" | "NEUTRAL"
    val seasons: String            // comma-separated: "SPRING,AUTUMN"
) {
    fun toDomain() = PantoneFhiColor(
        id = id,
        code = code,
        name = name,
        lab = LabColor(labL, labA, labB),
        hex = hex,
        temperature = ColorTemperature.valueOf(temperature),
        seasons = seasons.split(",").map { Season.valueOf(it.trim()) }
    )
}
