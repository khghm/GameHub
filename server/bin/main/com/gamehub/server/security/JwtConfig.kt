package com.gamehub.server.security

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val accessTokenValidityMs: Long,
    val refreshTokenValidityMs: Long
)