package com.gamehub.server.featureflags;

import kotlinx.serialization.Contextual;
import kotlinx.serialization.Serializable;
import java.time.Instant;
import java.util.UUID;

@Serializable
data class FeatureFlag(
    @Contextual val id: UUID,
    val key: String,
    val name: String,
    val description: String? = null,
    val isEnabled: Boolean,
    val environment: String? = null,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant
)

@Serializable
data class CreateFeatureFlagRequest(
    val key: String,
    val name: String,
    val description: String? = null,
    val isEnabled: Boolean = false,
    val environment: String? = null
)

@Serializable
data class UpdateFeatureFlagRequest(
    val name: String? = null,
    val description: String? = null,
    val isEnabled: Boolean? = null,
    val environment: String? = null
)
