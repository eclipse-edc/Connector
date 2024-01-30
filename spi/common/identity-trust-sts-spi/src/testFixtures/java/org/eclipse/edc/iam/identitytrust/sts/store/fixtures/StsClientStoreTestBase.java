/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.identitytrust.sts.store.fixtures;

import org.eclipse.edc.iam.identitytrust.sts.store.StsClientStore;
import org.eclipse.edc.spi.result.StoreFailure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.identitytrust.sts.store.fixtures.TestFunctions.createClient;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

/**
 * Base compliance tests for implementors of {@link StsClientStore}.
 */
public abstract class StsClientStoreTestBase {

    protected abstract StsClientStore getStsClientStore();

    protected String getRandomId() {
        return UUID.randomUUID().toString();
    }

    @Nested
    class Create {

        @Test
        @DisplayName("Save a single client that not exists")
        void create() {
            var client = createClient(getRandomId());
            assertThat(getStsClientStore().create(client)).isSucceeded();

            var clientFromDb = getStsClientStore().findByClientId(client.getId()).getContent();
            assertThat(client).usingRecursiveComparison().isEqualTo(clientFromDb);
        }
    }

    @Nested
    class FindById {
        @Test
        @DisplayName("Find client by ID that exists")
        void whenPresent() {
            var client = createClient(getRandomId());
            getStsClientStore().create(client);

            var policyFromDb = getStsClientStore().findByClientId(client.getId()).getContent();

            assertThat(client).usingRecursiveComparison().isEqualTo(policyFromDb);
        }

        @Test
        @DisplayName("Find client by ID when not exists")
        void whenNonexistent() {
            assertThat(getStsClientStore().findByClientId("nonexistent"))
                    .isFailed()
                    .extracting(StoreFailure::getReason)
                    .isEqualTo(StoreFailure.Reason.NOT_FOUND);
        }
    }
}
