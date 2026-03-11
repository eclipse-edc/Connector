/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.matchers;

import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.mockito.ArgumentMatcher;

public abstract class CatalogRequestMatcher implements ArgumentMatcher<CatalogRequestMessage> {

    public static CatalogRequestMatcher sentTo(String recipientId, String recipientUrl) {
        return new CatalogRequestMatcher() {
            @Override
            public boolean matches(CatalogRequestMessage argument) {
                return argument.getCounterPartyId().equals(recipientId) &&
                        argument.getCounterPartyAddress().equals(recipientUrl);
            }
        };
    }
}
