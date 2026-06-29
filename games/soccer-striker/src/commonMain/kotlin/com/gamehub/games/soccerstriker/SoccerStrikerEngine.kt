package com.gamehub.games.soccerstriker

import com.gamehub.shared.core.*
import com.gamehub.shared.engine.GameUpdateResult
import org.jbox2d.callbacks.ContactImpulse
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.*
import org.jbox2d.dynamics.contacts.Contact
import org.jbox2d.callbacks.ContactListener
import kotlin.math.*

private fun log(message: String) {
    println("[SoccerStriker] $message")
}

class SoccerStrikerEngine : GameDefinition<SoccerStrikerState, SoccerStrikerAction, GameResult> {
    override val metadata: GameMetadata = GameMetadata(
        id = "soccer-striker",
        name = "Soccer Striker (فوتبال انگشتی)",
        minPlayers = 2,
        maxPlayers = 2,
        description = "بازی فوتبال با دیسک‌ها با استفاده از فیزیک JBox2D"
    )

    override fun createInitialState(players: List<PlayerId>): SoccerStrikerState {
        return SoccerStrikerState.initial(players)
    }

    override fun validateAction(
        state: SoccerStrikerState,
        action: SoccerStrikerAction,
        playerId: PlayerId
    ): Boolean {
        return when (action) {
            is SoccerStrikerAction.SelectDisc -> {
                if (state.gameOver || state.isSimulating) return false
                if (state.currentPlayer != playerId) return false
                val disc = state.discs.find { it.id == action.discId }
                val currentTeam = if (state.currentPlayerIndex == 0) "red" else "blue"
                disc != null && disc.team == currentTeam
            }
            is SoccerStrikerAction.FlickDisc -> {
                if (state.gameOver || state.isSimulating) return false
                if (state.currentPlayer != playerId) return false
                val disc = state.discs.find { it.id == action.discId }
                val currentTeam = if (state.currentPlayerIndex == 0) "red" else "blue"
                disc != null && disc.team == currentTeam
            }
            is SoccerStrikerAction.AutoFlick -> {
                if (state.gameOver || state.isSimulating) return false
                if (state.currentPlayer != playerId) return false
                val disc = state.discs.find { it.id == action.discId }
                val currentTeam = if (state.currentPlayerIndex == 0) "red" else "blue"
                disc != null && disc.team == currentTeam
            }
            is SoccerStrikerAction.AnimationComplete -> {
                state.isSimulating
            }
            is SoccerStrikerAction.Reset -> true
            is SoccerStrikerAction.SkipTurn -> {
                !state.isSimulating
            }
        }
    }

    override fun applyAction(
        state: SoccerStrikerState,
        action: SoccerStrikerAction,
        playerId: PlayerId
    ): GameUpdateResult<SoccerStrikerState, GameResult> {
        val newState = when (action) {
            is SoccerStrikerAction.SelectDisc -> {
                state.copy(selectedDiscId = action.discId)
            }
            is SoccerStrikerAction.FlickDisc -> {
                simulatePhysics(state, action)
            }
            is SoccerStrikerAction.AutoFlick -> {
                val stateWithSelection = if (state.selectedDiscId == action.discId) {
                    state
                } else {
                    state.copy(selectedDiscId = action.discId)
                }
                val flickAction = SoccerStrikerAction.FlickDisc(action.discId, action.angle, action.power)
                simulatePhysics(stateWithSelection, flickAction)
            }
            is SoccerStrikerAction.AnimationComplete -> {
                // تصحیح نهایی روی آخرین فریم
                val finalFrame = state.animationFrames.lastOrNull()
                var finalDiscs = finalFrame?.discs ?: state.discs
                var finalBall = finalFrame?.ball ?: state.ball

                // اعمال اصلاح نهایی قوی
                val corrected = correctAllOverlaps(finalDiscs, finalBall, iterations = 10)
                finalDiscs = corrected.first
                finalBall = corrected.second

                val nextPlayerState = if (state.gameOver) {
                    state
                } else {
                    nextTurn(state)
                }

                nextPlayerState.copy(
                    discs = finalDiscs,
                    ball = finalBall,
                    isSimulating = false,
                    animationFrames = emptyList(),
                    currentAnimationFrame = 0
                )
            }
            is SoccerStrikerAction.Reset -> {
                SoccerStrikerState.initial(state.players)
            }
            is SoccerStrikerAction.SkipTurn -> {
                nextTurn(state)
            }
        }
        val result = getResult(newState)
        return GameUpdateResult(newState, result)
    }

