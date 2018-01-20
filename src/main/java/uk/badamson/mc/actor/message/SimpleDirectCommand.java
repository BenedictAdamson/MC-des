package uk.badamson.mc.actor.message;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * An enumeration of the simple, direct, elementary {@linkplain Command
 * commands} used by low-level unit (team, squad and platoon) leaders.
 * </p>
 * <p>
 * This enumeration of {@link Command} objects should include all commands that
 * hand signals can communicate. It should include all the commands in the US
 * Army Training Circular TC&nbsp;3-21.60, <cite>Visual Signals</cite>, dated 17
 * March 2017; that document replaced US Army Field Manual FM&nbsp;21-60 dated
 * September 1987.
 * </p>
 */
@Immutable
public final class SimpleDirectCommand extends AbstractMessage implements Command {

    private static final Map<SimpleRelativeLocation, SimpleDirectCommand> ASSEMBLE_INSTANCES;
    static {
	final Map<SimpleRelativeLocation, SimpleDirectCommand> map = new EnumMap<>(SimpleRelativeLocation.class);
	for (SimpleRelativeLocation location : SimpleRelativeLocation.values()) {
	    final SimpleDirectCommand command = new SimpleDirectCommand(Pronoun.YOU, SimpleVerb.ASSEMBLE, location);
	    map.put(location, command);
	}
	ASSEMBLE_INSTANCES = map;
    }

    private static final Map<SimpleFormationName, SimpleDirectCommand> CHANGE_FORMATION_INSTANCES;
    static {
	final Map<SimpleFormationName, SimpleDirectCommand> map = new EnumMap<>(SimpleFormationName.class);
	for (SimpleFormationName formation : SimpleFormationName.values()) {
	    final SimpleDirectCommand command = new SimpleDirectCommand(Pronoun.WE, SimpleVerb.CHANGE_FORMATION,
		    formation);
	    map.put(formation, command);
	}
	CHANGE_FORMATION_INSTANCES = map;
    }

    private static final Map<BattleDrillName, SimpleDirectCommand> PERFORM_BATTLE_DRILL_INSTANCES;
    static {
	final Map<BattleDrillName, SimpleDirectCommand> map = new EnumMap<>(BattleDrillName.class);
	for (BattleDrillName drill : BattleDrillName.values()) {
	    final SimpleDirectCommand command = new SimpleDirectCommand(Pronoun.WE, SimpleVerb.PERFORM_BATTLE_DRILL,
		    drill);
	    map.put(drill, command);
	}
	PERFORM_BATTLE_DRILL_INSTANCES = map;
    }

    /**
     * <p>
     * A command by a leader to indicate movement of an team or squad into a
     * dispersed formation.
     * </p>
     * <ul>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#WE
     * we}.</li>
     * <li>The {@linkplain #getVerb() verb} is
     * {@linkplain SimpleVerb#CHANGE_FORMATION change-formation}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is the
     * {@linkplain SimpleFormationName#DISPERSED disperse formation}.</li>
     * </ul>
     */
    public static final SimpleDirectCommand DISPERSE = new SimpleDirectCommand(Pronoun.WE, SimpleVerb.CHANGE_FORMATION,
	    SimpleFormationName.DISPERSED);

    /**
     * <p>
     * A command by a leader to indicate an individual, team or squad should join,
     * follow, or come forwards to, the leader.
     * </p>
     * <ul>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#YOU
     * you}.</li>
     * <li>The {@linkplain #getVerb() verb} is {@linkplain SimpleVerb#JOIN
     * change-formation}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is
     * {@linkplain Pronoun#ME me}.</li>
     * </ul>
     */
    public static final SimpleDirectCommand JOIN_ME = new SimpleDirectCommand(Pronoun.YOU, SimpleVerb.JOIN, Pronoun.ME);

    /**
     * <p>
     * A command by a leader to indicate an individual, team or squad should
     * increase speed, double time, or rush.
     * </p>
     * <ul>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#WE
     * we}.</li>
     * <li>The {@linkplain #getVerb() verb} is {@linkplain SimpleVerb#RUSH
     * rush}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is
     * {@linkplain Pronoun#IT it}.</li>
     * </ul>
     */
    public static final SimpleDirectCommand RUSH = new SimpleDirectCommand(Pronoun.WE, SimpleVerb.RUSH, Pronoun.IT);

    /**
     * <p>
     * A command by a leader to indicate an individual, team or squad should take
     * cover.
     * </p>
     * <ul>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#WE
     * we}.</li>
     * <li>The {@linkplain #getVerb() verb} is {@linkplain SimpleVerb#TAKE_COVER
     * take cover}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is
     * {@linkplain Pronoun#IT it}.</li>
     * </ul>
     */
    public static final SimpleDirectCommand TAKE_COVER = new SimpleDirectCommand(Pronoun.WE, SimpleVerb.TAKE_COVER,
	    Pronoun.IT);

