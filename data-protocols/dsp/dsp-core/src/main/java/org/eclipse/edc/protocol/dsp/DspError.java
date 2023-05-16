/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;

import java.util.List;
import java.util.Objects;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

/**
 * A dsp process error.
 */
public class DspError {

    private String type;
    private String processId;
    private String code;
    private List<String> messages;

    public JsonObject toJson() {
        var arrayBuilder = Json.createArrayBuilder();
        for (var m : messages) {
            arrayBuilder.add(m);
        }

        var objectBuilder = Json.createObjectBuilder()
                .add(JsonLdKeywords.CONTEXT, Json.createObjectBuilder().add(VOCAB, EDC_NAMESPACE))
                .add(JsonLdKeywords.TYPE, type)
                .add(DSPACE_PROPERTY_CODE, code)
                .add(DSPACE_PROPERTY_REASON, arrayBuilder.build());

        if (this.processId != null) {
            objectBuilder.add(DSPACE_PROCESS_ID, this.processId);
        }

        return objectBuilder.build();
    }

    public static class Builder {
        private final DspError error;

        public static DspError.Builder newInstance() {
            return new DspError.Builder();
        }

        public DspError.Builder type(String type) {
            this.error.type = type;
            return this;
        }

        public DspError.Builder processId(String processId) {
            this.error.processId = processId;
            return this;
        }

        public DspError.Builder code(String code) {
            this.error.code = code;
            return this;
        }

        public DspError.Builder messages(List<String> messages) {
            this.error.messages = messages;
            return this;
        }

        public DspError build() {
            Objects.requireNonNull(this.error.type, "type");

            return error;
        }

        private Builder() {
            error = new DspError();
        }
    }
}
