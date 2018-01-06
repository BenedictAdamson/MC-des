package uk.badamson.mc.actor.message;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A separately identifiable or utterable part of a {@linkplain Message
 * message}.
 * </p>
 */
@Immutable
public interface MessageElement {

    /**
     * <p>
     * Whether this object is <em>equivalent</em> to another object.
     * </p>
     * <p>
     * {@link MessageElement} objects have <i>value semantics</i>: this object is
     * equivalent to another object if, and only if, they have the same type and
     * their attributes are equivalent.
     * </p>
     * 
     * @param that
     *            The other object.
     * @return whether equivalent
     */
    @Override
    public boolean equals(Object that);

    /**
     * <p>
     * The information content of this message element; its length in a notional
     * essential compact form.
     * </p>
     * <ul>
     * <li>The information content of a message element is positive.</li>
     * <li>The information content of a message element is
     * {@linkplain Double#isFinite() finite}.</li>
     * <li>The unit of information content is bits of information. If information
     * content of a message element is <i>l</i>, that means there are about
     * 2<sup>l</sup> possible message elements of this type in a typical
     * context.</li>
     * </ul>
     * 
     * @return the information content
     */
    public double getInformationContent();
}
