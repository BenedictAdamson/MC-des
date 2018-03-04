package uk.badamson.mc.mind.message;

import uk.badamson.mc.mind.Mind;
import uk.badamson.mc.mind.medium.Medium;

/**
 * <p>
 * A message that an {@linkplain Mind actor} might send through a
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
