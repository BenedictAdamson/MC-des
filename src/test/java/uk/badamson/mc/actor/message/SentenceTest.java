package uk.badamson.mc.actor.message;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

/**
 * <p>
 * Unit tests of classes that implement the {@link Sentence} interface.
 * </p>
 */
public class SentenceTest {

    public static void assertInvariants(Sentence sentence) {
	MessageTest.assertInvariants(sentence);// inherited

	final Noun subject = sentence.getSubject();
	final Verb verb = sentence.getVerb();
	final Set<Noun> objects = sentence.getObjects();

	assertNotNull("Not null, subject", subject);// guard
	assertNotNull("Not null, verb", verb);// guard
	assertNotNull("Not null, objects", objects);// guard

	NounTest.assertInvariants(subject);
	VerbTest.assertInvariants(verb);
	for (final Noun object : objects) {
	    assertNotNull("Not null, object", object);// guard
	    NounTest.assertInvariants(object);
	}

	final double totalInformationContentOfElements = subject.getInformationContent() + verb.getInformationContent()
		+ totalInformationContent(objects);
	final double informationContent = sentence.getInformationContent();
	assertTrue("The information content of a sentence <" + informationContent
		+ "> exceeds the total information content of the subject, verb and objects elements of the message <"
		+ totalInformationContentOfElements + ">.", totalInformationContentOfElements < informationContent);
    }

    private static double totalInformationContent(Set<Noun> nouns) {
	double total = 0.0;
	for (final Noun noun : nouns) {
	    total += noun.getInformationContent();
	}
	return total;
    }
}
