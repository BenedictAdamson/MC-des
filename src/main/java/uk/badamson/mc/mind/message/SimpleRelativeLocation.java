package uk.badamson.mc.mind.message;

import java.util.Objects;

/**
 * <p>
 * A location that can be named as a {@linkplain Sentence#getObjects() object}
 * in a {@linkplain Sentence sentence}, which describes a location relative to
 * the another object, tersely but without precision.
 * </p>
 * <p>
 * The location is typically described relative to the sender of the message.
 * Objects of this type are intended to be used for the kinds of location that
 * can be indicated by pointing.
 * </p>
 */
public enum SimpleRelativeLocation implements Noun {

    FRONT_NEAR(Direction.FRONT, Range.NEAR),
    FRONT_MEDIUM(Direction.FRONT, Range.MEDIUM),
    FRONT_FAR(Direction.FRONT, Range.FAR),
    FRONT_RIGHT_NEAR(Direction.FRONT_RIGHT, Range.NEAR),
    FRONT_RIGHT_MEDIUM(Direction.FRONT_RIGHT, Range.MEDIUM),
    FRONT_RIGHT_FAR(Direction.FRONT_RIGHT, Range.FAR),
    RIGHT_NEAR(Direction.RIGHT, Range.NEAR),
    RIGHT_MEDIUM(Direction.RIGHT, Range.MEDIUM),
    RIGHT_FAR(Direction.RIGHT, Range.FAR),
    RIGHT_BACK_NEAR(Direction.RIGHT_BACK, Range.NEAR),
    RIGHT_BACK_MEDIUM(Direction.RIGHT_BACK, Range.MEDIUM),
    RIGHT_BACK_FAR(Direction.RIGHT_BACK, Range.FAR),
    BACK_NEAR(Direction.BACK, Range.NEAR),
    BACK_MEDIUM(Direction.BACK, Range.MEDIUM),
    BACK_FAR(Direction.BACK, Range.FAR),
    BACK_LEFT_NEAR(Direction.BACK_LEFT, Range.NEAR),
    BACK_LEFT_MEDIUM(Direction.BACK_LEFT, Range.MEDIUM),
    BACK_LEFT_FAR(Direction.BACK_LEFT, Range.FAR),
    LEFT_NEAR(Direction.LEFT, Range.NEAR),
    LEFT_MEDIUM(Direction.LEFT, Range.MEDIUM),
    LEFT_FAR(Direction.LEFT, Range.FAR),
    LEFT_FRONT_NEAR(Direction.LEFT_FRONT, Range.NEAR),
    LEFT_FRONT_MEDIUM(Direction.LEFT_FRONT, Range.MEDIUM),
    LEFT_FRONT_FAR(Direction.LEFT_FRONT, Range.FAR);

    /**
     * <p>
     * The direction of a {@link SimpleRelativeLocation}.
     * </p>
     */
    public enum Direction {
        FRONT(0, 1),
        FRONT_RIGHT(Math.sqrt(0.5), Math.sqrt(0.5)),
        RIGHT(1, 0),
        RIGHT_BACK(Math.sqrt(0.5), -Math.sqrt(0.5)),
        BACK(0, -1),
        BACK_LEFT(-Math.sqrt(0.5), -Math.sqrt(0.5)),
        LEFT(-1, 0),
        LEFT_FRONT(-Math.sqrt(0.5), Math.sqrt(0.5));

        private final double x;
        private final double y;

        private Direction(double x, double y) {
            this.x = x;
            this.y = y;
        }

        /**
         * <p>
         * The x component of a (unit) direction vector pointing in this direction, if
         * the {@linkplain #FRONT front} direction points in the positive y direction.
         * </p>
         * 
         * @return the x
         */
        public final double getX() {
            return x;
        }

        /**
         * <p>
         * The y component of a (unit) direction vector pointing in this direction, if
         * the {@linkplain #FRONT front} direction points in the positive y direction.
         * </p>
         * 
         * @return the y
         */
        public final double getY() {
            return y;
        }

    }// enum

    /**
     * <p>
     * How far away a {@link SimpleRelativeLocation} is. </p
     */
    public enum Range {
        NEAR, MEDIUM, FAR;
    }// enum

    private static final int N_RANGES = Range.values().length;
    /**
     * <p>
     * The {@linkplain SimpleRelativeLocation#getInformationContent() information
     * content} of a {@link SimpleRelativeLocation}.
     * </p>
     */
    public static final double INFORMATION_CONTENT = MessageElement.getInformationContent(values().length);

    /**
     * <p>
     * The {@link SimpleRelativeLocation} that has a given direction and range.
     * </p>
     * 
     * @param direction
     *            The {@linkplain #getDirection() direction} of the location.
     * @param range
     *            The {@linkplain #getRange() range} of the location.
     * @return the location; not null
     * @throws NullPointerException
     *             <ul>
     *             <li>If {@code direction} is null.</li>
     *             <li>If {@code range} is null.</li>
     *             </ul>
     */
    public static SimpleRelativeLocation getInstance(Direction direction, Range range) {
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(range, "range");

        return values()[range.ordinal() + direction.ordinal() * N_RANGES];
    }

    private final Direction direction;

    private final Range range;

    private SimpleRelativeLocation(Direction direction, Range range) {
        this.direction = direction;
        this.range = range;
    }

    /**
     * <p>
     * The direction of this location.
     * </p>
     * 
     * @return the direction; not null.
     */
    public final Direction getDirection() {
        return direction;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Objects of the {@link SimpleRelativeLocation} type have the additional
     * constraint that their information content is the
     * {@linkplain #INFORMATION_CONTENT same}.
     * </p>
     */
    @Override
    public final double getInformationContent() {
        return INFORMATION_CONTENT;
    }

    /**
     * <p>
     * How far away this location is.
     * </p>
     * 
     * @return the range; not null.
     */
    public final Range getRange() {
        return range;
    }
}
