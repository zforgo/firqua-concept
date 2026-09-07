package io.github.zforgo.firqua.devices;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import io.github.zforgo.firqua.assets.MeteoAsset;

@Entity
@DiscriminatorValue(value = "METEO")
@Table(name = "DEVICES_METEO_SENSORS")
public class MeteoSensorDevice extends Device<MeteoAsset> {

    {
        type = DeviceType.METEO;
    }

    @Override
    public void setAsset(MeteoAsset asset) {
        super.setAsset(asset);
    }
}
