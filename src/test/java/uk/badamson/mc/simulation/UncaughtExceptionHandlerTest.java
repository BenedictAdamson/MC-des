package uk.badamson.mc.simulation;

import java.util.ArrayList;
import java.util.List;

import org.opentest4j.MultipleFailuresError;

import net.jcip.annotations.GuardedBy;

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
 * Auxiliary test code for classes that implement the
 * {@link UncaughtExceptionHandler} interface.
 * </p>
 */
public class UncaughtExceptionHandlerTest {

    public static class RecordingUncaughtExceptionHandler implements UncaughtExceptionHandler {

        @GuardedBy("exceptions")
        private final List<Throwable> exceptions = new ArrayList<>();

        public final void rethrowExceptions() {
            synchronized (exceptions) {
                final int n = exceptions.size();
                if (n == 1) {
                    final AssertionError e = new AssertionError(exceptions.get(0));
                    exceptions.clear();
                    throw e;
                } else if (1 < n) {
                    final MultipleFailuresError e = new MultipleFailuresError("Uncaught exceptions", exceptions);
                    exceptions.clear();
                    throw e;
                }
            }
        }

        @Override
        public void uncaughtException​(final Throwable e) {
            synchronized (exceptions) {
                exceptions.add(e);
            }
        }
    }

    public static void assertInvariants(final UncaughtExceptionHandler handler) {
        // Do nothing
    }

    public static void assertInvariants(final UncaughtExceptionHandler handler1,
            final UncaughtExceptionHandler handler2) {
        // Do nothing
    }

    public static void uncaughtException​(final UncaughtExceptionHandler handler, final Throwable e) {
        try {
            handler.uncaughtException​(e);
        } catch (final AssertionError e2) {
            throw e2;
        } catch (final Throwable e2) {
            throw new AssertionError(e);
        }
    }
}
