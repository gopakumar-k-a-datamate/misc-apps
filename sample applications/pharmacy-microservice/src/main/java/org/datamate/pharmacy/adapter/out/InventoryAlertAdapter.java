package org.datamate.pharmacy.adapter.out;

import org.datamate.pharmacy.application.port.out.InventoryAlertPort;
import org.springframework.stereotype.Component;
import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;

@Component
public class InventoryAlertAdapter implements InventoryAlertPort {

    @EnableLogger
    private Logger log;

    @Override
    public void sendLowStockAlert(String medicationId, int currentStock) {
        // Simulates sending a message to a Kafka topic or calling an external API
        log.error("!!! INVENTORY ALERT !!! Medication {} is running low. Current Stock: {}. Procurement team has been notified automatically.", 
                medicationId, currentStock);
    }
}
