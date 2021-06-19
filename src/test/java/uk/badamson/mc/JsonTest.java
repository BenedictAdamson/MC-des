package uk.badamson.mc;
/*
 * © Copyright Benedict Adamson 2020,21.
 *
 * This file is part of MC.
 *
 * MC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with MC.  If not, see <https://www.gnu.org/licenses/>.
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * <p>
 * Auxiliary code for test JSON serialization and deserialization.
 * </p>
 */
public class JsonTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    public static String serialize(final Object object)
            throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(object);
    }

    @SuppressWarnings("unchecked")
    public static <TYPE> TYPE serializeAndDeserialize(final TYPE object) {
        Objects.requireNonNull(object, "object");
        try {
            final var serialized = serialize(object);
            return (TYPE) OBJECT_MAPPER.readValue(serialized, object.getClass());
        } catch (final JsonProcessingException e) {
            throw new UncheckedIOException("can not deserialize from JSON", e);
        }
    }
}
