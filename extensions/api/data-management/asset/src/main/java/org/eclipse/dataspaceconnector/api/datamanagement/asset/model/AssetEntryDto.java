package org.eclipse.dataspaceconnector.api.datamanagement.asset.model;

public class AssetEntryDto {

    private AssetDto asset;
    private DataAddress dataAddressDto;

    public AssetEntryDto(AssetDto asset, DataAddress dataAddressDto){
        this.asset = asset;
        this.dataAddressDto = dataAddressDto;
    }

    public AssetDto getAsset() {
        return asset;
    }

    public void setAsset(AssetDto asset) {
        this.asset = asset;
    }

    public DataAddress getDataAddressDto() {
        return dataAddressDto;
    }

    public void setDataAddressDto(DataAddress dataAddressDto) {
        this.dataAddressDto = dataAddressDto;
    }
}
