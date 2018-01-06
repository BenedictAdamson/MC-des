package uk.badamson.mc.actor.message;

import java.util.Set;

import uk.badamson.mc.actor.Actor;
import uk.badamson.mc.actor.Medium;

/**
 * <p>
 * A message that an {@linkplain Actor actor} might send through a
 * {@linkplain Medium medium} to other actors, which an actor (or actors) should
 * interpret as a command issued to them by the sender.
 * </p>
 * <p>
 * Objects of this class are therefore means by which actors can express orders
 * to sub-ordinates.
 * </p>
 */
public interface Command extends Message {

    /**
     * <p>
     * The things that the {@linkplain #getSubject() subject} is being commanded to
     * {@linkplain #getVerb() act} upon.
     * </p>
     * <ul>
     * <li>Always have a (non null) set of objects.</li>
     * <li>The set of objects does not have a null element.</li>
     * <li>Some of the objects may be <i>indirect objects</i>: things to use or
     * interact with to perform the action on the <i>direct objects</i>
     * </ul>
     * 
     * @return the objects
     */
    public Set<Noun> getObjects();

    /**
     * <p>
     * The person, element or unit that is being commanded to perform an
     * {@linkplain #getVerb() action}.
     * </p>
     * 
     * @return the subject; not null.
     */
    public Noun getSubject();

    /**
     * <p>
     * The action that the {@linkplain #getSubject() subject} is being commanded to
     * perform.
     * </p>
     * 
     * @return the verb; not null.
     */
    public Verb getVerb();
}
