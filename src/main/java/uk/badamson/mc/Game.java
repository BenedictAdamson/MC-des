package uk.badamson.mc;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import uk.badamson.mc.actor.Actor;
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
@ThreadSafe
public final class Game {

    private final Clock clock = new Clock(TimeUnit.MILLISECONDS, 0L);
    @GuardedBy("persons")
    private final Set<Person> persons = new HashSet<>();
    @GuardedBy("persons")
    private Person playedPerson;
    @GuardedBy("persons")
    private String name;

    /**
     * <p>
     * Construct a new instance of the Mission Command game.
     * </p>
     * <ul>
     * <li>This game has no {@linkplain #getPersons() simulated persons}.</li>
     * </ul>
     */
    public Game() {
        // Do nothing
    }

    /**
     * <p>
     * Create a new simulated person that is part of this game
     * </p>
     * <ul>
     * <li>Always creates a (non null) person.</li>
     * <li>The created person does not have an {@linkplain Person#getActor() actor}
     * (it is null).</li>
     * <li>The created person is one of the {@linkplain #getPersons() persons} of
     * this game.</li>
     * <li>Does not remove any persons from this game.</li>
     * <li>Adds one person to this game.</li>
     * </ul>
     * 
     * @return the created person.
     */
    public final Person createPerson() {
        final Person person = new Person(clock);
        synchronized (persons) {
            persons.add(person);
            return person;
        }
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
     * An identifier for this instance of the Mission Command game, suitable for
     * display to the player of this game.
     * </p>
     * 
     * @return the name, or null if it this no name.
     */
    public final String getName() {
        synchronized (persons) {
            return name;
        }
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
        synchronized (persons) {
            return Collections.unmodifiableSet(new HashSet<>(persons));
        }
    }

    /**
     * <p>
     * The {@linkplain #getPersons() simulated person} that the human player of this
     * game is playing.
     * </p>
     * <ul>
     * <li>The played person is null if the player is not (yet) playing a particular
     * person.</li>
     * <li>If the played person is not null, it is one of the
     * {@linkplain #getPersons() simulated persons} of this game.</li>
     * </ul>
     * 
     * @return the interface.
     */
    public final Person getPlayedPerson() {
        synchronized (persons) {
            return playedPerson;
        }
    }

    /**
     * <p>
     * Cease having a {@linkplain #getPlayedPerson() played person}.
     * </p>
     * <ul>
     * <li>If this had a (non null) {@linkplain #getPlayedPerson() played person},
     * the {@linkplain Person#getActor() actor} of that person becomes null .</li>
     * <li>The {@linkplain #getPlayedPerson() played person} becomes null.</li>
     * </ul>
     */
    public final void releaseControl() {
        synchronized (persons) {
            if (playedPerson != null) {
                playedPerson.setActor(null);
            }
            playedPerson = null;
        }
    }

    /**
     * <p>
     * Change the {@linkplain #getName() name} of this game.
     * </p>
     * 
     * @param name
     *            the name
     */
    public final void setName(String name) {
        synchronized (persons) {
            this.name = name;
        }
    }

    /**
     * <p>
     * Have the human player of this game. take control of one of the
     * {@linkplain #getPlayedPerson() simulated persons} of this game.
     * </p>
     * <ul>
     * <li>The {@linkplain #getPlayedPerson() played person} becomes the given
     * person.</li>
     * <li>The {@linkplain Person#getActor() actor} of the given person becomes the
     * given actor.</li>
     * </ul>
     * 
     * @param actor
     *            The interface through which the simulation interacts with the
     *            human player of this game.
     * @param person
     *            The person to be controlled.
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code actor} is null.</li>
     *             <li>If {@code person} is null.</li>
     *             </ul>
     * @throws IllegalArgumentException
     *             If {@code person} is not one of the {@linkplain #getPersons()
     *             persons} of this simulation.
     */
    public final void takeControl(Actor actor, Person person) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(person, "person");
        synchronized (persons) {
            if (!persons.contains(person)) {
                throw new IllegalArgumentException("person " + person + " is not one of the persons");
            }
            person.setActor(actor);
            playedPerson = person;
        }
    }
}
