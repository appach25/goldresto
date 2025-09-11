package com.goldresto.dto;

public class KitchenOrderItem {
    private int quantity;
    private String name;
    private String notes;

    public KitchenOrderItem(int quantity, String name, String notes) {
        this.quantity = quantity;
        this.name = name;
        this.notes = notes;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
