package org.eclipse.dataspaceconnector.iam.oauth2.core.rule;

import org.eclipse.dataspaceconnector.common.token.TokenValidationRulesRegistryImpl;
import org.eclipse.dataspaceconnector.iam.oauth2.core.Oauth2Configuration;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.Oauth2ValidationRulesRegistry;

/**
 * Registry for Oauth2 validation rules.
 */
public class Oauth2ValidationRulesRegistryImpl extends TokenValidationRulesRegistryImpl implements Oauth2ValidationRulesRegistry {

    public Oauth2ValidationRulesRegistryImpl(Oauth2Configuration configuration) {
        this.addRule(new Oauth2ValidationRule(configuration));
    }
}
