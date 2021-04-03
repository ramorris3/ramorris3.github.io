---
title: "Hybrid Entity Component Systems"
date: 2021-04-03
draft: false
summary: "Why (and how) I almost always use Ashley ECS for LibGDX projects."
tags: ["ashley ecs", "libgdx", "sample project", "shmup"]
---

When I'm starting a new LibGDX project, I almost always include [Ashley](https://github.com/libgdx/ashley/wiki).  However, I don't necessarily use it in the pure, decoupled, bags-o'-data way that most entity-component-system (ECS) libraries were meant to be used.

Today, I'm going to quickly introduce you to ECS's by talking about some of the problems they were built solve.  Then, I'm going to talk about how I personally adapt the tools of the brilliant Ashley ECS library to fit my project needs in a less conventional way: what I'll refer to as the "hybrid" approach to using ECS's.

## Object-Oriented Inheritence vs. Data-Driven Composition

I want to briefly go over the high-level idea of ECS's without going too deep. [Bob Nystrom](http://stuffwithstuff.com/) gives an in-depth explanation of the component design pattern in his fantastic book, [*Game Programming Patterns*](http://gameprogrammingpatterns.com/), which you can read for free online. Please check it out if you want a deeper dive (and a much more eloquent one, at that).

The Entity Component System design pattern is a decoupling pattern.  It's here to help us out when we have a single entity (player, enemy, item, whatever) that wants to span multiple domains of logic, but we don't want those domains to be coupled to one another.  

### Inheritance With A DRY-Friendly Base Class

We've all written `Player` classes that are a mile long.  If you're writing a simple platformer, then your `Player` object is going to be similar to your `Enemy` objects in so many different ways, but just different enough that you might struggle to reuse code.  

You might try to DRY up your code by pulling some of that shared logic -- position, velocity, acceleration, `isOnGround`, sprite rendering, etc. -- into a base class for both `Player` and `Enemy` to inherit from.  But depending on the complexity of your code, that might not end up being very readable or maintable.  

You've got loads of different domains for each entity, and maybe they overlap *most* of the time across all your game objects, but not all the time. For example, both your player and skeleton entities need to accelerate downwards and collide with platforms, and they both need to be able to move left and right.  But your player takes input from a controller to tell it how to move, while the skelly enemies have some kind of AI logic that tells them what to do.

Let's make it even *more* difficult by saying you have a ghost enemy that *doesn't* collide with any platforms -- it just floats through them -- and also *doesn't* have gravity to pull it downwards.  But you still need the system to acknowledge that this ghost is an enemy that can hurt the player, just like the other grounded skeleton enemies. Also, all three entity types cover the "draw" domain -- they need to be rendered on the screen.

![Illustrated image of complicated inheritance hierarchy with player, skeleton, and ghost](/entity-component-systems/inheritance.png "Inheritance can get complicated really quickly.")

### Bags of Data and Logical Systems

ECS's favor composition over inheritance.  Rather than creating a generalized inheritance tree of all that shared logic -- and as a result, setting ourselves up to frequently override it in painful ways -- we split each shared domain into its own component class.  Our player and enemy entities each own instances of these component classes, but only the ones that are relevant to them.

![Illustrated image of player, skeleton, and ghost classes with components](/entity-component-systems/components.png "With components, our game objects become declarative bags o' data.")

We still have one problem, though: where does the logic go?  We've moved the input processing into its own component (and that component is now only being used by the player, but not the skelly or the ghost.)  But we still need the code that processes input every frame, and we sure as hell don't want to clutter the `Player` class's `update` method with it.  This problem is even more relevant for components that are going to be used by lots of different entities: for example, the `DrawComponent` of every single entity that gets drawn to the screen.

This is where the "System" part of our Entity Component System comes in:  we have a `DrawSystem` class that says, "Hey, I don't care if you're a ghost, skeleton, player, or eggplant -- if you have a `DrawComponent`, I'm gonna draw ya."

![Illustrated image of logical systems](/entity-component-systems/systems.png "Systems don't have to be 1-to-1 with components.  For example, the physics system can handle movement and collisions differently depending on which components each entity has.")

When you break out the domains into components like this, you essentially reduce your game entities to simple bags of data.  These little parcels of data flow through a set of manageable and discrete systems in a given order each frame.  Each system is only concerned with its domain, and the entities that show up in that system for processing are only those which own components relevant to that same domain (`x`, `y` in a `PositionComponent`, processed by the `PhysicsSystem`, for example).

