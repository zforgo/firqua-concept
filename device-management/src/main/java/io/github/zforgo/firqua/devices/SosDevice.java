package io.github.zforgo.firqua.devices;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import io.github.zforgo.firqua.assets.SosAsset;

@Entity
@DiscriminatorValue(value = "SOS")
@Table(name = "DEVICES_SOS_STATIONS")
public class SosDevice extends Device<SosAsset> {

    {
        type = DeviceType.SOS;
    }

    @Override
    public void setAsset(SosAsset asset) {
        super.setAsset(asset);
    }
}
