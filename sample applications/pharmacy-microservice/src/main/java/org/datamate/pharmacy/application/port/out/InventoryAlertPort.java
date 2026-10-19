package org.datamate.pharmacy.application.port.out;

public interface InventoryAlertPort {
    void sendLowStockAlert(String medicationId, int currentStock);
}
