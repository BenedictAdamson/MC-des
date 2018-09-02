package uk.badamson.mc.mind.message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A simple, direct, elementary informative {@linkplain Message message}, as
 * might be used by low-level soldiers.
 * </p>
 * <p>
 * This enumeration of {@link Message} objects should include all messages that
 * hand signals can communicate. It should include all the messages in the US
 * Army Training Circular TC&nbsp;3-21.60, <cite>Visual Signals</cite>, dated 17
 * March 2017; that document replaced US Army Field Manual FM&nbsp;21-60 dated
 * September 1987.
 * </p>
 */
@Immutable
public class SimpleStatement extends AbstractMessage {

    /**
     * <p>
     * An attribute that the {@linkplain SimpleStatement#getSubject() subject} that
     * a {@linkplain SimpleStatement simple statement} can have.
     * </p>
     */
    public static enum SimplePredicate implements MessageElement {
    /**
     * <p>
     * The subject (a person) acknowledges a message that was sent to them.
     * </p>
     */
    ACKNOWLEDGE_MESSAGE,
    /**
     * <p>
     * The subject (a location) is the location where an enemy is visible.
     * </p>
     */
    HAS_ENEMY_IN_SIGHT,

    /**
     * <p>
     * The subject (a location) is a <dfn>danger area</dfn>, which is (or might be)
     * exposed to enemy observation or fire.
     * </p>
     */
    IS_DANGER_AREA;

        public static final double INFORMATION_CONTENT = Math.log(values().length) / Math.log(2);

        /**
         * {@inheritDoc}
         * 
         * <p>
         * Objects of the {@link SimpleStatement.SimplePredicate} type have the
         * additional constraint that their information content is the
         * {@linkplain #INFORMATION_CONTENT same}.
         * </p>
         */
        @Override
        public final double getInformationContent() {
            return INFORMATION_CONTENT;
        }
    }// enum

    private static final Map<SimpleRelativeLocation, SimpleStatement> ENEMY_IN_SIGHT_INSTANCES;

    static {
        final Map<SimpleRelativeLocation, SimpleStatement> map = new EnumMap<>(SimpleRelativeLocation.class);
        for (SimpleRelativeLocation location : SimpleRelativeLocation.values()) {
            final SimpleStatement statement = new SimpleStatement(location, SimplePredicate.HAS_ENEMY_IN_SIGHT);
            map.put(location, statement);
        }
        ENEMY_IN_SIGHT_INSTANCES = map;
    }

    /**
     * <p>
     * The extra information content of a {@link SimpleStatement}, over the
     * information content of its constituent {@linkplain MessageElement elements}.
     * </p>
     */
    public static final double EXTRA_INFORMATION_CONTENT = 5.0;

    /**
     * <p>
     * A statement to indicate that the area is a <dfn>danger area</dfn>, which is
     * (or might be) exposed to enemy observation or fire.
     * </p>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#IT
     * it}.</li>
     * <li>The {@linkplain #getPredicate() predicate} is that the location
     * {@linkplain SimplePredicate#IS_DANGER_AREA is a danger area}.</li>
     * </ul>
     */
    public static final SimpleStatement DANGER_AREA = new SimpleStatement(Pronoun.IT, SimplePredicate.IS_DANGER_AREA);

    /**
     * <p>
     * A statement to indicate that the sender acknowledges a message sent to them.
     * </p>
     * <li>The {@linkplain #getSubject() subject} is {@linkplain Pronoun#ME
     * me}.</li>
     * <li>The {@linkplain #getPredicate() predicate} is that the sender
     * {@linkplain SimplePredicate#ACKNOWLEDGE_MESSAGE acknowledges the
     * message}.</li>
     * </ul>
     */
    public static final SimpleStatement ACKNOWLEDGE_MESSAGE = new SimpleStatement(Pronoun.ME,
            SimplePredicate.ACKNOWLEDGE_MESSAGE);

    private static final SimpleStatement[] ALL;
    static {
        final List<SimpleStatement> list = new ArrayList<>();
        list.addAll(ENEMY_IN_SIGHT_INSTANCES.values());
        list.add(ACKNOWLEDGE_MESSAGE);
        list.add(DANGER_AREA);
        ALL = list.toArray(new SimpleStatement[0]);
    }

    /**
     * <p>
     * A statement to indicate that an enemy (or enemies) have been seen in a
     * {@linkplain SimpleRelativeLocation location}.
     * </p>
     * <ul>
     * <li>Always returns a (non null) instance.</li>
     * <li>The {@linkplain #getSubject() subject} is the given location.</li>
     * <li>The {@linkplain #getPredicate() predicate} is that the location
     * {@linkplain SimplePredicate#HAS_ENEMY_IN_SIGHT has enemy in sight}.</li>
     * </ul>
     * 
     * @return the statement
     * @throws NullPointerException
     *             If {@code location} is null.
     */
    public static final SimpleStatement getEnemyInSight(SimpleRelativeLocation location) {
        Objects.requireNonNull(location, "location");
        return ENEMY_IN_SIGHT_INSTANCES.get(location);
    }

    /**
     * <p>
     * An array holding each of the permitted values of the {@link SimpleStatement}
     * type.
     * </p>
     * <ul>
     * <li>Always returns a (non null) array of values.</li>
     * <li>The array of values has no null elements.</li>
     * <li>The array of values has no duplicate elements.</li>
     * </ul>
     * 
     * @return the values
     */
    public static final SimpleStatement[] values() {
        return Arrays.copyOf(ALL, ALL.length);
    }

    private final Noun subject;

    private final SimplePredicate predicate;

    private SimpleStatement(Noun subject, SimplePredicate predicate) {
        this.subject = subject;
        this.predicate = predicate;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * The information content exceeds the total for the message elements by the
     * {@linkplain #EXTRA_INFORMATION_CONTENT same extra amount}.
     * </p>
     * <ul>
     * <li>The message elements are the {@linkplain #getSubject() subject} and
     * {@linkplain #getPredicate() predicate}.
     * </ul>
     */
    @Override
    public final double getInformationContent() {
        return EXTRA_INFORMATION_CONTENT + subject.getInformationContent() + predicate.getInformationContent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final UnusableIncompleteMessage getPartialMessage(double partLength) {
        return new UnusableIncompleteMessage(partLength);
    }

    /**
     * <p>
     * The attribute that this statement declares the {@linkplain #getSubject()
     * subject} to have.
     * </p>
     * <ul>
     * <li>Always have a (non null) predicate.</li>
     * </ul>
     * 
     * @return the subject; not null.
     */
    public final SimplePredicate getPredicate() {
        return predicate;
    }

    /**
     * <p>
     * The thing that the statement describes.
     * </p>
     * 
     * @return the subject; not null.
     */
    public final Noun getSubject() {
        return subject;
    }

    @Override
    public final String toString() {
        return subject + " " + predicate;
    }

}
