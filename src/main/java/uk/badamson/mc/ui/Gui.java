package uk.badamson.mc.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.jcip.annotations.ThreadSafe;
import uk.badamson.mc.Game;
import uk.badamson.mc.Main;
import uk.badamson.mc.mind.MediumUnavailableException;
import uk.badamson.mc.mind.MessageTransferInProgress;
import uk.badamson.mc.mind.Mind;
import uk.badamson.mc.mind.MindInterface;
import uk.badamson.mc.mind.medium.HandSignals;
import uk.badamson.mc.mind.medium.Medium;
import uk.badamson.mc.mind.message.BattleDrillName;
import uk.badamson.mc.mind.message.Message;
import uk.badamson.mc.mind.message.MilitaryRole;
import uk.badamson.mc.mind.message.SimpleDirectCommand;
import uk.badamson.mc.mind.message.SimpleFormationName;
import uk.badamson.mc.mind.message.SimpleRelativeLocation;
import uk.badamson.mc.mind.message.SimpleStatement;
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
@ThreadSafe
public final class Gui implements AutoCloseable, Runnable {

    /**
     * <p>
     * The GUI of one instance of the Mission Command game.
     * </p>
     * <p>
     * For each instance of the game, it is possible to control one
     * {@linkplain Person person}.
     * </p>
     */
    @ThreadSafe
    public final class GameGui {

        /**
         * <p>
         * The GUI for the {@linkplain Game#getPerson() played person} of one instance
         * of the {@linkplain Game Mission Command Game}.
         * </p>
         */
        public final class PlayedPersonGui implements Mind {

            /**
             * <p>
             * The GUI for causing the {@linkplain Game#getPerson() played person} of one
             * instance of the {@linkplain Game Mission Command Game} to
             * {@linkplain MindInterface#beginSendingMessage(Medium, Message) send} a
             * {@linkplain HandSignals hand signal}.
             * </p>
             */
            public final class SendHandSignalGui {

                private final Shell handSignalDialog;
                private final List<Button> messageButtons = new ArrayList<>(
                        SimpleDirectCommand.values().length + SimpleStatement.values().length);
                private final Button sendButton;
                private final Listener selectMessage = event -> {
                    final Button selectedButton = (Button) event.widget;
                    for (Button messageButton : messageButtons) {
                        messageButton.setSelection(false);
                    }
                    selectedButton.setSelection(true);
                    selectedMessage = (Message) selectedButton.getData();
                    enableSending();
                };
                private Message selectedMessage;

