package uk.badamson.mc.mind.ai;

import java.util.Objects;

import uk.badamson.mc.mind.AbstractMind;
import uk.badamson.mc.mind.MessageTransferInProgress;
import uk.badamson.mc.mind.Mind;
import uk.badamson.mc.mind.medium.Medium;
import uk.badamson.mc.mind.message.Message;
import uk.badamson.mc.simulation.Clock;

/**
 * <p>
 * An artificial intelligence for providing a
 * {@linkplain uk.badamson.mc.simulation.Person simulated person} with a
 * {@linkplain Mind mind}.
 * </p>
 */
public final class AI extends AbstractMind {
    private final Clock clock;

    private Mind player;

    /**
     * <p>
     * Construct an artificial intelligence that is currently doing nothing.
     * </p>
     * <ul>
     * <li>The {@linkplain #getClock() clock} of this person is the given clock.
     * <li>
     * <li>This does not have an {@linkplain #getPlayer() actor} (it is null).</li>
     * </ul>
     * 
     * @param clock
     *            The clock of the simulated world that this person is in.
     * @throws NullPointerException
     *             If {@code clock} is null.
     */
    public AI(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * <p>
     * The clock of the simulated world that this simulated mind is in.
     * </p>
     * 
     * @return the clock; not null
     */
    public final Clock getClock() {
        return clock;
    }

    /**
     * <p>
     * The interface through which the simulation interacts with a player
     * controlling the {@linkplain uk.badamson.mc.simulation.Person simulated
     * person} that this artificial intelligence controls.
     * </p>
     * 
     * @return The player, or null if the person does not (yet) have a player.
     */
    public final Mind getPlayer() {
        return player;
    }

    /**
     * <p>
     * Change the interface through which the simulation interacts with a player
     * controlling this person.
     * </p>
     * 
     * @param player
     *            the interface to use from now on
     * @throws IllegalArgumentException
     *             If {@code player} is not null and the
     *             {@linkplain Mind#getActorInterface() actor interface} of the
     *             {@code actor} is not this object.
     */
    public final void setPlayer(Mind player) {
        this.player = player;
    }

    /**
     * {@inheritDoc}
     * 
     * <ul>
     * <li>If this AI has a (non null) {@linkplain #getPlayer() associated player},
     * the method delegates to the corresponding method of the player.</li>
     * </ul>
     * 
     * @throws NullPointerException
     *             {@inheritDoc}
     * @throws IllegalArgumentException
     *             {@inheritDoc}
     */
    @Override
    public void tellBeginReceivingMessage(Medium medium) {
        super.tellBeginReceivingMessage(medium);
        if (player != null) {
            player.tellBeginReceivingMessage(medium);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <ul>
     * <li>If this AI has a (non null) {@linkplain #getPlayer() associated player},
     * the method delegates to the corresponding method of the player.</li>
     * </ul>
     * 
     * @throws NullPointerException
     *             {@inheritDoc}
     */
    @Override
    public void tellMessageReceptionProgress(MessageTransferInProgress messageBeingReceived) {
        super.tellMessageReceptionProgress(messageBeingReceived);
        if (player != null) {
            player.tellMessageReceptionProgress(messageBeingReceived);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <ul>
     * <li>If this AI has a (non null) {@linkplain #getPlayer() associated player},
     * the method delegates to the corresponding method of the player.</li>
     * </ul>
     * 
     * @throws NullPointerException
     *             {@inheritDoc}
     * @throws IllegalArgumentException
     *             {@inheritDoc}
     */
    @Override
    public void tellMessageSendingEnded(MessageTransferInProgress transmissionProgress, Message fullMessage) {
        super.tellMessageSendingEnded(transmissionProgress, fullMessage);
        if (player != null) {
            player.tellMessageSendingEnded(transmissionProgress, fullMessage);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <ul>
     * <li>If this AI has a (non null) {@linkplain #getPlayer() associated player},
     * the method delegates to the corresponding method of the player.</li>
     * </ul>
     * 
     * @throws NullPointerException
     *             {@inheritDoc}
     * @throws IllegalArgumentException
     *             {@inheritDoc}
     */
    @Override
    public void tellMessageTransmissionProgress(MessageTransferInProgress transmissionProgress, Message fullMessage) {
        super.tellMessageTransmissionProgress(transmissionProgress, fullMessage);
        if (player != null) {
            player.tellMessageTransmissionProgress(transmissionProgress, fullMessage);
        }
    }

}
