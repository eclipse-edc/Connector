/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.contract;

import org.eclipse.edc.connector.contract.policy.PolicyArchiveImpl;
import org.eclipse.edc.connector.policy.spi.store.PolicyArchive;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EdcExtension.class)
class ContractCoreExtensionTest {

    @Test
    void shouldProvidePolicyArchive(PolicyArchive policyArchive) {
        assertThat(policyArchive).isNotNull().isInstanceOf(PolicyArchiveImpl.class);
    }
}
