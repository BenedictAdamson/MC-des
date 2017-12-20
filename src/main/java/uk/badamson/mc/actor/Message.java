package uk.badamson.mc.actor;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A message that an {@linkplain Actor actor} might send through a
 * {@linkplain Medium medium}.
 * </p>
 * <p>
 * The message may be a {@linkplain Command command}, and the medium may enable
 * the intended recipient of that command to receive the command. Objects of
 * this class are therefore means by which actors can express orders to
 * sub-ordinates.
 * </p>
 */
@Immutable
public interface Message {

}
