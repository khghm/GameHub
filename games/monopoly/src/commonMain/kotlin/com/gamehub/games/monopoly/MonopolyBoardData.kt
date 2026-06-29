package com.gamehub.games.monopoly

import kotlin.random.Random

object MonopolyBoardData {
    const val CELL_COUNT = 40
    const val START_SALARY = 200
    const val WIN_TARGET = 5000

    enum class CellType {
        START, PROPERTY, CHANCE, BANK, TRANSPORT, TEMPORARY, DEAL, MISSION, JAIL
    }

    data class CellInfo(
        val name: String,
        val type: CellType,
        val price: Int = 0,
        val rentBase: Int = 0,
        val group: String? = null,      // "brown", "lightBlue", "pink", "orange", "red", "yellow", "green", "darkBlue"
        val transportIndex: Int = -1,    // 0:قایق, 1:قطار, 2:هواپیما, 3:اتوبوس
        val buildCost: Int = 0           // هزینه ساخت هر خانه (برای PROPERTY)
    )

    // لیست کامل 40 خانه طبق سند
    val cells = listOf(
        CellInfo("شروع", CellType.START),
        CellInfo("خیابان ایران", CellType.PROPERTY, price = 60, rentBase = 2, group = "brown", buildCost = 50),
        CellInfo("شانس", CellType.CHANCE),
        CellInfo("خیابان آزادی", CellType.PROPERTY, price = 60, rentBase = 4, group = "brown", buildCost = 50),
        CellInfo("بانک", CellType.BANK),
        CellInfo("اسکله (قایق)", CellType.TRANSPORT, price = 200, rentBase = 25, transportIndex = 0),
        CellInfo("خیابان گلستان", CellType.PROPERTY, price = 100, rentBase = 6, group = "lightBlue", buildCost = 50),
        CellInfo("شانس", CellType.CHANCE),
        CellInfo("خیابان بهار", CellType.PROPERTY, price = 100, rentBase = 6, group = "lightBlue", buildCost = 50),
        CellInfo("خیابان نسترن", CellType.PROPERTY, price = 120, rentBase = 8, group = "lightBlue", buildCost = 50),
        CellInfo("زندان (بازدید)", CellType.JAIL),
        CellInfo("خیابان فردوسی", CellType.PROPERTY, price = 140, rentBase = 10, group = "pink", buildCost = 100),
        CellInfo("ملک موقت ۱", CellType.TEMPORARY, price = 80, rentBase = 50),
        CellInfo("خیابان حافظ", CellType.PROPERTY, price = 140, rentBase = 10, group = "pink", buildCost = 100),
        CellInfo("خیابان سعدی", CellType.PROPERTY, price = 160, rentBase = 12, group = "pink", buildCost = 100),
        CellInfo("ایستگاه راه‌آهن (قطار)", CellType.TRANSPORT, price = 200, rentBase = 25, transportIndex = 1),
        CellInfo("خیابان پاسداران", CellType.PROPERTY, price = 180, rentBase = 14, group = "orange", buildCost = 100),
        CellInfo("شانس", CellType.CHANCE),
        CellInfo("خیابان شریعتی", CellType.PROPERTY, price = 180, rentBase = 14, group = "orange", buildCost = 100),
        CellInfo("خیابان مطهری", CellType.PROPERTY, price = 200, rentBase = 16, group = "orange", buildCost = 100),
        CellInfo("ملک معامله", CellType.DEAL),
        CellInfo("خیابان طالقانی", CellType.PROPERTY, price = 220, rentBase = 18, group = "red", buildCost = 150),
        CellInfo("شانس", CellType.CHANCE),
        CellInfo("خیابان کریمخان", CellType.PROPERTY, price = 220, rentBase = 18, group = "red", buildCost = 150),
        CellInfo("خیابان فلسطین", CellType.PROPERTY, price = 240, rentBase = 20, group = "red", buildCost = 150),
        CellInfo("فرودگاه (هواپیما)", CellType.TRANSPORT, price = 200, rentBase = 25, transportIndex = 2),
        CellInfo("خیابان آفریقا", CellType.PROPERTY, price = 260, rentBase = 22, group = "yellow", buildCost = 150),
        CellInfo("ملک موقت ۲", CellType.TEMPORARY, price = 120, rentBase = 70),
        CellInfo("خیابان میرداماد", CellType.PROPERTY, price = 260, rentBase = 22, group = "yellow", buildCost = 150),
        CellInfo("خیابان ونک", CellType.PROPERTY, price = 280, rentBase = 24, group = "yellow", buildCost = 150),
        CellInfo("شانس", CellType.CHANCE),
        CellInfo("خیابان ولیعصر", CellType.PROPERTY, price = 300, rentBase = 26, group = "green", buildCost = 200),
        CellInfo("خیابان فرشته", CellType.PROPERTY, price = 300, rentBase = 26, group = "green", buildCost = 200),
        CellInfo("شانس", CellType.CHANCE),
        CellInfo("خیابان الهیه", CellType.PROPERTY, price = 320, rentBase = 28, group = "green", buildCost = 200),
        CellInfo("پایانه اتوبوس (اتوبوس)", CellType.TRANSPORT, price = 200, rentBase = 25, transportIndex = 3),
        CellInfo("خیابان سعادت‌آباد", CellType.PROPERTY, price = 350, rentBase = 35, group = "darkBlue", buildCost = 200),
        CellInfo("ملک مأموریت", CellType.MISSION),
        CellInfo("خیابان دریا", CellType.PROPERTY, price = 400, rentBase = 50, group = "darkBlue", buildCost = 200),
        CellInfo("ملک موقت ۳", CellType.TEMPORARY, price = 150, rentBase = 90)
    )

