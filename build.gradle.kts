plugins {
    id("org.jetbrains.kotlin.multiplatform") version libs.versions.kotlin.get() apply false
    id("org.jetbrains.kotlin.android") version libs.versions.kotlin.get() apply false
    id("org.jetbrains.kotlin.jvm") version libs.versions.kotlin.get() apply false
    id("org.jetbrains.kotlin.plugin.serialization") version libs.versions.kotlin.get() apply false
    id("org.jetbrains.kotlin.plugin.compose") version libs.versions.kotlin.get() apply false
    id("com.android.application") version libs.versions.agp.get() apply false
    id("com.android.library") version libs.versions.agp.get() apply false
    alias(libs.plugins.android.dynamic.feature) apply false
}