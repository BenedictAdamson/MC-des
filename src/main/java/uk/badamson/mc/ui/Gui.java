package uk.badamson.mc.ui;

import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import uk.badamson.mc.Main;

/**
 * <p>
 * The Facade through which high-level parts of the Mission Command game access
 * the Graphical User Interface of the game.
 * </p>
 */
public final class Gui implements AutoCloseable, Runnable {

    private final Main main;
    private final Display display = new Display();
    private final Shell shell;

    /**
     * <p>
     * Create a GUI for a given instance of the Mission Command game.
     * </p>
     * <ul>
     * <li>The constructor may allocate graphical resources and "open" a connection
     * to the system windowing and graphics system.</li>
     * <li>The constructor may set up, create and "open" the main application
     * window.</li>
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
        shell = new Shell(display);
        Label label = new Label(shell, SWT.CENTER);
        label.setText("Hello_world");
        label.setBounds(shell.getClientArea());
        shell.open();
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
        display.dispose();
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
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
    }

}
