package uk.badamson.mc;

import static org.junit.jupiter.api.Assertions.assertEquals;

/* 
 * © Copyright Benedict Adamson 2018.
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

/**
 * <p>
 * Auxiliary test code for classes that implement the {@link Comparable}
 * interface.
 */
public class ComparableTest {

    public static <T extends Comparable<T>> void assertComparableConsistentWithEquals(T object1, T object2) {
        assertEquals(object1.compareTo(object2) == 0, object1.equals(object2),
                "Natural ordering is consistent with equals");
    }

    public static <T extends Comparable<T>> void assertInvariants(T object) {
        assertInvariants(object, object);
    }

    public static <T extends Comparable<T>> void assertInvariants(T object1, T object2) {
        object1.compareTo(object2);// require only that it does not throw an exception
    }

}
