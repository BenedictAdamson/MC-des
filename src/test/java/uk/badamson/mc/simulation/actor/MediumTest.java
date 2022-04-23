package uk.badamson.mc.simulation.actor;
/*
 * © Copyright Benedict Adamson 2018-22.
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

import uk.badamson.dbc.assertions.ObjectVerifier;

import javax.annotation.Nonnull;

public class MediumTest {

    public static void assertInvariants(@Nonnull final Medium medium) {
        ObjectVerifier.assertInvariants(medium);
    }

    public static void assertInvariants(@Nonnull final Medium medium1, @Nonnull final Medium medium2) {
        ObjectVerifier.assertInvariants(medium1, medium2);
    }
}
