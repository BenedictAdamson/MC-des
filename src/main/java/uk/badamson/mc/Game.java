package uk.badamson.mc;

import java.util.concurrent.TimeUnit;

import uk.badamson.mc.actor.ActorInterface;
import uk.badamson.mc.simulation.Clock;

/**
 * <p>
 * One instance of the Mission Command game.
 * </p>
 * <p>
 * Each game consists of one simulated world.
 * </p>
 */
public final class Game {

    private final Clock clock = new Clock(TimeUnit.MILLISECONDS, 0L);
    private ActorInterface playerActorInterface;

    /**
     * <p>
     * Construct a new instance of the Mission Command game.
     * </p>
     */
    public Game() {
        // Do nothing
    }

    /**
     * <p>
     * The simulation clock of this game.
     * </p>
     * <p>
     * All simulated objects that are part of this game use this clock.
     * </p>
     * 
     * @return the clock; not null.
     */
    public final Clock getClock() {
        return clock;
    }

    /**
     * <p>
     * The API (service interface) through which the human player of this game
     * effect changes to the simulation of the person they are playing.
     * </p>
     * 
     * @return the interface; or null if the player is not (yet) playing a
     *         particular person.
     */
    public final ActorInterface getPlayerActorInterface() {
        return playerActorInterface;
    }

}
