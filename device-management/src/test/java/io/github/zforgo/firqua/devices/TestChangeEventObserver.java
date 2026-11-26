package io.github.zforgo.firqua.devices;

import jakarta.enterprise.context.ApplicationScoped;

import io.github.zforgo.firqua.TestEventObserver;
import io.github.zforgo.firqua.devices.events.DeviceStateChanged;

@ApplicationScoped
class TestChangeEventObserver extends TestEventObserver<DeviceStateChanged> {}
