package uk.badamson.mc.simulation;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.time.Duration;
import java.util.UUID;

import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Auxiliary test code for classes implementing the {@link ObjectStateId}
 * interface.
 * </p>
 */
public class ObjectStateIdTest {

    static final UUID OBJECT_A = UUID.randomUUID();
    static final UUID OBJECT_B = UUID.randomUUID();
    static final Duration DURATION_A = Duration.ZERO;
    static final Duration DURATION_B = Duration.ofSeconds(15);
    static final UUID VERSION_A = UUID.randomUUID();
    static final UUID VERSION_B = UUID.randomUUID();

    public static void assertInvariants(ObjectStateId id) {
        ObjectTest.assertInvariants(id);// inherited

        final UUID objectId = id.getObject();
        final Duration when = id.getWhen();

        assertNotNull("objectId", objectId);
        assertNotNull("when", when);
    }

    public static void assertInvariants(ObjectStateId id1, ObjectStateId id2) {
        ObjectTest.assertInvariants(id1, id2);// inherited

        final boolean equals = id1.equals(id2);
        assertFalse("ObjectStateId objects are equivalent only if they have equals object IDs",
                equals && !id1.getObject().equals(id2.getObject()));
        assertFalse("ObjectStateId objects are equivalent only if they have equals timestamps",
                equals && !id1.getWhen().equals(id2.getWhen()));
        assertFalse("ObjectStateId objects are equivalent only if they have equals version IDs",
                equals && !id1.getVersion().equals(id2.getVersion()));
    }

    private static void constructor(UUID object, Duration when, UUID version) {
        final ObjectStateId id = new ObjectStateId(object, when, version);

        assertInvariants(id);
        assertThat("The object ID of this ID is the given object ID.", id.getObject(), sameInstance(object));
        assertThat("The time-stamp of this ID is the given time-stamp.", id.getWhen(), sameInstance(when));
        assertThat("The version ID of this ID is the given version ID.", id.getVersion(), sameInstance(version));
    }

    private static void constructor_2Equal(UUID object, Duration when, UUID version) {
        // Copy attributes so can test that checks for equality rather than sameness
        final UUID object2 = new UUID(object.getMostSignificantBits(), object.getLeastSignificantBits());
        final Duration when2 = when.plus(Duration.ZERO);
        final UUID version2 = new UUID(version.getMostSignificantBits(), version.getLeastSignificantBits());

        final ObjectStateId id1 = new ObjectStateId(object, when, version);
        final ObjectStateId id2 = new ObjectStateId(object2, when2, version2);

        assertInvariants(id1, id2);
        assertThat("Equal.", id1, equalTo(id2));
    }

    @Test
    public void constructor_2DifferentObject() {
        final ObjectStateId id1 = new ObjectStateId(OBJECT_A, DURATION_A, VERSION_A);
        final ObjectStateId id2 = new ObjectStateId(OBJECT_B, DURATION_A, VERSION_A);

        assertInvariants(id1, id2);
        assertThat("Not equal.", id1, not(id2));
    }

    @Test
    public void constructor_2DifferentVersion() {
        final ObjectStateId id1 = new ObjectStateId(OBJECT_A, DURATION_A, VERSION_A);
        final ObjectStateId id2 = new ObjectStateId(OBJECT_A, DURATION_A, VERSION_B);

        assertInvariants(id1, id2);
        assertThat("Not equal.", id1, not(id2));
    }

    @Test
    public void constructor_2DifferentWhen() {
        final ObjectStateId id1 = new ObjectStateId(OBJECT_A, DURATION_A, VERSION_A);
        final ObjectStateId id2 = new ObjectStateId(OBJECT_A, DURATION_B, VERSION_A);

        assertInvariants(id1, id2);
        assertThat("Not equal.", id1, not(id2));
    }

    @Test
    public void constructor_2EqualA() {
        constructor_2Equal(OBJECT_A, DURATION_A, VERSION_A);
    }

    @Test
    public void constructor_2EqualB() {
        constructor_2Equal(OBJECT_B, DURATION_B, VERSION_B);
    }

    @Test
    public void constructor_A() {
        constructor(OBJECT_A, DURATION_A, VERSION_A);
    }

    @Test
    public void constructor_B() {
        constructor(OBJECT_B, DURATION_B, VERSION_B);
    }

}// class