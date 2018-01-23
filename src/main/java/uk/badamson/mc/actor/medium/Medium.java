package uk.badamson.mc.actor.medium;

import uk.badamson.mc.actor.Actor;
import uk.badamson.mc.actor.message.Message;

/**
 * <p>
 * A transmission medium (or means) through {@linkplain Actor actors} can send
 * {@linkplain Message messages}.
 * </p>
 */
public interface Medium {

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
