package com.gamehub.shared.graphics.platform

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.*

// ==================== Quality Tier Enum ====================
enum class QualityTier(
    val particleLimit: Int,
    val resolutionScale: Float,
    val fpsTarget: Int,
    val enableHeavyEffects: Boolean,
    val enableShaders: Boolean
) {
    LOW(20, 0.6f, 30, false, false),
    MEDIUM(50, 0.85f, 45, true, true),
    HIGH(100, 1.0f, 60, true, true)
}

// ==================== Thermal State Enum ====================
enum class ThermalState {
    NOMINAL,
    FAIR,
    SERIOUS,
    CRITICAL
}

// ==================== Battery State Enum ====================
enum class BatteryState {
    NORMAL,
    LOW,
    CHARGING
}

// ==================== Texture Compression Format ====================
enum class TextureCompressionFormat {
    ASTC,
    ETC2,
    ETC1,
    PVRTC,
    UNCOMPRESSED
}

// ==================== Platform Optimization Manager ====================
class PlatformOptimizationManager(
    private val targetFps: Int = 60
) {
    private val _qualityTier = MutableStateFlow(QualityTier.MEDIUM)
    val qualityTier: StateFlow<QualityTier> = _qualityTier

    private val _thermalState = MutableStateFlow(ThermalState.NOMINAL)
    val thermalState: StateFlow<ThermalState> = _thermalState

    private val _batteryState = MutableStateFlow(BatteryState.NORMAL)
    val batteryState: StateFlow<BatteryState> = _batteryState

    private val fpsHistory = mutableListOf<Int>()
    private val maxFpsHistory = 60

    // ------------------- Dynamic Resolution Scaling -------------------
    private var _drsScale = MutableStateFlow(1f)
    val drsScale: StateFlow<Float> = _drsScale

    fun updateFps(measuredFps: Int) {
        fpsHistory.add(measuredFps)
        if (fpsHistory.size > maxFpsHistory) fpsHistory.removeAt(0)

        updateThermalStateFromFps()
        updateDynamicResolution()
    }

    private fun updateThermalStateFromFps() {
        val avgFps = if (fpsHistory.isNotEmpty()) {
            fpsHistory.average().toInt()
        } else targetFps

        when {
            avgFps >= targetFps * 0.9 -> _thermalState.value = ThermalState.NOMINAL
            avgFps >= targetFps * 0.7 -> _thermalState.value = ThermalState.FAIR
            avgFps >= targetFps * 0.5 -> _thermalState.value = ThermalState.SERIOUS
            else -> _thermalState.value = ThermalState.CRITICAL
        }

        updateQualityTierFromStates()
    }

    private fun updateDynamicResolution() {
        val avgFps = fpsHistory.average().toFloat()
        val target = targetFps.toFloat()
        val scale = if (avgFps > target) {
            min(_drsScale.value + 0.02f, 1f)
        } else if (avgFps < target * 0.7) {
            max(_drsScale.value - 0.02f, 0.5f)
        } else {
            _drsScale.value
        }
        _drsScale.value = scale
    }

    // ------------------- Battery State -------------------
    fun updateBatteryState(newState: BatteryState) {
        _batteryState.value = newState
        updateQualityTierFromStates()
    }

    // ------------------- Thermal State (external setter for platform-specific code) -------------------
    fun setThermalState(newState: ThermalState) {
        _thermalState.value = newState
        updateQualityTierFromStates()
    }

    // ------------------- Quality Tier Decision Logic -------------------
    private fun updateQualityTierFromStates() {
        val baseTier = when (_batteryState.value) {
            BatteryState.LOW -> QualityTier.LOW
            BatteryState.NORMAL, BatteryState.CHARGING -> QualityTier.MEDIUM
        }

        val finalTier = when (_thermalState.value) {
            ThermalState.NOMINAL -> baseTier
            ThermalState.FAIR -> minQuality(baseTier, QualityTier.MEDIUM)
            ThermalState.SERIOUS -> QualityTier.LOW
            ThermalState.CRITICAL -> QualityTier.LOW
        }

        _qualityTier.value = finalTier
    }

    private fun minQuality(a: QualityTier, b: QualityTier): QualityTier {
        return if (a.ordinal < b.ordinal) a else b
    }

    // ------------------- Texture Compression Detection (Expect for platform specific) -------------------
    fun getBestTextureCompression(): TextureCompressionFormat = TextureCompressionFormat.UNCOMPRESSED

    // ------------------- Helpers for use in rendering -------------------
    fun getRenderResolutionScale(): Float = drsScale.value * qualityTier.value.resolutionScale
    fun getMaxParticleCount(): Int = qualityTier.value.particleLimit
    fun shouldRenderHeavyEffects(): Boolean = qualityTier.value.enableHeavyEffects
    fun shouldRenderCustomShaders(): Boolean = qualityTier.value.enableShaders
}
