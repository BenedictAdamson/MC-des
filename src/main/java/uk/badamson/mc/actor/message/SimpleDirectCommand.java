package uk.badamson.mc.actor.message;

import java.util.Collections;
import java.util.Set;

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
public final class SimpleDirectCommand implements Command {

    /**
     * <p>
     * A command by a leader to indicate movement of an individual, team or squad.
     * </p>
     * <ul>
     * <li>The {@linkplain #getVerb() verb} is
     * {@linkplain SimpleVerb#CHANGE_FORMATION change-formation}.</li>
     * <li>There is only one {@linkplain #getObjects() object}, which is the
     * {@linkplain SimpleFormationName#DISPERSE disperse formation}.</li>
     * </ul>
     */
    public static final SimpleDirectCommand DISPERSE = new SimpleDirectCommand();

    /**
     * <p>
     * The extra information content of a {@link SimpleDirectCommand}, over the
     * information content of its constituent {@linkplain MessageElement elements}.
     * </p>
     */
    public static final double EXTRA_INFORMATION_CONTENT = 2.0;

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
	return Collections.singleton(SimpleFormationName.DISPERSE);
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
	return SimpleVerb.CHANGE_FORMATION;
    }

}
