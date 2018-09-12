package uk.badamson.mc.simulation;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

    static final class TestObjectState extends AbstractObjectState {

        public TestObjectState(ObjectStateId id, Map<UUID, ObjectStateDependency> dependencies) {
            super(id, dependencies);
        }

        @Override
        public Map<UUID, ObjectState> createNextStates() {
            return Collections.singletonMap(getId().getObject(), (ObjectState) null);
        }

        @Override
        public String toString() {
            return "TestObjectState[" + getId() + "]";
        }

    }// class

    private static final UUID OBJECT_A = ObjectStateIdTest.OBJECT_A;
    private static final UUID OBJECT_B = ObjectStateIdTest.OBJECT_B;
    private static final UUID OBJECT_C = UUID.randomUUID();
    private static final Duration DURATION_A = Duration.ofSeconds(11);
    private static final Duration DURATION_B = Duration.ofSeconds(13);
    private static final Duration DURATION_C = Duration.ofSeconds(17);
    private static final UUID VERSION_A = ObjectStateIdTest.VERSION_A;
    private static final UUID VERSION_B = ObjectStateIdTest.VERSION_B;
    private static final UUID VERSION_C = UUID.randomUUID();

    public static void assertInvariants(AbstractObjectState state) {
        ObjectTest.assertInvariants(state);// inherited
        ObjectStateTest.assertInvariants(state);// inherited
    }

    public static void assertInvariants(AbstractObjectState state1, AbstractObjectState state2) {
        ObjectTest.assertInvariants(state1, state2);// inherited
        ObjectStateTest.assertInvariants(state1, state2);// inherited
    }

    private static void constructor(ObjectStateId id, Map<UUID, ObjectStateDependency> dependencies) {
        final AbstractObjectState state = new TestObjectState(id, dependencies);

        assertInvariants(state);
        assertThat("The ID of this state is the given ID.", state.getId(), sameInstance(id));
        assertThat("The dependencies of this state are equal to the given dependencies.", state.getDependencies(),
                equalTo(dependencies));
    }

    private static void constructor_2Equal(ObjectStateId id, Map<UUID, ObjectStateDependency> dependencies) {
        final ObjectStateId id2 = new ObjectStateId(id.getObject(), id.getWhen(), id.getVersion());
        final Map<UUID, ObjectStateDependency> dependencies2 = new HashMap<>(dependencies);

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

    private ObjectStateDependency dependencyA;
    private ObjectStateDependency dependencyB;

    @Test
    public void constructor_2DifferentDependencies() {
        final Map<UUID, ObjectStateDependency> dependencies1 = Collections
                .singletonMap(dependencyA.getDependedUpObject(), dependencyA);
        final Map<UUID, ObjectStateDependency> dependencies2 = Collections
                .singletonMap(dependencyB.getDependedUpObject(), dependencyB);
        final AbstractObjectState state1 = new TestObjectState(idC, dependencies1);
        final AbstractObjectState state2 = new TestObjectState(idC, dependencies2);

        assertInvariants(state1, state2);
        assertThat("Equal.", state1, equalTo(state2));
    }

    @Test
    public void constructor_2DifferentId() {
        final Map<UUID, ObjectStateDependency> dependencies = Collections
                .singletonMap(dependencyA.getDependedUpObject(), dependencyA);
        final AbstractObjectState state1 = new TestObjectState(idB, dependencies);
        final AbstractObjectState state2 = new TestObjectState(idC, dependencies);

        assertInvariants(state1, state2);
        assertThat("Not equal.", state1, not(state2));
    }

    @Test
    public void constructor_2EqualA() {
        constructor_2Equal(idB, Collections.singletonMap(dependencyA.getDependedUpObject(), dependencyA));
    }

    @Test
    public void constructor_2EqualB() {
        constructor_2Equal(idC, Collections.singletonMap(dependencyB.getDependedUpObject(), dependencyB));
    }

    @Test
    public void constructor_A() {
        constructor(idB, Collections.singletonMap(dependencyA.getDependedUpObject(), dependencyA));
    }

    @Test
    public void constructor_B() {
        constructor(idC, Collections.singletonMap(dependencyB.getDependedUpObject(), dependencyB));
    }

    @Before
    public void setUp() {
        idA = new ObjectStateId(OBJECT_A, DURATION_A, VERSION_A);
        idB = new ObjectStateId(OBJECT_B, DURATION_B, VERSION_B);
        idC = new ObjectStateId(OBJECT_C, DURATION_C, VERSION_C);

        dependencyA = new ObjectStateDependency(DURATION_A, idA);
        dependencyB = new ObjectStateDependency(DURATION_B.plusNanos(1L), idB);
    }
}