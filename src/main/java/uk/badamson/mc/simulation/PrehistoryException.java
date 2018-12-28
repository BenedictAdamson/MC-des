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

/**
 * <p>
 * An exception for indicating that the {@linkplain ObjectState state} of an
 * object can not be determined for a given point in time because that point in
 * time is before the {@linkplain Universe#getHistoryStart() start of history}.
 * </p>
 */
public final class PrehistoryException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    private PrehistoryException() {
        super("Point in time is before the start of history");
        // Do nothing
    }

}
