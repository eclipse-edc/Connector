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

package org.eclipse.edc.protocol.dsp.http.protocol;

import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersion;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersionRegistry;
import org.eclipse.edc.protocol.dsp.http.spi.DspProtocolParser;
import org.eclipse.edc.spi.result.Result;

import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP_SEPARATOR;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_08;

public class DspProtocolParserImpl implements DspProtocolParser {

    private final ProtocolVersionRegistry versionRegistry;

    public DspProtocolParserImpl(ProtocolVersionRegistry versionRegistry) {
        this.versionRegistry = versionRegistry;
    }

    @Override
    public Result<ProtocolVersion> parse(String protocol) {
        var protocolWithVersion = protocol.split(DATASPACE_PROTOCOL_HTTP_SEPARATOR);
        var protocolName = protocolWithVersion[0];

        if (!protocolName.equals(DATASPACE_PROTOCOL_HTTP)) {
            return Result.failure("Protocol %s not supported. Expected protocol: %s".formatted(protocolName, DATASPACE_PROTOCOL_HTTP));
        }

        if (protocolWithVersion.length == 1) {
            return Result.success(V_08);
        }
        var protocolVersion = protocolWithVersion[1];

        return versionRegistry.getAll().protocolVersions()
                .stream().filter(protoVersion -> protoVersion.version().equals(protocolVersion))
                .findFirst()
                .map(Result::success)
                .orElseGet(() -> Result.failure("Protocol %s with version %s not supported".formatted(protocolName, protocolVersion)));
    }
}
