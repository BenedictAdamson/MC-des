package uk.badamson.mc.actor.message;

import java.util.Collections;
import java.util.List;

import net.jcip.annotations.Immutable;

/**
 * <p>
 * A statement that two or more {@linkplain Noun things} have a
 * {@linkplain Relationship relationship or connection}.
 * </p>
 */
@Immutable
public interface RelationshipStatement extends Message {

    /**
     * <p>
     * The relationship or connection that exists between the
     * {@linkplain #getThings() things}.
     * </p>
     * 
     * @return the relationship; not null
     */
    public Relationship getRelationship();

    /**
     * <p>
     * The {@linkplain Noun things} that have the relationship or connection.
     * </p>
     * <ul>
     * <li>Always have a (non null) list of things.</li>
     * <li>The list of things is {@linkplain Collections#unmodifiableList(List)
     * unmodifiable}.</li>
     * <li>The list of things has no null elements.</li>
     * <li>The list of things has at least two elements.</li>
     * </ul>
     * 
     * @return the things
     */
    public List<Noun> getThings();
}
