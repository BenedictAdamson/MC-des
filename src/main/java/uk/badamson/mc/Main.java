package uk.badamson.mc;

import java.util.Arrays;

import uk.badamson.mc.ui.Gui;

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
public final class Main implements AutoCloseable, Runnable {

    /**
     * <p>
     * Run the Mission Command game.
     * </p>
     * <p>
     * The method simply creates and then delegates to a new instance (object) of
     * the {@link Main} class. This simplifies automated testing of the high-level
     * parts of the program.
     * </p>
     * 
     * @param args
     *            Command-line arguments provided to the program.
     */
    public static void main(String[] args) {
        try (final Main program = new Main(args)) {
            program.run();
        }
    }

    private final String[] args;

    /**
     * <p>
     * Construct an object with given attribute values.
     * </p>
     * 
     * @param args
     *            The command line arguments given to this instance of the Mission
     *            Command game.
     */
    Main(String[] args) {
        this.args = args == null ? null : Arrays.copyOf(args, args.length);
    }

    /**
     * <p>
     * Release any resources that this game instance holds.
     * </p>
     * 
     */
    @Override
    public final void close() {
        // TODO
    }

    /**
     * <p>
     * The command line arguments given to this instance of the Mission Command
     * game.
     * </p>
     * <ul>
     * <li>The method returns a copy of the command-line arguments; manipulating the
     * returned array will not change the sate of the this object.</li>
     * </ul>
     * 
     * @return the arguments.
     */
    public final String[] getArgs() {
        return args == null ? null : Arrays.copyOf(args, args.length);
    }

    /**
     * <p>
     * Run this instance of the Mission Command game.
     * </p>
     */
    @Override
    public final void run() {
        try (final Gui gui = new Gui(this)) {
            gui.run();
        }
    }

}
