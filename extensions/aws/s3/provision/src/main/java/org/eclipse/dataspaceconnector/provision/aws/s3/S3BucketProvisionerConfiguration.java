package org.eclipse.dataspaceconnector.provision.aws.s3;

import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import static org.eclipse.dataspaceconnector.provision.aws.AwsProvisionExtension.PROVISION_MAX_RETRY;
import static org.eclipse.dataspaceconnector.provision.aws.AwsProvisionExtension.PROVISION_MAX_ROLE_SESSION_DURATION;

public class S3BucketProvisionerConfiguration {

    private final int maxRetries;
    private final int roleMaxSessionDuration;

    private S3BucketProvisionerConfiguration(int maxRetries, int roleMaxSessionDuration) {
        this.maxRetries = maxRetries;
        this.roleMaxSessionDuration = roleMaxSessionDuration;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getRoleMaxSessionDuration() {
        return roleMaxSessionDuration;
    }

    public static class Builder {
        private int maxRetries;
        private int roleMaxSessionDuration;

        private Builder() {
        }

        public static Builder newInstance(ServiceExtensionContext context) {
            int maxRetries = Integer.parseInt(context.getSetting(PROVISION_MAX_RETRY, "10"));
            int roleMaxSessionDuration = Integer.parseInt(context.getSetting(PROVISION_MAX_ROLE_SESSION_DURATION, "3600"));
            return new Builder()
                    .maxRetries(maxRetries)
                    .roleMaxSessionDuration(roleMaxSessionDuration);
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder roleMaxSessionDuration(int roleMaxSessionDuration) {
            this.roleMaxSessionDuration = roleMaxSessionDuration;
            return this;
        }

        public S3BucketProvisionerConfiguration build() {
            return new S3BucketProvisionerConfiguration(maxRetries, roleMaxSessionDuration);
        }
    }
}
