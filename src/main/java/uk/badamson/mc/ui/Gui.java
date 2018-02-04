package uk.badamson.mc.ui;

import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import uk.badamson.mc.Game;
import uk.badamson.mc.Main;
import uk.badamson.mc.actor.ActorInterface;

/**
 * <p>
 * The Facade through which high-level parts of the program access the Graphical
 * User Interface of the Mission Command program.
 * </p>
 * <p>
 * Instances of this class are intended to be accessed by only thread running
 * the main event loop of the GUI.
 * </p>
 */
public final class Gui implements AutoCloseable, Runnable {

    /**
     * <p>
     * The GUI of one instance of the Mission Command game.
     * </p>
     */
    public final class GameGui {

        /**
         * <p>
         * The GUI for the {@linkplain Game#getPlayedPerson() played person} of one
         * instance of the {@linkplain Game Mission Command Game}
         * </p>
         */
        public final class PlayedPersonGui {

            private ActorInterface playedPerson;

            /**
             * @param playedPerson
             *            The API (service interface) through which this GUI effects changes
             *            to the simulation of the played person.
             * @throws NullPointerException
             *             If {@code playedPerson} is null.
             */
            PlayedPersonGui(ActorInterface playedPerson) {
                this.playedPerson = Objects.requireNonNull(playedPerson, "playedPerson");
            }

            /**
             * <p>
             * The API (service interface) through which this GUI effects changes to the
             * simulation of the played person.
             * </p>
             * <p>
             * The played person will usually be the same as the current
             * {@linkplain Game#getPlayedPerson() played person} of
             * {@linkplain Gui.GameGui#getGame() the game} of which this is the part of the
             * GUI. However, that can (temporarily) not be the case if the played person has
             * changed but the GUI has not yet updated for the change. In that case, this
             * class deactivates itself and performs operations that will cause it to be
             * "closed" and disposed of.
             * </p>
             * 
             * @return the interface; not null.
             */
            public final ActorInterface getPlayedPerson() {
                return playedPerson;
            }
        }// class

        private final Game game;

        private PlayedPersonGui currentControlledPersonGui;

        /**
         * <p>
         * Construct a GUI for a given game instance.
         * </p>
         * 
         * @param game
         *            The instance of the Mission Command game for which this is the
         *            GUI.
         * @throws NullPointerException
         *             If {@code game} is null.
         */
        GameGui(Game game) {
            this.game = Objects.requireNonNull(game, "game");
            changeCurrentControlledPersonGui(game.getPlayedPerson());
        }

        private void changeCurrentControlledPersonGui(final ActorInterface playedPerson) {
            currentControlledPersonGui = null;
            if (playedPerson != null) {
                currentControlledPersonGui = new PlayedPersonGui(playedPerson);
            }
        }

        /**
         * <p>
         * Retrieve or create the GUI for the {@linkplain Game#getPlayedPerson() played
         * person} of the {@linkplain #getGame() game} for which this is the GUI.
         * </p>
         * <ul>
         * <li>The current controlled person GUI is null if, and only if, the
         * {@linkplain #getGame() game} has no the {@linkplain Game#getPlayedPerson()
         * played person}.</li>
         * <li>If this has a (non null) controlled person GUI, the
         * {@linkplain Gui.GameGui.PlayedPersonGui#getPlayedPerson() played person} of
         * that GUI is the played person of the game.</li>
         * <li>Calling this method may cause lazy creation of a new GUI for the
         * controlled person.</li>
         * </ul>
         * 
         * @return the GUI
         */
        public PlayedPersonGui getCurrentPlayedPersonGui() {
            final ActorInterface playedPerson = game.getPlayedPerson();
            final ActorInterface currentPlayedPerson = currentControlledPersonGui == null ? null
                    : currentControlledPersonGui.getPlayedPerson();
            if (playedPerson != currentPlayedPerson) {
                changeCurrentControlledPersonGui(playedPerson);
            }
            return currentControlledPersonGui;
        }

        /**
         * <p>
         * The instance of the Mission Command game for which this is the GUI.
         * </p>
         * 
         * @return the game; not null.
         */
        public final Game getGame() {
            return game;
        }

    }// class

    private final Main main;
    private final Display display = new Display();
    private final Shell mainWindow;

    /**
     * <p>
     * Create a GUI for a given instance of the Mission Command game.
     * </p>
     * <ul>
     * <li>The constructor may allocate graphical resources and "open" a connection
     * to the system windowing and graphics system.</li>
     * <li>The constructor may set up, create and "open" a main application
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
        mainWindow = new Shell(display);
        Label label = new Label(mainWindow, SWT.CENTER);
        label.setText("Hello_world");
        label.setBounds(mainWindow.getClientArea());
        mainWindow.open();
    }

    /**
     * <p>
     * Alter this GUI so it also provides a GUI for a given instance of the Mission
     * Command game.
     * </p>
     * 
     * @param game
     *            The instance of the Mission Command game for which to provide a
     *            GUI.
     * @throws NullPointerException
     *             If {@code game} is null.
     * @return The GUI for the given game; not null.
     */
    public final GameGui addGame(Game game) {
        return new GameGui(game);
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
     * The Facade through which the users of the program, via the operating system,
     * interact with the program for which this is the GUI.
     * </p>
     * 
     * @return the main instance; not null.
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
        while (!mainWindow.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
    }
}