    // مختصات درصدی (همان کد قبلی قابل استفاده است)
    fun getCellCoords(index: Int): Pair<Float, Float> { /* ... همان پیاده‌سازی قبلی ... */
        val boardW = 80f
        val boardH = 80f
        val marginX = (100f - boardW) / 2f
        val marginY = (100f - boardH) / 2f
        val cellsPerSide = 10
        val step = boardW / cellsPerSide
        return when (index) {
            in 0..9 -> marginX + boardW - (index * step) to marginY + boardH
            in 10..19 -> marginX to marginY + boardH - ((index - 10) * step)
            in 20..29 -> marginX + ((index - 20) * step) to marginY
            else -> marginX + boardW to marginY + ((index - 30) * step)
        }
    }

    fun calculateRent(cell: CellInfo, houses: Int, hasFullGroup: Boolean = false): Int {
        var rent = when (houses) {
            0 -> cell.rentBase
            1 -> cell.rentBase * 2
            2 -> cell.rentBase * 3
            3 -> cell.rentBase * 4
            4 -> cell.rentBase * 5
            else -> cell.rentBase * 6
        }
        if (hasFullGroup && houses == 0) rent *= 2
        return rent
    }
    // هزینه‌های ساخت و اجاره‌های هر گروه
    data class GroupData(
        val buildCost: Int,               // هزینه ساخت یک خانه
        val rents: List<Int>              // اجاره برای ۰ تا ۵ خانه (۶ عدد)
    )

    // تعریف گروه‌های رنگی
    val groupData = mapOf(
        "brown" to GroupData(50, listOf(2, 4, 6, 8, 10, 12)),
        "lightBlue" to GroupData(50, listOf(6, 12, 18, 24, 30, 36)),
        "pink" to GroupData(100, listOf(10, 20, 30, 40, 50, 60)),
        "orange" to GroupData(100, listOf(14, 28, 42, 56, 70, 84)),
        "red" to GroupData(150, listOf(18, 36, 54, 72, 90, 108)),
        "yellow" to GroupData(150, listOf(22, 44, 66, 88, 110, 132)),
        "green" to GroupData(200, listOf(26, 52, 78, 104, 130, 156)),
        "darkBlue" to GroupData(200, listOf(35, 70, 105, 140, 175, 210))
    )
}