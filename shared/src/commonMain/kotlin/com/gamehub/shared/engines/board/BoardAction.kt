package com.gamehub.shared.engines.board

import com.gamehub.shared.core.GameAction
import kotlinx.serialization.Serializable

@Serializable
open class BoardAction(val row: Int, val col: Int) : GameAction()