Suddenly your code is decoupled, maintainable, and understandable; if you wanna add physics to an object (or debug a physics issue), you *only* have to look at physics code.  No more digging through a 5,000-line `Player.kt` file to figure out why the player falls through platforms when they should land on them.

## All-In With ECS's

By now, I'll assume you now have a decent enough high-level understanding of the ECS design pattern to be able to follow along with the code samples.  Ashley's API is well-designed enough that you shouldn't have any trouble, even if these concepts are new to you.  

However, if you want more details, check out the [Ashley documentation on GitHub](https://github.com/libgdx/ashley/wiki).  (For the following code samples, especially look at the ["How to use Ashley"](https://github.com/libgdx/ashley/wiki/How-to-use-Ashley) and ["Built in Entity Systems"](https://github.com/libgdx/ashley/wiki/Built-in-Entity-Systems) articles.)

Let's take a look at some Ashley code now.  In this example, we've just been enlightened by the godly, pure, decoupled glory of ECS's and we're going to break *everything* up:

```kotlin
class PositionComponent(val x: Float, val y: Float) : Component
class VelocityComponent(val x: Float, val y: Float) : Component

class MovementSystem : IteratingSystem(
    Family.all(PositionComponent::class.java, VelocityComponent::class.java).get()
) {
	private val pm: ComponentMapper<PositionComponent> = ComponentMapper.getFor(PositionComponent.class)
	private val vm: ComponentMapper<VelocityComponent> = ComponentMapper.getFor(VelocityComponent.class)

	override fun processEntity(entity: Entity?, deltaTime: Float) {
        if (entity != null) {
            position: PositionComponent = pm.get(entity)
            velocity: VelocityComponent = vm.get(entity)

            position.x += velocity.x * deltaTime
		    position.y += velocity.y * deltaTime
        }
	}
}

class Player(engine: Engine, x: Float, y: Float) : Entity() {
    private val position = PositionComponent(x, y)
    private val velocity = VelocityComponent(0f, 0f)
    // input component, draw component, etc.

    init {
        add(position)
        add(velocity)
        engine.addEntity(this)
    }
}
```

This is pretty neat, but your code is going to be a lot more complex than this.  You're going to have way more components, and you're going to have way more systems.  

When I first forayed into using Ashley in my projects, I ran into lots of hard-to-debug issues.  Usually they stemmed from splitting things up *too much*: The player wasn't moving when it was supposed to, enemies randomly disappeared off the screen, entities weren't cleaning themselves up when they died, etc.

When you're the only developer, and you split your logic into dozens of domains, then you have to intimately know all those domains.  You have to know which components are going to "mark" an entity as belonging to a particular system.  You might not even think through your domain-splitting enough, and end up with some architectural issues that cause you grief and the need to do a big refactor in the middle of your project.

## The Hybrid: Plug In and Forget

When you start out with ECS's, you might wring your hands and waste hours trying to figure out the *perfect* way to split your domains cleanly.  How atomic should your components be?  Do you have a single `BodyComponent` with all the physics info for your entities, or do you split it up into `PositionComponent`, `VelocityComponent`, etc?  Also, is _any_ amount of inheritance ok?

Of course it's ok.  I'm giving you permission to quit worrying about all of that.  The truth is, you're not here to come up with a perfectly pure, elegant, generic, flawless codebase.  You're here to make games!  If you have a system that works for you, then run with it.  My system happens to combine inheritance trees and an entity component system, and it has worked really well for me in my projects.  

Below, I'll show you what my setup usually looks like when I start a new project.  My goals with this hybrid system are to:

1. Worry about an entity's frame-by-frame gameplay logic, rather than worrying about managing families of entities.
2. Make it super-easy to manage temporary entity states.
3. Make it easy for my entities to clean up after themselves when I don't need them anymore.

It turns out that Ashley gives us a fantastic set of tools to accomplish each of these goals.

## 1. Plug-and-Play Entities

**Goal**: Worry about an entity's frame-by-frame gameplay logic, rather than worrying about managing families of entities.

**Solution**: Isolate each major “slice” of the core game loop in its own system, and then plug the entities into those systems once and forget about them.

&nbsp;

If you write a game without Ashley, it will probably look something like this:

```kotlin
abstract class Entity {
    abstract fun update(delta: Float)
    abstract fun draw()
}

class Player : Entity() {
    override fun update(delta: Float) {
        processInput()
        move()
        handleCollisions()
    }

    override fun draw() {
        drawSprite()
    }

    // ...
}

class Enemy : Entity() {
    override fun update(delta: Float) {
        moveTowardsPlayer()
        handleCollisions()
    }

    fun draw() {
        drawSprite()
    }

    // ...
}

class MyGame : Game() {
    private lateinit val batch: SpriteBatch
    private val entities: ArrayList<Entity> = listOf()
    
    init {
        val p = Player()
        entities.add(p)
    }

    override fun create() {
        batch = SpriteBatch()
    }

    override fun render(delta: Float) {
        if (readyToSpawn()) {
            val e = Enemy()
            entities.add(e)
        }
        
        entities.forEach { it.update(delta) }
        batch.begin()
        entities.forEach { it.draw() }
        batch.end()
    }

    // ...
}
```

This is an incredibly barren example, but it doesn't even address some of the most basic issues that come along with entity management in a game.  For example, who should be responsible for adding an entity to the game when it spawns?  And who's responsible for removing it when it dies?  In what order should we render these entities?  And so on.

Also, this code isn't very declarative.  We're writing code that goes into detail about *how* we're going to update and draw each component, rather than just declaring *what* each component should do when it's time to update and draw.

To address these problems, I use Ashley as a sort of plug-and-play solution for my entities.  The idea is this: isolate each major "slice" of the core game loop into its own system, and then plug the entities into those systems and forget about them. (We'll talk about removing them a little bit later.)

Let's dig in to an example of that same code, but using Ashley to facilitate this new plug-and-play format.  First, we'll slice our core game loop into two major domains: update, and draw.  We'll need a component and system for each domain.

```kotlin
interface Updatable {
    fun update(delta: Float)
}

class UpdateComponent : Component

class UpdateSystem : IteratingSystem(
    Family.all(UpdateComponent::class.java).get()
) {
    override fun processEntity(entity: Entity?, deltaTime: Float) {
        if (entity != null && entity is Updatable) {
            entity.update(deltaTime)
        }
    }
}

interface Drawable {
    fun draw()
}

class DrawComponent(val zIndex: Int = 0) : Component

class DrawSystem : SortedIteratingSystem(
    Family.all(DrawComponent::class.java).get(),
    ZComparator()
) {
    override fun processEntity(entity: Entity?, deltaTime: Float) {
        if (entity != null && entity is Drawable) {
            entity.draw()
        }
    }

    private static class ZComparator : Comparator<Entity> {
		private val dcm: ComponentMapper<DrawComponent> = ComponentMapper.getFor(DrawComponent::class.java)
		
		override fun compare(Entity e1, Entity e2) : Int {
			return (signum(dcm.get(e1).zIndex - dcm.get(e2).zIndex)).toInt();
		}
	}
}
```

For our `UpdateSystem`, we're just using Ashley's built-in `IteratingSystem` class.  To set this up, all we have to do is specify the components that put an entity in the "update" family (in this case, it's just the `UpdateComponent`).  After that, we just override `processEntity`.  Also, we're using a simple `Updatable` interface here to make it easy to call `update` on any entity that gets gathered up by the `UpdateSystem` for processing.

The `DrawSystem` is the exact same thing, but with one small difference: sorting.  Ashley's `SortedIteratingSystem` behaves exactly the same way as the `IteratingSystem`, except it takes in a custom entity comparator so that it knows in which order to process the entities.  In this case, it's going to sort them based on z-index, and so the player will always be drawn over the top of enemies, should they overlap.

Now that we've split up our domains, our entities become much more declarative:

```kotlin
class Player(game: Game) : Entity(), Updatable, Drawable {
    init {
        add(UpdateComponent())
        add(DrawComponent(zIndex = 1))
        game.engine.addEntity(this)
    }

    override fun update(delta: Float) {
        processInput()
        move()
        handleCollisions()
    }

    override fun draw() {
        drawSprite()
    }
}

class Enemy(game: Game) : Entity(), Updatable, Drawable {
    init {
        add(UpdateComponent())
        add(DrawComponent())
        game.engine.addEntity(this)
    }

    override fun update(delta: Float) {
        moveTowardsPlayer()
        handleCollisions()
    }

    override fun draw() {
        drawSprite()
    }
}
```

Our `Player` and `Enemy` classes are each saying, "Hey, I'm updatable, and here's what I do on update.  Also, I'm drawable, and here's what I draw, and at what z-index."  In their `init` blocks, they register themselves with the core ECS engine, which is a member of the main `Game` class (see below).

Our game class becomes pretty simple, too.  We instantiate an engine, add the update and draw systems in the order in which we want them to process each frame, and then we simply call `engine.update(delta)` each frame.

Notice too that since the Player and Enemy classes are each responsible for registering themselves with the core ECS engine, we don't even need to store any reference to them in our `Game` class.

```kotlin
class MyGame : Game() {
    private lateinit val batch: SpriteBatch
    private val engine = Engine()
    
    override fun create() {
        batch = SpriteBatch()
        engine.addSystem(UpdateSystem())
        engine.addSystem(DrawSystem())
        Player(this)
    }

    override fun render(delta: Float) {
        if (readyToSpawn()) {
            Enemy(this)
        }
        
        batch.begin()
        engine.update(delta)
        batch.end()
    }

    // ...
}
```

Our first goal is accomplished.  We let Ashley do all the heavy lifting when it comes to registering and processing entities.  With all of that entity management logic abstracted away from us, we get to focus almost all of our energy on describing the behavior of each entity via the `update` and `draw` methods.  We plug them into the core game loop, and then forget about managing them.

You might think that the verbosity here makes the ECS setup overkill, but it's not too hard to imagine how quickly the `Game` class could explode in complexity in a real scenario. The more domains you add, the more entity management logic you're writing, and the uglier (and harder to maintain) your `Game` class gets.  

## 2. Manage Temporary Entity States

**Goal**: Make it easy to manage temporary entity state without increasing logical complexity in our `Entity` classes.

**Solution**: Use system-less components as state "flags" and let the systems handle the state.

&nbsp;

So far we've used Ashley to hook our entities directly into the core game loop without having to worry about managing those entities by hand.  But we know that the ECS pattern offers so much more than just categorizing and managing lists of game objects.  We're going to use Ashley to manage entity state.  To illustrate this, let's look at a simple way to temporarily disable entity processing: the system-less `InactiveComponent`.

For this example, let's implement a bullet pool.  If you're not familiar with object pooling, the idea is simple: Rather than creating a new bullet object every time the player presses the "fire" button, you grab an existing bullet from a pool of dormant bullet objects and activate it.  When the bullet is done flying (i.e. it collides with something, or flies off the screen), you deactivate it and it goes back into the pool of inactive bullets. This is a pretty common game programming pattern meant to help manage your app's memory usage, and for a garbage-collected language like Kotlin/Java, it can make a world of performance difference.

I won't go into further detail in this article (I'm planning on dedicating a whole blog post to memory management). I'll just say that LibGDX has really great built-in support for object pooling, and that you can read up on it by going to the [Memory Management page on the LibGDX wiki](https://github.com/libgdx/libgdx/wiki/Memory-management).

So let's create an object pool of bullets and plug those bullets in to the core update/draw loop just like we did with enemies above.

```kotlin
class BulletManager(private val engine: Engine) {
    private val bulletPool = object: Pool<Bullet>() {
        override fun newObject() = Bullet()
    }

    private inner class Bullet : Entity(), Pool.Poolable, Updatable, Drawable {
        private var alive = false
        private val position = Vector2()

        init {
            add(UpdateComponent())
            add(DrawComponent())
            engine.addEntity(this)
        }

        fun revive(x: Float, y: Float) {
            alive = true
            position.set(x, y)
        }

        // method required by Pool.Poolable
        // this gets called when the object is "freed" back into the object pool
        override fun reset() {
            alive = false
        }

        override fun update(delta: Float) {
            if (alive) {
                y += delta * 100f
                if (offScreen() || collided()) {
                    bulletPool.free(this)
                }
            }
        }

        override fun draw() {
            if (alive) {
                drawSprite()
            }
        }
    }

    fun fireBullet(x: Float, y: Float) {
        val b = bulletPool.obtain()
        b.revive(x, y)
    }
}
```

So in this example we have a `BulletManager` class that encapsulates the object pool.  The rest of our codebase doesn't really need to know about the actual `Bullet` class that represents a single bullet.  It can be used like this in the `Player` class:

```kotlin
// in init
private val bullets = BulletManager(engine)

// in update()
if (fireButtonPressed) {
    bullets.fireBullet(position.x, position.y)
}
```

![Animated GIF of 2D space shooter game](/entity-component-systems/bullet-hell.gif "When you're spawning bullets this frequently, you don't want to create brand new instances in memory every single time a bullet is fired.")

Each individual bullet acts exactly like any other entity in our system so far.  It hooks into the game's core loop by registering with the ECS engine, and then it updates itself and draws itself when asked to do so.  The only difference here is that it *conditionally* updates and draws, only when it's alive.

It doesn't feel great to wrap entire methods in the `if (alive)` conditional.  In this simple example, that's somewhat manageable, but what if there are other domains of logic that we want to only apply to `alive` objects?  For example, instead of calling `handleCollision()` in our entities' `update` methods, let's say we have a `PhysicsSystem` that groups entities together by collision category and then handles all the collision checking for us.  Suddenly your bullets and everything else that they collide with need to know about that `alive` state, and check it before collisions are processed.

To solve this, we'll introduce a new component called `InactiveComponent` and just use it as a sort of flag for our ECS engine.  Any systems that process entities will simply ignore any entities with an `InactiveComponent` attached to them.  

First, we'll replace the `alive` boolean in the `Bullet` class with that new inactive flag component:

```kotlin
private inner class Bullet : Entity(), Pool.Poolable, Updatable, Drawable {
    private val inactive = InactiveComponent()
    private val position = Vector2()

    init {
        add(inactive)
        add(UpdateComponent())
        add(DrawComponent())
        engine.addEntity(this)
    }

    fun revive(x: Float, y: Float) {
        remove(InactiveComponent::class.java)
        position.set(x, y)
    }

    // method required by Pool.Poolable
    // this gets called when the object is "freed" back into the object pool
    override fun reset() {
        add(inactive)
    }

    override fun update(delta: Float) {
        y += delta * 100f
        if (offScreen() || collided()) {
            bulletPool.free(this)
        }
    }

    override fun draw() {
        drawSprite()
    }
}
```

The code hasn't changed really at all.  The only two differences are we're adding/removing the `InactiveComponent` instead of flipping a boolean switch, and we've removed the conditional from the update/draw methods.

Now, let's change the way our `UpdateSystem` and `DrawSystem` collect entities for processing.  They'll now exclude any entity with an `InactiveComponent`:

```kotlin
class UpdateSystem : IteratingSystem(
    Family
        .all(UpdateComponent::class.java)
        .exclude(InactiveComponent::class.java)
        .get()
) {
    override fun processEntity(entity: Entity?, deltaTime: Float) {
        if (entity != null && entity is Updatable) {
            entity.update(deltaTime)
        }
    }
}

// ...

class DrawSystem : SortedIteratingSystem(
    Family
        .all(DrawComponent::class.java)
        .exclude(InactiveComponent::class.java)
        .get(),
    ZComparator()
) {
    override fun processEntity(entity: Entity?, deltaTime: Float) {
        if (entity != null && entity is Drawable) {
            entity.draw()
        }
    }

    private static class ZComparator : Comparator<Entity> {
        // ...
	}
}
```

You might have expected us to manually need to check for the existence of the `InactiveComponent` on the entities within the `processEntity` method.  But the solution is much cleaner: we simply use `Family.exclude` in the system's constructor, and Ashley takes care of the rest for us.

This was a simple example, but there are lots of other ways you can use components to mix and match different pieces of state for your entities.  Here are some ideas (and you'll see some of these in action within the [sample project for this article](https://github.com/ramorris3/ramorris3.github.io/tree/main/examples/hybrid-ecs)):

* Add a `HitStunComponent` whenever an enemy gets hit.  This component doesn't stop the `DrawSystem` from rendering them, but it stops the `UpdateSystem` from updating them.  Add a barebones `HitStunSystem` which decrements a countdown on the `HitStunComponent`, and when the countdown finished, remove the component from the entity.
* Create a `FlashComponent` with a boolean `isDrawing` and a countdown timer.  In the `DrawSystem`, only draw entities who either don't have a flash component, or whose flash component's `isDrawing` is `true`.  Add a barebones `FlashSystem` which decrements the countdown on the `FlashComponent` and removes it from the entity when the countdown is done.
* Add a `FrozenComponent` to enemies that get hit with a freeze gun.  This excludes them from the `UpdateSystem`, but keeps them in the collision-checking `PhysicsSystem`, and continues to include them in the rendering of `DrawSystem`.  The `DrawSystem` could also draw them with a special blue-tint shader if it sees a `FrozenComponent`.

You can get pretty creative with it, so go crazy.

That takes care of our second goal.  Our core game loop logic within each entity is still clean and simple, and we don't have to worry about a bunch of state as it pertains to entity management.  We just let the systems take care of all of that.

## 3. Entity Clean-Up

**Goal**: Make it easy for my entities to clean up after themselves when I don't need them anymore.

**Solution**: Use `engine.removeEntity(entity)`, and Ashley takes care of that for you.  In some special cases, use an `EntityListener` to handle the `entityRemoved` event for other non-ECS cleanup logic.

&nbsp;

So we've talked about how to temporarily remove entities from our ECS engine by excluding them from certain systems.  But what do we do when it's time to remove them from the system altogether?  For example, what happens to an enemy when you kill it, or an item when you pick it up?

If we're only concerned with removing something from the engine, then Ashley already has us covered there: simply call `engine.removeEntity(entity)`.  This will schedule the entity for removal, meaning it won't be gathered by any of your systems.  Then, once the engine finishes its processing for that frame, it'll remove the entity from its internal list of entities.

Often that will be all you need.  But there *are* times when you need to be able to handle some other kind of general cleanup logic for each entity as you remove it from your ECS engine.  And in that case, you don't want to couple that cleanup in a way that requires you remember to do it every time you call `engine.removeEntity(entity)`.

With Ashley, we can register event listeners with our engine and let the cleanup logic happen in there.  We set this up one time and register it with the ECS engine, then forget about it.  This means we can plug our entities into the engine when we want, remove them when we need to, and forget about all the related logic that needs to execute whenever we remove entities from our game.

As a simple example, let's say that you're using [Box2D](https://github.com/libgdx/libgdx/wiki/Box2d) for your physics.  If so, all of your physical entities are going to plug in to your Box2D `World`, so they'll be referenced and managed in two places: in your Ashley `engine` and in your Box2D `world`.

In this case, you'll need to remove the Box2D body from your game when its related entity is removed from the Ashley engine.  Otherwise, you'll have entities which aren't updating or rendering, but which are still colliding and interacting with the other physical objects in your game.

Ashley makes this simple:

```kotlin
val physicsFamily = Family.all(PhysicsComponent::class.java).get()
val physicsListener = object : EntityListener {
    override fun entityAdded(entity: Entity?) {
        // get BodyComponent from entity, 
        // use it to add physics body to Box2D world
    }
    override fun entityRemoved(entity: Entity?) {
        // get BodyComponent from entity, 
        // use it to remove physics body from Box2D world
    }
}
engine.addEntityListener(physicsFamily, physicsListener)
```

Notice that I'm talking mostly about entity cleanup here, but that the entity listener feature of Ashley solves this broader problem of coupling between multiple systems outside of our ECS engine.  In the example above, we demonstrate how we not only remove the physics body from our `world` when it's removed from our core ECS `engine`, but also *add* it to the `world` when we first register it with our ECS `engine`.

## Conclusion

To recap:

* Entity Component Systems (ECS's) solve coupling problems that inheritance often isn't as well-equipped to solve.
* ECS's can make your game logic harder to reason with if you swing your pendulum too hard in the "split-up domains" direction.
* With our "hybrid" approach, we can use Ashley's tools to manage our entities in a meaningful way without over-engineering.
* With the hybrid approach, we get to plug our entities into the core game loop and then forget about them.
* We can also use Ashley to manage temporal state for our entities in a self-contained way without blowing up the logical complexity of our entity classes. 
* When it's time to remove entities from our ECS engine, we can use `EntityListener` objects to execute logic that needs to happen on entity removal in an isolated way -- again, without complicating our entity classes too much.

Thanks for joining me.  If you want to look at an example project, I've created a small arcade shooter that demonstrates my hybrid ECS setup.  You can check out [the complete source code here on GitHub](https://github.com/ramorris3/ramorris3.github.io/tree/main/examples/hybrid-ecs).
