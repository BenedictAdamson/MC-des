package uk.badamson.mc.actor.message;

import java.util.Collection;
import java.util.Objects;

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
     * The total {@linkplain #getInformationContent() information content} of a
     * collection of message elements; their total length in a notional essential
     * compact form.
     * </p>
     * <p>
     * The method treats a null element as having no (zero) information content
     * </p>
     * 
     * @return the information content
     * @throws NullPointerException
     *             If {@code elements} is null.
     */
    public static double getInformationContent(Collection<? extends MessageElement> elements) {
        Objects.requireNonNull(elements, "elements");
        double information = 0.0;
        for (MessageElement element : elements) {
            if (element != null) {
                information += element.getInformationContent();
            }
        }
        return information;
    }

    /**
     * <p>
     * The {@linkplain #getInformationContent() information content} provided by
     * having one value of a finite set of values, in a context where a value of
     * that set is expected.
     * </p>
     * <p>
     * The method treats an empty set as having no information content.
     * </p>
     * 
     * @param n
     *            the number of elements in the set
     * 
     * @return the information content
     */
    public static double getInformationContent(int n) {
        return 0 < n ? Math.log(n) / Math.log(2) : 0.0;
    }

    /**
     * <p>
     * The total {@linkplain #getInformationContent() information content} of some
     * (an array of) message elements; their total length in a notional essential
     * compact form.
     * </p>
     * <p>
     * The method treats a null element as having no (zero) information content.
     * </p>
     * 
     * @return the information content
     * @throws NullPointerException
     *             If {@code elements} is null.
     */
    public static double getInformationContent(MessageElement... elements) {
        Objects.requireNonNull(elements, "elements");
        double information = 0.0;
        for (MessageElement element : elements) {
            if (element != null) {
                information += element.getInformationContent();
            }
        }
        return information;
    }

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
     * <li>The information content of a message element is not negative.</li>
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
