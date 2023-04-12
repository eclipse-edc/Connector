package org.eclipse.edc.ms.dataverse.validator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MicrosoftDataverseValidatorTest {

    @Test
    public void testValidateEntityName() {
        // Test valid entity name
        MicrosoftDataverseValidator.validateEntityName("account");

        // Test entity name that is too short
        try {
            MicrosoftDataverseValidator.validateEntityName("");
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid entity name, the name may not be null, empty or blank", e.getMessage());
        }

        // Test entity name that is too long
        try {
            MicrosoftDataverseValidator.validateEntityName("a".repeat(129));
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid entity name length, the name must be between 1 and 128 characters long", e.getMessage());
        }
    }

    @Test
    public void testValidateServiceUri() {
        // Test valid service URI
        MicrosoftDataverseValidator.validateServiceUri("https://org.crm.dynamics.com");

        // Test service URI that is too short
        try {
            MicrosoftDataverseValidator.validateServiceUri("");
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid serviceUri name, the name may not be null, empty or blank", e.getMessage());
        }

        // Test service URI that is too long
        try {
            MicrosoftDataverseValidator.validateServiceUri("a".repeat(1025));
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid serviceUri name length, the name must be between 1 and 1024 characters long", e.getMessage());
        }
    }

    @Test
    public void testValidateServicePrincipalId() {
        // Test valid service principal ID
        MicrosoftDataverseValidator.validateServicePrincipalId("12345678-1234-1234-1234-123456789012");

        // Test service principal ID that is too short
        try {
            MicrosoftDataverseValidator.validateServicePrincipalId("");
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid servicePrincipalId name, the name may not be null, empty or blank", e.getMessage());
        }

        // Test service principal ID that is too long
        try {
            MicrosoftDataverseValidator.validateServicePrincipalId("a".repeat(37));
            fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid servicePrincipalId name length, the name must be between 36 and 36 characters long", e.getMessage());
        }
    }
}