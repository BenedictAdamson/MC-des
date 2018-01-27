package uk.badamson.mc.actor;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.actor.message.Message;

/**
 * <p>
 * Unit tests of the {@link AbstractActor} class.
 * </p>
 */
public class AbstractActorTest {

    public static void assertInvariants(AbstractActor actor) {
        ObjectTest.assertInvariants(actor);// inherited
        ActorTest.assertInvariants(actor);// inherited
    }

    public static void assertInvariants(AbstractActor actor1, AbstractActor actor2) {
        ObjectTest.assertInvariants(actor1, actor2);// inherited
        ActorTest.assertInvariants(actor1, actor2);// inherited
    }

    public static void tellBeginReceivingMessage(AbstractActor actor, MessageTransferInProgress receptionStarted) {
        ActorTest.tellBeginReceivingMessage(actor, receptionStarted);// inherited
        assertInvariants(actor);
    }

    public static void tellMessageReceptionProgress(AbstractActor actor, Message previousMessageSoFar,
            MessageTransferInProgress messageBeingReceived) {
        ActorTest.tellMessageReceptionProgress(actor, previousMessageSoFar, messageBeingReceived);
        assertInvariants(actor);
    }

    public static void tellMessageSendingEnded(AbstractActor actor, MessageTransferInProgress transmissionProgress,
            Message fullMessage) {
        ActorTest.tellMessageSendingEnded(actor, transmissionProgress, fullMessage);
        assertInvariants(actor);
    }

    public static void tellMessageTransmissionProgress(AbstractActor actor, Message previousMessageSoFar) {
        ActorTest.tellMessageTransmissionProgress(actor, previousMessageSoFar);
        assertInvariants(actor);
    }
}
