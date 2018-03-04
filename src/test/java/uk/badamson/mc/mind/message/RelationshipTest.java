package uk.badamson.mc.mind.message;

import uk.badamson.mc.mind.message.Relationship;

/**
 * <p>
 * Unit tests for classes that implement the {@link Relationship} interface.
 * </p>
 */
public class RelationshipTest {

    public static void assertInvariants(Relationship relationship) {
        MessageElementTest.assertInvariants(relationship);// inherited
    }
}
