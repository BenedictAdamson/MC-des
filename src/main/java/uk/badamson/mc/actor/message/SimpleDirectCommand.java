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
public final class SimpleDirectCommand implements Command {

    private static final Map<SimpleRelativeLocation, SimpleDirectCommand> ASSEMBLE_INSTANCES;
    static {
	final Map<SimpleRelativeLocation, SimpleDirectCommand> map = new EnumMap<>(SimpleRelativeLocation.class);
	for (SimpleRelativeLocation location : SimpleRelativeLocation.values()) {
	    final SimpleDirectCommand command = new SimpleDirectCommand(SimpleVerb.ASSEMBLE, location);
	    map.put(location, command);
	}
	ASSEMBLE_INSTANCES = map;
    }

    /**
     * <p>
     * A command by a leader to indicate movement of an team or squad into a
     * dispersed formation.
     * </p>
     * <ul>
     * <li>The {@linkplain #getVerb() verb} is
     * {@linkplain SimpleVerb#CHANGE_FORMATION change-formation}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is the
     * {@linkplain SimpleFormationName#DISPERSED disperse formation}.</li>
     * </ul>
     */
    public static final SimpleDirectCommand DISPERSE = new SimpleDirectCommand(SimpleVerb.CHANGE_FORMATION,
	    SimpleFormationName.DISPERSED);

    /**
     * <p>
     * A command by a leader to indicate an individual, team or squad should join,
     * follow, or come forwards to, the leader.
     * </p>
     * <ul>
     * <li>The {@linkplain #getVerb() verb} is {@linkplain SimpleVerb#JOIN
     * change-formation}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is
     * {@linkplain Pronoun#ME me}.</li>
     * </ul>
     */
    public static final SimpleDirectCommand JOIN_ME = new SimpleDirectCommand(SimpleVerb.JOIN, Pronoun.ME);

    /**
     * <p>
     * A command by a leader to indicate an individual, team or squad should
     * increase speed, double time, or rush.
     * </p>
     * <ul>
     * <li>The {@linkplain #getVerb() verb} is {@linkplain SimpleVerb#RUSH
     * change-formation}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is
     * {@linkplain Pronoun#IT it}.</li>
     * </ul>
     */
    public static final SimpleDirectCommand RUSH = new SimpleDirectCommand(SimpleVerb.RUSH, Pronoun.IT);

    /**
     * <p>
     * A command by a leader to indicate movement of an team or squad into a
     * {@linkplain SimpleFormationName#WEDGE wedge formation}.
     * </p>
     * <ul>
     * <li>The {@linkplain #getVerb() verb} is
     * {@linkplain SimpleVerb#CHANGE_FORMATION change-formation}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is the
     * {@linkplain SimpleFormationName#WEDGE wedge formation}.</li>
     * </ul>
     */
    public static final SimpleDirectCommand FORM_WEDGE = new SimpleDirectCommand(SimpleVerb.CHANGE_FORMATION,
	    SimpleFormationName.WEDGE);

    /**
     * <p>
     * A command by a leader to indicate movement of an team or squad into a
     * {@linkplain SimpleFormationName#VEE vee formation}.
     * </p>
     * <ul>
     * <li>The {@linkplain #getVerb() verb} is
     * {@linkplain SimpleVerb#CHANGE_FORMATION change-formation}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is the
     * {@linkplain SimpleFormationName#VEE vee formation}.</li>
     * </ul>
     */
    public static final SimpleDirectCommand FORM_VEE = new SimpleDirectCommand(SimpleVerb.CHANGE_FORMATION,
	    SimpleFormationName.VEE);

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
     * <li>The {@linkplain #getVerb() verb} is {@linkplain SimpleVerb#ASSEMBLE
     * change-formation}.</li>
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

    private final SimpleVerb verb;

    private final Set<Noun> objects;

    private SimpleDirectCommand(SimpleVerb verb, Noun object) {
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
	double information = EXTRA_INFORMATION_CONTENT + getSubject().getInformationContent()
		+ getVerb().getInformationContent();
	for (Noun object : getObjects()) {
	    information += object.getInformationContent();
	}
	return information;
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
     * The subject is always {@linkplain Pronoun#YOU you}.
     * </p>
     */
    @Override
    public final Pronoun getSubject() {
	return Pronoun.YOU;
    }

    /**
     * @return
     */
    @Override
    public final SimpleVerb getVerb() {
	return verb;
    }

}
