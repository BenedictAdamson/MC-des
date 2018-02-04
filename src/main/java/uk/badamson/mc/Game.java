package uk.badamson.mc;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import uk.badamson.mc.actor.ActorInterface;
import uk.badamson.mc.simulation.Clock;
import uk.badamson.mc.simulation.Person;

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
    private final Set<Person> persons = new HashSet<>();
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
     * The simulated persons in this game instance.
     * </p>
     * <ul>
     * <li>Always have a (non null) set of persons.</li>
     * <li>The set of persons does not have a null element.</li>
     * <li>The {@linkplain Person#getClock() clock} of each person is the
     * {@linkplain #getClock() clock} of this game.</li>
     * <li>The set of persons is not publicly
     * {@linkplain Collections#unmodifiableSet(Set) modifiable}.</li>
     * </ul>
     * 
     * @return the persons
     */
    public final Set<Person> getPersons() {
        return persons;
    }

    /**
     * <p>
     * The API (service interface) through which the human player of this game
     * effect changes to the simulation of the person they are playing.
     * </p>
     * <ul>
     * <li>The player actor interface is null if the player is not (yet) playing a
     * particular person.</li>
     * <li>If the player actor interface is not null, it is one of the
     * {@linkplain #getPersons() simulated persons} of this game.</li>
     * </ul>
     * 
     * @return the interface.
     */
    public final ActorInterface getPlayerActorInterface() {
        return playerActorInterface;
    }

}