                /**
                 * <p>
                 * Create this GUI, in a non-displayed (closed, invisible) state.
                 * </p>
                 */
                SendHandSignalGui() {
                    handSignalDialog = new Shell(gameWindow, SWT.DIALOG_TRIM);
                    handSignalDialog.setText("Mission Command: " + uiName + ": Hand Signal");
                    handSignalDialog.setData(this);
                    handSignalDialog.setEnabled(false);
                    handSignalDialog.setVisible(false);
                    handSignalDialog.addListener(SWT.Close, event -> {
                        /* Override standard behaviour to hide the dialog rather than dispose of it. */
                        event.doit = false;
                        close();
                    });
                    final GridLayout dialogLayout = new GridLayout(6, true);
                    handSignalDialog.setLayout(dialogLayout);
                    {
                        final Group assembleGroup = new Group(handSignalDialog, SWT.DEFAULT);
                        final SimpleRelativeLocationLayout groupLayout = new SimpleRelativeLocationLayout();
                        assembleGroup.setLayout(groupLayout);
                        assembleGroup.setText("Assemble");
                        for (SimpleRelativeLocation location : SimpleRelativeLocation.values()) {
                            createLocationButton(assembleGroup, SimpleDirectCommand.getAssembleInstance(location),
                                    location);
                        }
                        assembleGroup.pack(true);
                        final GridData gridData = new GridData();
                        gridData.horizontalSpan = 2;
                        gridData.verticalSpan = 9;
                        gridData.verticalAlignment = SWT.TOP;
                        gridData.horizontalAlignment = SWT.FILL;
                        assembleGroup.setLayoutData(gridData);
                    }
                    {
                        final Group allMoveGroup = new Group(handSignalDialog, SWT.DEFAULT);
                        final RowLayout groupLayout = new RowLayout(SWT.VERTICAL);
                        groupLayout.fill = true;
                        allMoveGroup.setLayout(groupLayout);
                        allMoveGroup.setText("All Move");
                        createMessageButton(allMoveGroup, SimpleDirectCommand.RUSH, "Rush");
                        createMessageButton(allMoveGroup, SimpleDirectCommand.QUICK_TIME, "Quick time");
                        createMessageButton(allMoveGroup, SimpleDirectCommand.HALT, "Halt");
                        createMessageButton(allMoveGroup, SimpleDirectCommand.HALT_AND_FREEZE, "Halt and freeze");
                        createMessageButton(allMoveGroup, SimpleDirectCommand.HALT_AND_TAKE_A_KNEE,
                                "Halt and take a knee");
                        createMessageButton(allMoveGroup, SimpleDirectCommand.HALT_AND_GO_PRONE, "Halt and go prone");
                        createMessageButton(allMoveGroup, SimpleDirectCommand.TAKE_COVER, "Take cover");
                        allMoveGroup.pack(true);
                        final GridData gridData = new GridData();
                        gridData.horizontalSpan = 2;
                        gridData.verticalSpan = 9;
                        gridData.verticalAlignment = SWT.TOP;
                        gridData.horizontalAlignment = SWT.FILL;
                        allMoveGroup.setLayoutData(gridData);
                    }
                    {
                        final Group formationGroup = new Group(handSignalDialog, SWT.DEFAULT);
                        final RowLayout groupLayout = new RowLayout(SWT.VERTICAL);// TODO
                        groupLayout.fill = true;
                        formationGroup.setLayout(groupLayout);
                        formationGroup.setText("Change Formation");
                        createMessageButton(formationGroup,
                                SimpleDirectCommand.getChangeFormationInstance(SimpleFormationName.WEDGE), "Wedge");
                        createMessageButton(formationGroup,
                                SimpleDirectCommand.getChangeFormationInstance(SimpleFormationName.ECHELON_LEFT),
                                "Echelon left");
                        createMessageButton(formationGroup,
                                SimpleDirectCommand.getChangeFormationInstance(SimpleFormationName.ECHELON_RIGHT),
                                "Echelon right");
                        createMessageButton(formationGroup,
                                SimpleDirectCommand.getChangeFormationInstance(SimpleFormationName.LINE), "Line");
                        createMessageButton(formationGroup,
                                SimpleDirectCommand.getChangeFormationInstance(SimpleFormationName.VEE), "Vee");
                        createMessageButton(formationGroup,
                                SimpleDirectCommand.getChangeFormationInstance(SimpleFormationName.STAGGERED_COLUMN),
                                "Staggered column");
                        createMessageButton(formationGroup,
                                SimpleDirectCommand.getChangeFormationInstance(SimpleFormationName.COLUMN), "Column");
                        createMessageButton(formationGroup,
                                SimpleDirectCommand.getChangeFormationInstance(SimpleFormationName.HERRINGBONE),
                                "Herringone");
                        createMessageButton(formationGroup,
                                SimpleDirectCommand.getChangeFormationInstance(SimpleFormationName.DISPERSED),
                                "Disperse");
                        formationGroup.pack(true);
                        final GridData gridData = new GridData();
                        gridData.horizontalSpan = 2;
                        gridData.verticalSpan = 9;
                        gridData.verticalAlignment = SWT.TOP;
                        gridData.horizontalAlignment = SWT.FILL;
                        formationGroup.setLayoutData(gridData);
                    }
                    {
                        final Group enemyInSightGroup = new Group(handSignalDialog, SWT.DEFAULT);
                        final SimpleRelativeLocationLayout groupLayout = new SimpleRelativeLocationLayout();
                        enemyInSightGroup.setLayout(groupLayout);
                        enemyInSightGroup.setText("Enemy in Sight");
                        for (SimpleRelativeLocation location : SimpleRelativeLocation.values()) {
                            createLocationButton(enemyInSightGroup, SimpleStatement.getEnemyInSight(location),
                                    location);
                        }
                        enemyInSightGroup.pack(true);
                        final GridData gridData = new GridData();
                        gridData.horizontalSpan = 2;
                        gridData.verticalSpan = 8;
                        gridData.verticalAlignment = SWT.BOTTOM;
                        gridData.horizontalAlignment = SWT.FILL;
                        enemyInSightGroup.setLayoutData(gridData);
                    }
                    {
                        final Group personMoveGroup = new Group(handSignalDialog, SWT.DEFAULT);
                        final RowLayout groupLayout = new RowLayout(SWT.VERTICAL);
                        groupLayout.fill = true;
                        personMoveGroup.setLayout(groupLayout);
                        personMoveGroup.setText("Person Move");
                        createMessageButton(personMoveGroup, SimpleDirectCommand.JOIN_ME, "Join me");
                        createMessageButton(personMoveGroup,
                                SimpleDirectCommand.getRoleForwardInstance(MilitaryRole.PLATOON_LEADER),
                                "Platoon leader forward");
                        createMessageButton(personMoveGroup,
                                SimpleDirectCommand.getRoleForwardInstance(MilitaryRole.PLATOON_SERGEANT),
                                "Platoon sergeant forward");
                        createMessageButton(personMoveGroup,
                                SimpleDirectCommand.getRoleForwardInstance(MilitaryRole.RTO), "RTO forward");
                        createMessageButton(personMoveGroup,
                                SimpleDirectCommand.getRoleForwardInstance(MilitaryRole.SQUAD_LEADER_1),
                                "Squad 1 leader forward");
                        createMessageButton(personMoveGroup,
                                SimpleDirectCommand.getRoleForwardInstance(MilitaryRole.SQUAD_LEADER_2),
                                "Squad 2 leader forward");
                        createMessageButton(personMoveGroup,
                                SimpleDirectCommand.getRoleForwardInstance(MilitaryRole.SQUAD_LEADER_3),
                                "Squad 3 leader forward");
                        createMessageButton(personMoveGroup,
                                SimpleDirectCommand.getRoleForwardInstance(MilitaryRole.SQUAD_LEADER_4),
                                "Squad 4 leader forward");
                        personMoveGroup.pack(true);
                        final GridData gridData = new GridData();
                        gridData.horizontalSpan = 2;
                        gridData.verticalSpan = 8;
                        gridData.verticalAlignment = SWT.TOP;
                        gridData.horizontalAlignment = SWT.FILL;
                        personMoveGroup.setLayoutData(gridData);
                    }
                    {
                        final Group battleDrillGroup = new Group(handSignalDialog, SWT.DEFAULT);
                        final RowLayout groupLayout = new RowLayout(SWT.VERTICAL);
                        groupLayout.fill = true;
                        battleDrillGroup.setLayout(groupLayout);
                        battleDrillGroup.setText("Battle Drill");
                        createMessageButton(battleDrillGroup,
                                SimpleDirectCommand.getPerformBattleDrillInstance(BattleDrillName.CONTACT_LEFT),
                                "Contact left");
                        createMessageButton(battleDrillGroup,
                                SimpleDirectCommand.getPerformBattleDrillInstance(BattleDrillName.CONTACT_RIGHT),
                                "Contact right");
                        createMessageButton(battleDrillGroup,
                                SimpleDirectCommand.getPerformBattleDrillInstance(BattleDrillName.AIR_ATTACK),
                                "Air attack");
                        createMessageButton(battleDrillGroup,
                                SimpleDirectCommand.getPerformBattleDrillInstance(BattleDrillName.CBRN_DANGER),
                                "CBRN attack");
                        battleDrillGroup.pack(true);
                        final GridData gridData = new GridData();
                        gridData.horizontalSpan = 2;
                        gridData.verticalSpan = 8;
                        gridData.verticalAlignment = SWT.TOP;
                        gridData.horizontalAlignment = SWT.FILL;
                        battleDrillGroup.setLayoutData(gridData);
                    }
                    {
                        final Label spacer = new Label(handSignalDialog, SWT.DEFAULT);
                        spacer.setSize(0, 0);
                        spacer.setVisible(false);
                        spacer.pack(true);
                        final GridData gridData = new GridData();
                        gridData.horizontalSpan = 2;
                        gridData.verticalSpan = 3;
                        gridData.verticalAlignment = SWT.TOP;
                        gridData.heightHint = 0;
                        spacer.setLayoutData(gridData);
                    }
                    {
                        final Group checkGroup = new Group(handSignalDialog, SWT.DEFAULT);
                        final RowLayout groupLayout = new RowLayout(SWT.VERTICAL);
                        groupLayout.fill = true;
                        checkGroup.setLayout(groupLayout);
                        checkGroup.setText("Check");
                        createMessageButton(checkGroup, SimpleDirectCommand.CHECK_MAP, "Map check");
                        createMessageButton(checkGroup, SimpleDirectCommand.CHECK_PACES, "Pace count");
                        createMessageButton(checkGroup, SimpleDirectCommand.CHECK_NUMER_PRESENT, "Head count");
                        checkGroup.pack(true);
                        final GridData gridData = new GridData();
                        gridData.horizontalSpan = 2;
                        gridData.verticalSpan = 3;
                        gridData.verticalAlignment = SWT.TOP;
                        gridData.horizontalAlignment = SWT.FILL;
                        checkGroup.setLayoutData(gridData);
                    }
                    {
                        final Group miscGroup = new Group(handSignalDialog, SWT.DEFAULT);
                        final RowLayout groupLayout = new RowLayout(SWT.VERTICAL);
                        groupLayout.fill = true;
                        miscGroup.setLayout(groupLayout);
                        miscGroup.setText("Miscellaneous");
                        createMessageButton(miscGroup, SimpleStatement.ACKNOWLEDGE_MESSAGE, "Message acknowleded");
                        createMessageButton(miscGroup, SimpleStatement.DANGER_AREA, "Danger area");
                        createMessageButton(miscGroup, SimpleDirectCommand.FIX_BAYONET, "Fix bayonets");
                        miscGroup.pack(true);
                        final GridData gridData = new GridData();
                        gridData.horizontalSpan = 2;
                        gridData.verticalSpan = 3;
                        gridData.verticalAlignment = SWT.TOP;
                        gridData.horizontalAlignment = SWT.FILL;
                        miscGroup.setLayoutData(gridData);
                    }
                    {
                        final Label spacer = new Label(handSignalDialog, SWT.DEFAULT);
                        spacer.setSize(0, 0);
                        spacer.setVisible(false);
                        spacer.pack(true);
                        final GridData gridData = new GridData();
                        gridData.horizontalSpan = 4;
                        gridData.verticalAlignment = SWT.BOTTOM;
                        gridData.heightHint = 0;
                        spacer.setLayoutData(gridData);
                    }
                    {
                        final Button cancelButton = new Button(handSignalDialog, SWT.PUSH | SWT.CANCEL);
                        cancelButton.setText("Cancel");
                        cancelButton.addListener(SWT.Selection, event -> close());
                        cancelButton.pack(true);
                        final GridData gridData = new GridData();
                        gridData.horizontalSpan = 1;
                        gridData.verticalSpan = 1;
                        gridData.verticalAlignment = SWT.BOTTOM;
                        cancelButton.setLayoutData(gridData);
                    }
                    {
                        sendButton = new Button(handSignalDialog, SWT.PUSH | SWT.OK);
                        sendButton.setText("Signal");
                        sendButton.setEnabled(false);
                        handSignalDialog.setDefaultButton(sendButton);
                        sendButton.addListener(SWT.Selection, event -> send());
                        sendButton.pack(true);
                        final GridData gridData = new GridData();
                        gridData.horizontalSpan = 1;
                        gridData.verticalSpan = 1;
                        gridData.verticalAlignment = SWT.BOTTOM;
                        gridData.horizontalAlignment = SWT.FILL;
                        sendButton.setLayoutData(gridData);
                    }
                    handSignalDialog.pack(true);
                }

