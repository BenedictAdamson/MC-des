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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ThreadTest {

    public static void get(final Future<Void> future) {
        try {
            future.get();
        } catch (final InterruptedException e) {
            throw new AssertionError(e);
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof AssertionError) {
                throw (AssertionError) cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else {
                throw new AssertionError(e);
            }
        }
    }

    public static void get(final List<Future<Void>> futures) {
        final List<Throwable> exceptions = new ArrayList<>(futures.size());
        for (final var future : futures) {
            try {
                get(future);
            } catch (Exception | AssertionError e) {
                exceptions.add(e);
            }
        }
        final int nExceptions = exceptions.size();
        if (0 < nExceptions) {
            final Throwable e = exceptions.get(0);
            for (int i = 1; i < nExceptions; ++i) {
                e.addSuppressed(exceptions.get(i));
            }
            if (e instanceof AssertionError) {
                throw (AssertionError) e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new AssertionError(e);
            }
        }
    }

    public static Future<Void> runInOtherThread(final CountDownLatch ready, final Runnable operation) {
        final CompletableFuture<Void> future = new CompletableFuture<Void>();
        final Thread thread = new Thread(() -> {
            try {
                ready.await();
                operation.run();
            } catch (final Throwable e) {
                future.completeExceptionally(e);
                return;
            }
            future.complete(null);
        });
        thread.start();
        return future;
    }
}
