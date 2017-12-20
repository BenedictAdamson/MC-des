package uk.badamson.mc.actor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for classes that implement the {@link ActorInterface} interface
 */
public class ActorInterfaceTest {

	public static void assertInvariants(ActorInterface actorInterface) {
		final Medium sendingMedium = actorInterface.getSendingMedium();
		final Message sendingMessage = actorInterface.getSendingMessage();

		assertEquals("The sending message is null if, and only if, the sending medium is null.", sendingMessage == null,
				sendingMedium == null);
	}

	public static void beginSendingMessage(ActorInterface actorInterface, Medium medium, Message message)
			throws MediumUnavailableException {
		try {
			actorInterface.beginSendingMessage(medium, message);
		} catch (MediumUnavailableException e) {
			assertInvariants(actorInterface);
			throw e;
		}
		assertInvariants(actorInterface);
		assertSame("The given medium is the current sending medium.", medium, actorInterface.getSendingMedium());
		assertSame("The given message is the current sending message.", message, actorInterface.getSendingMessage());
	}
}