                /**
                 * <p>
                 * Hide this GUID.
                 * </p>
                 */
                public final void close() {
                    handSignalDialog.setVisible(false);
                }

                private Button createLocationButton(final Composite parent, Message message,
                        SimpleRelativeLocation location) {
                    final Button button = new Button(parent, SWT.RADIO);
                    button.setData(message);
                    button.setLayoutData(location);
                    button.pack();
                    messageButtons.add(button);
                    button.addListener(SWT.Selection, selectMessage);
                    return button;
                }

                private Button createMessageButton(Composite parent, Message message, String text) {
                    final Button button = new Button(parent, SWT.RADIO);
                    button.setText(text);
                    button.setData(message);
                    messageButtons.add(button);
                    button.addListener(SWT.Selection, selectMessage);
                    return button;
                }

                private void enableSending() {
                    sendButton.setEnabled(true);
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
                    try {
                        beginSendingMesage(HandSignals.INSTANCE, selectedMessage);
                    } catch (MediumUnavailableException e) {
                        // TODO pop up error dialog
                        // TODO prevent dialog being opened again
                        sendButton.setEnabled(false);
                    }
                    close();
                }

            }// class

            private final MindInterface person;
            private String uiName;

            /**
             * @param person
             *            The API (service interface) through which this GUI effects changes
             *            to the simulation of the played person.
             * @param uiName
             *            The name to use to identify this person in the GUI.
             * @throws NullPointerException
             *             <ul>
             *             <li>If {@code person} is null.</li>
             *             <li>If {@code uiName} is null.</li>
             *             </ul>
             * @throws IllegalArgumentException
             *             If {@code person} is not one of the {@linkplain Game#getPersons()
             *             simulated persons} of the {@linkplain Gui.GameGui#getGame() game}
             *             for which this is a GUI.
             */
            PlayedPersonGui(MindInterface person, String uiName) {
                this.person = Objects.requireNonNull(person, "person");
                this.uiName = Objects.requireNonNull(uiName, "uiName");
                if (!getGame().getPersons().contains(person)) {
                    throw new IllegalArgumentException(
                            "person is not one of the simulated persons of the game for which this is a GUI");
                }
                for (Medium medium : person.getMedia()) {
                    final MenuItem mediumItem = new MenuItem(sendMenu, SWT.PUSH);
                    mediumItem.setText(medium.toString() + "...");
                    if (medium == HandSignals.INSTANCE) {
                        final SendHandSignalGui sendHandSignalGui = new SendHandSignalGui();
                        mediumItem.setData(sendHandSignalGui);
                        mediumItem.addListener(SWT.Selection, event -> sendHandSignalGui.open());
                    }
                }
                sendMenu.setEnabled(true);
                clearTransmissionInProgress();
            }

