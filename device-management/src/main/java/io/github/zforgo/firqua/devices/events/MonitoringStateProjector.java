package io.github.zforgo.firqua.devices.events;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@ApplicationScoped
public class MonitoringStateProjector {

    @Transactional(TxType.MANDATORY)
    void handleInternalChange(@Observes DeviceStateChanged event) {
        switch (event.eventType()) {
            case CREATED,
                    UPDATED ->
                exportEvent(event.deviceId());
            case REMOVED -> {
                //TODO implement later (fire exportedEvent without payload to remove tombstones
            }
        }
    }

    private void exportEvent(Long deviceId) {
        //TODO implement later
    }
}
