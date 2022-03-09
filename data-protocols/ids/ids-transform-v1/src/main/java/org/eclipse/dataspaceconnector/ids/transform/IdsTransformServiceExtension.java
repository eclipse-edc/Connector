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
 *       Daimler TSS GmbH - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Arrays;

public class IdsTransformServiceExtension implements ServiceExtension {

    @Inject
    private TransformerRegistry registry;

    @Override
    public String name() {
        return "IDS Transform Extension";
    }

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {
        registerTransformers(serviceExtensionContext);
    }

    private void registerTransformers(ServiceExtensionContext serviceExtensionContext) {

        Arrays.asList(
                new ActionToIdsActionTransformer(),
                new AssetToIdsArtifactTransformer(),
                new AssetToIdsRepresentationTransformer(),
                new AssetToIdsResourceTransformer(),
                new ConnectorToIdsConnectorTransformer(),
                new ConstraintToIdsConstraintTransformer(),
                new ConstraintToIdsLogicalConstraintTransformer(),
                new ContractOfferToIdsContractOfferTransformer(),
                new ContractAgreementToIdsContractAgreementTransformer(),
                new CatalogToIdsResourceCatalogTransformer(),
                new DutyToIdsDutyTransformer(),
                new ExpressionToIdsLeftOperandTransformer(),
                new ExpressionToIdsRdfResourceTransformer(),
                new IdsArtifactToAssetTransformer(),
                new IdsBinaryOperatorToOperatorTransformer(),
                new IdsConstraintToConstraintTransformer(),
                new IdsLogicalConstraintToConstraintTransformer(),
                new IdsContractAgreementToContractAgreementTransformer(),
                new IdsContractOfferToContractOfferTransformer(),
                new IdsContractRequestToContractOfferTransformer(),
                new IdsIdToUriTransformer(),
                new IdsLeftOperandToExpressionTransformer(),
                new IdsPermissionToPermissionTransformer(),
                new IdsProhibitionToProhibitionTransformer(),
                new IdsRdfResourceToExpressionTransformer(),
                new IdsRepresentationToAssetTransformer(),
                new IdsResourceToAssetTransformer(),
                new IdsResourceCatalogToDataCatalogTransformer(),
                new OfferedAssetToIdsResourceTransformer(),
                new OperatorToIdsBinaryOperatorTransformer(),
                new PermissionToIdsPermissionTransformer(),
                new ProhibitionToIdsProhibitionTransformer(),
                new SecurityProfileToIdsSecurityProfileTransformer(),
                new UriToIdsIdTransformer()
        ).forEach(registry::register);
    }

}
