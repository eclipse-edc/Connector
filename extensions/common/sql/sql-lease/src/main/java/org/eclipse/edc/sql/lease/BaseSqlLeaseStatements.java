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

package org.eclipse.edc.sql.lease;

import org.eclipse.edc.sql.lease.spi.LeaseStatements;

/**
 * Default SQL statements for the {@code edc_lease} table.
 *
 * @deprecated the lease is now stored on the entity row (see {@code org.eclipse.edc.sql.lease.StatefulEntityStatements}), this will be removed in a future release.
 */
@Deprecated(since = "1.0.0")
public class BaseSqlLeaseStatements implements LeaseStatements {
 
}
