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

package org.eclipse.edc.validator.registration;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.validator.registration.spi.store.SchemaValidatorRegistrationStore;
import org.eclipse.edc.validator.registration.store.InMemorySchemaValidatorRegistrationStore;

import static org.eclipse.edc.validator.registration.SchemaValidationDefaultServicesExtension.NAME;

@Extension(NAME)
public class SchemaValidationDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Schema Validator Registration Default Services";

    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public SchemaValidatorRegistrationStore schemaValidatorRegistrationStore() {
        return new InMemorySchemaValidatorRegistrationStore(criterionOperatorRegistry);
    }
}
