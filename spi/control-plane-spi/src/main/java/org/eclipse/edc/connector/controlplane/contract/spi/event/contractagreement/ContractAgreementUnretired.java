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
 * This event is raised when a ContractAgreement has been un-retired.
 */
@JsonDeserialize(builder = ContractAgreementUnretired.Builder.class)
public class ContractAgreementUnretired extends ContractAgreementEvent {

    private ContractAgreementUnretired() {
    }

    @Override
    public String name() {
        return "contract.agreement.unretired";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ContractAgreementEvent.Builder<ContractAgreementUnretired, Builder> {

        private Builder() {
            super(new ContractAgreementUnretired());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}
