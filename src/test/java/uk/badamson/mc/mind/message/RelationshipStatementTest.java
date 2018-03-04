package uk.badamson.mc.mind.message;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

/**
 * <p>
 * Unit tests of classes that implement the {@link RelationshipStatement}
 * interface.
 * </p>
 */
public class RelationshipStatementTest {

    public static void assertInvariants(RelationshipStatement statement) {
        MessageTest.assertInvariants(statement);// inherited

        final List<Noun> things = statement.getThings();
        final Relationship relationship = statement.getRelationship();

        assertNotNull("Always have a list of things.", things);// guard
        assertNotNull("Always have a relationship.", relationship);// guard

        for (Noun thing : things) {
            assertNotNull("The list of things has no null elements.", thing);// guard
            NounTest.assertInvariants(thing);
        }
        assertTrue("The list of things has at least two elements.", 2 <= things.size());

        RelationshipTest.assertInvariants(relationship);
    }
}
