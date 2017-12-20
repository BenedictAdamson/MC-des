package uk.badamson.mc.actor;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * An incomplete message that is too short to provide any useful information.
 * </p>
 */
@Immutable
public final class UnusableIncompleteMessage implements Message {

    private final double length;

    /**
     * <p>
     * Construct an {@link UnusableIncompleteMessage} with a given length.
     * </p>
     * 
     * @param length
     *            The {@linkplain #getLength() length} of this message, in bits of
     *            information.
     * @throws IllegalArgumentException
     *             If {@code length} is not positive.
     */
    public UnusableIncompleteMessage(double length) {
	if (length <= 0.0) {
	    throw new IllegalArgumentException("length " + length);
	}
	this.length = length;
    }

    @Override
    public final double getLength() {
	return length;
    }

}
