package uk.badamson.mc.actor.message;

/**
 * <p>
 * {@linkplain Noun nouns} for describing battle drills.
 * </p>
 * <p>
 * These nouns are for the elementary drills that low-level units (teams, squads
 * and platoons) use.
 * <p>
 * This enumeration of {@link Noun} objects should include all drills that hand
 * signals can communicate. It should include all the drills in the US Army
 * Training Circular TC&nbsp;3-21.60, <cite>Visual Signals</cite>, dated 17
 * March 2017; that document replaced US Army Field Manual FM&nbsp;21-60 dated
 * September 1987.
 * </p>
 */
public enum BattleDrillName implements Noun {
    CONTACT_LEFT,
    CONTACT_RIGHT,
    AIR_ATTACK,

    /**
     * <p>
     * The battle drill for when there is a chemical, biological, radiological or
     * nuclear attack or contamination.
     * </p>
     */
    CBRN_DANGER;

    /**
     * <p>
     * The {@linkplain #getInformationContent() information content} of a
     * {@link BattleDrillName}.
     * </p>
     */
    public static final double INFORMATION_CONTENT = Math.log(values().length) / Math.log(2);

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Objects of the {@link BattleDrillName} type have the additional constraint
     * that their information content is the {@linkplain #INFORMATION_CONTENT same}.
     * </p>
     */
    @Override
    public double getInformationContent() {
	return INFORMATION_CONTENT;
    }
}
