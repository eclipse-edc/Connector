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

import org.eclipse.edc.protocol.dsp.http.spi.api.DspBaseWebhookAddress;
import org.eclipse.edc.protocol.spi.DataspaceProfile;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.DefaultParticipantIdExtractionFunction;
import org.eclipse.edc.protocol.spi.service.DataspaceProfileService;
import org.eclipse.edc.protocol.spi.store.DataspaceProfileStore;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.Arrays;
import java.util.Map;

import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;

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

    @Inject
    private DataspaceProfileStore store;

    @Inject
    private TransactionContext transactionContext;

    private DataspaceProfileMapper mapper;

    @Override
    public void initialize(ServiceExtensionContext context) {
        mapper = new DataspaceProfileMapper(dspWebhookAddress, participantIdExtractionFunction);
    }

    @Override
    public void prepare() {
        transactionContext.execute(() -> {
            // seed the profiles defined via configuration into the store, so the store is the single
            // source of truth for what gets registered.
            profileConfiguration.values().stream()
                    .map(this::toDataspaceProfile)
                    .forEach(this::upsert);

            // populate the registry from the store
            try (var stream = store.findAll(QuerySpec.max())) {
                stream.map(mapper::toContext).forEach(profileRegistry::register);
            }
        });
    }

    @Provider
    public DataspaceProfileService dataspaceProfileService() {
        return new DataspaceProfileServiceImpl(transactionContext, store, profileRegistry,
                new DataspaceProfileMapper(dspWebhookAddress, participantIdExtractionFunction));
    }

    private void upsert(DataspaceProfile profile) {
        var result = store.create(profile);
        if (result.failed() && result.reason() == ALREADY_EXISTS) {
            store.update(profile);
        }
    }

    private DataspaceProfile toDataspaceProfile(DataspaceProfileConfiguration config) {
        var jsonLdContextsUrl = Arrays.stream(config.jsonLdContextsUrl().split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        return DataspaceProfile.Builder.newInstance()
                .name(config.name())
                .protocolVersion(config.protocolVersion())
                .path("/" + config.name())
                .binding(config.protocolBinding())
                .namespace(config.protocolNamespace())
                .jsonLdContextsUrl(jsonLdContextsUrl)
                .build();
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
