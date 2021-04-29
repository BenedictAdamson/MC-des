package uk.badamson.mc;
/*
 * © Copyright Benedict Adamson 2018,2021.
 *
 * This file is part of MC-des.
 *
 * MC-des is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MC-des is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MC-des.  If not, see <https://www.gnu.org/licenses/>.
 */

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.annotation.Nonnull;

/**
 * <p>
 * Auxiliary test code for classes that implement the {@link Comparable}
 * interface.
 */
public class ComparableTest {

    public static <T extends Comparable<T>> void assertComparableConsistentWithEquals(@Nonnull final T object1,
            @Nonnull final T object2) {
        assertTrue(object1.compareTo(object2) == 0 == object1.equals(object2),
                "Natural ordering is consistent with equals");
    }

    public static <T extends Comparable<T>> void assertInvariants(@Nonnull final T object) {
        assertInvariants(object, object);

        assertThrows(NullPointerException.class, () -> object.compareTo(null),
                "compareTo(null) throws NullPointerException");
    }

    public static <T extends Comparable<T>> void assertInvariants(@Nonnull final T object1, @Nonnull final T object2) {
        try {
            object1.compareTo(object2);// require only that it does not throw an exception
        } catch (final Exception e) {
            throw new AssertionError("Does not throw exceptions");
        }
    }

}
