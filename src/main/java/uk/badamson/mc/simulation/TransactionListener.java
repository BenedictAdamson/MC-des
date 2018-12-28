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

import java.util.UUID;

import edu.umd.cs.findbugs.annotations.NonNull;
import net.jcip.annotations.ThreadSafe;

/**
 * <p>
 * An object that can respond to {@link Universe.Transaction} events.
 * </p>
 */
@ThreadSafe
public interface TransactionListener {
    /**
     * <p>
     * An action to perform when (if) a transaction aborts its commit operation.
     * commit operation.
     * </p>
     */
    public void onAbort();

    /**
     * <p>
     * An action to perform when (if) a transaction successfully completes its
     * </p>
     */
    public void onCommit();

    /**
     * <p>
     * An action to perform when (if) a transaction creates a new object.
     * </p>
     * <p>
     * The method can be called before the transaction commits.
     * </p>
     * 
     * @param object
     *            The object created by the transaction.
     * @throws NullPointerException
     *             (Optionally) if {@code object} is null.
     * @throws IllegalStateException
     *             (Optionally) if this method of listener has previously been
     *             called for the same transaction and object.
     */
    public void onCreate(@NonNull UUID object);

}// interface