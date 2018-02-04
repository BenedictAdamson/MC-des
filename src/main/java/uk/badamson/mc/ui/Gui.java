package uk.badamson.mc.ui;

import java.util.Objects;

import uk.badamson.mc.Main;

/**
 * <p>
 * The Facade through which high-level parts of the Mission Command game access
 * the Graphical User Interface of the game.
 * </p>
 */
public final class Gui implements AutoCloseable, Runnable {

    private final Main main;

    /**
     * <p>
     * Create a GUI for a given instance of the Mission Command game.
     * </p>
     * <ul>
     * <li>The constructor may allocate graphical resources and "open" a connection
     * to the system windowing and graphics system.</li>
     * </ul>
     * 
     * @param main
     *            The instance of the Mission Command game that this GUI
     *            manipulates.
     * @throws NullPointerException
     *             If {@code main} is null.
     */
    public Gui(Main main) {
        this.main = Objects.requireNonNull(main, "main");
    }

    /**
     * <p>
     * Release any graphical resources, and "close" connections to the system
     * windowing and graphics system, that this GUI holds.
     * </p>
     * 
     */
    @Override
    public final void close() {
        // TODO
    }

    /**
     * <p>
     * The instance of the Mission Command game that this GUI manipulates.
     * </p>
     * 
     * @return the game instance; not null.
     */
    public final Main getMain() {
        return main;
    }

    /**
     * <p>
     * Run the main event loop of the GUI.
     * </p>
     * <p>
     * The method does not return until the GUI stops running, which typically is
     * the first stage of a shutdown of the program.
     * </p>
     */
    @Override
    public final void run() {
        // TODO
    }

}
