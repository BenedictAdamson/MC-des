package uk.badamson.mc.actor.medium;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.actor.message.SimpleDirectCommand;
import uk.badamson.mc.actor.message.SimpleStatement;

/**
 * <p>
 * Hand signals, considered as a {@linkplain Medium medium} for communicating
 * {@linkplain Message messages}.
 * </p>
 * <p>
 * This medium can communicate only {@linkplain SimpleStatement simple
 * statements} and {@linkplain SimpleDirectCommand simple direct commands}.
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
