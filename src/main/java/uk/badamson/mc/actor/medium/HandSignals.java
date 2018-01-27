package uk.badamson.mc.actor.medium;

import java.util.Objects;

import net.jcip.annotations.Immutable;
import uk.badamson.mc.actor.message.Message;
import uk.badamson.mc.actor.message.SimpleDirectCommand;
import uk.badamson.mc.actor.message.SimpleStatement;

/**
 * <p>
 * Hand signals, considered as a {@linkplain Medium medium} for communicating
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

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This medium can communicate only {@linkplain SimpleStatement simple
     * statements} and {@linkplain SimpleDirectCommand simple direct commands}.
     * </p>
     * 
     * @param message
     *            {@inheritDoc}
     * @throws NullPointerException
     *             {@inheritDoc}
     */
    @Override
    public final boolean canConvey(Message message) {
        Objects.requireNonNull(message, "message");
        return message instanceof SimpleDirectCommand || message instanceof SimpleStatement;
    }

    @Override
    public final double getTypicalTransmissionRate() {
        return 4.0;
    }

    @Override
    public final String toString() {
        return "HandSignals";
    }

}
