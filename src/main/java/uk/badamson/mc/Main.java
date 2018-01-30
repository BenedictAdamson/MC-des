package uk.badamson.mc;

/**
 * <p>
 * The entry point(s) of the Mission Command game.
 * </p>
 * <p>
 * This is the Facade through which the users of the program, via the operating
 * system, should interact with the program(s) when.
 * </p>
 * <p>
 * Classes used by this class might, in part, have to be loaded before execution
 * of the {@link #main(String[])} function can begin. Failures in that linking
 * of course can not be handled by any error handling code in the
 * {@link #main(String[])} function, and so will typically have low-level and
 * not very helpful error handling. This class therefore minimizes the number of
 * uses it makes of other classes. It tries to provide some basic error handling
 * using only features of the Java standard library, and delegates to other
 * classes to do most of the work. In particular, note that this class is
 * directly derived from {@link Object}.
 * </p>
 */
public final class Main {

    private Main() {
        throw new AssertionError("Main is a static class");
    }

    /**
     * <p>
     * Run the Mission Command game.
     * </p>
     * 
     * @param args
     *            Command-line arguments provided to the program.
     */
    public static void main(String[] args) {
        // TODO
    }

}
