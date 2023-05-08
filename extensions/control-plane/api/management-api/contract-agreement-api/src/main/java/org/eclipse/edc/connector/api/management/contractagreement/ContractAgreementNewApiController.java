/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.api.management.contractagreement;

import jakarta.json.JsonObject;
import org.eclipse.edc.api.query.QuerySpecDto;

import java.util.List;

public class ContractAgreementNewApiController implements ContractAgreementNewApi {
    @Override
    public List<JsonObject> getAllAgreements(QuerySpecDto querySpecDto) {
        return null;
    }

    @Override
    public List<JsonObject> queryAllAgreements(QuerySpecDto querySpecDto) {
        return null;
    }

    @Override
    public JsonObject getContractAgreement(String id) {
        return null;
    }
}
