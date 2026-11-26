package io.github.zforgo.firqua.devices.events;

import jakarta.enterprise.context.ApplicationScoped;

import io.github.zforgo.firqua.TestEventObserver;

@ApplicationScoped
class TestStateEventObserver extends TestEventObserver<DeviceMonitoringStateChanged> {}
