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

package org.eclipse.edc.policy.cel;

import org.eclipse.edc.policy.cel.store.CelExpressionStore;
import org.eclipse.edc.policy.cel.store.InMemoryCelExpressionStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;

import static org.eclipse.edc.policy.cel.CelPolicyCoreExtension.NAME;

@Extension(NAME)
public class CelPolicyDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Common Expression Language Policy Default Services Extension";

    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public CelExpressionStore policyExpressionStore() {
        return new InMemoryCelExpressionStore(criterionOperatorRegistry);
    }

}
