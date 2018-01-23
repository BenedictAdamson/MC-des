package uk.badamson.mc.actor.medium;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.actor.Actor;
import uk.badamson.mc.actor.message.Message;

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
