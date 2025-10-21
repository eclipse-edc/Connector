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

package org.eclipse.edc.connector.controlplane.services.asset;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.event.AssetCreated;
import org.eclipse.edc.connector.controlplane.asset.spi.event.AssetDeleted;
import org.eclipse.edc.connector.controlplane.asset.spi.event.AssetEvent;
import org.eclipse.edc.connector.controlplane.asset.spi.event.AssetUpdated;
import org.eclipse.edc.connector.controlplane.asset.spi.observe.AssetListener;
import org.eclipse.edc.spi.event.EventRouter;

/**
 * Listener responsible for creating and publishing events regarding Asset state changes
 */
public class AssetEventListener implements AssetListener {
    private final EventRouter eventRouter;

    public AssetEventListener(EventRouter eventRouter) {
        this.eventRouter = eventRouter;
    }

    @Override
    public void created(Asset asset) {
        var builder = AssetCreated.Builder.newInstance();

        eventRouter.publish(withBaseProperties(builder, asset));
    }

    @Override
    public void deleted(Asset asset) {
        var builder = AssetDeleted.Builder.newInstance();

        eventRouter.publish(withBaseProperties(builder, asset));
    }

    @Override
    public void updated(Asset asset) {
        var builder = AssetUpdated.Builder.newInstance();

        eventRouter.publish(withBaseProperties(builder, asset));
    }

    private <E extends AssetEvent> E withBaseProperties(AssetEvent.Builder<E, ?> builder, Asset asset) {
        return builder
                .assetId(asset.getId())
                .participantContextId(asset.getParticipantContextId())
                .build();
    }

}
