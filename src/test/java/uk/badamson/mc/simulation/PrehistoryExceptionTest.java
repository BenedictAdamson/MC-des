package uk.badamson.mc.simulation;
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

import uk.badamson.mc.ObjectTest;

/**
 * <p>
 * Unit tests for the {@link PrehistoryException} class.
 * </p>
 */
public class PrehistoryExceptionTest {

    public static void assertInvariants(PrehistoryException exception) {
        ObjectTest.assertInvariants(exception);// inherited
    }

    public static void assertInvariants(PrehistoryException exception1, PrehistoryException exception2) {
        ObjectTest.assertInvariants(exception1, exception2);// inherited
    }

}
