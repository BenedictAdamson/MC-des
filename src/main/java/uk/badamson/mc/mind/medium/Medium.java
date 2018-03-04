package uk.badamson.mc.mind.medium;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.mind.Actor;
import uk.badamson.mc.mind.message.Message;

/**
 * <p>
 * A transmission medium (or means) through which {@linkplain Actor actors} can
 * send {@linkplain Message messages}, from their perspective.
 * </p>
 */
@Immutable
public interface Medium {

    /**
     * <p>
     * Whether this medium can be used to convey a given messages.
     * </p>
     * <p>
     * Some media are not capable of encoding and convey some kinds of message.
     * Media able to send text or speech can convey any message, given enough time,
     * but media relying on explicit encoding of all possible messages (such as
     * {@linkplain HandSignals hand signals}) can send only those explicitly encoded
     * messages.
     * </p>
     * 
     * @param message
     *            The message of interest
     * @return whether the message can be conveyed
     * @throws NullPointerException
     *             If {@code message} is null.
     */
    public boolean canConvey(Message message);

    /**
     * <p>
     * Whether this object is <em>equivalent</em> to another object.
     * </p>
     * <p>
     * {@link Medium} objects have <i>value semantics</i>: this object is equivalent
     * to another object if, and only if, they have the same type and their
     * attributes are equivalent.
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
     * The (typical) rate at which this medium can transmit messages.
     * </p>
     * <ul>
     * <li>The transmission rate is measured in bits of information per second.
     * Faster media have higher rates. The typical time (in seconds) to send a
     * {@linkplain Message message} through the medium is the
     * {@linkplain Message#getInformationContent() length} (information content in
     * bits of information) divided by the transmission rate (in bits per
     * second).</li>
     * <li>The typical transmission rate is positive.</li>
     * <li>The typical transmission rate {@linkplain Double#isFinite(double) is
     * finite}.</li>
     * </ul>
     * 
     * @return the typical transmission rate
     */
    public double getTypicalTransmissionRate();
}
