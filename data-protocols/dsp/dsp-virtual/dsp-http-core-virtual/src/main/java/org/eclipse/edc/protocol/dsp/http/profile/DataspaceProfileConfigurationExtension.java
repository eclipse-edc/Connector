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

package org.eclipse.edc.protocol.dsp.http.profile;

import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.protocol.dsp.http.spi.api.DspBaseWebhookAddress;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.DefaultParticipantIdExtractionFunction;
import org.eclipse.edc.protocol.spi.ProtocolVersion;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.Arrays;
import java.util.Map;

@Extension(value = DataspaceProfileConfigurationExtension.NAME)
public class DataspaceProfileConfigurationExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Profile Configuration Extension";

    public static final String EDC_DATASPACE_PROFILES_PREFIX = "edc.dataspace.profiles";

    public static final String PROFILE_NAME = "name";
    public static final String PROTOCOL_VERSION = "protocol.version";
    public static final String PROTOCOL_BINDING = "protocol.binding";
    public static final String PROTOCOL_NAMESPACE = "protocol.namespace";
    public static final String PROFILE_JSON_LD_CONTEXT = "jsonld.context.urls";

    @Configuration(context = EDC_DATASPACE_PROFILES_PREFIX)
    private Map<String, DataspaceProfileConfiguration> profileConfiguration;

    @Inject
    private DefaultParticipantIdExtractionFunction participantIdExtractionFunction;

    @Inject
    private DataspaceProfileContextRegistry profileRegistry;

    @Inject
    private DspBaseWebhookAddress dspWebhookAddress;

    @Override
    public void initialize(ServiceExtensionContext context) {
        profileConfiguration.values().stream()
                .map(this::toDataspaceProfileContext)
                .forEach(profileRegistry::register);
    }

    private DataspaceProfileContext toDataspaceProfileContext(DataspaceProfileConfiguration config) {
        var namespace = new JsonLdNamespace(config.protocolNamespace());
        var protocolVersion = new ProtocolVersion(config.protocolVersion(), "/" + config.name(), config.protocolBinding());

        var jsonLdContextsUrl = Arrays.stream(config.jsonLdContextsUrl().split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        return new DataspaceProfileContext(
                config.name(),
                protocolVersion,
                () -> dspWebhookAddress.get() + "/" + config.name(),
                participantIdExtractionFunction,
                namespace,
                jsonLdContextsUrl);
    }

    @Settings
    private record DataspaceProfileConfiguration(
            @Setting(
                    key = PROFILE_NAME,
                    description = "The name of the dataspace profile.")
            String name,
            @Setting(
                    key = PROTOCOL_VERSION,
                    description = "The version of the DSP protocol this profile binds to.")
            String protocolVersion,
            @Setting(
                    key = PROTOCOL_BINDING,
                    description = "The protocol binding (e.g. 'http') of the DSP protocol this profile binds to.")
            String protocolBinding,
            @Setting(
                    key = PROTOCOL_NAMESPACE,
                    description = "The JSON-LD namespace of the dataspace profile.")
            String protocolNamespace,
            @Setting(
                    key = PROFILE_JSON_LD_CONTEXT,
                    description = "The JSON-LD context URLs of the dataspace profile.")
            String jsonLdContextsUrl
    ) {

    }
}
