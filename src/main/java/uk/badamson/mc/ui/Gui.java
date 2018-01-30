package uk.badamson.mc.ui;

/**
 * <p>
 * The Facade through which high-level parts of the Mission Command game access
 * the Graphical User Interface of the game.
 * </p>
 */
public final class Gui {

    /**
     * <p>
     * Run the main event loop of the GUI.
     * </p>
     * <p>
     * The method does not return until the GUI stops running, which typically
     * indicates that the program should exit.
     * </p>
     */
    public static final void mainEventLoop() {
        // TODO
    }

    private Gui() {
        throw new AssertionError("This is a static class");
    }
}
