package uk.badamson.mc.actor.message;

/**
 * <p>
 * {@linkplain Verb verbs} for describing simple actions.
 * </p>
 * <p>
 * This enumeration of {@link Verb} objects should include all actions that hand
 * signals can communicate. It should include all the commands in the US Army
 * Training Circular TC&nbsp;3-21.60, <cite>Visual Signals</cite>, dated 17
 * March 2017; that document replaced US Army Field Manual FM&nbsp;21-60 dated
 * September 1987.
 * </p>
 */
public enum SimpleVerb implements Verb {

    /**
     * <p>
     * A verb for declaring that the {@linkplain Sentence#getSubject() subject} of
     * the {@linkplain Sentence sentence} has changed formation, or to
     * {@linkplain Command command} that the subject changes formation.
     * </p>
     * <p>
     * The {@linkplain Sentence#getSubject() subject} of the sentence should be the
     * {@linkplain FormationName name of the formation}.
     * </p>
     */
    CHANGE_FORMATION;

    public static final double INFORMATION_CONTENT = 1.0;

    /**
     * {@inheritDoc}
     * 
     * <p>
     * All simple verbs have the {@linkplain #INFORMATION_CONTENT same information
     * content}.
     * </p>
     */
    @Override
    public double getInformationContent() {
	return INFORMATION_CONTENT;
    }
}
