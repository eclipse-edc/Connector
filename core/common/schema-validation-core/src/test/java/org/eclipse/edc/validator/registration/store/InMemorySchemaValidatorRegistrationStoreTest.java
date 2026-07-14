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

package org.eclipse.edc.validator.registration.store;

import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.validator.registration.spi.store.SchemaValidatorRegistrationStore;
import org.eclipse.edc.validator.registration.spi.store.testfixtures.SchemaValidatorRegistrationStoreTestBase;

class InMemorySchemaValidatorRegistrationStoreTest extends SchemaValidatorRegistrationStoreTestBase {

    private final InMemorySchemaValidatorRegistrationStore store = new InMemorySchemaValidatorRegistrationStore(CriterionOperatorRegistryImpl.ofDefaults());

    @Override
    protected SchemaValidatorRegistrationStore getStore() {
        return store;
    }
}
