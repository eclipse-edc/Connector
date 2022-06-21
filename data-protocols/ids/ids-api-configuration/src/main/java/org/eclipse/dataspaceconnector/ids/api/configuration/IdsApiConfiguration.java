/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.configuration;

/**
 * Holds configuration information for the IDS API context.
 */
public class IdsApiConfiguration {
    
    private final String contextAlias;
    
    private final String path;
    
    public IdsApiConfiguration(String contextAlias, String path) {
        this.contextAlias = contextAlias;
        this.path = path;
    }
    
    public String getContextAlias() {
        return contextAlias;
    }
    
    public String getPath() {
        return path;
    }
    
}
