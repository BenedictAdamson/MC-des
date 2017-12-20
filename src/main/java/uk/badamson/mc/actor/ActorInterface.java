
package uk.badamson.mc.actor;

import java.util.Set;

/**
 * <p>
 * The API (service interface) through which a human or AI players ("actors") of
 * the Mission Command game effect changes to the simulation.
 * </p>
 */
public interface ActorInterface {

	/**
	 * <p>
	 * Indicate that the {@linkplain #getActor() actor} begins transmitting a
	 * message through a given transmission medium.
	 * </p>
	 * <p>
	 * The message may be a command, and the medium may enable the intended
	 * recipient of that command to receive the comand. This method is therefore the
	 * means by which actors can issue orders to sub-ordinates.
	 * </p>
	 * 
	 * @param medium
	 *            The transmission medium (or means) through which to send the
	 *            message.
	 * @param message
	 *            The message to begin sending.
	 * @throws NullPointerException
	 *             <ul>
	 *             </li>If {@code medium} is null.
	 *             <li></li>If {@code message} is null.
	 *             <li>
	 *             </ul>
	 * @throws MediumUnavailableException
	 *             If the {@code medium} is not one of the {@linkplain #getMedia()
	 *             currently available media}.
	 */
	public void beginSendingMessage(Medium medium, Message message) throws MediumUnavailableException;

	/**
	 * <p>
	 * The actor for which this is the service interface.
	 * </p>
	 * 
	 * @return The actor; not null.
	 */
	public Actor getActor();

	/**
	 * <p>
	 * The current set of transmission media (or means) through which the
	 * {@linkplain #getActor() actor} can send {@linkplain Message messages}.
	 * </p>
	 * <ul>
	 * <li>Always have a (non null) set of media.</li>
	 * <li>The set of media does not contain an null element.</li>
	 * <li>The set of media may change as means of communication become available
	 * and cease to be available.
	 * </ul>
	 * 
	 * @return the media.
	 */
	public Set<Medium> getMedia();
}
