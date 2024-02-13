package org.eclipse.edc.identitytrust;

import org.eclipse.edc.identitytrust.model.VerifiableCredential;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;

import java.util.List;
import java.util.function.Function;

/**
 * This is a marker interface for functions that convert a {@link List<VerifiableCredential>} to a ClaimToken.
 * Implementors decide which information should be extracted from a {@link VerifiableCredential} and how it should be represented
 * inside the {@link ClaimToken}.
 * For example, an implementor could choose to simply attach the list of credentials to the ClaimToken using a reasonable key.
 */
public interface ClaimTokenCreatorFunction extends Function<List<VerifiableCredential>, Result<ClaimToken>> {
}