    // تابع اصلی تصحیح همپوشانی برای همه جفت‌ها با تکرار
    private fun correctAllOverlaps(
        discs: List<DiscData>,
        ball: BallData,
        iterations: Int = 5
    ): Pair<List<DiscData>, BallData> {
        val discRadius = SoccerStrikerState.DISC_RADIUS
        val ballRadius = SoccerStrikerState.BALL_RADIUS
        val positions = mutableMapOf<String, Pair<Float, Float>>()
        discs.forEach { positions[it.id] = it.x to it.y }
        positions["ball"] = ball.x to ball.y

        fun getRadius(id: String): Float = if (id == "ball") ballRadius else discRadius

        repeat(iterations) {
            val ids = positions.keys.toList()
            for (i in ids.indices) {
                for (j in i + 1 until ids.size) {
                    val id1 = ids[i]
                    val id2 = ids[j]
                    val (x1, y1) = positions[id1] ?: continue
                    val (x2, y2) = positions[id2] ?: continue
                    val dx = x2 - x1
                    val dy = y2 - y1
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < 0.0001f) continue
                    val r1 = getRadius(id1)
                    val r2 = getRadius(id2)
                    val minDist = r1 + r2
                    if (dist < minDist) {
                        val overlap = minDist - dist
                        // اصلاح تدریجی: ۵۰٪ همپوشانی در هر تکرار
                        val correction = overlap * 0.5f
                        val nx = dx / dist
                        val ny = dy / dist
                        positions[id1] = (x1 - nx * correction) to (y1 - ny * correction)
                        positions[id2] = (x2 + nx * correction) to (y2 + ny * correction)
                    }
                }
            }
        }

