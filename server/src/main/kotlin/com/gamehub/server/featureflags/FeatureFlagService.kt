package com.gamehub.server.featureflags

import com.gamehub.server.repository.FeatureFlagRepository
import java.util.UUID

class FeatureFlagService(
    private val repository: FeatureFlagRepository
) {

    suspend fun getAllFeatureFlags(environment: String? = null): List<FeatureFlag> {
        return repository.getAll(environment)
    }

    suspend fun getFeatureFlagById(id: UUID): FeatureFlag? {
        return repository.getById(id)
    }

    suspend fun getFeatureFlagByKey(key: String): FeatureFlag? {
        return repository.getByKey(key)
    }

    suspend fun isFeatureEnabled(key: String, environment: String? = null): Boolean {
        val flag = repository.getByKey(key) ?: return false
        if (!flag.isEnabled) return false
        if (environment != null && flag.environment != null && flag.environment != environment) {
            return false
        }
        return true
    }

    suspend fun createFeatureFlag(request: CreateFeatureFlagRequest): FeatureFlag {
        // Check if key already exists
        repository.getByKey(request.key)?.let {
            throw IllegalArgumentException("Feature flag with key '${request.key}' already exists")
        }
        return repository.create(
            key = request.key,
            name = request.name,
            description = request.description,
            isEnabled = request.isEnabled,
            environment = request.environment
        )
    }

    suspend fun updateFeatureFlag(id: UUID, request: UpdateFeatureFlagRequest): FeatureFlag? {
        return repository.update(
            id = id,
            name = request.name,
            description = request.description,
            isEnabled = request.isEnabled,
            environment = request.environment
        )
    }

    suspend fun deleteFeatureFlag(id: UUID): Boolean {
        return repository.delete(id)
    }
}
