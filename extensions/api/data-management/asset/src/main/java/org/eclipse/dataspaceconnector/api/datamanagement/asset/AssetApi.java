package org.eclipse.dataspaceconnector.api.datamanagement.asset;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.AssetEntryDto;
import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.TransferRequestDto;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;

import java.util.List;

@OpenAPIDefinition
public interface AssetApi {

    void createAsset(AssetEntryDto assetEntryDto);

    List<AssetDto> getAllAssets(Integer offset, Integer limit, String filterExpression, SortOrder sortOrder, String sortField);

    AssetDto getAsset(String id);

    void removeAsset(String id);

}
