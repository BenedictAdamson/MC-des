package uk.badamson.mc.simulation;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Auxiliary test code for the {@link AbstractObjectState} interface.
 * </p>
 */
public class AbstractObjectStateTest {

    private static final class TestObjectState extends AbstractObjectState {

        public TestObjectState(ObjectStateId id, Set<ObjectStateId> dependencies) {
            super(id, dependencies);
        }

        @Override
        public Map<UUID, ObjectState> createNextStates() {
            return Collections.singletonMap(getId().getObject(), (ObjectState) null);
        }

    }// class
    private static final UUID OBJECT_A = ObjectStateIdTest.OBJECT_A;
    private static final UUID OBJECT_B = ObjectStateIdTest.OBJECT_B;
    private static final UUID OBJECT_C = UUID.randomUUID();
    private static final Duration DURATION_A = Duration.ofSeconds(23);
    private static final Duration DURATION_B = ObjectStateIdTest.DURATION_B;

    private static final Duration DURATION_C = ObjectStateIdTest.DURATION_A;
    public static void assertInvariants(AbstractObjectState state) {
        ObjectTest.assertInvariants(state);// inherited
        ObjectStateTest.assertInvariants(state);// inherited
    }
    public static void assertInvariants(AbstractObjectState state1, AbstractObjectState state2) {
        ObjectTest.assertInvariants(state1, state2);// inherited
        ObjectStateTest.assertInvariants(state1, state2);// inherited
    }

    private static void constructor(ObjectStateId id, Set<ObjectStateId> dependencies) {
        final AbstractObjectState state = new TestObjectState(id, dependencies);

        assertInvariants(state);
        assertThat("The ID of this state is the given ID.", state.getId(), sameInstance(id));
        assertThat("The dependencies of this state are equal to the given dependencies.", state.getDependencies(),
                equalTo(dependencies));
    }

    private static void constructor_2Equal(ObjectStateId id, Set<ObjectStateId> dependencies) {
        final ObjectStateId id2 = new ObjectStateId(id.getObject(), id.getWhen());
        final Set<ObjectStateId> dependencies2 = new HashSet<>(dependencies);

        final AbstractObjectState state1 = new TestObjectState(id, dependencies);
        final AbstractObjectState state2 = new TestObjectState(id2, dependencies2);

        assertInvariants(state1, state2);
        assertThat("Equal.", state1, equalTo(state2));
    }

    public static Map<UUID, ObjectState> createNextStates(AbstractObjectState state) {
        final Map<UUID, ObjectState> nextStates = ObjectStateTest.createNextStates(state);// inherited

        assertInvariants(state);

        return nextStates;
    }

    private ObjectStateId idA;

    private ObjectStateId idB;

    private ObjectStateId idC;

    @Test
    public void constructor_2DifferentDependencies() {
        final Set<ObjectStateId> dependencies = Set.of(idC);
        final AbstractObjectState state1 = new TestObjectState(idA, Set.of(idB));
        final AbstractObjectState state2 = new TestObjectState(idA, Set.of(idC));

        assertInvariants(state1, state2);
        assertThat("Equal.", state1, equalTo(state2));
    }

    @Test
    public void constructor_2DifferentId() {
        final Set<ObjectStateId> dependencies = Set.of(idC);
        final AbstractObjectState state1 = new TestObjectState(idA, dependencies);
        final AbstractObjectState state2 = new TestObjectState(idB, dependencies);

        assertInvariants(state1, state2);
        assertThat("Not equal.", state1, not(state2));
    }

    @Test
    public void constructor_2EqualA() {
        constructor_2Equal(idA, Set.of());
    }

    @Test
    public void constructor_2EqualB() {
        constructor_2Equal(idB, Set.of(idC));
    }

    @Test
    public void constructor_A() {
        constructor(idA, Set.of());
    }

    @Test
    public void constructor_B() {
        constructor(idB, Set.of(idC));
    }

    @Before
    public void setUp() {
        idA = new ObjectStateId(OBJECT_A, DURATION_A);
        idB = new ObjectStateId(OBJECT_B, DURATION_B);
        idC = new ObjectStateId(OBJECT_C, DURATION_C);
    }
}