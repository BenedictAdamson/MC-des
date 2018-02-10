package uk.badamson.mc.ui;

import java.util.Objects;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Decorations;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import uk.badamson.mc.Game;
import uk.badamson.mc.Main;
import uk.badamson.mc.actor.Actor;
import uk.badamson.mc.actor.ActorInterface;
import uk.badamson.mc.actor.MessageTransferInProgress;
import uk.badamson.mc.actor.medium.Medium;
import uk.badamson.mc.actor.message.Message;
import uk.badamson.mc.simulation.Person;

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
         * The GUI for the {@linkplain Game#getPerson() played person} of one instance
         * of the {@linkplain Game Mission Command Game}
         * </p>
         */
        public final class PlayedPersonGui implements Actor {

            private final ActorInterface person;

            /**
             * @param parentWindow
             *            The composite control which will be the parent of the GUI elements
             *            for this GUI.
             * @param person
             *            The API (service interface) through which this GUI effects changes
             *            to the simulation of the played person.
             * @throws NullPointerException
             *             <ul>
             *             <li>If {@code parentControl} is null.</li>
             *             <li>If {@code person} is null.</li>
             *             </ul>
             * @throws IllegalArgumentException
             *             If {@code person} is not one of the {@linkplain Game#getPersons()
             *             simulated persons} of the {@linkplain Gui.GameGui#getGame() game}
             *             for which this is a GUI.
             */
            PlayedPersonGui(Decorations parentWindow, ActorInterface person) {
                Objects.requireNonNull(parentWindow, "parentControl");
                this.person = Objects.requireNonNull(person, "person");
                if (!getGame().getPersons().contains(person)) {
                    throw new IllegalArgumentException(
                            "person is not one of the simulated persons of the game for which this is a GUI");
                }
                {
                    final Menu menuBar = new Menu(parentWindow, SWT.BAR);
                    {
                        final MenuItem sendMenuItem = new MenuItem(menuBar, SWT.CASCADE);
                        sendMenuItem.setText("Send");
                        final Menu sendMenu = new Menu(menuBar);
                        sendMenuItem.setMenu(sendMenu);
                        final Set<Medium> media = person.getMedia();
                        for (Medium medium : media) {
                            final MenuItem mediumItem = new MenuItem(sendMenu, SWT.PUSH);
                            mediumItem.setText(medium.toString() + "...");
                            mediumItem.setData(medium);
                        }
                    }
                    parentWindow.setMenuBar(menuBar);
                }
                {
                    final Group transmissionInProgressGroup = new Group(parentWindow, SWT.DEFAULT);
                    final RowLayout layout = new RowLayout(SWT.VERTICAL);
                    layout.fill = true;
                    transmissionInProgressGroup.setLayout(layout);
                    transmissionInProgressGroup.setText("Message being sent");
                    final Label mediumLabel = new Label(transmissionInProgressGroup, SWT.LEFT);
                    mediumLabel.setText("Not sending");
                    final Text message = new Text(transmissionInProgressGroup, SWT.MULTI | SWT.LEFT | SWT.READ_ONLY);
                    message.setText("");
                    final ProgressBar progress = new ProgressBar(transmissionInProgressGroup, SWT.HORIZONTAL);
                    progress.setMinimum(0);
                    progress.setSelection(0);
                    progress.setMaximum(Integer.MAX_VALUE);
                    // TODO TransmissionInProgress
                    transmissionInProgressGroup.pack();
                }
                {
                    final Group messagesBeingReceivedGroup = new Group(parentWindow, SWT.DEFAULT);
                    messagesBeingReceivedGroup.setText("Messages being received");
                    // TODO MessagesBeingReceived
                    messagesBeingReceivedGroup.pack(true);
                }
            }

            /**
             * <p>
             * The API (service interface) through which this GUI effects changes to the
             * simulation of the played person.
             * </p>
             * <ul>
             * <li>Always have a (non null) person simulation interface.</li>
             * <li>The person simulation interface is one of the
             * {@linkplain Game#getPersons() simulated persons} of the
             * {@linkplain Gui.GameGui#getGame() game} for which this is a GUI..</li>
             * </ul>
             * <p>
             * The played person will usually be the same as the current
             * {@linkplain Game#getPerson() played person} of
             * {@linkplain Gui.GameGui#getGame() the game} of which this is the part of the
             * GUI. However, that can (temporarily) not be the case if the played person has
             * changed but the GUI has not yet updated for the change. In that case, this
             * class deactivates itself and performs operations that will cause it to be
             * "closed" and disposed of.
             * </p>
             * 
             * @return the interface; not null.
             */
            public final ActorInterface getPerson() {
                return person;
            }

            @Override
            public void tellBeginReceivingMessage(MessageTransferInProgress receptionStarted) {
                // TODO Auto-generated method stub

            }

            @Override
            public void tellMessageReceptionProgress(Message previousMessageSoFar,
                    MessageTransferInProgress messageBeingReceived) {
                // TODO Auto-generated method stub

            }

            @Override
            public void tellMessageSendingEnded(MessageTransferInProgress transmissionProgress, Message fullMessage) {
                // TODO Auto-generated method stub

            }

            @Override
            public void tellMessageTransmissionProgress(MessageTransferInProgress transmissionProgress,
                    Message fullMessage) {
                // TODO Auto-generated method stub

            }

        }// class

        private final Game game;
        private final Shell gameWindow;

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
            this.gameWindow = new Shell(mainWindow);
            this.gameWindow.setLayout(new RowLayout(SWT.VERTICAL));
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

        /**
         * <p>
         * Have the user of this GUI take control of one of the
         * {@linkplain Game#getPlayedPerson() simulated persons} of the
         * {@linkplain #getGame() game} that this GUI controls.
         * </p>
         * <ul>
         * <li>The {@linkplain Game#getPlayedPerson() played person} of the game that
         * his GUI controls becomes the given person.</li>
         * <li>The {@linkplain Person#getActor() actor} of the given person becomes the
         * {@linkplain Gui.GameGui.PlayedPersonGui GUI} that this returns.</li>
         * </ul>
         * 
         * @param person
         *            The person to be controlled.
         * @return the GUI through which the user controls the person.
         * @throws NullPointerException
         *             If {@code person} is null.
         * @throws IllegalArgumentException
         *             If {@code person} is not one of the {@linkplain Game#getPersons()
         *             persons} of the {@linkplain #getGame() game} for which this is
         *             the GUI.
         */
        public final PlayedPersonGui takeControl(Person person) {
            final PlayedPersonGui gui = new PlayedPersonGui(gameWindow, person);
            gameWindow.pack(true);
            gameWindow.open();
            game.takeControl(gui, person);
            return gui;
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
