package com.microsoft.dagx.transfer.core.provision;

import com.microsoft.dagx.policy.model.AtomicConstraintFunction;
import com.microsoft.dagx.policy.model.Duty;
import com.microsoft.dagx.policy.model.Permission;
import com.microsoft.dagx.policy.model.Prohibition;
import com.microsoft.dagx.spi.transfer.provision.ResourceDefinitionGenerator;
import com.microsoft.dagx.spi.transfer.provision.ResourceManifestGenerator;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceManifest;

import java.util.Map;

/**
 *
 */
public class ResourceManifestGeneratorImpl implements ResourceManifestGenerator {

    @Override
    public void registerBuilder(ResourceDefinitionGenerator builder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerPermissionFunctions(Map<String, AtomicConstraintFunction<String, Permission, Boolean>> functions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerProhibitionFunctions(Map<String, AtomicConstraintFunction<String, Prohibition, Boolean>> functions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerObligationFunctions(Map<String, AtomicConstraintFunction<String, Duty, Boolean>> functions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResourceManifest generateClientManifest(DataRequest dataRequest) {
        return new ResourceManifest();
    }

    @Override
    public ResourceManifest generateProviderManifest(DataRequest dataRequest) {
        return new ResourceManifest();
    }
}
