/*
 *  Copyright (c) 2022 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH
 *
 */

package org.eclipse.edc.gcp.common;

import org.eclipse.edc.junit.testfixtures.MockVault;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class GcpCredentialsTest {

    private GcpCredentials gcpCredential;

    String accessTokenKeyName = "access_token_key_name_test";

    String invalidAccessTokenKeyName = "invalid_access_token_key_name_test";

    String serviceAccountKeyName = "service_account_key_name_test";

    String invalidServiceAccountKeyName = "invalid_service_account_key_name_test";


    private String serviceAccountFileInB64;

    @BeforeEach
    public void setUp() {
        var vault = new MockVault();
        var tokenValue = UUID.randomUUID();
        var serviceAccountFileInJson = "{\n" +
                "  \"type\": \"service_account\",\n" +
                "  \"project_id\": \"project-test-1\",\n" +
                "  \"private_key_id\": \"id1\",\n" +
                "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEuwIBADANBgkqhkiG9w0BAQEFAASCBKUwggShAgEAAoIBAQC6zzVgbCoFq0WS\\nhZBsaW70ntwOmSVfufvaNtVahXU1kLqJ+h5chwXENyQ5A4md941KS/x+lpK1Z/Nv" +
                "\\nWwk/G7Uf5N5GI006JShKVsXeTl/CbAh8cLDHgG35ZXtFU24lWSDbdYu2qldxzJlL\\nr8E55ATsHwFBTezRwyTdsPNPS2F9Zx6fJme19WJMpkZDbImbxt8MxM1uI//6R0Xh\\nRqCvfnEGXRlj049uSKdRHExOCvp7EWEwfFznJqjFVUHbivR2p6szlq4dLkuexZko" +
                "\\nEOSx2wWnzRW/NMg/5hqeLscbtyq7rjLfQTKcwTi4z8dymCHRf0RUzFX8ybgCt6y6\\nfpy8eY4/AgMBAAECgf8O4Mc02ITyCxWNpBWVcF50HRZpoXODOndw9+0GHfBGDMDO\\nkbmD+lltABO3zBBUdjguFjSF4HgkFviv8/O6WhcEne6kNZ1UhC9NvGKUJ1R5GVp3" +
                "\\nOWX3YTTvRGzwfBge/c+R49j1pSmnEUDrYrCA+gujNGMsUE+aZTd8N6nT0ZCrCHbW\\niGeAAzjWZbQ+tx/ygotYGUDG5qqtQwZl52krKQ3OlMlmltcNddrLg/dBMeHu8g+S\\nbQivyd2QW0K9oglklreA6GnUUEOCt80hODIbBbpdyxVXcsDtNfffddqgf8j8xQm3" +
                "\\npLUmufVW5gXlYnFogiSwKHPOvzmVPGyx/rBkmfkCgYEA4GSlIyE3vBSk+UkNa4zH\\n1sxUGq+3fi0vEEMsZsoOpsPPL4UJnktHnwiBkBsH5dIhLY/2NY9OHtIzMPTYvvRc\\nqG6RyQ61DTIjkVbNbfvpV4CAFnHRCh4ms/ZOukhegOfaRKNGADld1eUzsmShFTbV" +
                "\\nuOZQLeE+PyEvJegCRya+AVkCgYEA1R9WdxbUq5bEyiU8euIOe8jCfICADFVQ957+\\noZ4XMprXX/uNBa2IIEgkXW2uWYCL09fL+UlVcUu5iyVqXaRLBzD3/JW9IfCzCm1A\\nxaMpezv1vLyZp0n0VvTN2uag6t5M/FkYCmp+m+VzQi82dFm0DuTFulOix78lYVHA" +
                "\\nwFKbwVcCgYByqdtczS+e02nN3M+XwrOnhnf/vwTj3BDtnXXF/MBp5SstHC1jDxLF\\nKGKUkcuCW9MKZkMo8Va5Fy6DeMp9IX9rrjye4f4QhSt5rEKDTjPZu9c4IObx5aBf\\nW6C1Ph/UfSWi50/w81+I2nuFUDikD4Y82qvkFfJp7foaw6jOVPTI2QKBgQC7kZQY" +
                "\\nxbgwuEXEH1eGUxQqL3uz9ag8so3LEVzLQwbpm8t4Bz2LRLnsp3GR5KkwzmjB7kfv\\nw3H2f43x/+EIP0NlNdzbqbHGgEAjKhp6luo4MoJJNLgKupTYPyY5xQbVDwc0hPka\\nmbWKYTu6gTDs39IP1ZqMLXWzVPCCIWCCI3I/iwKBgArrjiqmWQ55xRbdGCvw5RCv" +
                "\\ncIyuvwZTRYGrkGDIq7HHcFQDTqcxMMQhe0ox9WgQDuu8hAsrVcJv2Ia/14W3xQPT\\nyGAc9EtS112c3ZOYCyuPY4NgoIad9TvpHvpiaLNukbIUAIimoNbyWAUwmkTjTjkC\\n4X+WEhgfmekYzcrk6nfS\\n-----END PRIVATE KEY-----\\n\",\n" +
                "  \"client_email\": \"test@project-test-1.iam.gserviceaccount.com\",\n" +
                "  \"client_id\": \"client_id1\",\n" +
                "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/project-test-1.iam.gserviceaccount.com\"\n" +
                "}";

        var tokenInJson = "{\"edctype\":\"dataspaceconnector:gcptoken\"" +
                ",\"token\":\"" + tokenValue + "\"," +
                "\"expiration\":1665998128000}";
        serviceAccountFileInB64 = Base64.getEncoder().encodeToString(serviceAccountFileInJson.getBytes());
        var tokenInB64 = Base64.getEncoder().encodeToString(tokenInJson.getBytes());


        vault.storeSecret(accessTokenKeyName, tokenInJson);
        vault.storeSecret(invalidAccessTokenKeyName, "{\"token\":\"" + tokenValue + "\"");
        vault.storeSecret(serviceAccountKeyName, serviceAccountFileInJson);
        vault.storeSecret(invalidServiceAccountKeyName, "{\"type\": \"service_account\" }");
        gcpCredential = new GcpCredentials(vault, new TypeManager(), mock(Monitor.class));
    }

    @Test
    void testResolveGoogleCredentialWhenTokenKeyNameIsProvided() {
        var gcpCred = gcpCredential.resolveGoogleCredentialsFromDataAddress(accessTokenKeyName, null, null);
        assert (gcpCred != null);
    }

    @Test
    void testResolveGoogleCredentialWhenInvalidTokenIsProvided() {
        Exception thrown = assertThrows(EdcException.class, () -> gcpCredential.resolveGoogleCredentialsFromDataAddress(invalidAccessTokenKeyName,  null, null));
        assert (thrown.getMessage().contains("valid GcpAccessToken format"));
    }

    @Test
    void testResolveGoogleCredentialPriorityWhenTokenIsInvalid() {
        Exception thrown = assertThrows(EdcException.class, () -> gcpCredential.resolveGoogleCredentialsFromDataAddress(invalidAccessTokenKeyName, serviceAccountKeyName, null));

        assert (thrown.getMessage().contains("valid GcpAccessToken format"));
    }


    @Test
    void testResolveGoogleCredentialWhenServiceAccountKeyNameIsProvided() {
        var gcpCred = gcpCredential.resolveGoogleCredentialsFromDataAddress(null, serviceAccountKeyName, null);
        assert (gcpCred != null);
    }

    @Test
    void testResolveGoogleCredentialWhenInvalidServiceAccountKeyNameIsProvided() {
        Exception thrown = assertThrows(GcpException.class, () -> gcpCredential.resolveGoogleCredentialsFromDataAddress(null,
                invalidServiceAccountKeyName, null));
    }

    @Test
    void testResolveGoogleCredentialWhenServiceAccountValueIsProvided() {
        var gcpCred = gcpCredential.resolveGoogleCredentialsFromDataAddress(null, null, serviceAccountFileInB64);
        assert (gcpCred != null);
    }

    @Test
    void testResolveGoogleCredentialWhenInvalidServiceAccountValueIsProvided() {
        Exception thrown = assertThrows(EdcException.class, () -> gcpCredential.resolveGoogleCredentialsFromDataAddress(null, null, serviceAccountFileInB64 + "makeItWrongB64"));
        assert (thrown.getMessage().contains("valid base64 format"));
    }

    @Test
    void testResolveGoogleCredentialPriorityWhenInvalidServiceAccountValueIsProvided() {
        var gcpCred = gcpCredential.resolveGoogleCredentialsFromDataAddress(null, serviceAccountKeyName, serviceAccountFileInB64 + "makeItWrongB64");
        assert (gcpCred != null);
    }
}
