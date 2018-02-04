package uk.badamson.mc;

import uk.badamson.mc.actor.ActorInterface;

/**
 * <p>
 * One instance of the Mission Command game.
 * </p>
 */
public final class Game {

    private ActorInterface playerActorInterface;

    /**
     * <p>
     * The API (service interface) through which the human player of this game
     * effect changes to the simulation of the person they are playing.
     * </p>
     * 
     * @return the interface; or null if the player is not (yet) playing a
     *         particular person.
     */
    public final ActorInterface getPlayerActorInterface() {
        return playerActorInterface;
    }

}
