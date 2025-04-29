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

package org.eclipse.edc.tck.dsp.identity;

import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;

/**
 * Loads a no-op identity service.
 */
public class TckIdentityExtension implements ServiceExtension {
    private static final String NAME = "DSP TCK Identity";

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public IdentityService identityService() {
        return new NoopIdentityService();
    }

    @Provider
    public AudienceResolver audienceResolver() {
        return m -> Result.success(m.getCounterPartyId());
    }
}
