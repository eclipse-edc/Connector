package org.eclipse.dataspaceconnector.aws.s3.provision;

import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.sts.model.Credentials;

public class S3ProvisionResponse {
    private final Role role;
    private final Credentials credentials;

    public S3ProvisionResponse(Role role, Credentials credentials) {
        this.role = role;
        this.credentials = credentials;
    }

    public Role getRole() {
        return role;
    }

    public Credentials getCredentials() {
        return credentials;
    }
}
