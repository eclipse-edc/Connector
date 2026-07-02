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
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.DefaultParticipantIdExtractionFunction;
import org.eclipse.edc.protocol.spi.service.DataspaceProfileService;
import org.eclipse.edc.protocol.spi.store.DataspaceProfileStore;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataspaceProfileServiceImplTest {

    private static final String BASE_WEBHOOK = "http://localhost:8080/protocol";

    private final DataspaceProfileStore store = mock();
    private final DataspaceProfileContextRegistry registry = mock();
    private final DspBaseWebhookAddress webhookAddress = mock();
    private final DefaultParticipantIdExtractionFunction idExtractionFunction = mock();

    private final DataspaceProfileService service = new DataspaceProfileServiceImpl(new NoopTransactionContext(), store, registry,
            new DataspaceProfileMapper(webhookAddress, idExtractionFunction));

    private DataspaceProfile profile() {
        return DataspaceProfile.Builder.newInstance()
                .name("dsp2025_1").protocolVersion("2025-1").path("/dsp2025_1").binding("HTTPS")
                .namespace("https://w3id.org/dspace/v1").build();
    }

    @Test
    void create_persistsAndRegisters() {
        when(webhookAddress.get()).thenReturn(BASE_WEBHOOK);
        var profile = profile();
        when(store.create(profile)).thenReturn(StoreResult.success(profile));

        var result = service.create(profile);

        assertThat(result).matches(r -> r.succeeded());
        var captor = ArgumentCaptor.forClass(DataspaceProfileContext.class);
        verify(registry).register(captor.capture());
        var registered = captor.getValue();
        assertThat(registered.name()).isEqualTo("dsp2025_1");
        assertThat(registered.protocolVersion().version()).isEqualTo("2025-1");
        assertThat(registered.protocolVersion().path()).isEqualTo("/dsp2025_1");
        assertThat(registered.protocolVersion().binding()).isEqualTo("HTTPS");
        assertThat(registered.webhook().url()).isEqualTo(BASE_WEBHOOK + "/dsp2025_1");
        assertThat(registered.idExtractionFunction()).isSameAs(idExtractionFunction);
    }

    @Test
    void create_doesNotRegister_whenStoreFails() {
        var profile = profile();
        when(store.create(profile)).thenReturn(StoreResult.alreadyExists("exists"));

        var result = service.create(profile);

        assertThat(result).matches(r -> r.failed());
        verify(registry, never()).register(any());
    }

    @Test
    void deleteById_touchesStoreOnly() {
        var profile = profile();
        when(store.delete("dsp2025_1")).thenReturn(StoreResult.success(profile));

        var result = service.deleteById("dsp2025_1");

        assertThat(result).matches(r -> r.succeeded());
        verify(store).delete("dsp2025_1");
        verify(registry, never()).register(any());
    }
}