        val newDiscs = discs.map { disc ->
            val (x, y) = positions[disc.id] ?: (disc.x to disc.y)
            disc.copy(x = x, y = y)
        }
        val (bx, by) = positions["ball"] ?: (ball.x to ball.y)
        val newBall = ball.copy(x = bx, y = by)
        return newDiscs to newBall
    }

    private fun simulatePhysics(
        state: SoccerStrikerState,
        action: SoccerStrikerAction.FlickDisc
    ): SoccerStrikerState {
        val physics = SoccerPhysicsEngine(state.physicsConfig)
        physics.setupWorld(state)

        val normalizedPower = action.power.coerceIn(0f, 1f)
        physics.applyFlick(action.discId, action.angle, normalizedPower)

        val result = physics.simulate()

        var newScoreRed = state.scoreRed
        var newScoreBlue = state.scoreBlue
        var isKickOff = state.isKickOff
        val currentTeam = if (state.currentPlayerIndex == 0) "red" else "blue"

        if (result.goalScored != null) {
            val goalTeam = result.goalScored
            if (isKickOff && goalTeam == currentTeam) {
                log("Own goal on kickoff ignored")
            } else {
                if (goalTeam == "red") newScoreRed++ else newScoreBlue++
                isKickOff = true
                log("Goal scored by $goalTeam! Score: $newScoreRed - $newScoreBlue")
            }
        } else {
            isKickOff = false
        }

        val (finalDiscs, finalBall) = if (result.goalScored != null && !(state.isKickOff && result.goalScored == currentTeam)) {
            resetToInitialPositions()
        } else {
            result.discs to result.ball
        }

        // اصلاح نهایی قوی روی آخرین وضعیت
        val correctedFinal = correctAllOverlaps(finalDiscs, finalBall, iterations = 10)
        val correctedDiscs = correctedFinal.first
        val correctedBall = correctedFinal.second

        val animationFrames = if (result.goalScored != null && !(state.isKickOff && result.goalScored == currentTeam)) {
            result.animationFrames + AnimationFrame(correctedDiscs, correctedBall)
        } else {
            val correctedFrames = result.animationFrames.toMutableList()
            if (correctedFrames.isNotEmpty()) {
                correctedFrames[correctedFrames.lastIndex] = AnimationFrame(correctedDiscs, correctedBall)
            }
            correctedFrames
        }

        val gameOver = newScoreRed >= SoccerStrikerState.WIN_SCORE || newScoreBlue >= SoccerStrikerState.WIN_SCORE
        val winner = when {
            newScoreRed >= SoccerStrikerState.WIN_SCORE -> state.players.getOrNull(0)
            newScoreBlue >= SoccerStrikerState.WIN_SCORE -> state.players.getOrNull(1)
            else -> null
        }

        return state.copy(
            discs = animationFrames.first().discs,
            ball = animationFrames.first().ball,
            scoreRed = newScoreRed,
            scoreBlue = newScoreBlue,
            isKickOff = isKickOff,
            selectedDiscId = null,
            isSimulating = true,
            animationFrames = animationFrames,
            currentAnimationFrame = 0,
            gameOver = gameOver,
            winner = winner
        )
    }

    private fun resetToInitialPositions(): Pair<List<DiscData>, BallData> {
        val redDiscs = listOf(
            DiscData("red-1", "red", SoccerStrikerState.FIELD_WIDTH / 2, 200f - 100f),
            DiscData("red-2", "red", SoccerStrikerState.FIELD_WIDTH / 2 - 150f, 200f),
            DiscData("red-3", "red", SoccerStrikerState.FIELD_WIDTH / 2 + 150f, 200f),
            DiscData("red-4", "red", SoccerStrikerState.FIELD_WIDTH / 2 - 100f, 200f + 100f),
            DiscData("red-5", "red", SoccerStrikerState.FIELD_WIDTH / 2 + 100f, 200f + 100f)
        )
        val blueDiscs = listOf(
            DiscData("blue-1", "blue", SoccerStrikerState.FIELD_WIDTH / 2, SoccerStrikerState.FIELD_HEIGHT - 200f + 100f),
            DiscData("blue-2", "blue", SoccerStrikerState.FIELD_WIDTH / 2 - 150f, SoccerStrikerState.FIELD_HEIGHT - 200f),
            DiscData("blue-3", "blue", SoccerStrikerState.FIELD_WIDTH / 2 + 150f, SoccerStrikerState.FIELD_HEIGHT - 200f),
            DiscData("blue-4", "blue", SoccerStrikerState.FIELD_WIDTH / 2 - 100f, SoccerStrikerState.FIELD_HEIGHT - 200f - 100f),
            DiscData("blue-5", "blue", SoccerStrikerState.FIELD_WIDTH / 2 + 100f, SoccerStrikerState.FIELD_HEIGHT - 200f - 100f)
        )
        return (redDiscs + blueDiscs) to BallData(SoccerStrikerState.FIELD_WIDTH / 2, SoccerStrikerState.FIELD_HEIGHT / 2)
    }

    private fun nextTurn(state: SoccerStrikerState): SoccerStrikerState {
        val nextIndex = (state.currentPlayerIndex + 1) % state.players.size
        return state.copy(currentPlayerIndex = nextIndex)
    }

    override fun isTerminal(state: SoccerStrikerState): Boolean {
        return state.gameOver
    }

    override fun getResult(state: SoccerStrikerState): GameResult? {
        if (!isTerminal(state)) return null
        return state.winner?.let { GameResult.Win(it) } ?: GameResult.Draw
    }

    override fun getPlayers(state: SoccerStrikerState): List<PlayerId> = state.players

    override fun setCurrentPlayer(state: SoccerStrikerState, playerId: PlayerId): SoccerStrikerState {
        val index = state.players.indexOf(playerId)
        return if (index >= 0) state.copy(currentPlayerIndex = index) else state
    }
}

data class PhysicsResult(
    val discs: List<DiscData>,
    val ball: BallData,
    val goalScored: String? = null,
    val animationFrames: List<AnimationFrame> = emptyList()
)

class SoccerPhysicsEngine(private val config: PhysicsConfig) {
    private val world: World = World(Vec2(0f, 0f))
    private val discBodies = mutableMapOf<String, Body>()
    private lateinit var ballBody: Body
    private var goalScored: String? = null

    companion object {
        private const val PIXELS_PER_METER = 50f
        private const val CATEGORY_WALL = 0x0001
        private const val CATEGORY_DISC = 0x0002
        private const val CATEGORY_BALL = 0x0004
        private const val CATEGORY_GOAL_SENSOR = 0x0008
        private const val CATEGORY_GOAL_WALL = 0x0010

        private const val MASK_WALL = CATEGORY_DISC or CATEGORY_BALL
        private const val MASK_DISC = CATEGORY_WALL or CATEGORY_DISC or CATEGORY_BALL or CATEGORY_GOAL_WALL
        private const val MASK_BALL = CATEGORY_WALL or CATEGORY_DISC or CATEGORY_GOAL_SENSOR
        private const val MASK_GOAL_SENSOR = CATEGORY_BALL
        private const val MASK_GOAL_WALL = CATEGORY_DISC
    }

