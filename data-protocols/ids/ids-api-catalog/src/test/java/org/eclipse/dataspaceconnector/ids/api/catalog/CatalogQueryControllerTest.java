package org.eclipse.dataspaceconnector.ids.api.catalog;

import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.QueryLanguage;
import de.fraunhofer.iais.eis.QueryMessageBuilder;
import de.fraunhofer.iais.eis.QueryScope;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.daps.DapsService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.DataEntry;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;


class CatalogQueryControllerTest {

    private final QueryEngine queryEngine = mock(QueryEngine.class);
    private final DapsService dapsService = mock(DapsService.class);

    private final CatalogQueryController queryController = new CatalogQueryController(queryEngine, dapsService);

    @Test
    void queryShouldReturnListOfAssets() {
        ClaimToken claimToken = ClaimToken.Builder.newInstance().build();
        expect(dapsService.verifyAndConvertToken("token"))
                .andReturn(new VerificationResult(claimToken));
        DataEntry dataEntry = DataEntry.Builder.newInstance().id("anId").build();
        expect(queryEngine.execute(anyString(), eq(claimToken), anyString(), eq("queryLanguage"), eq("query")))
                .andReturn(Arrays.asList(dataEntry));
        replay(dapsService, queryEngine);
        var queryMessage = new QueryMessageBuilder()
                ._issuerConnector_(URI.create("http://localhost:8182"))
                ._securityToken_(new DynamicAttributeTokenBuilder()._tokenValue_("token").build())
                .build();
        queryMessage.setProperty("query", "query");
        queryMessage.setProperty("queryLanguage", "queryLanguage");

        var response = queryController.query(queryMessage);

        assertThat(response.getStatus()).isEqualTo(200);
        List<DataEntry> result = (List<DataEntry>) response.getEntity();
        assertThat(result).containsOnly(dataEntry);
    }
}