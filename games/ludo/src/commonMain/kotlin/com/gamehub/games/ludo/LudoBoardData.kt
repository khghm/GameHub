package com.gamehub.games.ludo

// نگاشت هر خانه به مختصات درصدی
object LudoBoardData {
    data class CellCoord(val xPercent: Float, val yPercent: Float)

    val cellCoords = mapOf(
        // آبی
        "bp1" to CellCoord(26.60f, 86.45f), "bp2" to CellCoord(26.60f, 73.55f),
        "bp3" to CellCoord(13.60f, 86.45f), "bp4" to CellCoord(13.60f, 73.55f),
        "bs"  to CellCoord(43.20f, 89.60f), "b1"  to CellCoord(43.10f, 83.10f),
        "b2"  to CellCoord(43.10f, 76.50f), "b3"  to CellCoord(43.10f, 69.90f),
        "b4"  to CellCoord(43.10f, 63.20f), "b5"  to CellCoord(36.50f, 56.75f),
        "b6"  to CellCoord(29.85f, 56.75f), "b7"  to CellCoord(23.30f, 56.75f),
        "b8"  to CellCoord(16.70f, 56.75f), "b9"  to CellCoord(10.20f, 56.75f),
        "b10" to CellCoord(3.70f, 56.75f), "b11" to CellCoord(3.70f, 50.10f),
        "b12" to CellCoord(3.70f, 43.30f),
        "be1" to CellCoord(49.80f, 89.60f), "be2" to CellCoord(49.90f, 83.10f),
        "be3" to CellCoord(49.90f, 76.50f), "be4" to CellCoord(49.90f, 69.90f),
        "be5" to CellCoord(49.90f, 63.20f), "be6" to CellCoord(49.95f, 56.80f),
        // قرمز
        "rp1" to CellCoord(13.65f, 27.15f), "rp2" to CellCoord(26.50f, 27.15f),
        "rp3" to CellCoord(13.65f, 14.15f), "rp4" to CellCoord(26.50f, 14.15f),
        "rs"  to CellCoord(10.30f, 43.40f), "r1"  to CellCoord(16.65f, 43.40f),
        "r2"  to CellCoord(23.25f, 43.30f), "r3"  to CellCoord(29.85f, 43.30f),
        "r4"  to CellCoord(36.50f, 43.30f), "r5"  to CellCoord(43.05f, 36.75f),
        "r6"  to CellCoord(43.05f, 30.10f), "r7"  to CellCoord(43.05f, 23.50f),
        "r8"  to CellCoord(43.15f, 16.90f), "r9"  to CellCoord(43.15f, 10.40f),
        "r10" to CellCoord(43.05f, 3.90f), "r11" to CellCoord(49.75f, 3.90f),
        "r12" to CellCoord(56.55f, 3.90f),
        "re1" to CellCoord(10.20f, 50.10f), "re2" to CellCoord(16.70f, 50.10f),
        "re3" to CellCoord(23.30f, 50.10f), "re4" to CellCoord(29.85f, 50.10f),
        "re5" to CellCoord(36.50f, 50.10f), "re6" to CellCoord(43.05f, 49.90f),
        // سبز
        "gp1" to CellCoord(73.05f, 13.45f), "gp2" to CellCoord(86.05f, 13.45f),
        "gp3" to CellCoord(73.05f, 26.35f), "gp4" to CellCoord(86.05f, 26.35f),
        "gs"  to CellCoord(56.45f, 10.40f), "g1"  to CellCoord(56.45f, 16.90f),
        "g2"  to CellCoord(56.55f, 23.50f), "g3"  to CellCoord(56.55f, 30.10f),
        "g4"  to CellCoord(56.55f, 36.75f), "g5"  to CellCoord(63.25f, 43.15f),
        "g6"  to CellCoord(69.90f, 43.15f), "g7"  to CellCoord(76.50f, 43.15f),
        "g8"  to CellCoord(83.10f, 43.25f), "g9"  to CellCoord(89.60f, 43.15f),
        "g10" to CellCoord(96.10f, 43.15f), "g11" to CellCoord(96.10f, 49.85f),
        "g12" to CellCoord(96.10f, 56.65f),
        "ge1" to CellCoord(49.75f, 10.40f), "ge2" to CellCoord(49.80f, 16.85f),
        "ge3" to CellCoord(49.75f, 23.50f), "ge4" to CellCoord(49.75f, 30.10f),
        "ge5" to CellCoord(49.75f, 36.75f), "ge6" to CellCoord(50.05f, 43.20f),
        // زرد
        "yp1" to CellCoord(86.35f, 73.05f), "yp2" to CellCoord(73.45f, 73.05f),
        "yp3" to CellCoord(86.35f, 86.05f), "yp4" to CellCoord(73.45f, 86.05f),
        "ys"  to CellCoord(89.50f, 56.55f), "y1"  to CellCoord(83.10f, 56.55f),
        "y2"  to CellCoord(76.50f, 56.65f), "y3"  to CellCoord(69.90f, 56.65f),
        "y4"  to CellCoord(63.25f, 56.65f), "y5"  to CellCoord(56.60f, 63.20f),
        "y6"  to CellCoord(56.60f, 69.90f), "y7"  to CellCoord(56.60f, 76.50f),
        "y8"  to CellCoord(56.40f, 83.10f), "y9"  to CellCoord(56.40f, 89.60f),
        "y10" to CellCoord(56.60f, 96.10f), "y11" to CellCoord(49.90f, 96.10f),
        "y12" to CellCoord(43.10f, 96.10f),
        "ye1" to CellCoord(89.60f, 49.95f), "ye2" to CellCoord(83.10f, 49.95f),
        "ye3" to CellCoord(76.50f, 49.85f), "ye4" to CellCoord(69.90f, 49.85f),
        "ye5" to CellCoord(63.25f, 49.85f), "ye6" to CellCoord(56.55f, 50.20f)
    )