    init {
        world.setContactListener(object : ContactListener {
            override fun beginContact(contact: Contact) {
                val fixtureA = contact.fixtureA
                val fixtureB = contact.fixtureB
                val userDataA = fixtureA.body.userData as? String
                val userDataB = fixtureB.body.userData as? String

                if ((userDataA == "ball" && userDataB?.startsWith("goal-") == true) ||
                    (userDataB == "ball" && userDataA?.startsWith("goal-") == true)) {
                    val goalTeam = if (userDataA?.startsWith("goal-") == true) {
                        userDataA.removePrefix("goal-")
                    } else {
                        userDataB?.removePrefix("goal-") ?: ""
                    }
                    goalScored = goalTeam
                    log("Goal detected for team $goalTeam")
                }
            }

            override fun endContact(contact: Contact) {}
            override fun preSolve(contact: Contact, oldManifold: org.jbox2d.collision.Manifold) {}
            override fun postSolve(contact: Contact, impulse: ContactImpulse) {}
        })
    }

    fun setupWorld(state: SoccerStrikerState) {
        var body = world.bodyList
        while (body != null) {
            val next = body.next
            world.destroyBody(body)
            body = next
        }
        discBodies.clear()
        goalScored = null

        val halfWidth = SoccerStrikerState.FIELD_WIDTH / 2 / PIXELS_PER_METER
        val halfHeight = SoccerStrikerState.FIELD_HEIGHT / 2 / PIXELS_PER_METER
        val goalHalfWidth = SoccerStrikerState.GOAL_WIDTH / 2 / PIXELS_PER_METER
        val wallThickness = 0.4f
        val goalDepth = SoccerStrikerState.GOAL_DEPTH / PIXELS_PER_METER

        createWall(-halfWidth - wallThickness / 2, 0f, wallThickness, halfHeight * 2)
        createWall(halfWidth + wallThickness / 2, 0f, wallThickness, halfHeight * 2)

        createWall(-(halfWidth + goalHalfWidth) / 2, halfHeight + wallThickness / 2, (halfWidth - goalHalfWidth) / 2, wallThickness)
        createWall((halfWidth + goalHalfWidth) / 2, halfHeight + wallThickness / 2, (halfWidth - goalHalfWidth) / 2, wallThickness)
        createWall(-(halfWidth + goalHalfWidth) / 2, -halfHeight - wallThickness / 2, (halfWidth - goalHalfWidth) / 2, wallThickness)
        createWall((halfWidth + goalHalfWidth) / 2, -halfHeight - wallThickness / 2, (halfWidth - goalHalfWidth) / 2, wallThickness)

        createGoalSensor(0f, halfHeight - goalDepth / 2, goalHalfWidth, goalDepth, "red")
        createGoalSensor(0f, -halfHeight + goalDepth / 2, goalHalfWidth, goalDepth, "blue")
        createGoalWall(0f, halfHeight - goalDepth / 2, goalHalfWidth, goalDepth)
        createGoalWall(0f, -halfHeight + goalDepth / 2, goalHalfWidth, goalDepth)

        state.discs.forEach { disc ->
            val body = createDisc(disc.x, disc.y, SoccerStrikerState.DISC_RADIUS / PIXELS_PER_METER, disc.team, config)
            body.setLinearVelocity(Vec2(0f, 0f))
            body.angularVelocity = 0f
            discBodies[disc.id] = body
        }

        ballBody = createBall(state.ball.x, state.ball.y, SoccerStrikerState.BALL_RADIUS / PIXELS_PER_METER, config)
        ballBody.setLinearVelocity(Vec2(0f, 0f))
        ballBody.angularVelocity = 0f
    }

