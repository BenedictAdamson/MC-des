package uk.badamson.mc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

/**
 * <p>
 * Unit tests of the {@link Main} class.
 * </p>
 */
public class MainTest {

    private static final String ARG_0 = "mc";
    private static final String ARG_1 = "--gui";

    public static void assertInvariants(Main main) {
        ObjectTest.assertInvariants(main);// inherited
    }

    public static void assertInvariants(Main main1, Main main2) {
        ObjectTest.assertInvariants(main1, main2);// inherited
    }

    private static Main constructor(String[] args) {
        final Main main = new Main(args);

        assertInvariants(main);
        assertArrayEquals("args", args, main.getArgs());
        if (args != null) {
            assertNotSame("args are copied", args, main.getArgs());
        }

        return main;
    }

    @Test
    public void constructor_0() {
        constructor(new String[] {});
    }

    @Test
    public void constructor_1() {
        constructor(new String[] { ARG_0 });
    }

    @Test
    public void constructor_1Null() {
        constructor(new String[] { null });
    }

    @Test
    public void constructor_2() {
        constructor(new String[] { ARG_0, ARG_1 });
    }

    @Test
    public void constructor_null() {
        constructor(null);
    }
}
