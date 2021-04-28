package uk.badamson.mc.simulation;

import net.jcip.annotations.ThreadSafe;

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

/**
 * <p>
 * A {@linkplain FunctionalInterface functional interface} providing a means for
 * reporting or recording an exception that was unexpected and thus not caught
 * and handled through normal means.
 * </p>
 */
@FunctionalInterface
@ThreadSafe
public interface UncaughtExceptionHandler {

    /**
     * <p>
     * Report or record an unexpected and thus uncaught exception.
     * </p>
     * <p>
     * This method must refrain from throwing or propagating exceptions. Such
     * exceptions are likely to be silently caught and ignored.
     * </p>
     *
     * @param e
     *            The exception; not null.
     */
    public void uncaughtException(Throwable e);
}
