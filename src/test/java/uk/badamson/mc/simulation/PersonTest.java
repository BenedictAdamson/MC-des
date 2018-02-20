package uk.badamson.mc.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.actor.AbstractActor;
import uk.badamson.mc.actor.Actor;
import uk.badamson.mc.actor.ActorInterfaceTest;
import uk.badamson.mc.actor.MediumUnavailableException;
import uk.badamson.mc.actor.MessageTransferInProgress;
import uk.badamson.mc.actor.medium.HandSignals;
import uk.badamson.mc.actor.medium.Medium;
import uk.badamson.mc.actor.message.Message;
import uk.badamson.mc.actor.message.SimpleDirectCommand;
import uk.badamson.mc.actor.message.SimpleRelativeLocation;
import uk.badamson.mc.actor.message.SimpleStatement;

/**
 * <p>
 * Unit tests of the {@link Person} class.
 * </p>
 */
public class PersonTest {
    private static final long TIME_1 = ClockTest.TIME_1;
    private static final long TIME_2 = ClockTest.TIME_2;

    public static void assertInvariants(Person person) {
        ObjectTest.assertInvariants(person);// inherited
        ActorInterfaceTest.assertInvariants(person);// inherited

        assertNotNull("Not null, clock", person.getClock());
    }

    public static void assertInvariants(Person person1, Person person2) {
        ObjectTest.assertInvariants(person1, person2);// inherited
        ActorInterfaceTest.assertInvariants(person1, person2);// inherited
    }

    public static void beginSendingMessage(Person person, Medium medium, Message message)
            throws MediumUnavailableException {
        try {
            ActorInterfaceTest.beginSendingMessage(person, medium, message);// inherited
        } catch (MediumUnavailableException e) {
            assertInvariants(person);
            throw e;
        }
        assertInvariants(person);
    }

    private static Person constructor(Clock clock) {
        final Person person = new Person(clock);

        assertInvariants(person);
        assertSame("clock", clock, person.getClock());
        assertNull("actor", person.getActor());
        assertEquals("The media through which this actor can send messages consists of hand signals}.",
                Collections.singleton(HandSignals.INSTANCE), person.getMedia());
        assertEquals("This actor is receiving no messages.", Collections.EMPTY_SET, person.getMessagesBeingReceived());
        assertNull("This actor is not transmitting} a message.", person.getTransmissionInProgress());

        return person;
    }

    public static void haltSendingMessage(Person person) {
        ActorInterfaceTest.haltSendingMessage(person);// inherited

        assertInvariants(person);
    }

    public static void setActor(Person person, Actor actor) {
        person.setActor(actor);

        assertInvariants(person);
        assertSame("actor", actor, person.getActor());
    }

    private Clock clock1;

    private Clock clock2;

    @Test
    public void advanceAfterHaltSendingMessage() {
        final double fraction = 0.25;
        final SimpleDirectCommand message = SimpleDirectCommand.CHECK_MAP;
        assert 0.0 < fraction && fraction < 1.0;
        final Clock clock = new Clock(TimeUnit.MICROSECONDS, TIME_1);
        final Person person = new Person(clock);
        final Medium medium = HandSignals.INSTANCE;
        final double informationInMessage = message.getInformationContent();
        final double transmissionTime = informationInMessage / medium.getTypicalTransmissionRate();

        final AbstractActor actor = new AbstractActor();
        person.setActor(actor);
        try {
            person.beginSendingMessage(medium, message);
        } catch (MediumUnavailableException e) {
            throw new AssertionError(e);
        }
        clock.advanceSeconds(transmissionTime * fraction);
        person.haltSendingMessage();

        clock.advanceSeconds(transmissionTime * (1.0 - fraction) * 20);

        assertInvariants(person);
    }

