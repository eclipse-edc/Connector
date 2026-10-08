/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld;

import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class CachedDocumentRegistryTest {

    @Test
    void shouldGetCachedDocuments() {
        var contexts = CachedDocumentRegistry.getDocuments().collect(Result.collector());

        assertThat(contexts).isSucceeded();
    }
}
