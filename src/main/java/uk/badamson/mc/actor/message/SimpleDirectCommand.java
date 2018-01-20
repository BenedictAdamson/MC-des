package uk.badamson.mc.actor.message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
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

    private static final Map<MilitaryRole, SimpleDirectCommand> ROLE_FORWARD_INSTANCES;
    static {
	final Map<MilitaryRole, SimpleDirectCommand> map = new EnumMap<>(MilitaryRole.class);
	for (MilitaryRole role : MilitaryRole.values()) {
	    final SimpleDirectCommand command = new SimpleDirectCommand(role, SimpleVerb.JOIN, Pronoun.ME);
	    map.put(role, command);
	}
	ROLE_FORWARD_INSTANCES = map;
    }

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
     * <li>This is one of the {@linkplain #values() array of finite values} of the
     * {@link SimpleDirectCommand} type.
     * </ul>
     */
    public static final SimpleDirectCommand JOIN_ME = new SimpleDirectCommand(Pronoun.YOU, SimpleVerb.JOIN, Pronoun.ME);

    /**
     * <p>
     * A command by a leader to indicate an element, team or squad should increase
     * speed, double time, or rush.
     * </p>
     * <ul>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#WE
     * we}.</li>
     * <li>The {@linkplain #getVerb() verb} is {@linkplain SimpleVerb#RUSH
     * rush}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is
     * {@linkplain Pronoun#IT it}.</li>
     * <li>This is one of the {@linkplain #values() array of finite values} of the
     * {@link SimpleDirectCommand} type.
     * </ul>
     */
    public static final SimpleDirectCommand RUSH = new SimpleDirectCommand(Pronoun.WE, SimpleVerb.RUSH, Pronoun.IT);

    /**
     * <p>
     * A command by a leader to indicate an element, team or squad should halt.
     * </p>
     * <ul>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#WE
     * we}.</li>
     * <li>The {@linkplain #getVerb() verb} is {@linkplain SimpleVerb#HALT
     * halt}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is
     * {@linkplain Pronoun#IT it}.</li>
     * <li>This is one of the {@linkplain #values() array of finite values} of the
     * {@link SimpleDirectCommand} type.
     * </ul>
     */
    public static final SimpleDirectCommand HALT = new SimpleDirectCommand(Pronoun.WE, SimpleVerb.HALT, Pronoun.IT);

    /**
     * <p>
     * A command by a leader to indicate an element, team or squad should halt and
     * take a knee.
     * </p>
     * <ul>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#WE
     * we}.</li>
     * <li>The {@linkplain #getVerb() verb} is
     * {@linkplain SimpleVerb#HALT_AND_TAKE_A_KNEE halt and take a knee}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is
     * {@linkplain Pronoun#IT it}.</li>
     * <li>This is one of the {@linkplain #values() array of finite values} of the
     * {@link SimpleDirectCommand} type.
     * </ul>
     */
    public static final SimpleDirectCommand HALT_AND_TAKE_A_KNEE = new SimpleDirectCommand(Pronoun.WE,
	    SimpleVerb.HALT_AND_TAKE_A_KNEE, Pronoun.IT);

    /**
     * <p>
     * A command by a leader to indicate an element, team or squad should halt and
     * go prone.
     * </p>
     * <ul>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#WE
     * we}.</li>
     * <li>The {@linkplain #getVerb() verb} is
     * {@linkplain SimpleVerb#HALT_AND_GO_PRONE halt and go prone}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is
     * {@linkplain Pronoun#IT it}.</li>
     * <li>This is one of the {@linkplain #values() array of finite values} of the
     * {@link SimpleDirectCommand} type.
     * </ul>
     */
    public static final SimpleDirectCommand HALT_AND_GO_PRONE = new SimpleDirectCommand(Pronoun.WE,
	    SimpleVerb.HALT_AND_GO_PRONE, Pronoun.IT);

    /**
     * <p>
     * A command by a leader to indicate an element, team or squad should take
     * cover.
     * </p>
     * <ul>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#WE
     * we}.</li>
     * <li>The {@linkplain #getVerb() verb} is {@linkplain SimpleVerb#TAKE_COVER
     * take cover}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is
     * {@linkplain Pronoun#IT it}.</li>
     * <li>This is one of the {@linkplain #values() array of finite values} of the
     * {@link SimpleDirectCommand} type.
     * </ul>
     */
    public static final SimpleDirectCommand TAKE_COVER = new SimpleDirectCommand(Pronoun.WE, SimpleVerb.TAKE_COVER,
	    Pronoun.IT);

    /**
     * <p>
     * A command by a leader to indicate an element, team or squad should move fast.
     * </p>
     * <ul>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#WE
     * we}.</li>
     * <li>The {@linkplain #getVerb() verb} is {@linkplain SimpleVerb#QUICK_TIME
     * quick time}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is
     * {@linkplain Pronoun#IT it}.</li>
     * <li>This is one of the {@linkplain #values() array of finite values} of the
     * {@link SimpleDirectCommand} type.
     * </ul>
     */
    public static final SimpleDirectCommand QUICK_TIME = new SimpleDirectCommand(Pronoun.WE, SimpleVerb.QUICK_TIME,
	    Pronoun.IT);

    /**
     * <p>
     * A command by a leader to indicate an element, team or squad should fix their
     * bayonet(s0 to their gun(s).
     * </p>
     * <ul>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#WE
     * we}.</li>
     * <li>The {@linkplain #getVerb() verb} is {@linkplain SimpleVerb#FIX_BAYONET
     * fix-bayonet}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is
     * {@linkplain Pronoun#IT it} (meaning, the subjects own bayonet).</li>
     * <li>This is one of the {@linkplain #values() array of finite values} of the
     * {@link SimpleDirectCommand} type.
     * </ul>
     */
    public static final SimpleDirectCommand FIX_BAYONET = new SimpleDirectCommand(Pronoun.WE, SimpleVerb.FIX_BAYONET,
	    Pronoun.IT);

    /**
     * <p>
     * A command by a leader to indicate the element, team, squad or platoon will
     * stop to check the map.
     * </p>
     * <ul>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#WE
     * we}.</li>
     * <li>The {@linkplain #getVerb() verb} is {@linkplain SimpleVerb#CHECK_MAP
     * check map}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is
     * {@linkplain Pronoun#IT it} (meaning, the subjects own bayonet).</li>
     * <li>This is one of the {@linkplain #values() array of finite values} of the
     * {@link SimpleDirectCommand} type.
     * </ul>
     */
    public static final SimpleDirectCommand CHECK_MAP = new SimpleDirectCommand(Pronoun.WE, SimpleVerb.CHECK_MAP,
	    Pronoun.IT);

    /**
     * <p>
     * A command by a leader to indicate the element, team, squad or platoon will
     * check the number of paces (distance) they have travelled.
     * </p>
     * <ul>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#WE
     * we}.</li>
     * <li>The {@linkplain #getVerb() verb} is {@linkplain SimpleVerb#CHECK_PACES
     * check paces}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is
     * {@linkplain Pronoun#IT it} (meaning, the subjects own bayonet).</li>
     * <li>This is one of the {@linkplain #values() array of finite values} of the
     * {@link SimpleDirectCommand} type.
     * </ul>
     */
    public static final SimpleDirectCommand CHECK_PACES = new SimpleDirectCommand(Pronoun.WE, SimpleVerb.CHECK_PACES,
	    Pronoun.IT);

    /**
     * <p>
     * A command by a leader to indicate the element, team, squad or platoon will
     * check the number of people present (perform a head-count).
     * </p>
     * <ul>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#WE
     * we}.</li>
     * <li>The {@linkplain #getVerb() verb} is
     * {@linkplain SimpleVerb#CHECK_NUMER_PRESENT check number present}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is
     * {@linkplain Pronoun#IT it} (meaning, the subjects own bayonet).</li>
     * <li>This is one of the {@linkplain #values() array of finite values} of the
     * {@link SimpleDirectCommand} type.
     * </ul>
     */
    public static final SimpleDirectCommand CHECK_NUMER_PRESENT = new SimpleDirectCommand(Pronoun.WE,
	    SimpleVerb.CHECK_NUMER_PRESENT, Pronoun.IT);

    private static final SimpleDirectCommand[] ALL;
    static {
	final List<SimpleDirectCommand> list = new ArrayList<>();
	list.addAll(ASSEMBLE_INSTANCES.values());
	list.addAll(CHANGE_FORMATION_INSTANCES.values());
	list.addAll(PERFORM_BATTLE_DRILL_INSTANCES.values());
	list.addAll(ROLE_FORWARD_INSTANCES.values());
	list.add(CHECK_MAP);
	list.add(CHECK_NUMER_PRESENT);
	list.add(CHECK_PACES);
	list.add(FIX_BAYONET);
	list.add(HALT);
	list.add(HALT_AND_TAKE_A_KNEE);
	list.add(HALT_AND_GO_PRONE);
	list.add(JOIN_ME);
	list.add(QUICK_TIME);
	list.add(RUSH);
	list.add(TAKE_COVER);
	ALL = list.toArray(new SimpleDirectCommand[0]);
    }

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

    /**
     * <p>
     * A command by a leader to indicate that a person in a given
     * {@linkplain MilitaryRole role} should come forward to the leader's location.
     * </p>
     * <ul>
     * <li>Always returns a (non null) command.</li>
     * <li>The {@linkplain #getSubject() subject} is the given role.</li>
     * <li>The {@linkplain #getVerb() verb} is {@linkplain SimpleVerb#JOIN
     * join}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is
     * {@linkplain Pronoun#ME me}.</li>
     * </ul>
     * 
     * @param role
     *            The role of the person who should come forward.
     * @throws NullPointerException
     *             If {@code role} is null
     */
    public static SimpleDirectCommand getRoleForwardInstance(MilitaryRole role) {
	Objects.requireNonNull(role, "role");
	return ROLE_FORWARD_INSTANCES.get(role);
    }

    /**
     * <p>
     * An array holding each of the permitted values of the
     * {@link SimpleDirectCommand} type.
     * </p>
     * <ul>
     * <li>Always returns a (non null) array of values.</li>
     * <li>The array of values has no null elements.</li>
     * <li>The array of values has no duplicate elements.</li>
     * </ul>
     * 
     * @return the values
     */
    public static final SimpleDirectCommand[] values() {
	return Arrays.copyOf(ALL, ALL.length);
    }

    private final Noun subject;

    private final SimpleVerb verb;

    private final Set<Noun> objects;

    private SimpleDirectCommand(Noun subject, SimpleVerb verb, Noun object) {
	this.subject = subject;
	this.verb = verb;
	this.objects = Collections.singleton(object);
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (!(obj instanceof SimpleDirectCommand))
	    return false;
	SimpleDirectCommand other = (SimpleDirectCommand) obj;
	return subject.equals(other.subject) && verb.equals(other.verb) && objects.equals(other.objects);
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
     * The subject is often either {@linkplain Pronoun#YOU you} or
     * {@linkplain Pronoun#WE we}.
     * </p>
     */
    @Override
    public final Noun getSubject() {
	return subject;
    }

    /**
     * @return
     */
    @Override
    public final SimpleVerb getVerb() {
	return verb;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + subject.hashCode();
	result = prime * result + verb.hashCode();
	result = prime * result + objects.hashCode();
	return result;
    }

    @Override
    public String toString() {
	return "SimpleDirectCommand [" + subject + " " + verb + " " + objects + "]";
    }

}
