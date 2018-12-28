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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * <p>
 * Unit tests and auxiliary test methods for class that implement the
 * {@link TransactionListener} interface.
 * </p>
 */
public class TransactionListenerTest {

    static class CountingTransactionListener implements TransactionListener {

        private int aborts = 0;
        private int commits = 0;
        private final Set<UUID> created = new HashSet<>();

        final synchronized int getAborts() {
            return aborts;
        }

        final synchronized int getCommits() {
            return commits;
        }

        final synchronized Set<UUID> getCreated() {
            return new HashSet<>(created);
        }

        final synchronized int getEnds() {
            return aborts + commits;
        }

        @Override
        public synchronized void onAbort() {
            assertEquals(0, aborts, "Aborts at most once");
            ++aborts;
        }

        @Override
        public synchronized void onCommit() {
            assertEquals(0, commits, "Commits at most once");
            ++commits;
        }

        @Override
        public synchronized void onCreate(@NonNull UUID object) {
            assertNotNull(object, "object");
            created.add(object);
        }

    }// class

    public static void assertInvariants(TransactionListener listener) {
        // Do nothing
    }

    public static void assertInvariants(TransactionListener listener1, TransactionListener listener2) {
        // Do nothing
    }

}
