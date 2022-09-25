/*
 *  Copyright (c) 2022 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.policy.model;

/**
 * Wrapper for a  PolicyDefinition#getId(). Used to format a simple string as JSON.
 */
public class PolicyDefinitionId {
    private final String id;
    public PolicyDefinitionId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}