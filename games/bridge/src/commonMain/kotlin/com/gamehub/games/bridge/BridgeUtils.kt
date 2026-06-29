package com.gamehub.games.bridge

fun getValidActions(state: BridgeState, seat: Seat): List<BridgeAction> {
    return if (state.biddingPhase) {
        getValidBids(state, seat)
    } else {
        getValidPlays(state, seat)
    }
}

fun getValidBids(state: BridgeState, seat: Seat): List<BridgeAction> {
    val validActions = mutableListOf<BridgeAction>()
    validActions.add(BridgeAction.MakeBid(Bid.Pass))

    // Valid calls
    val lastCall = state.biddingHistory.lastOrNull { it.second is Bid.Call }?.second as Bid.Call?
    val minLevel = if (lastCall == null) 1 else lastCall.level
    for (level in minLevel..7) {
        val suitsToCheck = if (level == minLevel && lastCall != null) {
            Suit.bidSuits.filter { it.ordinal > lastCall.suit.ordinal }
        } else {
            Suit.bidSuits
        }
        for (suit in suitsToCheck) {
            validActions.add(BridgeAction.MakeBid(Bid.Call(level, suit)))
        }
    }

    // Check double
    val lastAction = state.biddingHistory.lastOrNull()?.second
    if (lastCall != null) {
        val lastCallSeat = state.biddingHistory.lastOrNull { it.second is Bid.Call }?.first
        if (lastCallSeat?.team() != seat.team() && lastAction !is Bid.Double && lastAction !is Bid.Redouble) {
            validActions.add(BridgeAction.MakeBid(Bid.Double))
        }
    }

    // Check redouble
    if (lastAction == Bid.Double) {
        val lastDoubleSeat = state.biddingHistory.lastOrNull { it.second == Bid.Double }?.first
        val lastCallSeat = state.biddingHistory.lastOrNull { it.second is Bid.Call }?.first
        if (lastDoubleSeat?.team() != seat.team() && lastCallSeat?.team() == seat.team()) {
            validActions.add(BridgeAction.MakeBid(Bid.Redouble))
        }
    }

    return validActions
}

fun getValidPlays(state: BridgeState, seat: Seat): List<BridgeAction> {
    val hand = state.hands[seat] ?: return emptyList()
    val leadSuit = state.currentTrick.leadSuit

    val validCards = if (leadSuit != null) {
        val hasLeadSuit = hand.any { it.suit == leadSuit }
        if (hasLeadSuit) {
            hand.filter { it.suit == leadSuit }
        } else {
            hand
        }
    } else {
        hand
    }

    return validCards.map { BridgeAction.PlayCard(it) }
}