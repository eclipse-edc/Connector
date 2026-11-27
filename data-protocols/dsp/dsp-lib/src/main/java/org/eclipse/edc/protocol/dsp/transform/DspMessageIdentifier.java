/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transform;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.regex.Pattern;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VALUE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;

/**
 * Reads the identifier of an incoming DSP message, which a receiver uses to recognize a message that the counter-party
 * sends again (e.g. because the acknowledgement got lost).
 * <p>
 * The message {@code @id} is only used as identifier when the sender gives it that meaning: when it is a
 * {@code urn:uuid:} value that differs from the message's {@code consumerPid} and {@code providerPid}. Any other
 * {@code @id} is ignored, so that a counter-party that sets the {@code @id} differently can't have a message mistaken
 * for one that was already processed.
 */
public final class DspMessageIdentifier {

    public static final String URN_UUID_PREFIX = "urn:uuid:";

    private static final Pattern URN_UUID = Pattern.compile(
            "^" + URN_UUID_PREFIX + "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private DspMessageIdentifier() {
    }

    /**
     * Returns the identifier of the given expanded DSP message, or null if its {@code @id} doesn't qualify as one.
     *
     * @param message      the expanded message.
     * @param forNamespace maps a DSP term to its IRI in the namespace of the message.
     * @return the message identifier, or null.
     */
    public static @Nullable String of(JsonObject message, Function<String, String> forNamespace) {
        var id = message.get(ID) instanceof JsonString string ? string.getString() : null;
        if (id == null || !URN_UUID.matcher(id).matches()) {
            return null;
        }

        var uuid = id.substring(URN_UUID_PREFIX.length());
        for (var pidTerm : new String[]{ DSPACE_PROPERTY_CONSUMER_PID_TERM, DSPACE_PROPERTY_PROVIDER_PID_TERM }) {
            var pid = stringValue(message.get(forNamespace.apply(pidTerm)));
            if (id.equals(pid) || uuid.equals(pid)) {
                return null;
            }
        }
        return id;
    }

    private static @Nullable String stringValue(@Nullable JsonValue value) {
        if (value instanceof JsonArray array) {
            return array.isEmpty() ? null : stringValue(array.get(0));
        }
        if (value instanceof JsonObject object) {
            return stringValue(object.get(VALUE));
        }
        if (value instanceof JsonString string) {
            return string.getString();
        }
        return null;
    }
}
