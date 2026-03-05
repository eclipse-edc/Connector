/*
 *  Copyright (c) 2026 Think-it GmbH
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

package org.eclipse.edc.sql.lease;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.sql.lease.spi.LeaseStatements;

import static org.eclipse.edc.sql.lease.SqlLeaseStatementsExtension.NAME;

@Extension(NAME)
public class SqlLeaseStatementsExtension implements ServiceExtension {

    public static final String NAME = "SQL Lease Statements";

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public LeaseStatements leaseStatements() {
        return new BaseSqlLeaseStatements();
    }

}
