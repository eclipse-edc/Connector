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

package org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.v3;

import jakarta.ws.rs.Path;
import org.eclipse.edc.connector.controlplane.api.management.contractnegotiation.BaseContractNegotiationApiController;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

@Path("/v3/contractnegotiations")
public class ContractNegotiationApiV3Controller extends BaseContractNegotiationApiController implements ContractNegotiationApiV3 {
    public ContractNegotiationApiV3Controller(ContractNegotiationService service, TypeTransformerRegistry transformerRegistry, Monitor monitor, JsonObjectValidatorRegistry validatorRegistry) {
        super(service, transformerRegistry, monitor, validatorRegistry);
    }
}
