package io.github.zforgo.firqua.devices;

import java.util.UUID;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

import io.github.zforgo.firqua.assets.AssetAdminService;
import io.github.zforgo.firqua.assets.AssetCreateDto;
import io.github.zforgo.firqua.assets.SosAssetCreateDto;
import io.github.zforgo.firqua.test.liquibase.LiquibaseMigration;

import static io.github.zforgo.firqua.test.liquibase.RunMode.PER_CLASS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

@QuarkusTest
@LiquibaseMigration(runMode = PER_CLASS, dropFirst = true)
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EffectiveStateChangeServiceTest {

    @Inject
    AssetAdminService assetService;

    @Inject
    DeviceService deviceService;

    @ParameterizedTest
    @MethodSource("createParams")
    @DisplayName("Effective enabled flag calculated properly during creation")
    public void create(AssetCreateDto assetDto, DeviceCreateDto deviceDto, boolean expected) {
        var asset = assetService.createAsset(assetDto);
        deviceDto.assetId = asset.id;
        var device = deviceService.createDevice(deviceDto);

        assertEquals(expected, device.effectiveEnabled);
    }

    @ParameterizedTest
    @MethodSource("deviceToggleParams")
    public void toggleDevice(AssetCreateDto assetDto, DeviceCreateDto deviceDto, boolean expected) {
        var asset = assetService.createAsset(assetDto);
        deviceDto.assetId = asset.id;
        var device = deviceService.createDevice(deviceDto);
        deviceDto.enabled = !deviceDto.enabled;
        device = deviceService.updateDevice(device.id, deviceDto);
        assertEquals(expected, device.effectiveEnabled);
    }

    @ParameterizedTest
    @MethodSource("assetToggleParams")
    public void toggleAsset(AssetCreateDto assetDto, DeviceCreateDto deviceDto, boolean expected) {
        var asset = assetService.createAsset(assetDto);
        deviceDto.assetId = asset.id;
        var device = deviceService.createDevice(deviceDto);

        assetDto.enabled = !assetDto.enabled;
        assetService.updateAsset(asset.id, assetDto);

        device = deviceService.getById(device.id);
        assertEquals(expected, device.effectiveEnabled);
    }

    static Stream<Arguments> assetToggleParams() {
        return Stream.of(
                argumentSet(
                        "Disabling Asset of a disabled Device, then Device should be enabled disabled",
                        supplyAsset(true), supplyDevice(false), false
                ),
                argumentSet(
                        "Disabling Asset of an enabled Device, then Device should be enabled disabled",
                        supplyAsset(true), supplyDevice(true), false
                ),
                argumentSet(
                        "Enabling Asset of a disabled Device, then Device should be enabled disabled",
                        supplyAsset(false), supplyDevice(false), false
                ),
                argumentSet(
                        "Enabling Asset of an enabled Device, then Device should be enabled enabled",
                        supplyAsset(false), supplyDevice(true), true
                )
        );
    }

    static Stream<Arguments> deviceToggleParams() {
        return Stream.of(
                argumentSet(
                        "Enabling Device with an enabled Asset should be enabled enabled",
                        supplyAsset(true), supplyDevice(false), true
                ),
                argumentSet(
                        "Disabling Device with an enabled Asset should be enabled disabled",
                        supplyAsset(true), supplyDevice(true), false
                ),
                argumentSet(
                        "Enabling Device with a disabled Asset should be enabled disabled",
                        supplyAsset(false), supplyDevice(false), false
                ),
                argumentSet(
                        "Disabling Device with a disabled Asset should be enabled disabled",
                        supplyAsset(false), supplyDevice(true), false
                )
        );
    }

    static Stream<Arguments> createParams() {
        return Stream.of(
                argumentSet(
                        "Enabled Asset created with disabled Device should be disabled",
                        supplyAsset(true), supplyDevice(false), false
                ),
                argumentSet(
                        "Enabled Asset created with enabled Device should be enabled",
                        supplyAsset(true), supplyDevice(true), true
                ),
                argumentSet(
                        "Disabled Asset created with disabled Device should be disabled",
                        supplyAsset(false), supplyDevice(false), false
                ),
                argumentSet(
                        "Disabled Asset created with enabled Device should be disabled",
                        supplyAsset(false), supplyDevice(true), false
                )
        );
    }

    private static SosAssetCreateDto supplyAsset(boolean enabled) {
        var dto = new SosAssetCreateDto();
        dto.stationId = UUID.randomUUID().toString();
        dto.name = UUID.randomUUID().toString();
        dto.serviceProvider = "AT&T";
        dto.vendor = "J&R Technology";
        dto.model = "JR321-SC";
        dto.enabled = enabled;
        dto.setIpAddress("192.168.1.%d".formatted(System.nanoTime() % 250));
        return dto;
    }

    private static SosDeviceCreateDto supplyDevice(boolean enabled) {
        var dto = new SosDeviceCreateDto();
        dto.organisationId = 1L;
        dto.enabled = enabled;
        dto.name = UUID.randomUUID().toString();
        return dto;
    }
}
