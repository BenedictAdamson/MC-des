package uk.badamson.mc.ui;

import java.util.Objects;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
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
import uk.badamson.mc.actor.medium.HandSignals;
import uk.badamson.mc.actor.medium.Medium;
import uk.badamson.mc.actor.message.BattleDrillName;
import uk.badamson.mc.actor.message.Message;
import uk.badamson.mc.actor.message.MilitaryRole;
import uk.badamson.mc.actor.message.SimpleDirectCommand;
import uk.badamson.mc.actor.message.SimpleFormationName;
import uk.badamson.mc.actor.message.SimpleRelativeLocation;
import uk.badamson.mc.actor.message.SimpleStatement;
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
         * of the {@linkplain Game Mission Command Game}.
         * </p>
         */
        public final class PlayedPersonGui implements Actor {

            /**
             * <p>
             * The GUI for causing the {@linkplain Game#getPerson() played person} of one
             * instance of the {@linkplain Game Mission Command Game} to
             * {@linkplain ActorInterface#beginSendingMessage(Medium, Message) send} a
             * {@linkplain HandSignals hand signal}.
             * </p>
             */
            public final class SendHandSignalGui {

                private final Shell handSignalDialog;

                /**
                 * <p>
                 * Create this GUI, in a non-displayed (closed, invisible) state.
                 * </p>
                 */
                SendHandSignalGui() {
                    handSignalDialog = new Shell(gameWindow, SWT.DIALOG_TRIM);
                    handSignalDialog.setText("Hand Signal");
                    handSignalDialog.setData(this);
                    handSignalDialog.setEnabled(false);
                    handSignalDialog.setVisible(false);
                    final RowLayout layout = new RowLayout(SWT.HORIZONTAL);// TODO
                    handSignalDialog.setLayout(layout);
                    {
                        final Group allMoveGroup = new Group(handSignalDialog, SWT.DEFAULT);
                        final RowLayout groupLayout = new RowLayout(SWT.VERTICAL);
                        layout.fill = true;
                        allMoveGroup.setLayout(groupLayout);
                        allMoveGroup.setText("All Move");
                        addMessageButton(allMoveGroup, SimpleDirectCommand.RUSH, "Rush");
                        addMessageButton(allMoveGroup, SimpleDirectCommand.QUICK_TIME, "Quick time");
                        addMessageButton(allMoveGroup, SimpleDirectCommand.HALT, "Halt");
                        addMessageButton(allMoveGroup, SimpleDirectCommand.HALT_AND_FREEZE, "Halt and freeze");
                        addMessageButton(allMoveGroup, SimpleDirectCommand.HALT_AND_TAKE_A_KNEE,
                                "Halt and take a knee");
                        addMessageButton(allMoveGroup, SimpleDirectCommand.HALT_AND_GO_PRONE, "Halt and go prone");
                        addMessageButton(allMoveGroup, SimpleDirectCommand.TAKE_COVER, "Take cover");
                        allMoveGroup.pack(true);
                    }
                    {
                        final Group checkGroup = new Group(handSignalDialog, SWT.DEFAULT);
                        final RowLayout groupLayout = new RowLayout(SWT.VERTICAL);
                        layout.fill = true;
                        checkGroup.setLayout(groupLayout);
                        checkGroup.setText("Check");
                        addMessageButton(checkGroup, SimpleDirectCommand.CHECK_MAP, "Map check");
                        addMessageButton(checkGroup, SimpleDirectCommand.CHECK_PACES, "Pace count");
                        addMessageButton(checkGroup, SimpleDirectCommand.CHECK_NUMER_PRESENT, "Head count");
                        checkGroup.pack(true);
                    }
                    {
                        final Group personMoveGroup = new Group(handSignalDialog, SWT.DEFAULT);
                        final RowLayout groupLayout = new RowLayout(SWT.VERTICAL);
                        layout.fill = true;
                        personMoveGroup.setLayout(groupLayout);
                        personMoveGroup.setText("Person Move");
                        addMessageButton(personMoveGroup, SimpleDirectCommand.JOIN_ME, "Join me");
                        addMessageButton(personMoveGroup,
                                SimpleDirectCommand.getRoleForwardInstance(MilitaryRole.PLATOON_LEADER),
                                "Platoon leader forward");
                        addMessageButton(personMoveGroup,
                                SimpleDirectCommand.getRoleForwardInstance(MilitaryRole.PLATOON_SERGEANT),
                                "Platoon sergeant forward");
                        addMessageButton(personMoveGroup, SimpleDirectCommand.getRoleForwardInstance(MilitaryRole.RTO),
                                "RTO forward");
                        addMessageButton(personMoveGroup,
                                SimpleDirectCommand.getRoleForwardInstance(MilitaryRole.SQUAD_LEADER_1),
                                "Squad 1 leader forward");
                        addMessageButton(personMoveGroup,
                                SimpleDirectCommand.getRoleForwardInstance(MilitaryRole.SQUAD_LEADER_2),
                                "Squad 2 leader forward");
                        addMessageButton(personMoveGroup,
                                SimpleDirectCommand.getRoleForwardInstance(MilitaryRole.SQUAD_LEADER_3),
                                "Squad 3 leader forward");
                        addMessageButton(personMoveGroup,
                                SimpleDirectCommand.getRoleForwardInstance(MilitaryRole.SQUAD_LEADER_4),
                                "Squad 4 leader forward");
                        personMoveGroup.pack(true);
                    }
                    {
                        final Group assembleGroup = new Group(handSignalDialog, SWT.DEFAULT);
                        final RowLayout groupLayout = new RowLayout(SWT.VERTICAL);// TODO
                        layout.fill = true;
                        assembleGroup.setLayout(groupLayout);
                        assembleGroup.setText("Assemble");
                        for (SimpleRelativeLocation location : SimpleRelativeLocation.values()) {
                            addMessageButton(assembleGroup, SimpleDirectCommand.getAssembleInstance(location),
                                    location.toString());
                        }
                        assembleGroup.pack(true);
                    }
                    {
                        final Group formationGroup = new Group(handSignalDialog, SWT.DEFAULT);
                        final RowLayout groupLayout = new RowLayout(SWT.VERTICAL);// TODO
                        layout.fill = true;
                        formationGroup.setLayout(groupLayout);
                        formationGroup.setText("Change Formation");
                        addMessageButton(formationGroup,
                                SimpleDirectCommand.getChangeFormationInstance(SimpleFormationName.WEDGE), "Wedge");
                        addMessageButton(formationGroup,
                                SimpleDirectCommand.getChangeFormationInstance(SimpleFormationName.ECHELON_LEFT),
                                "Echelon left");
                        addMessageButton(formationGroup,
                                SimpleDirectCommand.getChangeFormationInstance(SimpleFormationName.ECHELON_RIGHT),
                                "Echelon right");
                        addMessageButton(formationGroup,
                                SimpleDirectCommand.getChangeFormationInstance(SimpleFormationName.LINE), "Line");
                        addMessageButton(formationGroup,
                                SimpleDirectCommand.getChangeFormationInstance(SimpleFormationName.VEE), "Vee");
                        addMessageButton(formationGroup,
                                SimpleDirectCommand.getChangeFormationInstance(SimpleFormationName.STAGGERED_COLUMN),
                                "Staggered column");
                        addMessageButton(formationGroup,
                                SimpleDirectCommand.getChangeFormationInstance(SimpleFormationName.COLUMN), "Column");
                        addMessageButton(formationGroup,
                                SimpleDirectCommand.getChangeFormationInstance(SimpleFormationName.HERRINGBONE),
                                "Herringone");
                        addMessageButton(formationGroup,
                                SimpleDirectCommand.getChangeFormationInstance(SimpleFormationName.DISPERSED),
                                "Disperse");
                        formationGroup.pack(true);
                    }
                    {
                        final Group battleDrillGroup = new Group(handSignalDialog, SWT.DEFAULT);
                        final RowLayout groupLayout = new RowLayout(SWT.VERTICAL);
                        layout.fill = true;
                        battleDrillGroup.setLayout(groupLayout);
                        battleDrillGroup.setText("Battle Drill");
                        addMessageButton(battleDrillGroup,
                                SimpleDirectCommand.getPerformBattleDrillInstance(BattleDrillName.CONTACT_LEFT),
                                "Contact left");
                        addMessageButton(battleDrillGroup,
                                SimpleDirectCommand.getPerformBattleDrillInstance(BattleDrillName.CONTACT_RIGHT),
                                "Contact right");
                        addMessageButton(battleDrillGroup,
                                SimpleDirectCommand.getPerformBattleDrillInstance(BattleDrillName.AIR_ATTACK),
                                "Air attack");
                        addMessageButton(battleDrillGroup,
                                SimpleDirectCommand.getPerformBattleDrillInstance(BattleDrillName.CBRN_DANGER),
                                "CBRN attack");
                        battleDrillGroup.pack(true);
                    }
                    {
                        final Group enemyInSightGroup = new Group(handSignalDialog, SWT.DEFAULT);
                        final RowLayout groupLayout = new RowLayout(SWT.VERTICAL);// TODO
                        layout.fill = true;
                        enemyInSightGroup.setLayout(groupLayout);
                        enemyInSightGroup.setText("Enemy in Sight");
                        for (SimpleRelativeLocation location : SimpleRelativeLocation.values()) {
                            addMessageButton(enemyInSightGroup, SimpleStatement.getEnemyInSight(location),
                                    location.toString());
                        }
                        enemyInSightGroup.pack(true);
                    }
                    {
                        final Group miscGroup = new Group(handSignalDialog, SWT.DEFAULT);
                        final RowLayout groupLayout = new RowLayout(SWT.VERTICAL);
                        layout.fill = true;
                        miscGroup.setLayout(groupLayout);
                        miscGroup.setText("Miscellaneous");
                        addMessageButton(miscGroup, SimpleStatement.ACKNOWLEDGE_MESSAGE, "Message acknowleded");
                        addMessageButton(miscGroup, SimpleStatement.DANGER_AREA, "Danger area");
                        addMessageButton(miscGroup, SimpleDirectCommand.FIX_BAYONET, "Fix bayonets");
                        miscGroup.pack(true);
                    }
                    {
                        final Button cancelButton = new Button(handSignalDialog, SWT.PUSH);
                        cancelButton.setText("Cancel");
                        cancelButton.addListener(SWT.Selection, event -> close());
                    }
                    {
                        final Button sendButton = new Button(handSignalDialog, SWT.PUSH);
                        sendButton.setText("Send");
                        handSignalDialog.setDefaultButton(sendButton);
                        sendButton.addListener(SWT.Selection, event -> send());
                    }
                    handSignalDialog.pack(true);
                }

                private void addMessageButton(Composite parent, Message message, String text) {
                    final Button button = new Button(parent, SWT.RADIO);
                    button.setText(text);
                    button.setData(message);
                }

                /**
                 * <p>
                 * Hide this GUID.
                 * </p>
                 */
                public final void close() {
                    handSignalDialog.close();
                }

                /**
                 * <p>
                 * Display this GUI.
                 * </p>
                 */
                public final void open() {
                    handSignalDialog.setEnabled(true);
                    handSignalDialog.setMinimized(false);
                    handSignalDialog.open();
                }

                private final void send() {
                    // TODO
                    close();
                }

            }//

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
                            if (medium == HandSignals.INSTANCE) {
                                final SendHandSignalGui sendHandSignalGui = new SendHandSignalGui();
                                mediumItem.setData(sendHandSignalGui);
                                mediumItem.addListener(SWT.Selection, event -> sendHandSignalGui.open());
                            }
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
