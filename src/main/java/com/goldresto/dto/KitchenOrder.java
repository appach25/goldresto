package com.goldresto.dto;

import java.util.List;
import java.util.ArrayList;

public class KitchenOrder {
    private String orderNumber;
    private String tableNumber;
    private String serverName;
    private List<KitchenOrderItem> items;

    public KitchenOrder() {
        this.items = new ArrayList<>();
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public List<KitchenOrderItem> getItems() {
        return items;
    }

    public void setItems(List<KitchenOrderItem> items) {
        this.items = items;
    }

    public void addItem(KitchenOrderItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }
}
