package no.nsf.wordfeudpresenterbackend

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import wordfeudapi.RestWordFeudClient
import wordfeudapi.domain.BoardType.Normal
import wordfeudapi.domain.Game
import wordfeudapi.domain.RuleSet.Norwegian
import wordfeudapi.domain.Tile
import wordfeudapi.exception.WordFeudLoginRequiredException
import java.lang.Thread.sleep

var botClient: RestWordFeudClient = RestWordFeudClient()

fun main() {

    //TODO hold en map over botClienter, så kan man ha flere spill gående på en gang
    val mapOfBotClients = mutableMapOf<String, RestWordFeudClient>()

    val port = System.getenv("PORT")?.toInt() ?: 23567
    embeddedServer(Netty, port) {
        install(CORS) {
            anyHost()
        }
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
            }
        }
        routing {

            post("invite") {
                val invitationRequest = call.receive<InvitationRequest>()
                botClient = RestWordFeudClient()
                botClient.logon(invitationRequest.inviter, invitationRequest.inviter)
                val invite = botClient.invite(invitationRequest.invitee, Norwegian, Normal)

                for (i in 0..60) {
                    println("pausing 1 sec whilst waiting for notification")
                    sleep(1000)
                    val notifications = botClient.notifications
                    notifications.entries.forEach { notificationEntry ->
                        if (notificationEntry.type == "new_game" && notificationEntry.username == invitationRequest.invitee) {
                            println("found new game")
                            call.respond(InvitationResponse(gameId = notificationEntry.gameId))
                            return@post
                        }
                    }
                }
                call.respond(InvitationResponse(error = "Waited one minute without recieving notification about new game"))
            }

            post("game") {
                val gameRequest = call.receive<GameRequest>()
                try {
                    val game = botClient.getGame(gameRequest.gameId)
                    call.respond(mapToGameResponse(game))
                } catch (exception: WordFeudLoginRequiredException) {
                    println("Fetching of games failed, attempting to login")
                    botClient.logon(gameRequest.player1, gameRequest.player1)
                    val game = botClient.getGame(gameRequest.gameId)
                    call.respond(mapToGameResponse(game))
                }

                //call.respond(jsonGameResponse)
            }

            get("") {
                call.respond("I'm alive!")
            }
        }
    }.start(wait = true)
}

fun mapToGameResponse(game: Game): GameResponse {
    return GameResponse(
        gameId = game.id,
        player1Score = game.me.score,
        player2Score = game.opponent.score,
        isRunning = game.isRunning,
        tiles = mapGameToTileList(game),
        lastMove = mapToLastMove(game)
    )
}

fun mapGameToTileList(game: Game): List<CharArray> {
    val a = charArrayOf('2', '0', '0', '0', '4', '0', '0', '1', '0', '0', '4', '0', '0', '0', '2')
    val b = charArrayOf('0', '1', '0', '0', '0', '2', '0', '0', '0', '2', '0', '0', '0', '1', '0')
    val c = charArrayOf('0', '0', '3', '0', '0', '0', '1', '0', '1', '0', '0', '0', '3', '0', '0')
    val d = charArrayOf('0', '0', '0', '2', '0', '0', '0', '3', '0', '0', '0', '2', '0', '0', '0')
    val e = charArrayOf('4', '0', '0', '0', '3', '0', '1', '0', '1', '0', '3', '0', '0', '0', '4')
    val f = charArrayOf('0', '2', '0', '0', '0', '2', '0', '0', '0', '2', '0', '0', '0', '2', '0')
    val g = charArrayOf('0', '0', '1', '0', '1', '0', '0', '0', '0', '0', '1', '0', '1', '0', '0')
    val h = charArrayOf('1', '0', '0', '3', '0', '0', '0', '0', '0', '0', '0', '3', '0', '0', '1')
    val i = charArrayOf('0', '0', '1', '0', '1', '0', '0', '0', '0', '0', '1', '0', '1', '0', '0')
    val j = charArrayOf('0', '2', '0', '0', '0', '2', '0', '0', '0', '2', '0', '0', '0', '2', '0')
    val k = charArrayOf('4', '0', '0', '0', '3', '0', '1', '0', '1', '0', '3', '0', '0', '0', '4')
    val l = charArrayOf('0', '0', '0', '2', '0', '0', '0', '3', '0', '0', '0', '2', '0', '0', '0')
    val m = charArrayOf('0', '0', '3', '0', '0', '0', '1', '0', '1', '0', '0', '0', '3', '0', '0')
    val n = charArrayOf('0', '1', '0', '0', '0', '2', '0', '0', '0', '2', '0', '0', '0', '1', '0')
    val o = charArrayOf('2', '0', '0', '0', '4', '0', '0', '1', '0', '0', '4', '0', '0', '0', '2')

    val charBoard = mutableListOf(a, b, c, d, e, f, g, h, i, j, k, l, m, n, o)
    game.tiles
        ?.forEach {
            charBoard[it.y][it.x] = if (it.isWildcard) it.character.toLowerCase() else it.character
        }
    return charBoard
}

fun mapToLastMove(game: Game): LastMoveResponse? {
    val lastMove = game.lastMove ?: return null
    val player = if (lastMove.user_id == game.me.id) game.me.username else game.opponentName
    return LastMoveResponse(player, lastMove.move_type, lastMove.main_word ?: "", lastMove.points, mapToCoordinates(lastMove.move))
}

fun mapToCoordinates(tiles: List<Tile>): List<Coordinate> {
    return tiles.map { Coordinate(it.x, it.y) }
}

data class InvitationRequest(val inviter: String, val invitee: String)
data class InvitationResponse(val gameId: Long? = null, val error: String? = null)
data class GameRequest(val gameId: Long, val player1: String)
data class GameResponse(
    val gameId: Long,
    val player1Score: Int,
    val player2Score: Int,
    val isRunning: Boolean,
    val tiles: List<CharArray>,
    val lastMove: LastMoveResponse?
)

data class LastMoveResponse(
    val player: String,
    val moveType: String,
    val word: String,
    val points: Int,
    val letterPlacments: List<Coordinate>
)

data class Coordinate(val x: Int, val y: Int)

