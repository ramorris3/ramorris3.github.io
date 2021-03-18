---
title: "Kotlin High-Order Functions: directionalForEach"
date: 2021-03-17T15:08:48-06:00
draft: false
summary: "Use high-order functions in Kotlin to avoid messy, duplicated code in a tile-based puzzle game."
tags: ["kotlin", "sample project", "puzzle games", "rogueslide"]
---

When you have a tile-based game, it's common to store actors in a double-nested list, and then have each actor perform an action by iterating over the list.  But what if you don't always want your actors to act in left-to-right, top-down order?  We're going to use [high-order functions in Kotlin](https://kotlinlang.org/docs/lambdas.html) to avoid messy, duplicated code.

## The Problem: Slidey Tiles
Let's look at a simple 2048-style puzzle game.  The user gives directional input, and all the tiles on the board move based on that input.

![Simple example of a common tile-sliding puzzle game](/directional-foreach/stars-example.gif "Our example project is like 2048 lite: stars don't grow when they combine.")

The simplest (or at least most intuitive) way to store these tiles in memory would be to create a 2d array with nullable entries. Assuming the tiles are all responsible for their own update and draw logic, it's really easy to update every tile at once each frame:

```kotlin
// empty 2d array of nullable Tile objects
val tiles = Array(numRows) { arrayOfNulls<Tile?>(numCols) }

// update all tiles
tiles.forEach { row -> row.forEach { it?.update(delta) } }

// draw all tiles
tiles.forEach { row -> row.forEach { it?.draw() } }
```

This works out great when the order in which the tile does a thing doesn't matter.  But what about when the order *does* matter? Look at this example of a game board, and let's say the user tries to slide all the tiles down.

![Example board state](/directional-foreach/board-state.png "If the user slides all the tiles down, what's going to happen if we iterate through the tile objects from left to right, top to bottom in our code?")

Here, we need to iterate over every single tile, and move that tile downwards as far as it can go (until it collides with another tile, or hits the edge of the board).  You might try to iterate like we did above:

```kotlin
// Bad
tiles.forEach { row -> row.forEach { tile -> 
    while(!tile.atBottomOfBoard) {
        val nextTile = tile.nextTileBelow
        if (nextTile != null) {
            tile.collide(nextTile)
            return
        }
        tile.moveDown()
    }
} }
```

When you playtest, you'll quickly see some problems with this: Number 1 on the top row can't move down, because 3 is blocking it!  And number 2 on the top row will stay put, because the 4's beneath it haven't slid down and combined yet.  In more general terms, you need to slide each tile downwards by iterating from the bottom up instead of going top-to-bottom.


## The Naive Approach With Lots Of Duplicated Code

So now you need to take that logic that slides a single tile and handle it for each direction.  Let's start with something like this:

```kotlin
when (dir) {
    Direction.LEFT -> {
        (0 until rows).forEach { ty ->
            (0 until cols).forEach { tx ->
                val tile = tiles[ty][tx]
                if (tile != null) {
                    while(!tile.atLeftOfBoard) {
                        val nextTile = tile.nextTileLeft
                        if (nextTile != null) {
                            tile.collide(nextTile)
                            return
                        }
                        tile.moveLeft()
                    }                    
                }
            }
        }
    }
    Direction.RIGHT -> {
        (0 until rows).forEach { ty ->
            (cols - 1 downTo 0).forEach { tx ->
                val tile = tiles[ty][tx]
                if (tile != null) {
                    while(!tile.atRightOfBoard) {
                        val nextTile = tile.nextTileRight
                        if (nextTile != null) {
                            tile.collide(nextTile)
                            return
                        }
                        tile.moveRight()
                    }                    
                }
            }
        }
    }
    Direction.DOWN -> {
        (0 until cols).forEach { tx ->
            (0 until rows).forEach { ty ->
                val tile = tiles[ty][tx]
                if (tile != null) {
                    while(!tile.atBottomOfBoard) {
                        val nextTile = tile.nextTileDown
                        if (nextTile != null) {
                            tile.collide(nextTile)
                            return
                        }
                        tile.moveDown()
                    }                    
                }
            }
        }
    }
    Direction.UP -> {
        (0 until cols).forEach { tx ->
            (rows - 1 downTo 0).forEach { ty ->
                val tile = tiles[ty][tx]
                if (tile != null) {
                    while(!tile.atTopOfBoard) {
                        val nextTile = tile.nextTileUp
                        if (nextTile != null) {
                            tile.collide(nextTile)
                            return
                        }
                        tile.moveUp()
                    }                    
                }
            }
        }
    }
}
```

