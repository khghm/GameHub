package com.gamehub.shared.graphics.physics

/**
 * رابط (interface) برای شنود رویدادهای برخورد.
 * متدها به‌صورت پیش‌فرض خالی هستند تا پیاده‌سازی اختیاری باشد.
 */
interface PhysicsContactListener {
    /** هنگامی که دو جسم شروع به تماس می‌کنند فراخوانی می‌شود. */
    fun onContactEnter(manifold: ContactManifold) {}

    /** در هر فریمی که تماس همچنان برقرار است فراخوانی می‌شود. */
    fun onContactStay(manifold: ContactManifold) {}

    /** هنگامی که دو جسم از تماس خارج می‌شوند فراخوانی می‌شود. */
    fun onContactExit(bodyA: PhysicsBody, bodyB: PhysicsBody) {}

    /** برای سنسورها: وقتی جسمی وارد ناحیه سنسور می‌شود. */
    fun onSensorEnter(sensor: PhysicsBody, other: PhysicsBody) {}

    /** برای سنسورها: وقتی جسمی از ناحیه سنسور خارج می‌شود. */
    fun onSensorExit(sensor: PhysicsBody, other: PhysicsBody) {}
}

/**
 * رابط برای شنود رویدادهای افزودن/حذف بدنه.
 */
interface PhysicsBodyListener {
    fun onBodyAdded(body: PhysicsBody) {}
    fun onBodyRemoved(body: PhysicsBody) {}
}