    private void beginSendingMessage_default(SimpleDirectCommand message) {
        final Person person = new Person(clock1);
        final Medium medium = HandSignals.INSTANCE;
        try {
            ActorInterfaceTest.beginSendingMessage(person, medium, message);
        } catch (MediumUnavailableException e) {
            throw new AssertionError(e);
        }
        assertInvariants(person);
    }

    private void beginSendingMessage_default(SimpleStatement message) {
        final Person person = new Person(clock1);
        final Medium medium = HandSignals.INSTANCE;
        try {
            ActorInterfaceTest.beginSendingMessage(person, medium, message);
        } catch (MediumUnavailableException e) {
            throw new AssertionError(e);
        }
        assertInvariants(person);
    }

    @Test
    public void beginSendingMessage_defaultSimpleDirectCommands() {
        for (SimpleDirectCommand message : SimpleDirectCommand.values()) {
            beginSendingMessage_default(message);
        }
    }

    @Test
    public void beginSendingMessage_defaultSimpleStatements() {
        for (SimpleStatement message : SimpleStatement.values()) {
            beginSendingMessage_default(message);
        }
    }

    @Test
    public void changeSendingMessage() {
        final double interruptionFraction = 0.25;
        final Message message1 = SimpleDirectCommand.CHECK_MAP;
        final Message message2 = SimpleDirectCommand.CHECK_PACES;

        final Clock clock = new Clock(TimeUnit.MICROSECONDS, TIME_1);
        final Person person = new Person(clock);
        final Medium medium = HandSignals.INSTANCE;
        final double informationInMessage = message1.getInformationContent();
        final double transmissionTime = informationInMessage / medium.getTypicalTransmissionRate();

        final AtomicInteger nEndMessages = new AtomicInteger(0);
        final AbstractActor actor = new AbstractActor() {

            @Override
            public final void tellMessageSendingEnded(MessageTransferInProgress transmissionProgress,
                    Message fullMessage) {
                super.tellMessageSendingEnded(transmissionProgress, fullMessage);
                nEndMessages.incrementAndGet();
            }

        };
        person.setActor(actor);
        try {
            person.beginSendingMessage(medium, message1);
        } catch (MediumUnavailableException e) {
            throw new AssertionError(e);
        }
        clock.advanceSeconds(transmissionTime * interruptionFraction);
        person.haltSendingMessage();
        try {
            beginSendingMessage(person, medium, message2);
        } catch (MediumUnavailableException e) {
            throw new AssertionError(e);
        }

        clock.advance(0L);
        assertEquals("Called tellMessageSendingEnded", 1, nEndMessages.get());
    }

    @Test
    public void constructor_A() {
        constructor(clock1);
    }

    @Test
    public void constructor_B() {
        constructor(clock2);
    }

    private void haltSendingMessage(Message message, final double fraction) {
        assert 0.0 < fraction && fraction < 1.0;
        final Clock clock = new Clock(TimeUnit.MICROSECONDS, TIME_1);
        final Person person = new Person(clock);
        final Medium medium = HandSignals.INSTANCE;
        final double informationInMessage = message.getInformationContent();
        final double transmissionTime = informationInMessage / medium.getTypicalTransmissionRate();
        final double informationSent = fraction * informationInMessage;

        final AtomicInteger nEndMessages = new AtomicInteger(0);
        final AbstractActor actor = new AbstractActor() {

            @Override
            public final void tellMessageSendingEnded(MessageTransferInProgress transmissionProgress,
                    Message fullMessage) {
                super.tellMessageSendingEnded(transmissionProgress, fullMessage);
                nEndMessages.incrementAndGet();
                assertEquals("information sent", informationSent,
                        transmissionProgress.getMessageSofar().getInformationContent(), informationInMessage * 0.01);
            }

        };
        person.setActor(actor);
        try {
            person.beginSendingMessage(medium, message);
        } catch (MediumUnavailableException e) {
            throw new AssertionError(e);
        }
        clock.advanceSeconds(transmissionTime * fraction);

        haltSendingMessage(person);
        clock.advance(0L);
        assertEquals("Called tellMessageSendingEnded", 1, nEndMessages.get());
    }

