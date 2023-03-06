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

package org.eclipse.edc.protocol.dsp.api.configuration;

public class DspApiConfiguration {
    
    private String contextAlias;
    private String dspWebhookAddress;
    
    public DspApiConfiguration(String contextAlias, String dspWebhookAddress) {
        this.contextAlias = contextAlias;
        this.dspWebhookAddress = dspWebhookAddress;
    }
    
    public String getContextAlias() {
        return contextAlias;
    }
    
    public String getDspWebhookAddress() {
        return dspWebhookAddress;
    }
    
}
