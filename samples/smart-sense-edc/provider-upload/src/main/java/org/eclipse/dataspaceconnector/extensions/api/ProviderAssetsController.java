package org.eclipse.dataspaceconnector.extensions.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.AssetApi;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetEntryDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.DataAddressDto;
import org.eclipse.dataspaceconnector.api.exception.ObjectExistsException;
import org.eclipse.dataspaceconnector.api.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;

@Path("/assets")
public class ProviderAssetsController implements AssetApi {

    private final Monitor monitor;
    private final S3Configurations s3Configurations;
    private final AssetServiceImplementation service;
    private final DtoTransformerRegistry transformerRegistry;

    public ProviderAssetsController(Monitor monitor,
                                    S3Configurations s3Configurations,
                                    AssetServiceImplementation service,
                                    DtoTransformerRegistry transformerRegistry) {
        this.monitor = monitor;
        this.s3Configurations = s3Configurations;
        this.service = service;
        this.transformerRegistry = transformerRegistry;
    }

    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @POST
    public void createAssets() {
        System.out.println("--------------------------------");
        S3Client client = s3Configurations.provide(null, null);
        String key = UUID.randomUUID().toString() + ".pdf";
        client.putObject(
                PutObjectRequest.builder().bucket("smartsense-edc-provider").key(key).build(),
                RequestBody.fromFile(new File("/home/dilip/Desktop/DemoProject/edc/smartsense-edc/samples/smart-sense-edc/api/src/main/resources/providerDocuments/Document_1.pdf"))
        );
        DataAddressDto dataAddressDto = DataAddressDto.Builder.newInstance().properties(Map.of("addressDTO", key, "type", "amazonS3", "keyName", key)).build();

        /**
         *
         * public static final String PROPERTY_ID = "asset:prop:id";
         *     public static final String PROPERTY_NAME = "asset:prop:name";
         *     public static final String PROPERTY_DESCRIPTION = "asset:prop:description";
         *     public static final String PROPERTY_VERSION = "asset:prop:version";
         *     public static final String PROPERTY_CONTENT_TYPE = "asset:prop:contenttype";
         */
        AssetDto assetDto = AssetDto.Builder.newInstance().properties(Map.of("assetDTO", "key", "asset:prop:id", UUID.randomUUID().toString())).build();
        createAsset(AssetEntryDto.Builder.newInstance().asset(assetDto).dataAddress(dataAddressDto).build());
        System.out.println("------------------------------");
    }

    @Override
    public void createAsset(AssetEntryDto assetEntryDto) {
        System.out.println("===============================");
        var assetResult = transformerRegistry.transform(assetEntryDto.getAsset(), Asset.class);
        var dataAddressResult = transformerRegistry.transform(assetEntryDto.getDataAddress(), DataAddress.class);
        if (assetResult.failed() || dataAddressResult.failed()) {
            throw new IllegalArgumentException("Request is not well formatted");
        }
        var dataAddress = dataAddressResult.getContent();
        var asset = assetResult.getContent();
        var result = service.create(asset, dataAddress);
        if (result.succeeded()) {
            monitor.debug(format("Asset created %s", assetEntryDto.getAsset()));
            monitor.debug(format("=============== %s", asset.getId()));
        } else {
            handleFailedResult(result, asset.getId());
        }
    }

    @Override
    public List<AssetDto> getAllAssets(Integer offset, Integer limit, String filterExpression, SortOrder sortOrder, String sortField) {
        return null;
    }

    @Override
    public AssetDto getAsset(String id) {
        return null;
    }

    @Override
    public void removeAsset(String id) {

    }

    private void handleFailedResult(ServiceResult<Asset> result, String id) {
        switch (result.reason()) {
            case NOT_FOUND:
                throw new ObjectNotFoundException(Asset.class, id);
            case CONFLICT:
                throw new ObjectExistsException(Asset.class, id);
            default:
                throw new EdcException("unexpected error");
        }
    }
}