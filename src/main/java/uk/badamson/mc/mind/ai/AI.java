package uk.badamson.mc.mind.ai;

import uk.badamson.mc.mind.AbstractMind;
import uk.badamson.mc.mind.Mind;

/**
 * <p>
 * An artificial intelligence for providing a
 * {@linkplain uk.badamson.mc.simulation.Person simulated person} with a
 * {@linkplain Mind mind}.
 * </p>
 */
public final class AI extends AbstractMind {
    private Mind player;

    /**
     * <p>
     * Construct an artificial intelligence that is currently doing nothing.
     * </p>
     * <ul>
     * <li>This does not have an {@linkplain #getPlayer() actor} (it is null).
     * </ul>
     */
    public AI() {
        // Do nothing
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
}
