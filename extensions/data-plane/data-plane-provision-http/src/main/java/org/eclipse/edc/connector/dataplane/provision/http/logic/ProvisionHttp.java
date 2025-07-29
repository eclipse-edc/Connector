/*
 *  Copyright (c) 2025 Think-it GmbH
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

package org.eclipse.edc.connector.dataplane.provision.http.logic;

public interface ProvisionHttp {

    /**
     * Represent the DataAddress type that's evaluated by the ResourceDefinitionGeneratorManager and also the resource
     * type supported by the provisioner and deprovisioner
     */
    String PROVISION_HTTP_TYPE = "ProvisionHttp";
}
