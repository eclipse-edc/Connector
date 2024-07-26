/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.sql.bootstrapper;

import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

public interface SqlSchemaBootstrapper {
    /**
     * Extensions that operate a store based on an SQL database and thus require a certain database structure to be present,
     * can use this class to have their schema auto-generated. The entire DDL has to be in a file that is available from the resources.
     * <p>
     * Note that all DDL statements <strong>must</strong> be queued during the {@link ServiceExtension#initialize(ServiceExtensionContext)} phase.
     * During the {@link ServiceExtension#prepare()} phase
     *
     * @param datasourceName The name of the datasource against which the statements are to be run
     * @param resourceName   An SQL DDL statement. Cannot contain prepared statements. Do not add DML statements here!
     */
    default void addStatementFromResource(String datasourceName, String resourceName) {
        addStatementFromResource(datasourceName, resourceName, getClass().getClassLoader());
    }

    /**
     * Extensions that operate a store based on an SQL database and thus require a certain database structure to be present,
     * can use this class to have their schema auto-generated. The entire DDL has to be in a file that is available from the resources.
     * <p>
     * Note that all DDL statements <strong>must</strong> be queued during the {@link ServiceExtension#initialize(ServiceExtensionContext)} phase.
     * During the {@link ServiceExtension#prepare()} phase
     *
     * @param datasourceName The name of the datasource against which the statements are to be run
     * @param resourceName   An SQL DDL statement. Cannot contain prepared statements. Do not add DML statements here!
     * @param classLoader    A classloader which is used to resolve the resource
     */
    void addStatementFromResource(String datasourceName, String resourceName, ClassLoader classLoader);
}