    private fun createWall(x: Float, y: Float, halfWidth: Float, halfHeight: Float) {
        val bodyDef = BodyDef().apply {
            type = BodyType.STATIC
            position.set(x, y)
        }
        val shape = PolygonShape().apply { setAsBox(halfWidth, halfHeight) }
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            restitution = 0.5f
            friction = 0.3f
            filter.categoryBits = CATEGORY_WALL
            filter.maskBits = MASK_WALL
        }
        val body = world.createBody(bodyDef)
        body.createFixture(fixtureDef)
    }

    private fun createGoalSensor(x: Float, y: Float, halfWidth: Float, halfHeight: Float, team: String) {
        val bodyDef = BodyDef().apply {
            type = BodyType.STATIC
            position.set(x, y)
            userData = "goal-$team"
        }
        val shape = PolygonShape().apply { setAsBox(halfWidth, halfHeight) }
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            isSensor = true
            filter.categoryBits = CATEGORY_GOAL_SENSOR
            filter.maskBits = MASK_GOAL_SENSOR
        }
        val body = world.createBody(bodyDef)
        body.createFixture(fixtureDef)
    }

    private fun createGoalWall(x: Float, y: Float, halfWidth: Float, halfHeight: Float) {
        val bodyDef = BodyDef().apply {
            type = BodyType.STATIC
            position.set(x, y)
        }
        val shape = PolygonShape().apply { setAsBox(halfWidth, halfHeight) }
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            restitution = config.discRestitution
            friction = config.discFriction
            filter.categoryBits = CATEGORY_GOAL_WALL
            filter.maskBits = CATEGORY_DISC
        }
        val body = world.createBody(bodyDef)
        body.createFixture(fixtureDef)
    }

    private fun createDisc(x: Float, y: Float, radius: Float, team: String, config: PhysicsConfig): Body {
        val bodyDef = BodyDef().apply {
            type = BodyType.DYNAMIC
            position.set(
                (x - SoccerStrikerState.FIELD_WIDTH / 2) / PIXELS_PER_METER,
                -(y - SoccerStrikerState.FIELD_HEIGHT / 2) / PIXELS_PER_METER
            )
            userData = "disc-$team"
        }
        val shape = CircleShape().apply { m_radius = radius }
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            restitution = config.discRestitution
            friction = config.discFriction
            density = config.discDensity
            filter.categoryBits = CATEGORY_DISC
            filter.maskBits = MASK_DISC
        }
        val body = world.createBody(bodyDef)
        body.createFixture(fixtureDef)
        body.linearDamping = config.discLinearDamping
        body.angularDamping = config.discAngularDamping
        return body
    }

    private fun createBall(x: Float, y: Float, radius: Float, config: PhysicsConfig): Body {
        val bodyDef = BodyDef().apply {
            type = BodyType.DYNAMIC
            position.set(
                (x - SoccerStrikerState.FIELD_WIDTH / 2) / PIXELS_PER_METER,
                -(y - SoccerStrikerState.FIELD_HEIGHT / 2) / PIXELS_PER_METER
            )
            userData = "ball"
        }
        val shape = CircleShape().apply { m_radius = radius }
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            restitution = config.ballRestitution
            friction = config.ballFriction
            density = config.ballDensity
            filter.categoryBits = CATEGORY_BALL
            filter.maskBits = MASK_BALL
        }
        val body = world.createBody(bodyDef)
        body.createFixture(fixtureDef)
        body.linearDamping = config.ballLinearDamping
        body.angularDamping = config.ballAngularDamping
        return body
    }

    fun applyFlick(discId: String, angle: Float, power: Float) {
        val body = discBodies[discId] ?: return
        val impulseMagnitude = power * config.flickPowerFactor / PIXELS_PER_METER
        val impulse = Vec2(
            cos(angle) * impulseMagnitude,
            -sin(angle) * impulseMagnitude
        )
        body.applyLinearImpulse(impulse, body.worldCenter)
    }

    fun simulate(): PhysicsResult {
        val timeStep = 1f / 60f
        val velocityIterations = 10
        val positionIterations = 6
        val maxSteps = 600
        val frames = mutableListOf<AnimationFrame>()

        frames.add(captureFrame())

        var steps = 0
        while (steps < maxSteps) {
            world.step(timeStep, velocityIterations, positionIterations)
            steps++

            // 🔧 اصلاح همه جفت‌ها در هر گام (با تکرار ۵ بار)
            repeat(5) {
                resolveAllOverlaps()
            }

            // محدود کردن سرعت
            val maxSpeedMeters = config.maxSpeed / PIXELS_PER_METER
            var body = world.bodyList
            while (body != null) {
                if (body.type == BodyType.DYNAMIC) {
                    val v = body.linearVelocity
                    val speed = sqrt(v.x * v.x + v.y * v.y)
                    if (speed > maxSpeedMeters) {
                        val scale = maxSpeedMeters / speed
                        body.setLinearVelocity(Vec2(v.x * scale, v.y * scale))
                    }
                }
                body = body.next
            }

            if (steps % 2 == 0) {
                frames.add(captureFrame())
            }

            if (goalScored != null) {
                frames.add(captureFrame())
                break
            }

            val energyThreshold = 0.05f
            val totalKE = calculateTotalEnergy()
            if (totalKE < energyThreshold && steps > 60) break
        }

        // اصلاح نهایی
        repeat(5) {
            resolveAllOverlaps()
        }
        frames.add(captureFrame())

        val finalFrame = frames.last()
        return PhysicsResult(finalFrame.discs, finalFrame.ball, goalScored, frames)
    }

    // تابع اصلاح همپوشانی برای همه جفت‌های پویا
    private fun resolveAllOverlaps() {
        val bodies = mutableListOf<Body>()
        var body = world.bodyList
        while (body != null) {
            if (body.type == BodyType.DYNAMIC) {
                bodies.add(body)
            }
            body = body.next
        }

        val discRadiusMeters = SoccerStrikerState.DISC_RADIUS / PIXELS_PER_METER
        val ballRadiusMeters = SoccerStrikerState.BALL_RADIUS / PIXELS_PER_METER

        for (i in bodies.indices) {
            for (j in i + 1 until bodies.size) {
                val b1 = bodies[i]
                val b2 = bodies[j]

                val pos1 = b1.position
                val pos2 = b2.position
                val dx = pos2.x - pos1.x
                val dy = pos2.y - pos1.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < 0.0001f) continue

                val r1 = getRadius(b1, discRadiusMeters, ballRadiusMeters)
                val r2 = getRadius(b2, discRadiusMeters, ballRadiusMeters)
                if (r1 <= 0f || r2 <= 0f) continue

                val minDist = r1 + r2
                if (dist < minDist) {
                    val overlap = minDist - dist
                    // اصلاح تدریجی ۵۰٪
                    val correction = overlap * 0.5f
                    val nx = dx / dist
                    val ny = dy / dist

                    b1.setTransform(
                        Vec2(pos1.x - nx * correction / 2, pos1.y - ny * correction / 2),
                        b1.angle
                    )
                    b2.setTransform(
                        Vec2(pos2.x + nx * correction / 2, pos2.y + ny * correction / 2),
                        b2.angle
                    )
                }
            }
        }
    }

    private fun getRadius(body: Body, discR: Float, ballR: Float): Float {
        val userData = body.userData as? String
        return when {
            userData == "ball" -> ballR
            userData?.startsWith("disc-") == true -> discR
            else -> 0f
        }
    }

    private fun captureFrame(): AnimationFrame {
        val discs = discBodies.map { (id, body) ->
            val team = if (id.startsWith("red")) "red" else "blue"
            DiscData(
                id = id,
                team = team,
                x = body.position.x * PIXELS_PER_METER + SoccerStrikerState.FIELD_WIDTH / 2,
                y = -body.position.y * PIXELS_PER_METER + SoccerStrikerState.FIELD_HEIGHT / 2,
                vx = body.linearVelocity.x * PIXELS_PER_METER,
                vy = -body.linearVelocity.y * PIXELS_PER_METER,
                angle = body.angle,
                angularVelocity = body.angularVelocity
            )
        }
        val ball = BallData(
            x = ballBody.position.x * PIXELS_PER_METER + SoccerStrikerState.FIELD_WIDTH / 2,
            y = -ballBody.position.y * PIXELS_PER_METER + SoccerStrikerState.FIELD_HEIGHT / 2,
            vx = ballBody.linearVelocity.x * PIXELS_PER_METER,
            vy = -ballBody.linearVelocity.y * PIXELS_PER_METER
        )
        return AnimationFrame(discs, ball)
    }

    private fun calculateTotalEnergy(): Float {
        var total = 0f
        var body = world.bodyList
        while (body != null) {
            if (body.type == BodyType.DYNAMIC) {
                val v = body.linearVelocity
                total += v.x * v.x + v.y * v.y
            }
            body = body.next
        }
        return total
    }
}