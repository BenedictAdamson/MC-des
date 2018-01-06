package uk.badamson.mc.actor.message;

/**
 * <p>
 * {@linkplain Noun nouns} for describing simple unit formations.
 * </p>
 * <p>
 * These nouns are for the elementary formations that low-level units (teams,
 * squads and platoons) use.
 * <p>
 * This enumeration of {@link Noun} objects should include all formations that
 * hand signals can communicate. It should include all the formations in the US
 * Army Training Circular TC&nbsp;3-21.60, <cite>Visual Signals</cite>, dated 17
 * March 2017; that document replaced US Army Field Manual FM&nbsp;21-60 dated
 * September 1987.
 * </p>
 */
public enum SimpleFormationName implements Noun {
    /**
     * <p>
     * A non specific formation.
     * </p>
     */
    DISPERSE;

    public static final double INFORMATION_CONTENT = 3.0;

    /**
     * @return
     */
    @Override
    public double getInformationContent() {
	return INFORMATION_CONTENT;
    }
}