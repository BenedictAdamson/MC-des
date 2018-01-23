package uk.badamson.mc.actor.medium;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * Hand signals, considered as a {@linkplain Medium medium} for sending
 * {@linkplain Message messages}.
 * </p>
 */
@Immutable
public final class HandSignals implements Medium {

    /**
     * <p>
     * The sole instance of the {@link HandSignals} class.
     * </p>
     */
    public static final HandSignals INSTANCE = new HandSignals();

    @Override
    public final double getTypicalTransmissionRate() {
	return 4.0;
    }

    @Override
    public final String toString() {
	return "HandSignals";
    }

}
