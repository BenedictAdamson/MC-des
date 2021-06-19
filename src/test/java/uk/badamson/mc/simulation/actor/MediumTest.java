package uk.badamson.mc.simulation.actor;
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

import uk.badamson.dbc.assertions.CollectionTest;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;

public class MediumTest {

    static <STATE> void addAll(@Nonnull final Medium<STATE> medium, @Nonnull final Collection<Signal<STATE>> signals) {
        final Set<Signal<STATE>> mediumSignals0 = medium.getSignals();

        medium.addAll(signals);

        assertInvariants(medium);
        final Set<Signal<STATE>> mediumSignals = medium.getSignals();
        assertAll("signals contains all",
                () -> assertThat("original signals", mediumSignals.containsAll(mediumSignals0)),
                () -> assertThat("added signals", mediumSignals.containsAll(signals)));
    }

    static <STATE> void assertInvariants(@Nonnull final Medium<STATE> medium) {
        final Set<Signal<STATE>> signals = medium.getSignals();
        assertThat("signals", signals, notNullValue());// guard
        CollectionTest.assertForAllElements("signals", signals, signal -> {
            assertThat("signal", signal, notNullValue());// guard
            SignalTest.assertInvariants(signal);
        });
    }

    static <STATE> void removeAll(@Nonnull final Medium<STATE> medium,
                                  @Nonnull final Collection<Signal<STATE>> signals) {
        medium.removeAll(signals);

        assertInvariants(medium);
        final Set<Signal<STATE>> mediumSignals = medium.getSignals();
        assertThat("has none of the removed signals", Collections.disjoint(mediumSignals, signals));
    }

    static final class RecordingMedium<STATE> implements Medium<STATE> {
        private final Set<Signal<STATE>> signals = ConcurrentHashMap.newKeySet();

        @Override
        public void addAll(@Nonnull final Collection<Signal<STATE>> signals) {
            this.signals.addAll(signals);
        }

        @Nonnull
        @Override
        public Set<Signal<STATE>> getSignals() {
            return Set.copyOf(signals);
        }

        @Override
        public void removeAll(@Nonnull final Collection<Signal<STATE>> signals) {
            this.signals.removeAll(signals);
        }

    }// class
}
