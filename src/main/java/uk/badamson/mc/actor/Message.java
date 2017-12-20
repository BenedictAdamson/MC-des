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

	/**
	 * <p>
	 * The length of this message.
	 * </p>
	 * <ul>
	 * <li>The length of a message is positive.</li>
	 * <li>The length of a message is {@linkplain Double#isFinite() finite}.</li>
	 * <li>The unit of message length is bits of information.</li>
	 * </ul>
	 * 
	 * @return the length
	 */
	public double getLength();
}
