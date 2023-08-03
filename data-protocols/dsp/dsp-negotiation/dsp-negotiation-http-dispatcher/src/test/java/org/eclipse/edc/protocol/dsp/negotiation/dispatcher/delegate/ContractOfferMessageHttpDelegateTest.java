/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.negotiation.dispatcher.delegate;

import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.testfixtures.dispatcher.DspHttpDispatcherDelegateTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContractOfferMessageHttpDelegateTest extends DspHttpDispatcherDelegateTestBase<ContractOfferMessage> {
    
    private ContractOfferMessageHttpDelegate delegate;
    
    @BeforeEach
    void setUp() {
        delegate = new ContractOfferMessageHttpDelegate(serializer);
    }

    @Test
    void parseResponse_shouldReturnNullFunction() {
        testParseResponse_shouldReturnNullFunction_whenResponseBodyNotProcessed();
    }
    
    @Override
    protected DspHttpDispatcherDelegate<ContractOfferMessage, ?> delegate() {
        return delegate;
    }

}