    @Test
    public void haltSendingMessage_A() {
        final double fraction = 0.25;
        haltSendingMessage(SimpleDirectCommand.CHECK_MAP, fraction);
    }

    @Test
    public void haltSendingMessage_B() {
        final double fraction = 0.5;
        haltSendingMessage(SimpleDirectCommand.getAssembleInstance(SimpleRelativeLocation.FRONT_NEAR), fraction);
    }

    @Test
    public void sendingMessageAdvanceToEndNoActor() {
        final long time0 = TIME_1;
        final Message message = SimpleDirectCommand.CHECK_MAP;
        final Clock clock = new Clock(TimeUnit.MICROSECONDS, time0);
        final Person person = new Person(clock);
        final Medium medium = HandSignals.INSTANCE;
        final double informationInMessage = message.getInformationContent();
        final double transmissionTime = informationInMessage / medium.getTypicalTransmissionRate();

        try {
            person.beginSendingMessage(medium, message);
        } catch (MediumUnavailableException e) {
            throw new AssertionError(e);
        }

        clock.advanceSeconds(transmissionTime * 1E3);

        /*
         * Check state before checking invariants, because checking the invariants might
         * cause lazy updating.
         */
        assertNull("No transmission in progress.", person.getTransmissionInProgress());
        assertInvariants(person);
    }

    private void sendingMessageIncremental(final long time0, Message message, final int nSteps) {
        assert 2 <= nSteps;
        final Clock clock = new Clock(TimeUnit.MICROSECONDS, time0);
        final Person person = new Person(clock);
        final Medium medium = HandSignals.INSTANCE;
        final double informationInMessage = message.getInformationContent();
        final double transmissionTime = informationInMessage / medium.getTypicalTransmissionRate();
        final double dt = transmissionTime / nSteps;

        final AtomicInteger nProgressMessages = new AtomicInteger(0);
        final AtomicInteger nEndMessages = new AtomicInteger(0);
        final AtomicReference<Message> messageSoFar = new AtomicReference<Message>(null);
        final AbstractActor actor = new AbstractActor() {

            @Override
            public void tellMessageSendingEnded(MessageTransferInProgress transmissionProgress, Message fullMessage) {
                super.tellMessageSendingEnded(transmissionProgress, fullMessage);
                assertInvariants(person);
                assertSame("fullMessage", message, fullMessage);
                assertTrue("Previously called some progress messages", 0 < nProgressMessages.get());
                assertEquals("Calls tellMessageSendingEnded only once", 0, nEndMessages.get());
                nEndMessages.incrementAndGet();
            }

            @Override
            public void tellMessageTransmissionProgress(MessageTransferInProgress transmissionProgress,
                    Message fullMessage) {
                super.tellMessageTransmissionProgress(transmissionProgress, fullMessage);
                assertInvariants(person);
                nProgressMessages.incrementAndGet();
                messageSoFar.set(transmissionProgress.getMessageSofar());
            }
        };
        person.setActor(actor);
        try {
            person.beginSendingMessage(medium, message);
        } catch (MediumUnavailableException e) {
            throw new AssertionError(e);
        }
        double informationSentSoFar = 0.0;
        /*
         * Do an extra step to ensure we advance past the end of transmission.
         */
        for (int i = 0; i < nSteps + 1; ++i) {
            clock.advanceSeconds(dt);
            assertInvariants(person);
            final MessageTransferInProgress transferInProgress = person.getTransmissionInProgress();
            if (transferInProgress != null) {
                final double informationSent = transferInProgress.getMessageSofar().getInformationContent();
                assertTrue("Amount of information sent does not decrease", informationSentSoFar <= informationSent);
                assertTrue("At most sent all the information", informationSent <= informationInMessage);
                informationSentSoFar = informationSent;
            }
        }

        assertInvariants(person);
        assertTrue("Called some progress messages", 0 < nProgressMessages.get());
        assertEquals("Called tellMessageSendingEnded", 1, nEndMessages.get());
    }

