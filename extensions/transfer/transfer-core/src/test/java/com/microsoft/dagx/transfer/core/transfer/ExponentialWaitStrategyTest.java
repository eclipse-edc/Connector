/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.core.transfer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
class ExponentialWaitStrategyTest {

    @Test
    void verifyBackoff() {
        ExponentialWaitStrategy strategy = new ExponentialWaitStrategy(5000);

       assertEquals(5000, strategy.retryInMillis());
       assertEquals(10000, strategy.retryInMillis());

       strategy.success(); // reset
        
       assertEquals(5000, strategy.retryInMillis());

    }
}
