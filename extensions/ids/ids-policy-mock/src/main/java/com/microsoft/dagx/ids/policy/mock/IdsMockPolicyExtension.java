package com.microsoft.dagx.ids.policy.mock;

import com.microsoft.dagx.ids.spi.policy.IdsPolicyService;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;

import java.util.Set;

import static com.microsoft.dagx.ids.spi.policy.IdsPolicyExpressions.ABS_SPATIAL_POSITION;

/**
 * Registers test policy functions.
 */
public class IdsMockPolicyExtension implements ServiceExtension {

    @Override
    public Set<String> requires() {
        return Set.of("ids.core");
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var policyService = context.getService(IdsPolicyService.class);

        // handle region restriction
        policyService.registerRequestPermissionFunction(ABS_SPATIAL_POSITION, (operator, rightValue, permission, policyContext) -> rightValue != null && rightValue.equals(policyContext.getClaimToken().getClaims().get("region")));

        monitor.info("Initialized IDS Mock Policy extension");
    }

}
