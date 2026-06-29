package com.gamehub.shared.graphics

import com.gamehub.shared.graphics.platform.PlatformOptimizationManager
import com.gamehub.shared.graphics.platform.QualityTier

enum class DeviceTier {
    LOW,    // دستگاه‌های ضعیف (RAM < 3GB)
    MEDIUM, // دستگاه‌های متوسط
    HIGH    // دستگاه‌های قوی
}

object DeviceTierDetector {
    private var baseTier: DeviceTier = DeviceTier.MEDIUM
    private var currentTier: DeviceTier = DeviceTier.MEDIUM
    private var fpsHistory = mutableListOf<Int>()
    private const val MAX_FPS_HISTORY = 30

    var optimizationManager: PlatformOptimizationManager = PlatformOptimizationManager()

    fun initialize(
        totalRamGb: Double,
        isLowEndDevice: Boolean = false,
        optimizationManager: PlatformOptimizationManager = PlatformOptimizationManager()
    ) {
        this.optimizationManager = optimizationManager
        baseTier = when {
            isLowEndDevice || totalRamGb < 3.0 -> DeviceTier.LOW
            totalRamGb < 6.0 -> DeviceTier.MEDIUM
            else -> DeviceTier.HIGH
        }
        currentTier = baseTier
        fpsHistory.clear()
    }

    fun updateFps(fps: Int) {
        fpsHistory.add(fps)
        if (fpsHistory.size > MAX_FPS_HISTORY) {
            fpsHistory.removeAt(0)
        }
        optimizationManager.updateFps(fps)

        if (fpsHistory.size >= 10) {
            val avgFps = fpsHistory.average().toInt()
            currentTier = when {
                avgFps < 20 -> DeviceTier.LOW
                avgFps < 40 -> DeviceTier.MEDIUM
                else -> baseTier
            }
        }
    }

    fun getCurrentTier(): DeviceTier = currentTier

    fun isLowTier(): Boolean = getCurrentTier() == DeviceTier.LOW ||
            optimizationManager.qualityTier.value == QualityTier.LOW
    fun isHighTier(): Boolean = getCurrentTier() == DeviceTier.HIGH &&
            optimizationManager.qualityTier.value == QualityTier.HIGH

    fun shouldEnableHeavyEffects(): Boolean = optimizationManager.shouldRenderHeavyEffects()
    fun getMaxParticleCount(): Int = optimizationManager.getMaxParticleCount()

    fun getAnimationDurationScale(): Float = when (getCurrentTier()) {
        DeviceTier.LOW -> 0.7f
        else -> 1.0f
    }
}