Yikes.  There are a lot of blocks of code that look *very* similar, but just different enough to be hard to repeat.  Also, the code here has been simplified quite a bit. In the actual [example project](https://github.com/ramorris3/halcyon), there's more that goes into tile sliding and collisions: increasing the player's score and updating the UI accordingly, checking if the board is full (game over), etc.  It goes without saying that your directional loops are going to get huge if you're not careful.

Let's start by making tile slides/collisions generic, and then plopping that function in there.

```kotlin
when (dir) {
    Direction.LEFT -> {
        (0 until rows).forEach { ty ->
            (0 until cols).forEach { tx ->
                val tile = tiles[ty][tx]
                tile?.slide(dir)
            }
        }
    }
    Direction.RIGHT -> {
        (0 until rows).forEach { ty ->
            (cols - 1 downTo 0).forEach { tx ->
                val tile = tiles[ty][tx]
                tile?.slide(dir)
            }
        }
    }
    Direction.DOWN -> {
        (0 until cols).forEach { tx ->
            (0 until rows).forEach { ty ->
                val tile = tiles[ty][tx]
                tile?.slide(dir)
            }
        }
    }
    Direction.UP -> {
        (0 until cols).forEach { tx ->
            (rows - 1 downTo 0).forEach { ty ->
                val tile = tiles[ty][tx]
                tile?.slide(dir)
            }
        }
    }
}
```

That's much better, but you'll find yourself repeating that big outer `when(dir)` block any time you want to directionally iterate over tiles and do anything other than sliding the tiles.

**Note:** If this feels like a contrived example, that's because it is. But this was a very real, practical problem for [my game Rogueslide](https://store.steampowered.com/app/1443100/Rogueslide/).  

![Rogueslide demo gif](/directional-foreach/rogueslide-example.gif)

In that game, slide logic is separated from collide logic.  Even more difficult, tiles each behave differently depending on their type, but all of their behaviors are impacted by the slide direction.  If each tile has methods that all need to be called in a directional order, then we're looking at tons of unnecessary `when(dir)` blocks scattered throughout our code.

```kotlin
// Bad example with tons of repeated directional double-loops
def slideTiles(dir: Direction) {
    when(dir) {
        Direction.LEFT -> {
            (0 until rows).forEach { ty ->
                (0 until cols).forEach { tx ->
                    val tile = tiles[ty][tx]
                    tile?.slide(dir)
                }
            }
        },
        // ... and for the other 3 directions
    }
}

def collideTiles(dir: Direction) {
    when(dir) {
        Direction.LEFT -> {
            (0 until rows).forEach { ty ->
                (0 until cols).forEach { tx ->
                    val tile = tiles[ty][tx]
                    tile?.collide(dir)
                }
            }
        },
        // ... and for the other 3 directions
    }
}

def preSlide(dir: Direction) {
    when(dir) {
        Direction.LEFT -> {
            (0 until rows).forEach { ty ->
                (0 until cols).forEach { tx ->
                    val tile = tiles[ty][tx]
                    tile?.preSlide(dir)
                }
            }
        },
        // ... and for the other 3 directions
    }
}

def postSlide(dir: Direction) {
    when(dir) {
        Direction.LEFT -> {
            (0 until rows).forEach { ty ->
                (0 until cols).forEach { tx ->
                    val tile = tiles[ty][tx]
                    tile?.postSlide(dir)
                }
            }
        },
        // ... and for the other 3 directions        
    }
}
```

You'll find yourself doing a lot of copying/pasting if you follow this pattern.

## The DRY-friendly Solution: High-order Functions and Lambdas

This is where [high-order functions](https://kotlinlang.org/docs/lambdas.html) become our friend. Higher-order functions are simply functions that take other functions (lambdas) as arguments. They're very useful when we have some outer "wrapper" logic that doesn't really care about what happens inside.  If you've worked with promises in JavaScript, you're probably pretty familiar with this concept.

A great example of a higher-order function is Kotlin's `forEach` method, which we've already used extensively above. When you call `forEach` on a list, you want to do something specific to each item in the list.  But you don't want to have to write the "wrapper" logic that loops through every item each time.

We're going to do a modified version of the `forEach` method.  This new method - which we'll call `directionalForEach` - will generalize two pieces of wrapper logic for us:
  
  * Give us every non-empty tile in our 2d list of tiles
  * Give them to us in the order we care about, depending on the directional input from the user.

The `directionalForEach` function will then take **another** function - we'll call it a `callback` - as its argument.  Remember, the directional loop doesn't care what each tile does.  It doesn't care about what happens in the callback.  It's *only* concerned with the order in which those tiles do the thing.

If you're getting lost in the lingo (functions on functions with functions), bear with me.  This will make much more sense when you see it in action below:

```kotlin
// Now that this method is generalized, we only have to do this 
// directional looping logic once.
def directionalForEach(dir: Direction, callback: ((d: Direction, t: tile) -> Unit)) {
    when(dir) {
        Direction.LEFT -> {
            (0 until rows).forEach { ty ->
                (0 until cols).forEach { tx ->
                    val tile = tiles[ty][tx]
                    if (tile != null) {
                        callback.invoke(dir, tile)
                    }
                }
            }
        },
        // ... and for the other 3 directions
    }
}

// Now we can do all kinds of directionally-dependent things without 
// repeating the nasty when(dir) block
val direction = getDirectionFromInput()
directionalForEach(direction) { dir, tile -> tile.preSlide(dir) }
directionalForEach(direction) { dir, tile -> tile.slide(dir) }
directionalForEach(direction) { dir, tile -> tile.collide(dir) }
directionalForEach(direction) { dir, tile -> tile.postSlide(dir) }
```

This is much cleaner, and becomes a lot more valuable as we add more directionally-dependent tile behaviors throughout our code.  We only have to write the directional-loop logic once.  Once that's in place and generalized, we get to compartmentalize our tile logic into small, bite-sized functions that only care about the current tile, and the input direction.  

As an added bonus, [Kotlin has syntactic sugar](https://kotlinlang.org/docs/lambdas.html#passing-trailing-lambdas) where you can leave the lambda expression outside of the higher-order function's parentheses if it's the last argument.  So that's neat.

## Overview and Example Project

To wrap things up, here are some takeaways:

* A higher-order function is a function that takes functions as parameters, or returns a function.
* High-order functions are useful when you have some "wrapper" logic that doesn't care about what happens inside.

Even though this article's example was fairly contrived, I hope it helps you think of ways you can generalize your logic and reuse tedious bits of code. There are limitless use cases. To give you some ideas, here are some of the many places I've used high-order functions in my game code:

* A `Collision` class with an local lambda variable called `callback` that gets invoked whenever the collision happens.
* A `Timer` utility class that plugs into the game's core loop, decrements itself over time, and invokes a callback when it finishes.
* An optional `animationEnd` lambda on a custom `AnimationsManager` class that executes arbitrary logic whenever an animation finishes.

Thanks for tuning in.  If you're interested in looking at some source code, I've published a [complete sample LibGDX project for this article to github]().  (The `directionalForEach` code lives in `Board.kt`.)
