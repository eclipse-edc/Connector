/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.spi;

import java.util.List;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;


public record AssociateDataspaceProfileContext(List<String> profiles) {

    public static final String ASSOCIATE_DATASPACE_PROFILE_CONTEXT_TYPE_TERM = "AssociateDataspaceProfile";
    public static final String ASSOCIATE_DATASPACE_PROFILE_CONTEXT_TYPE_IRI = EDC_NAMESPACE + ASSOCIATE_DATASPACE_PROFILE_CONTEXT_TYPE_TERM;

    public static final String ASSOCIATE_DATASPACE_PROFILE_CONTEXT_PROFILES_TERM = "profiles";
    public static final String ASSOCIATE_DATASPACE_PROFILE_CONTEXT_PROFILES_IRI = EDC_NAMESPACE + ASSOCIATE_DATASPACE_PROFILE_CONTEXT_PROFILES_TERM;
}
