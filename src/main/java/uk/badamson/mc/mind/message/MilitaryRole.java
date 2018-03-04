package uk.badamson.mc.mind.message;

/**
 * <p>
 * A {@linkplain Noun noun} with describing a common military unit role or
 * responsibility.
 * </p>
 */
public enum MilitaryRole implements Noun {

    /**
     * <p>
     * Radio Telephone Operator, radio man.
     * </p>
     */
    RTO,

    /**
     * <p>
     * The leader of squad number&nbsp;1; a corporal or sergeant.
     * </p>
     */
    SQUAD_LEADER_1,

    /**
     * <p>
     * The leader of squad number&nbsp;2; a corporal or sergeant.
     * </p>
     */
    SQUAD_LEADER_2,

    /**
     * <p>
     * The leader of squad number&nbsp;3; a corporal or sergeant.
     * </p>
     */
    SQUAD_LEADER_3,

    /**
     * <p>
     * The leader of squad number&nbsp;4; a corporal or sergeant.
     * </p>
     */
    SQUAD_LEADER_4,

    /**
     * <p>
     * The second in command of a platoon; a sergeant.
     * </p>
     */
    PLATOON_SERGEANT,

    /**
     * <p>
     * The leader of a platoon; a lieutenant.
     * </p>
     */
    PLATOON_LEADER;

    private static final MilitaryRole[] SQUAD_LEADER = { SQUAD_LEADER_1, SQUAD_LEADER_2, SQUAD_LEADER_3,
            SQUAD_LEADER_4 };

    public static final double INFORMATION_CONTENT = MessageElement.getInformationContent(values().length);

    /**
     * <p>
     * The leader of the squad that has a given ID number.
     * </p>
     * 
     * @param squad
     *            The ID number of the squad.
     * @return the leader of the given squad; not null.
     * @throws IndexOutOfBoundsException
     *             If {@code squad} is outside the range [1,4].
     */
    public static MilitaryRole getSquadLeader(int squad) {
        return SQUAD_LEADER[squad - 1];
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Objects of the {@link Pronoun} type have the additional constraint that their
     * information content is the {@linkplain #INFORMATION_CONTENT same}.
     * </p>
     */
    @Override
    public final double getInformationContent() {
        return INFORMATION_CONTENT;
    }

}
