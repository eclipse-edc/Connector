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

package org.eclipse.dataspaceconnector.ids.api.multipart.handler.description;

import de.fraunhofer.iais.eis.Artifact;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ArtifactDescriptionRequestHandler extends AbstractDescriptionRequestHandler<Asset, Artifact> {
    private final AssetIndex assetIndex;

    public ArtifactDescriptionRequestHandler(
            @NotNull Monitor monitor,
            @NotNull String connectorId,
            @NotNull AssetIndex assetIndex,
            @NotNull IdsTransformerRegistry transformerRegistry) {
        super(
                connectorId,
                monitor,
                transformerRegistry,
                IdsType.ARTIFACT,
                Artifact.class
        );
        this.assetIndex = Objects.requireNonNull(assetIndex);
    }

    @Override
    protected Asset retrieveObject(@NotNull IdsId idsId, @NotNull ClaimToken claimToken) {
        return assetIndex.findById(idsId.getValue());
    }
}
