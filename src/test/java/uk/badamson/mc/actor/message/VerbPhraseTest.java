package uk.badamson.mc.actor.message;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

/**
 * <p>
 * Unit tests of classes that implement the {@link VerbPhrase} interface.
 * </p>
 */
public class VerbPhraseTest {

    public static void assertInvariants(VerbPhrase phrase) {
	MessageElementTest.assertInvariants(phrase);// inherited

	final Verb verb = phrase.getVerb();
	final Set<Noun> objects = phrase.getObjects();

	assertNotNull("Not null, verb", verb);// guard
	assertNotNull("Not null, objects", objects);// guard

	VerbTest.assertInvariants(verb);
	for (final Noun object : objects) {
	    assertNotNull("Not null, object", object);// guard
	    NounTest.assertInvariants(object);
	}

	final double totalInformationContentOfElements = verb.getInformationContent()
		+ totalInformationContent(objects);
	final double informationContent = phrase.getInformationContent();
	assertTrue(
		"The information content of a VerbPhrase <" + informationContent
			+ "> exceeds the total information content of the verb and objects elements of the phrase <"
			+ totalInformationContentOfElements + ">.",
		totalInformationContentOfElements < informationContent);
    }

    static double totalInformationContent(Set<Noun> nouns) {
	double total = 0.0;
	for (final Noun noun : nouns) {
	    total += noun.getInformationContent();
	}
	return total;
    }
}
