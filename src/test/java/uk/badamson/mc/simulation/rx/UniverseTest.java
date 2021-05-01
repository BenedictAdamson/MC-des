package uk.badamson.mc.simulation.rx;
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

import javax.annotation.Nonnull;

import uk.badamson.mc.ObjectTest;

public class UniverseTest {

    public static <STATE> void assertInvariants(@Nonnull final Universe<STATE> universe) {
        ObjectTest.assertInvariants(universe);// inherited
    }

    public static <STATE> void assertInvariants(@Nonnull final Universe<STATE> universe1,
            @Nonnull final Universe<STATE> universe2) {
        ObjectTest.assertInvariants(universe1, universe2);// inherited
    }
}
