package uk.badamson.mc.actor;

import static org.junit.Assert.assertNotNull;

/**
 * <p>
 * Unit tests for classes that implement the {@link MessageTransferInProgress}
 * interface.
 * </p>
 */
public class MessageTransferInProgressTest {

    public static void assertInvariants(MessageTransferInProgress progress) {
	assertNotNull("Not null, medium", progress.getMedium());
    }
}