            final void beginSendingMesage(Medium medium, Message fullMessage) throws MediumUnavailableException {
                if (person.getTransmissionInProgress() != null) {
                    person.haltSendingMessage();
                }
                person.beginSendingMessage(medium, fullMessage);
                transmissionMediumLabel.setText(medium.toString());
                transmissionInProgressMessage.setText(fullMessage.toString());
                transmissionInProgressBar.setSelection(0);
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
            public final MindInterface getPerson() {
                return person;
            }

            private void setTransmissionInProgressBar(final MessageTransferInProgress transmissionProgress,
                    final Message fullMessage) {
                final double lengthSent = transmissionProgress.getMessageSofar().getInformationContent();
                final double length = fullMessage.getInformationContent();
                final int progress = (int) ((lengthSent / length) * Integer.MAX_VALUE);
                transmissionInProgressBar.setSelection(progress);
            }

            @Override
            public void tellBeginReceivingMessage(MessageTransferInProgress receptionStarted) {
                // TODO Auto-generated method stub

            }

            @Override
            public void tellMessageReceptionProgress(MessageTransferInProgress messageBeingReceived) {
                // TODO Auto-generated method stub

            }

            @Override
            public void tellMessageSendingEnded(MessageTransferInProgress transmissionProgress, Message fullMessage) {
                display.asyncExec(() -> {
                    setTransmissionInProgressBar(transmissionProgress, fullMessage);
                    transmissionInProgressBar.update();
                    clearTransmissionInProgress();
                });
            }

            @Override
            public void tellMessageTransmissionProgress(final MessageTransferInProgress transmissionProgress,
                    final Message fullMessage) {
                display.asyncExec(() -> {
                    setTransmissionInProgressBar(transmissionProgress, fullMessage);
                });
            }

        }// class

