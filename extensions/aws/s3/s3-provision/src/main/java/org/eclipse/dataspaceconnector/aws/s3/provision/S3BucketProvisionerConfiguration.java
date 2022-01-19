package org.eclipse.dataspaceconnector.aws.s3.provision;

public class S3BucketProvisionerConfiguration {

    private final int maxRetries;
    private final int roleMaxSessionDuration;

    public S3BucketProvisionerConfiguration(int maxRetries, int roleMaxSessionDuration) {
        this.maxRetries = maxRetries;
        this.roleMaxSessionDuration = roleMaxSessionDuration;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getRoleMaxSessionDuration() {
        return roleMaxSessionDuration;
    }

}
