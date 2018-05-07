package com.turo.pushy.console;

import com.turo.pushy.apns.DeliveryPriority;
import javafx.scene.control.ListCell;

public class DeliveryPriorityListCell extends ListCell<DeliveryPriority> {

    @Override
    public void updateItem(final DeliveryPriority deliveryPriority, final boolean empty) {
        super.updateItem(deliveryPriority, empty);

        if (!empty) {
            // TODO Localize
            setText(deliveryPriority == DeliveryPriority.IMMEDIATE ? "Immediate" : "Conserve power");
        } else {
            setText(null);
        }
    }
}
