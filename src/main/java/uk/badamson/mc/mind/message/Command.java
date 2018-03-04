package uk.badamson.mc.mind.message;

import uk.badamson.mc.mind.Mind;
import uk.badamson.mc.mind.medium.Medium;

/**
 * <p>
 * A message that a {@linkplain Mind mind} might send through a
 * {@linkplain Medium medium} to other minds, which a mind (or minds) should
 * interpret as a command issued to them by the sender.
 * </p>
 * <p>
 * Objects of this class are therefore means by which minds can express orders
 * to sub-ordinates.
 * </p>
 */
public interface Command extends Sentence {
}
