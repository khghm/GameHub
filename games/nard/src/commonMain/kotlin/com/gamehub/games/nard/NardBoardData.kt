package com.gamehub.games.nard

// Nard board coordinate data 
// Note: Adjust these coordinates if your custom nard.jpg has different dimensions
data class NardCoord(
    val xPercent: Float,
    val yPercent: Float
)

object NardBoardData {
    // Coordinates for each of the 24 points, bar areas, and bear off
    val pointCoords = mapOf(
        // Top row left (points 13-18) - starting from leftmost on top
        "13" to NardCoord(9.5f, 12.0f),
        "14" to NardCoord(16.5f, 12.0f),
        "15" to NardCoord(23.5f, 12.0f),
        "16" to NardCoord(30.5f, 12.0f),
        "17" to NardCoord(37.5f, 12.0f),
        "18" to NardCoord(44.5f, 12.0f),
        // Top row right (points 19-24)
        "19" to NardCoord(55.5f, 12.0f),
        "20" to NardCoord(62.5f, 12.0f),
        "21" to NardCoord(69.5f, 12.0f),
        "22" to NardCoord(76.5f, 12.0f),
        "23" to NardCoord(83.5f, 12.0f),
        "24" to NardCoord(90.5f, 12.0f),
        // Bottom row left (points 12-7) - starting from leftmost on bottom
        "12" to NardCoord(9.5f, 88.0f),
        "11" to NardCoord(16.5f, 88.0f),
        "10" to NardCoord(23.5f, 88.0f),
        "9"  to NardCoord(30.5f, 88.0f),
        "8"  to NardCoord(37.5f, 88.0f),
        "7"  to NardCoord(44.5f, 88.0f),
        // Bottom row right (points 6-1)
        "6"  to NardCoord(55.5f, 88.0f),
        "5"  to NardCoord(62.5f, 88.0f),
        "4"  to NardCoord(69.5f, 88.0f),
        "3"  to NardCoord(76.5f, 88.0f),
        "2"  to NardCoord(83.5f, 88.0f),
        "1"  to NardCoord(90.5f, 88.0f),
        // Bar areas
        "bar_white" to NardCoord(50.0f, 45.0f),
        "bar_black" to NardCoord(50.0f, 55.0f),
        // Bear off areas
        "bear_white" to NardCoord(3.0f, 50.0f),
        "bear_black" to NardCoord(97.0f, 50.0f)
    )

    // Return the list of point indices (1-24) in order
    val allPoints = listOf(
        24, 23, 22, 21, 20, 19,
        18, 17, 16, 15, 14, 13,
        12, 11, 10, 9, 8, 7,
        6, 5, 4, 3, 2, 1
    )
}