    // مسیرهای کامل هر رنگ (لیست خانه‌ها)
    val paths = mapOf(
        "blue" to listOf(
            "bs","b1","b2","b3","b4","b5","b6","b7","b8","b9","b10","b11","b12",
            "rs","r1","r2","r3","r4","r5","r6","r7","r8","r9","r10","r11","r12",
            "gs","g1","g2","g3","g4","g5","g6","g7","g8","g9","g10","g11","g12",
            "ys","y1","y2","y3","y4","y5","y6","y7","y8","y9","y10","y11",
            "be1","be2","be3","be4","be5","be6"
        ),
        "red" to listOf(
            "rs","r1","r2","r3","r4","r5","r6","r7","r8","r9","r10","r11","r12",
            "gs","g1","g2","g3","g4","g5","g6","g7","g8","g9","g10","g11","g12",
            "ys","y1","y2","y3","y4","y5","y6","y7","y8","y9","y10","y11","y12",
            "bs","b1","b2","b3","b4","b5","b6","b7","b8","b9","b10","b11",
            "re1","re2","re3","re4","re5","re6"
        ),
        "green" to listOf(
            "gs","g1","g2","g3","g4","g5","g6","g7","g8","g9","g10","g11","g12",
            "ys","y1","y2","y3","y4","y5","y6","y7","y8","y9","y10","y11","y12",
            "bs","b1","b2","b3","b4","b5","b6","b7","b8","b9","b10","b11","b12",
            "rs","r1","r2","r3","r4","r5","r6","r7","r8","r9","r10","r11",
            "ge1","ge2","ge3","ge4","ge5","ge6"
        ),
        "yellow" to listOf(
            "ys","y1","y2","y3","y4","y5","y6","y7","y8","y9","y10","y11","y12",
            "bs","b1","b2","b3","b4","b5","b6","b7","b8","b9","b10","b11","b12",
            "rs","r1","r2","r3","r4","r5","r6","r7","r8","r9","r10","r11","r12",
            "gs","g1","g2","g3","g4","g5","g6","g7","g8","g9","g10","g11",
            "ye1","ye2","ye3","ye4","ye5","ye6"
        )
    )

    // رنگ‌های متناظر با اندیس بازیکن (ترتیب نوبت: blue, red, green, yellow)
    val playerColors = listOf("blue", "red", "green", "yellow")
}