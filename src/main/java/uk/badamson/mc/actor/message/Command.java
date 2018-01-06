package uk.badamson.mc.actor.message;

import uk.badamson.mc.actor.Actor;
import uk.badamson.mc.actor.Medium;

/**
 * <p>
 * A message that an {@linkplain Actor actor} might send through a
 * {@linkplain Medium medium} to other actors, which an actor (or actors) should
 * interpret as a command issued to them by the sender.
 * </p>
 * <p>
 * Objects of this class are therefore means by which actors can express orders
 * to sub-ordinates.
 * </p>
 */
public interface Command extends Sentence {
}