        private final Game game;
        private final Text transmissionInProgressMessage;
        private final ProgressBar transmissionInProgressBar;
        private final Label transmissionMediumLabel;
        private final Menu sendMenu;
        private final Label timeLabel;
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
            gameWindow = new Shell(display);
            gameWindow.setData(this);
            if (game.getName() == null) {
                gameWindow.setText("Misson Command Game");
            } else {
                gameWindow.setText("Misson Command: " + game.getName());
            }
            final RowLayout gameWindowLayout = new RowLayout(SWT.VERTICAL);
            gameWindowLayout.fill = true;
            gameWindow.setLayout(gameWindowLayout);
            {
                final Menu menuBar = new Menu(gameWindow, SWT.BAR);
                {
                    final MenuItem sendMenuItem = new MenuItem(menuBar, SWT.CASCADE);
                    sendMenuItem.setText("Send");
                    sendMenu = new Menu(menuBar);
                    sendMenuItem.setMenu(sendMenu);
                    sendMenuItem.setEnabled(false);
                }
                gameWindow.setMenuBar(menuBar);
            }
            {
                timeLabel = new Label(gameWindow, SWT.LEFT | SWT.HORIZONTAL);
                updateClockDisplay();
            }
            {
                final Button runButton = new Button(gameWindow, SWT.PUSH);
                runButton.setText("Advance Simulation");
                runButton.addListener(SWT.Selection, event -> advanceSimulation(game));
            }
            {
                final Group transmissionInProgressGroup = new Group(gameWindow, SWT.DEFAULT);
                final RowLayout layout = new RowLayout(SWT.VERTICAL);
                layout.fill = true;
                transmissionInProgressGroup.setLayout(layout);
                transmissionInProgressGroup.setText("Message being sent");
                transmissionMediumLabel = new Label(transmissionInProgressGroup, SWT.LEFT);
                transmissionInProgressMessage = new Text(transmissionInProgressGroup,
                        SWT.MULTI | SWT.LEFT | SWT.READ_ONLY);
                transmissionInProgressBar = new ProgressBar(transmissionInProgressGroup, SWT.HORIZONTAL);
                transmissionInProgressBar.setMinimum(0);
                transmissionInProgressBar.setMaximum(Integer.MAX_VALUE);
                clearTransmissionInProgress();
                transmissionInProgressGroup.setEnabled(false);
                transmissionInProgressGroup.pack();
            }
            {
                final Group messagesBeingReceivedGroup = new Group(gameWindow, SWT.DEFAULT);
                messagesBeingReceivedGroup.setText("Messages being received");
                // TODO MessagesBeingReceived
                messagesBeingReceivedGroup.setEnabled(false);
                messagesBeingReceivedGroup.pack(true);
            }
            gameWindow.pack(true);
            gameWindow.open();
        }

