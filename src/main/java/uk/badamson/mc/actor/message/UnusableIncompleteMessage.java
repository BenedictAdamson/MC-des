package uk.badamson.mc.actor.message;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * An incomplete message that is too short to provide any useful information.
 * </p>
 */
@Immutable
public final class UnusableIncompleteMessage extends AbstractMessage {

    private final double length;

    /**
     * <p>
     * Construct an {@link UnusableIncompleteMessage} with a given length.
     * </p>
     * 
     * @param length
     *            The {@linkplain #getInformationContent() length} of this message,
     *            in bits of information.
     * @throws IllegalArgumentException
     *             If {@code length} is not positive.
     */
    public UnusableIncompleteMessage(double length) {
	if (length <= 0.0) {
	    throw new IllegalArgumentException("length " + length);
	}
	this.length = length;
    }

    /**
     * <p>
     * Whether this object is <em>equivalent</em> to another object.
     * </p>
     * <p>
     * {@link UnusableIncompleteMessage} objects have <i>value semantics</i>: this
     * object is equivalent to another object if, and only if, the other object is
     * also an {@link UnusableIncompleteMessage} and they have equivalent
     * {@linkplain #getInformationContent() lengths}.
     * </p>
     * 
     * @param that
     *            The other object.
     * @return whether equivalent
     */
    @Override
    public final boolean equals(Object that) {
	if (this == that)
	    return true;
	if (that == null)
	    return false;
	if (!(that instanceof UnusableIncompleteMessage))
	    return false;
	final UnusableIncompleteMessage other = (UnusableIncompleteMessage) that;
	return length == other.length;
    }

    @Override
    public final double getInformationContent() {
	return length;
    }

    /**
     * {@inheritDoc}
     * 
     * @param partLength
     *            {@inheritDoc}
     * @return {@inheritDoc}
     * @throws IllegalArgumentException
     *             {@inheritDoc}
     */
    @Override
    public final UnusableIncompleteMessage getPartialMessage(double partLength) {
	if (getInformationContent() <= partLength) {
	    throw new IllegalArgumentException("partLength <" + partLength
		    + "> is not less than the length of this message <" + getInformationContent() + ">.");
	}
	return new UnusableIncompleteMessage(partLength);
    }

    @Override
    public final int hashCode() {
	final int prime = 31;
	int result = 1;
	final long temp = Double.doubleToLongBits(length);
	result = prime * result + (int) (temp ^ (temp >>> 32));
	return result;
    }

    @Override
    public final String toString() {
	return "UnusableIncompleteMessage [length=" + length + "]";
    }

}
