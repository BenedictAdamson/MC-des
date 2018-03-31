package uk.badamson.mc.physics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import uk.badamson.mc.ObjectTest;
import uk.badamson.mc.math.ImmutableVectorN;

/**
 * <p>
 * Unit tests for classes that implement the {@link ObjectStateSpaceMapper}
 * interface.
 */
public class ObjectStateSpaceMapperTest {

    public static <OBJECT> void assertInvariants(ObjectStateSpaceMapper<OBJECT> mapper) {
        // Do nothing
    }

    public static <OBJECT> void assertInvariants(ObjectStateSpaceMapper<OBJECT> mapper1,
            ObjectStateSpaceMapper<OBJECT> mapper2) {
        // Do nothing
    }

    public static <OBJECT> void fromObject(ObjectStateSpaceMapper<OBJECT> mapper, double[] state, OBJECT object) {
        mapper.fromObject(state, object);

        assertInvariants(mapper);// check for side-effects
        ObjectTest.assertInvariants(object);// check for side-effects
    }

    public static <OBJECT> void fromToObjectSymmetry(ObjectStateSpaceMapper<OBJECT> mapper, double[] state,
            OBJECT original) {
        mapper.fromObject(state, original);
        final ImmutableVectorN stateVector = ImmutableVectorN.create(state);

        final OBJECT reconstructed = toObject(mapper, stateVector);

        assertEquals("Symmetric", original, reconstructed);
    }

    public static <OBJECT> OBJECT toObject(ObjectStateSpaceMapper<OBJECT> mapper, ImmutableVectorN state) {
        OBJECT object = mapper.toObject(state);

        assertInvariants(mapper);// check for side-effects
        assertNotNull("result", object);
        ObjectTest.assertInvariants(object);

        return object;
    }
}
