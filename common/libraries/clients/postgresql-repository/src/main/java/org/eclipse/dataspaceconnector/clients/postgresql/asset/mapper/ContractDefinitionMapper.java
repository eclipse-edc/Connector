/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.clients.postgresql.asset.mapper;

import org.eclipse.dataspaceconnector.clients.postgresql.asset.serializer.EnvelopePacker;
import org.eclipse.dataspaceconnector.clients.postgresql.row.RowMapper;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ContractDefinitionMapper implements RowMapper<ContractDefinition> {

    private static final String ID_COLUMN = "id";
    private static final String SELECTOR_EXPRESSION_COLUMN = "asset_selector_expression";
    private static final String ACCESS_POLICY_COLUMN = "access_policy";
    private static final String CONTRACT_POLICY_COLUMN = "contract_policy";

    @Override
    public ContractDefinition mapRow(ResultSet resultSet) throws SQLException {
        String id = resultSet.getString(ID_COLUMN);
        AssetSelectorExpression selectorExpression = EnvelopePacker.unpack(resultSet.getString(SELECTOR_EXPRESSION_COLUMN));
        Policy contractPolicy = EnvelopePacker.unpack(resultSet.getString(CONTRACT_POLICY_COLUMN));
        Policy accessPolicy = EnvelopePacker.unpack(resultSet.getString(ACCESS_POLICY_COLUMN));

        return ContractDefinition.Builder.newInstance()
                .id(id)
                .selectorExpression(selectorExpression)
                .contractPolicy(contractPolicy)
                .accessPolicy(accessPolicy)
                .build();
    }

}
