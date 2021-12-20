package org.eclipse.dataspaceconnector.provision.aws.s3;

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
