/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.http.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.protocol.dsp.http.spi.serialization.JsonLdRemoteMessageSerializer;
import org.eclipse.edc.protocol.dsp.spi.transform.DspProtocolTypeTransformerRegistry;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_CONTEXT_SEPARATOR;

/**
 * Serializes {@link RemoteMessage}s to JSON-LD.
 */
public class JsonLdRemoteMessageSerializerImpl implements JsonLdRemoteMessageSerializer {

    private final TypeManager typeManager;
    private final String typeContext;
    private final JsonLd jsonLdService;
    private final String scopePrefix;
    private final DspProtocolTypeTransformerRegistry dspTransformerRegistry;
    private final DataspaceProfileContextRegistry dataspaceProfileContextRegistry;

    public JsonLdRemoteMessageSerializerImpl(DspProtocolTypeTransformerRegistry dspTransformerRegistry,
                                             TypeManager typeManager, String typeContext, JsonLd jsonLdService, DataspaceProfileContextRegistry dataspaceProfileContextRegistry, String scopePrefix) {
        this.dspTransformerRegistry = dspTransformerRegistry;
        this.typeManager = typeManager;
        this.typeContext = typeContext;
        this.jsonLdService = jsonLdService;
        this.scopePrefix = scopePrefix;
        this.dataspaceProfileContextRegistry = dataspaceProfileContextRegistry;
    }

    /**
     * Serializes a {@link RemoteMessage} to JSON-LD. The message is first transformed using the
     * {@link TypeTransformerRegistry}, then the resulting JSON-LD structure is compacted using
     * the given JSON-LD context before returning it as a string.
     *
     * @param message the message to serialize
     * @return the serialized message
     */
    @Override
    public String serialize(RemoteMessage message) {
        try {
            var transformerRegistryResult = dspTransformerRegistry.forProtocol(message.getProtocol());
            if (transformerRegistryResult.failed()) {
                throw new EdcException(format("Failed to serialize %s: %s", message.getClass().getSimpleName(), join(", ", transformerRegistryResult.getFailureMessages())));

            }

            var transformerRegistry = transformerRegistryResult.getContent();
            var transformResult = transformerRegistry.transform(message, JsonObject.class);

            if (transformResult.succeeded()) {

                var protocolVersion = dataspaceProfileContextRegistry.getProtocolVersion(message.getProtocol());
                if (protocolVersion == null) {
                    throw new EdcException(format("No protocol version found for protocol: %s", message.getProtocol()));
                }
                var compacted = jsonLdService.compact(transformResult.getContent(), scopePrefix + DSP_CONTEXT_SEPARATOR + protocolVersion.version());
                if (compacted.succeeded()) {
                    return typeManager.getMapper(typeContext).writeValueAsString(compacted.getContent());
                }
                throw new EdcException("Failed to compact JSON-LD: " + compacted.getFailureDetail());
            }
            throw new EdcException(format("Failed to transform %s: %s", message.getClass().getSimpleName(), join(", ", transformResult.getFailureMessages())));
        } catch (JsonProcessingException e) {
            throw new EdcException(format("Failed to serialize %s", message.getClass().getSimpleName()), e);
        }
    }

}
