package uk.badamson.mc.actor.message;

import java.util.Set;

/**
 * <p>
 * An {@linkplain #getVerb() action} and the ({@linkplain #getObjects() objects}
 * acted upon.
 * </p>
 */
public interface VerbPhrase extends MessageElement {

    /**
     * {@inheritDoc}
     * 
     * <p>
     * The information content of a verb phrase has the following constraints in
     * addition to the constraints on a
     * {@linkplain MessageElement#getInformationContent() message element}.
     * </p>
     * <ul>
     * <li>The information content of a verb phrase exceeds the total
     * {@linkplain MessageElement#getInformationContent() information content} of
     * the {@linkplain #getVerb() verb} and {@linkplain #getObjects() objects}
     * elements of the phrase. The extra information content provides information
     * about the relationship of the message elements.</li>
     * </ul>
     * 
     * @return the information content
     */
    @Override
    public double getInformationContent();

    /**
     * <p>
     * The things that the {@linkplain #getSubject() subject} is
     * {@linkplain #getVerb() acting} upon, or is being commanded to act upon.
     * </p>
     * <ul>
     * <li>Always have a (non null) set of objects.</li>
     * <li>The set of objects does not have a null element.</li>
     * <li>Some of the objects may be <i>indirect objects</i>: things used or
     * interacted with to perform the action on the <i>direct objects</i>
     * </ul>
     * 
     * @return the objects
     */
    public Set<Noun> getObjects();

    /**
     * <p>
     * The action that the {@linkplain #getSubject() subject} performed or is being
     * commanded to perform.
     * </p>
     * 
     * @return the verb; not null.
     */
    public Verb getVerb();
}