        private void advanceSimulation(Game game) {
            game.getClock().advanceSeconds(1.0);
            updateClockDisplay();
        }

        private void clearTransmissionInProgress() {
            transmissionMediumLabel.setText("Not sending");
            transmissionInProgressMessage.setText("");
            transmissionInProgressBar.setSelection(0);
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
         * <li>The {@linkplain Person#getPlayer() actor} of the given person becomes the
         * {@linkplain Gui.GameGui.PlayedPersonGui GUI} that this returns.</li>
         * </ul>
         * 
         * @param person
         *            The person to be controlled.
         * @param uiName
         *            The name to use to identify this person in the GUI.
         * @return the GUI through which the user controls the person.
         * @throws NullPointerException
         *             <ul>
         *             <li>If {@code person} is null.</li>
         *             <li>If {@code uiName} is null.</li>
         *             </ul>
         * @throws IllegalArgumentException
         *             If {@code person} is not one of the {@linkplain Game#getPersons()
         *             persons} of the {@linkplain #getGame() game} for which this is
         *             the GUI.
         */
        public final PlayedPersonGui takeControl(Person person, String uiName) {
            final PlayedPersonGui gui = new PlayedPersonGui(person, uiName);
            gameWindow.setText("Mission Command: " + uiName);
            game.takeControl(gui, person);
            return gui;
        }

        private void updateClockDisplay() {
            timeLabel.setText(Long.toString(game.getClock().getTime()));
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
        mainWindow.setText("Misson Command");
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
