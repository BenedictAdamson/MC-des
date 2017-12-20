package uk.badamson.mc.actor;

/**
 * <p>
 * A message that an {@linkplain Actor actor} might send through a
 * {@linkplain Medium medium}.
 * </p>
 * <p>
 * The message may be a command, and the medium may enable the intended
 * recipient of that command to receive the command. Objects of this class are
 * therefore means by which actors can express orders to sub-ordinates.
 * </p>
 */
public interface Message {

}
