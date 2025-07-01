/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.http.transform;

import org.eclipse.edc.protocol.dsp.spi.transform.DspProtocolTypeTransformerRegistry;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_CONTEXT_SEPARATOR;

public class DspProtocolTypeTransformerRegistryImpl implements DspProtocolTypeTransformerRegistry {

    private final TypeTransformerRegistry transformerRegistry;
    private final String transformerContextPrefix;
    private final DataspaceProfileContextRegistry dataspaceProfileContextRegistry;

    public DspProtocolTypeTransformerRegistryImpl(TypeTransformerRegistry transformerRegistry, String transformerContextPrefix, DataspaceProfileContextRegistry dataspaceProfileContextRegistry) {
        this.transformerRegistry = transformerRegistry;
        this.transformerContextPrefix = transformerContextPrefix;
        this.dataspaceProfileContextRegistry = dataspaceProfileContextRegistry;
    }

    @Override
    public Result<TypeTransformerRegistry> forProtocol(String protocol) {
        var protocolVersion = dataspaceProfileContextRegistry.getProtocolVersion(protocol);

        if (protocolVersion == null) {
            return Result.failure(String.format("No protocol version found for protocol: %s", protocol));
        }
        return Result.success(transformerRegistry.forContext(transformerContextPrefix + DSP_CONTEXT_SEPARATOR + protocolVersion.version()));
    }

}
