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

package org.eclipse.edc.connector.controlplane.transform.edc.dataspaceprofile.to;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transform.edc.dataspaceprofile.from.JsonObjectToAssociateDataspaceProfileContextTransformer;
import org.eclipse.edc.protocol.spi.AssociateDataspaceProfileContext;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.test.TestJsonLd.expand;
import static org.eclipse.edc.protocol.spi.AssociateDataspaceProfileContext.ASSOCIATE_DATASPACE_PROFILE_CONTEXT_PROFILES_TERM;
import static org.eclipse.edc.protocol.spi.AssociateDataspaceProfileContext.ASSOCIATE_DATASPACE_PROFILE_CONTEXT_TYPE_TERM;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class JsonObjectToAssociateDataspaceProfileContextTransformerTest {

    private final TransformerContext context = mock();
    private final JsonObjectToAssociateDataspaceProfileContextTransformer transformer = new JsonObjectToAssociateDataspaceProfileContextTransformer();

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(JsonObject.class);
        assertThat(transformer.getOutputType()).isEqualTo(AssociateDataspaceProfileContext.class);
    }

    @Test
    void transform() {
        var json = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE))
                .add(TYPE, ASSOCIATE_DATASPACE_PROFILE_CONTEXT_TYPE_TERM)
                .add(ASSOCIATE_DATASPACE_PROFILE_CONTEXT_PROFILES_TERM, Json.createArrayBuilder(List.of("profile1", "profile2")))
                .build();

        var result = transformer.transform(expand(json), context);

        assertThat(result).isNotNull();
        assertThat(result.profiles()).containsExactly("profile1", "profile2");
        verifyNoInteractions(context);
    }

    @Test
    void transform_withSingleProfile() {
        var json = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE))
                .add(TYPE, ASSOCIATE_DATASPACE_PROFILE_CONTEXT_TYPE_TERM)
                .add(ASSOCIATE_DATASPACE_PROFILE_CONTEXT_PROFILES_TERM, "only-profile")
                .build();

        var result = transformer.transform(expand(json), context);

        assertThat(result).isNotNull();
        assertThat(result.profiles()).containsExactly("only-profile");
        verifyNoInteractions(context);
    }

    @Test
    void transform_withMissingProfiles_shouldReturnEmptyList() {
        var json = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE))
                .add(TYPE, ASSOCIATE_DATASPACE_PROFILE_CONTEXT_TYPE_TERM)
                .build();

        var result = transformer.transform(expand(json), context);

        assertThat(result).isNotNull();
        assertThat(result.profiles()).isEmpty();
        verifyNoInteractions(context);
    }

}
