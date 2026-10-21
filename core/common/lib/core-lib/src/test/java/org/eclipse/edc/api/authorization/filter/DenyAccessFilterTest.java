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

package org.eclipse.edc.api.authorization.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DenyAccessFilterTest {

    @Test
    void filter_abortsWithForbidden() {
        var request = mock(ContainerRequestContext.class);

        new DenyAccessFilter().filter(request);

        verify(request).abortWith(argThat(r -> r.getStatus() == 403));
    }
}
