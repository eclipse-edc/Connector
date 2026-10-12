/*
 *  Copyright (c) 2026 Think-it GmbH
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

package org.eclipse.edc.connector.controlplane.contract.spi.event.contractagreement;

import com.fasterxml.jackson.annotation.JsonCreator;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * This event is raised when a ContractAgreement has been retired.
 */
@JsonDeserialize(builder = ContractAgreementRetired.Builder.class)
public class ContractAgreementRetired extends ContractAgreementEvent {

    private String reason;

    private ContractAgreementRetired() {
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String name() {
        return "contract.agreement.retired";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ContractAgreementEvent.Builder<ContractAgreementRetired, Builder> {

        private Builder() {
            super(new ContractAgreementRetired());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder reason(String reason) {
            event.reason = reason;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}
