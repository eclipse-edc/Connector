package com.microsoft.dagx.ids.spi.daps;

import com.microsoft.dagx.ids.spi.domain.iam.DynamicAttributeToken;

/**
 * A Dynamic Attribute Provisioning Services as defined by IDS.
 */
public interface DapsService {

    /**
     * Obtains a dynamic attribute token for the current runtime.
     */
    DynamicAttributeToken obtainToken();

}
