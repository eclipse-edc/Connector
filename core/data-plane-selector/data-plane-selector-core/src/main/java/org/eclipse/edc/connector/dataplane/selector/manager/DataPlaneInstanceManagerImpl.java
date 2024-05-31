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

package org.eclipse.edc.connector.dataplane.selector.manager;

import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.manager.DataPlaneInstanceManager;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.statemachine.AbstractStateEntityManager;
import org.eclipse.edc.statemachine.StateMachineManager;

public class DataPlaneInstanceManagerImpl extends AbstractStateEntityManager<DataPlaneInstance, DataPlaneInstanceStore> implements DataPlaneInstanceManager {

    private DataPlaneInstanceManagerImpl() {
    }

    @Override
    protected StateMachineManager.Builder configureStateMachineManager(StateMachineManager.Builder builder) {
        return builder;
    }

    public static class Builder
            extends AbstractStateEntityManager.Builder<DataPlaneInstance, DataPlaneInstanceStore, DataPlaneInstanceManagerImpl, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new DataPlaneInstanceManagerImpl());
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}