    /**
     * <p>
     * A command by a leader to indicate an individual, team or squad should move
     * fast.
     * </p>
     * <ul>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#WE
     * we}.</li>
     * <li>The {@linkplain #getVerb() verb} is {@linkplain SimpleVerb#QUICK_TIME
     * quick time}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is
     * {@linkplain Pronoun#IT it}.</li>
     * </ul>
     */
    public static final SimpleDirectCommand QUICK_TIME = new SimpleDirectCommand(Pronoun.WE, SimpleVerb.QUICK_TIME,
	    Pronoun.IT);

    /**
     * <p>
     * A command by a leader to indicate an individual, team or squad should fix
     * their bayonet(s0 to their gun(s).
     * </p>
     * <ul>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#WE
     * we}.</li>
     * <li>The {@linkplain #getVerb() verb} is {@linkplain SimpleVerb#FIX_BAYONET
     * fix-bayonet}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is
     * {@linkplain Pronoun#IT it} (meaning, the subjects own bayonet).</li>
     * </ul>
     */
    public static final SimpleDirectCommand FIX_BAYONET = new SimpleDirectCommand(Pronoun.WE, SimpleVerb.FIX_BAYONET,
	    Pronoun.IT);

    /**
     * <p>
     * The extra information content of a {@link SimpleDirectCommand}, over the
     * information content of its constituent {@linkplain MessageElement elements}.
     * </p>
     */
    public static final double EXTRA_INFORMATION_CONTENT = 2.0;

    /**
     * <p>
     * A command by a leader to indicate movement of a team or squad together (from
     * a dispersed formation) to a {@linkplain SimpleRelativeLocation location}.
     * </p>
     * <ul>
     * <li>Always returns a (non null) instance.</li>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#YOU
     * you}.</li>
     * <li>The {@linkplain #getVerb() verb} is {@linkplain SimpleVerb#ASSEMBLE
     * assemble}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is the given
     * location.</li>
     * </ul>
     * 
     * @return the command
     * @throws NullPointerException
     *             If {@code location} is null.
     */
    public static final SimpleDirectCommand getAssembleInstance(SimpleRelativeLocation location) {
	Objects.requireNonNull(location, "location");
	return ASSEMBLE_INSTANCES.get(location);
    }

    /**
     * <p>
     * A command by a leader to indicate movement of an team or squad into a
     * {@linkplain SimpleFormationName formation}.
     * </p>
     * <ul>
     * <li>Always returns a (non null) command.</li>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#WE
     * we}.</li>
     * <li>The {@linkplain #getVerb() verb} is
     * {@linkplain SimpleVerb#CHANGE_FORMATION change-formation}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is the given
     * formation.</li>
     * </ul>
     * 
     * @param formation
     *            The formation to move into.
     * @throws NullPointerException
     *             If {@code formation} is null
     */
    public static SimpleDirectCommand getChangeFormationInstance(SimpleFormationName formation) {
	Objects.requireNonNull(formation, "formation");
	return CHANGE_FORMATION_INSTANCES.get(formation);
    }

    /**
     * <p>
     * A command by a leader to indicate that a team or squad should perform a
     * {@linkplain BattleDrillName battle drill}.
     * </p>
     * <ul>
     * <li>Always returns a (non null) command.</li>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#WE
     * we}.</li>
     * <li>The {@linkplain #getVerb() verb} is
     * {@linkplain SimpleVerb#PERFORM_BATTLE_DRILL perform-battle-drill}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is the given
     * drill.</li>
     * </ul>
     * 
     * @param drill
     *            The drill to perform.
     * @throws NullPointerException
     *             If {@code drill} is null
     */
    public static SimpleDirectCommand getPerformBattleDrillInstance(BattleDrillName drill) {
	Objects.requireNonNull(drill, "drill");
	return PERFORM_BATTLE_DRILL_INSTANCES.get(drill);
    }

    private final Pronoun subject;

    private final SimpleVerb verb;

    private final Set<Noun> objects;

    private SimpleDirectCommand(Pronoun subject, SimpleVerb verb, Noun object) {
	this.subject = subject;
	this.verb = verb;
	this.objects = Collections.singleton(object);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * The information content exceeds the total for the message elements by the
     * {@linkplain #EXTRA_INFORMATION_CONTENT same extra amount}.
     * </p>
     */
    @Override
    public final double getInformationContent() {
	return EXTRA_INFORMATION_CONTENT + getSubject().getInformationContent() + getVerb().getInformationContent()
		+ MessageElement.getInformationContent(getObjects());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Set<Noun> getObjects() {
	return objects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final UnusableIncompleteMessage getPartialMessage(double partLength) {
	return new UnusableIncompleteMessage(partLength);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * The subject is either {@linkplain Pronoun#YOU you} or {@linkplain Pronoun#WE
     * we}.
     * </p>
     */
    @Override
    public final Pronoun getSubject() {
	return subject;
    }

    /**
     * @return
     */
    @Override
    public final SimpleVerb getVerb() {
	return verb;
    }

}