    @Test
    public void sendingMessageIncrementalA() {
        sendingMessageIncremental(TIME_1, SimpleDirectCommand.CHECK_MAP, 4);
    }

    @Test
    public void sendingMessageIncrementalB() {
        sendingMessageIncremental(TIME_2, SimpleDirectCommand.getAssembleInstance(SimpleRelativeLocation.FRONT_NEAR),
                8);
    }

    @Test
    public void sendingMessageLargeAdvance() {
        final long time0 = TIME_1;
        final Message message = SimpleDirectCommand.CHECK_MAP;
        final Clock clock = new Clock(TimeUnit.MICROSECONDS, time0);
        final Person person = new Person(clock);
        final Medium medium = HandSignals.INSTANCE;
        final double informationInMessage = message.getInformationContent();
        final double transmissionTime = informationInMessage / medium.getTypicalTransmissionRate();

        final AtomicInteger nEndMessages = new AtomicInteger(0);
        final AbstractActor actor = new AbstractActor() {

            @Override
            public void tellMessageSendingEnded(MessageTransferInProgress transmissionProgress, Message fullMessage) {
                super.tellMessageSendingEnded(transmissionProgress, fullMessage);
                assertInvariants(person);
                assertSame("fullMessage", message, fullMessage);
                assertEquals("Calls tellMessageSendingEnded only once", 0, nEndMessages.get());
                nEndMessages.incrementAndGet();
            }

            @Override
            public void tellMessageTransmissionProgress(MessageTransferInProgress transmissionProgress,
                    Message fullMessage) {
                super.tellMessageTransmissionProgress(transmissionProgress, fullMessage);
                assertInvariants(person);
            }
        };
        person.setActor(actor);
        try {
            person.beginSendingMessage(medium, message);
        } catch (MediumUnavailableException e) {
            throw new AssertionError(e);
        }

        clock.advanceSeconds(transmissionTime * 20);

        /*
         * Check for calls to the actor before checking invariants, because checking the
         * invariants might cause lazy updating and then calls to the actor.
         */
        assertEquals("Called tellMessageSendingEnded", 1, nEndMessages.get());
        assertNull("No transmission in progress.", person.getTransmissionInProgress());
        assertInvariants(person);
    }

    @Test
    public void sendingMessageLargeAdvanceNoActor() {
        final long time0 = TIME_1;
        final Message message = SimpleDirectCommand.CHECK_MAP;
        final Clock clock = new Clock(TimeUnit.MICROSECONDS, time0);
        final Person person = new Person(clock);
        final Medium medium = HandSignals.INSTANCE;
        final double informationInMessage = message.getInformationContent();
        final double transmissionTime = informationInMessage / medium.getTypicalTransmissionRate();

        try {
            person.beginSendingMessage(medium, message);
        } catch (MediumUnavailableException e) {
            throw new AssertionError(e);
        }

        clock.advanceSeconds(transmissionTime * 20);

        /*
         * Check state before checking invariants, because checking the invariants might
         * cause lazy updating.
         */
        assertNull("No transmission in progress.", person.getTransmissionInProgress());
        assertInvariants(person);
    }

    private void setActor() {
        final Person person = new Person(clock1);
        final Actor actor = new AbstractActor();

        setActor(person, actor);
    }

    @Test
    public void setActor_A() {
        setActor();
    }

    @Test
    public void setActor_B() {
        setActor();
    }

    @Before
    public void setup() {
        clock1 = new Clock(TimeUnit.MILLISECONDS, TIME_1);
        clock2 = new Clock(TimeUnit.MILLISECONDS, TIME_2);
    }
}
