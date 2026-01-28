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

package org.eclipse.edc.policy.cel.function.context;

import org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorContext;
import org.eclipse.edc.spi.result.Result;

import java.util.Map;

/**
 * Supplies context data for CEL expression evaluation specific to PolicyMonitorContext.
 */
public class PolicyMonitorContextMapper implements CelContextMapper<PolicyMonitorContext> {

    private final AgreementContextMapper agreementContextSupplier;

    public PolicyMonitorContextMapper(AgreementContextMapper agreementContextSupplier) {
        this.agreementContextSupplier = agreementContextSupplier;
    }

    @Override
    public Result<Map<String, Object>> mapContext(PolicyMonitorContext context) {
        return agreementContextSupplier.mapContext(context);
    }
}
