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
    DISPERSED,

    /**
     * <p>
     * A formation with a the minimum width and maximum depth.
     * </p>
     */
    COLUMN,

    /**
     * <p>
     * A formation with the right flank forward of the centre and the left flank
     * rearward of the centre.
     * </p>
     */
    ECHELON_LEFT,

    /**
     * <p>
     * A formation with the left flank forward of the centre and the right flank
     * rearward of the centre.
     * </p>
     */
    ECHELON_RIGHT,

    /**
     * <p>
     * A staggered column formation with elements facing forward and outwards
     * </p>
     */
    HERRINGBONE,

    /**
     * <p>
     * A formation with the centre neither forward nor rearward of the flanks.
     * </p>
     */
    LINE,

    /**
     * <p>
     * A formation with two columns next to each other, with column column slightly
     * further forwards.
     * </p>
     */
    STAGGERED_COLUMN,

    /**
     * <p>
     * A formation with the centre rearward of the flanks.
     * </p>
     */
    VEE,

    /**
     * <p>
     * A formation with the centre forward of the flanks.
     * </p>
     */
    WEDGE;

    /**
     * <p>
     * The {@linkplain #getInformationContent() information content} of a
     * {@link SimpleFormationName}.
     * </p>
     */
    public static final double INFORMATION_CONTENT = MessageElement.getInformationContent(values().length);

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Objects of the {@link SimpleFormationName} type have the additional
     * constraint that their information content is the
     * {@linkplain #INFORMATION_CONTENT same}.
     * </p>
     */
    @Override
    public double getInformationContent() {
        return INFORMATION_CONTENT;
    }
}