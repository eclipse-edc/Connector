/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.participantcontext.config.defaults.store;

import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStoreTestBase;

public class InMemoryParticipantContextConfigStoreTest extends ParticipantContextConfigStoreTestBase {
    
    private final InMemoryParticipantContextConfigStore store = new InMemoryParticipantContextConfigStore();

    @Override
    protected ParticipantContextConfigStore getStore() {
        return store;
    }
}
