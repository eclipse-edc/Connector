/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.api.datamanagement.contractagreement.service;

import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;

import java.util.Collection;

/**
 * Service that permits actions and queries on ContractAgreement entity.
 *
 */
public interface ContractAgreementService {

    /**
     * Returns a contract agreement by its id
     *
     * @param contractAgreementId id of the contract agreement
     * @return the contract agreement, null if it's not found
     */
    ContractAgreement findById(String contractAgreementId);

    /**
     * Query contract agreements
     *
     * @param query request
     * @return the collection of contract agreements that match the query
     */
    Collection<ContractAgreement> query(QuerySpec query);

}
