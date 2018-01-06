package uk.badamson.mc.actor.message;

/**
 * <p>
 * A {@linkplain Noun noun} with a referent implied by the context and relative
 * to the sender or receiver of a {@linkplain Message message}.
 * </p>
 */
public enum Pronoun implements Noun {

    /**
     * <p>
     * The sender of a message.
     * </p>
     */
    ME,

    /**
     * <p>
     * The receiver(s) of a message.
     * </p>
     * <p>
     * In {@linkplain Command commands}, the {@linkplain Command#getSubject()
     * subject} is often implicitly the receiver of the message: "come here!", for
     * example,
     * </p>
     */
    YOU,

    /**
     * <p>
     * Something other than the sender or receiver of the message.
     * </p>
     * <p>
     * Depending on the context, this corresponds to English it, this, that, him,
     * her or your.
     * </p>
     */
    IT;

    public static final double INFORMATION_CONTENT = Math.log(values().length) / Math.log(2);

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
