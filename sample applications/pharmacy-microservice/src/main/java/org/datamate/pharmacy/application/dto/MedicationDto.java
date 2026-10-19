package org.datamate.pharmacy.application.dto;

public class MedicationDto {
    private String id;
    private String name;
    private String drugClass; // e.g. OTC, PRESCRIPTION, CONTROLLED
    private int currentStock;
    private int minimumStockThreshold;

    public MedicationDto(String id, String name, String drugClass, int currentStock, int minimumStockThreshold) {
        this.id = id;
        this.name = name;
        this.drugClass = drugClass;
        this.currentStock = currentStock;
        this.minimumStockThreshold = minimumStockThreshold;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDrugClass() { return drugClass; }
    public void setDrugClass(String drugClass) { this.drugClass = drugClass; }
    public int getCurrentStock() { return currentStock; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }
    public int getMinimumStockThreshold() { return minimumStockThreshold; }
    public void setMinimumStockThreshold(int minimumStockThreshold) { this.minimumStockThreshold = minimumStockThreshold; }
}
