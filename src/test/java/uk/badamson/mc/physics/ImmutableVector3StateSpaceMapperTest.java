package uk.badamson.mc.physics;

import uk.badamson.mc.math.ImmutableVector3;
import uk.badamson.mc.math.ImmutableVector3Test;
import uk.badamson.mc.math.ImmutableVectorN;

/**
 * <p>
 * Unit tests for classes that implement the {@link VectorStateSpaceMapper}
 * interface.
 */
public class ImmutableVector3StateSpaceMapperTest {

    public static void assertInvariants(ImmutableVector3StateSpaceMapper mapper) {
        VectorStateSpaceMapperTest.assertInvariants(mapper);// inherited
    }

    public static void assertInvariants(ImmutableVector3StateSpaceMapper mapper1,
            ImmutableVector3StateSpaceMapper mapper2) {
        VectorStateSpaceMapperTest.assertInvariants(mapper1, mapper2);// inherited
    }

    public static void fromObject(ImmutableVector3StateSpaceMapper mapper, double[] state, ImmutableVector3 vector) {
        VectorStateSpaceMapperTest.fromObject(mapper, state, vector);

        assertInvariants(mapper);// check for side-effects
        ImmutableVector3Test.assertInvariants(vector);// check for side-effects
    }

    public static ImmutableVector3 toObject(ImmutableVector3StateSpaceMapper mapper, ImmutableVectorN state) {
        ImmutableVector3 vector = VectorStateSpaceMapperTest.toObject(mapper, state);

        assertInvariants(mapper);// check for side-effects
        ImmutableVector3Test.assertInvariants(vector);

        return vector;
    }
}
