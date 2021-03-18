package dev.littlegames.highorderfunctions

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import dev.littlegames.highorderfunctions.Constants.cols
import dev.littlegames.highorderfunctions.Constants.rows

enum class Direction(val x: Int, val y: Int) {
    LEFT(-1, 0),
    RIGHT(1, 0),
    DOWN(0, -1),
    UP(0, 1)
}

class Board(private val game: LittleGame) : InputAdapter() {
    private var tiles = Array(rows) { arrayOfNulls<Tile?>(cols) }
    private val sliding: Boolean
        get() = tiles.any { rows -> rows.any { it?.sliding ?: false } }
    private var score = 0
        set(value) {
            game.score = value
            field = value
        }
    private var multiplier = 1
    private var slidingEnabled = true

    init {
        spawnTile()
    }

    private fun getOpenPosition() : Pair<Int, Int> {
        val openTiles = arrayListOf<Pair<Int, Int>>()
        tiles.forEachIndexed { ty, row ->
            row.forEachIndexed { tx, tile ->
                if (tile == null) {
                    openTiles.add(Pair(tx, ty))
                }
            }
        }
        if (openTiles.size > 0) {
            return openTiles.shuffled().take(1)[0]
        }
        return Pair(-1, -1)
    }

    private fun placeTile(tile: Tile, tx: Int, ty: Int) {
        tiles[ty][tx] = tile
        tile.tx = tx
        tile.ty = ty
    }

    private fun spawnTile() {
        val (tx, ty) = getOpenPosition()
        if (tx == -1 || ty == -1) {
            gameOver()
        } else {
            val tile = Tile(this, tx, ty)
            tiles[ty][tx] = tile
        }
    }

    private fun removeTile(tile: Tile) {
        tiles[tile.ty][tile.tx] = null
    }

    private fun reset() {
        slidingEnabled = true
        score = 0
        multiplier = 1
        tiles = Array(rows) { arrayOfNulls<Tile?>(cols) }
        spawnTile()
        game.reset()
    }

    private fun gameOver() {
        slidingEnabled = false
        game.gameOver()
    }

    /**
     * Helper method for iterating over all tiles on the board.
     *
     * Depending on which direction the tiles are "sliding", we need to
     * resolve movement and collisions in a certain order.  For example, if all
     * tiles slide to the right, then we need to make sure we move the right-most
     * tile first, because otherwise the tile would be erroneously "blocked" by
     * its neighbor and wouldn't slide when it was expected to.
     *
     * @param dir The "from" direction.
     * @param cb The callback function whose logic should be executed on each tile.  The
     * callback takes in direction and coordinates as params so you can easily access
     * the "current" tile and generically access its neighbor.
     */
    private fun directionalForEach(dir: Direction, cb: ((d: Direction, tx: Int, ty: Int) -> Unit)) {
        when (dir) {
            Direction.LEFT -> {
                (0 until rows).forEach { ty ->
                    (0 until cols).forEach { tx ->
                        cb.invoke(dir, tx, ty)
                    }
                }
            }
            Direction.RIGHT -> {
                (0 until rows).forEach { ty ->
                    (cols - 1 downTo 0).forEach { tx ->
                        cb.invoke(dir, tx, ty)
                    }
                }
            }
            Direction.DOWN -> {
                (0 until cols).forEach { tx ->
                    (0 until rows).forEach { ty ->
                        cb.invoke(dir, tx, ty)
                    }
                }
            }
            Direction.UP -> {
                (0 until cols).forEach { tx ->
                    (rows - 1 downTo 0).forEach { ty ->
                        cb.invoke(dir, tx, ty)
                    }
                }
            }
        }
    }

    /** Callback for moving a single tile.  Invoked by directionalForEach. */
    private fun slideTile(dir: Direction, tx: Int, ty: Int) {
        var tile = tiles[ty][tx]
        if (tile != null) {
            // clear tile from board in prep for placing it in new position
            removeTile(tile)

            // incrementally move tile in the specified direction, trying x dir first.
            var newTx = tx
            if (dir.x != 0) {
                while(dir.x == -1 && newTx > 0 || dir.x == 1 && newTx < cols - 1) {
                    val otherTile = tiles[ty][newTx + dir.x]
                    // if other tile exists, and they match, "combine" them by
                    // upgrading the other tile and not placing this tile back
                    // on the board.  Otherwise, replace this tile, because it's
                    // blocked by the other tile
                    if (otherTile != null) {
                        if (tile.type == otherTile.type) {
                            // score bonus points if it's a star
                            val points = otherTile.upgrade()
                            if (points > 0) {
                                score += points * multiplier
                                multiplier++
                            }
                            otherTile.combined = true
                        } else {
                            placeTile(tile, newTx, ty)
                        }
                        return
                    }
                    newTx += dir.x
                }
            }

            // same thing, but for y movement if applicable
            var newTy = ty
            if (dir.y != 0) {
                while (dir.y == -1 && newTy > 0 || dir.y == 1 && newTy < rows - 1) {
                    val otherTile = tiles[newTy + dir.y][tx]
                    if (otherTile != null) {
                        if (tile.type == otherTile.type) {
                            val points = otherTile.upgrade()
                            if (points > 0) {
                                score += points * multiplier
                                multiplier++
                            }
                        } else {
                            placeTile(tile, tx, newTy)
                        }
                        return
                    }
                    newTy += dir.y
                }
            }

            // if incremental movement test resolved without collisions, place tile in the board
            // array in its new position on the edge of the board.
            placeTile(tile, newTx, newTy)
        }
    }

    override fun keyDown(keycode: Int): Boolean {
        // reset game with "R"
        if (keycode == Input.Keys.R) {
            reset()
            return true
        }

        // use arrow keys to slide tiles
        if (slidingEnabled) {
            val isDirectional = (keycode == Input.Keys.LEFT
                    || keycode == Input.Keys.RIGHT
                    || keycode == Input.Keys.DOWN
                    || keycode == Input.Keys.UP)
            if (isDirectional && !sliding) {
                val slideDirection = when(keycode) {
                    Input.Keys.LEFT -> Direction.LEFT
                    Input.Keys.RIGHT -> Direction.RIGHT
                    Input.Keys.DOWN -> Direction.DOWN
                    Input.Keys.UP -> Direction.UP
                    else -> throw Error("Unhandled movement key: $keycode")
                }
                tiles.forEach { row -> row.forEach { it?.combined = false } } // reset "combined" flag
                directionalForEach(slideDirection) { d, tx, ty -> slideTile(d, tx, ty) }
                score++ // score a point for each turn
                spawnTile()
                return true
            }
        }

        return false
    }

    fun update(delta: Float) {
        tiles.forEach { row -> row.forEach { it?.update(delta) } }
    }

    fun draw() {
        tiles.forEach { row -> row.forEach { it?.draw() } }
    }
}