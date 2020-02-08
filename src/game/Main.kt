package game

import java.util.*

const val winningValue = 21

enum class Suit {
    Heart, Diamond, Spade, Club
}

enum class Rank(val value: Int) {
    King(10),
    Queen(10),
    Jack(10),
    Ten(10),
    Nine(9),
    Eight(8),
    Seven(7),
    Six(6),
    Five(5),
    Four(4),
    Three(3),
    Two(2),
    Ace(1)
}

data class Card(val suit: Suit, val rank: Rank)

data class Deck(val cards: List<Card>) {
    fun dealCard(): Pair<Card, Deck> = Pair(
        cards.first(),
        copy(cards = cards.subList(1, cards.size))
    )

    companion object {
        fun shuffle(): Deck = Deck(
            Suit.values().flatMap { suit ->
                Rank.values().map { rank -> Card(suit, rank) }
            }.shuffled()
        )
    }
}

data class Hand(val cards: List<Card>) {
    val value = cards.map { card -> card.rank.value }.sum()
    val containsAce = cards.any { card -> card.rank == Rank.Ace }
    val specialValue = if (containsAce) value + 10 else value
    val isBlackJack = value == winningValue || specialValue == winningValue
    val isBust = value > winningValue
    val bestValue = {
        // TODO I mean come one, its a bloody list, how hard is it to find the max element??
        val list = listOf(value, specialValue).filter { value -> value <= winningValue }
        if (list.isNotEmpty()) {
            list.max()!!
        } else {
            0
        }
    }
    fun winsOver(other: Hand): Boolean = bestValue() > other.bestValue()
    fun showCards(dealer: Boolean = false): String = if (dealer) "${cards.first().rank.value} X" else cards.map { c -> c.rank.value }.joinToString(", ")
    fun addCard(card: Card): Hand {
        val newCards = cards.toMutableList() // TODO WTF?? Why? Like really?
        newCards.add(card)
        return copy(cards = newCards.toList())
    }

    companion object {
        data class Hands(val playerHand: Hand, val dealerHand: Hand, val deck: Deck)
        fun dealHands(deck: Deck): Hands {
            val (firstCard, deck1) = deck.dealCard()
            val (secondCard, deck2) = deck1.dealCard()
            val (thirdCard, deck3) = deck2.dealCard()
            val (fourthCard, deck4) = deck3.dealCard()
            return Hands(
                Hand(listOf(firstCard, secondCard)),
                Hand(listOf(thirdCard, fourthCard)),
                deck4
            )
        }
    }
}

data class GameState(val credit: Int)

val randomShuffler: (List<Card>) -> List<Card> = { cards -> cards.shuffled() }

val shouldContinue: () -> Boolean = {
    println("Do you want to continue?")
    "y".equals(Scanner(System.`in`).nextLine(), ignoreCase = true)
}

val shouldStand: () -> Boolean = {
    println("Hit or Stand?")
    "s".equals(Scanner(System.`in`).nextLine(), ignoreCase = true)
}

val betSupplier: (Int) -> Int = { currentCredit ->
    println("Please enter bet (credit: $currentCredit) ")
    var newBet = Scanner(System.`in`).nextInt()
    while (newBet > currentCredit) {
        println("Too high. Please enter bet (credit: $currentCredit) ")
        newBet = Scanner(System.`in`).nextInt()
    }
    newBet
}

fun showCards(playerHand: Hand, dealerHand: Hand, showDealer: Boolean) {
    println("Dealer hand: " + dealerHand.showCards(showDealer))
    println("Player hand: " + playerHand.showCards(false))
}

fun summary(playerHand: Hand, dealerHand: Hand, won: Boolean): Boolean {
    println("*** You " + (if (won) "win" else "loose!") + " ***")
    showCards(playerHand, dealerHand, false)
    return won
}

fun roundLoop(
    playerHand: Hand,
    dealerHand: Hand,
    deck: Deck,
    stand: Boolean,
    shouldStand: () -> Boolean
): Boolean {
    return when {
        playerHand.isBust -> {
            summary(playerHand, dealerHand, false)
        }
        stand -> {
            summary(playerHand, dealerHand, dealerHand.isBust || playerHand.winsOver(dealerHand))
        }
        else -> {
            val round = hitOrStand(playerHand, dealerHand, deck, shouldStand)
            roundLoop(round.playerHand, round.dealerHand, round.deck, round.stand, shouldStand)
        }
    }
}

data class RoundResult(val playerHand: Hand, val dealerHand: Hand, val deck: Deck, val stand: Boolean)

fun hitOrStand(
    playerHand: Hand,
    dealerHand: Hand,
    deck: Deck,
    shouldStand: () -> Boolean
): RoundResult {
    var newPlayerHand = playerHand
    var newDealerHand = dealerHand
    var newDeck = deck
    showCards(newPlayerHand, newDealerHand, true)
    val stand = shouldStand.invoke()
    if (stand) {
        while (newDealerHand.value < 17) {
            val dealt = newDeck.dealCard()
            newDealerHand = newDealerHand.addCard(dealt.first)
            newDeck = dealt.second
        }
    } else {
        val dealt = deck.dealCard()
        newPlayerHand = newPlayerHand.addCard(dealt.first)
        newDeck = dealt.second
    }
    return RoundResult(newPlayerHand, newDealerHand, newDeck, stand)
}


fun gameLoop(gameState: GameState,
             shuffleFn: (cards: List<Card>) -> List<Card>,
             betSupplier: (Int) -> Int,
             shouldContinue: () -> Boolean,
             shouldStand: () -> Boolean) {
    val deck = Deck.shuffle()
    val bet = betSupplier.invoke(gameState.credit)
    val hands = Hand.dealHands(deck)
    val playerWon = roundLoop(hands.playerHand, hands.dealerHand, hands.deck, false, shouldStand)
    val newState = GameState(gameState.credit + if (playerWon) bet else -bet)
    if (newState.credit > 0 && shouldContinue.invoke()) {
        gameLoop(newState, shuffleFn, betSupplier, shouldContinue, shouldStand)
    } else if (newState.credit <= 0) {
        println("You have no money left")
    } else {
        println("Exiting")
    }
}

fun main() {
    val gameState = GameState(100)
    gameLoop(gameState, randomShuffler, betSupplier, shouldContinue, shouldStand)
}