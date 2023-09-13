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

package org.eclipse.edc.connector.policy.monitor.spi;

import org.eclipse.edc.spi.entity.StatefulEntity;

import static org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorEntryStates.COMPLETED;
import static org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorEntryStates.FAILED;
import static org.eclipse.edc.connector.policy.monitor.spi.PolicyMonitorEntryStates.STARTED;

public class PolicyMonitorEntry extends StatefulEntity<PolicyMonitorEntry> {

    private String contractId;

    @Override
    public PolicyMonitorEntry copy() {
        var builder = Builder.newInstance().contractId(contractId);
        return copy(builder);
    }

    @Override
    public String stateAsString() {
        return PolicyMonitorEntryStates.from(state).name();
    }

    public String getContractId() {
        return contractId;
    }

    public void transitionToStarted() {
        transitionTo(STARTED.code());
    }

    public void transitionToCompleted() {
        transitionTo(COMPLETED.code());
    }

    public void transitionToFailed(String errorDetail) {
        this.errorDetail = errorDetail;
        transitionTo(FAILED.code());
    }

    public static class Builder extends StatefulEntity.Builder<PolicyMonitorEntry, Builder> {

        private Builder(PolicyMonitorEntry entity) {
            super(entity);
        }

        public static Builder newInstance() {
            return new Builder(new PolicyMonitorEntry());
        }

        public Builder contractId(String contractId) {
            entity.contractId = contractId;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public PolicyMonitorEntry build() {
            return super.build();
        }
    }
}
