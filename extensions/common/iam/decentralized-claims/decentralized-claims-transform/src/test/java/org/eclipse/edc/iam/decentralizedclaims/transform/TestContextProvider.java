/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.transform;

import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_NAMESPACE_V_1_0;
import static org.eclipse.edc.iam.decentralizedclaims.spi.DcpConstants.DSPACE_DCP_V_1_0_CONTEXT;
import static org.mockito.Mockito.mock;

public class TestContextProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
                Arguments.of(new TestContext(DSPACE_DCP_V_1_0_CONTEXT, DSPACE_DCP_NAMESPACE_V_1_0, createJsonLd(DSPACE_DCP_V_1_0_CONTEXT))));
    }

    private JsonLd createJsonLd(String activeContext) {
        var jsonLd = new TitaniumJsonLd(mock());
        jsonLd.registerCachedDocument("https://identity.foundation/presentation-exchange/submission/v1", TestUtils.getFileFromResourceName("presentation_ex.json").toURI());
        jsonLd.registerCachedDocument(DSPACE_DCP_V_1_0_CONTEXT, TestUtils.getFileFromResourceName("document/dcp.v1.0.jsonld").toURI());
        jsonLd.registerContext(activeContext);
        return jsonLd;
    }

    public record TestContext(String context, JsonLdNamespace namespace, JsonLd jsonLd) {

        public String toIri(String term) {
            return namespace.toIri(term);
        }
    }
}
