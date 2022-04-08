/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.azure.cosmos.util;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosStoredProcedureProperties;
import org.eclipse.dataspaceconnector.common.annotations.ExcludeFromCodeCoverageGeneratedReport;

import java.util.Scanner;


/**
 * Helper class used in tests to upload stored procedures into CosmosDb container.
 */
@ExcludeFromCodeCoverageGeneratedReport
public class StoredProcedureTestUtils {

    /**
     * Uploads stored procedure into a container.
     *
     * @param container to upload the stored procedure to
     * @param name of stored procedure js file
     */
    public static void uploadStoredProcedure(CosmosContainer container, String name) {
        // upload stored procedure
        var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name + ".js");
        if (is == null) {
            throw new AssertionError("The input stream referring to the " + name + " file cannot be null!");
        }

        var s = new Scanner(is).useDelimiter("\\A");
        if (!s.hasNext()) {
            throw new IllegalArgumentException("Error loading resource with name " + ".js");
        }
        var body = s.next();
        var props = new CosmosStoredProcedureProperties(name, body);

        var scripts = container.getScripts();
        if (scripts.readAllStoredProcedures().stream().noneMatch(sp -> sp.getId().equals(name))) {
            scripts.createStoredProcedure(props);
        }
    }

}
