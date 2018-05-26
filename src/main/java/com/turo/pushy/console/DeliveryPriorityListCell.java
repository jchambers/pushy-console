package com.turo.pushy.console;

import com.turo.pushy.apns.DeliveryPriority;
import javafx.scene.control.ListCell;

import java.util.ResourceBundle;

class DeliveryPriorityListCell extends ListCell<DeliveryPriority> {

    @Override
    public void updateItem(final DeliveryPriority deliveryPriority, final boolean empty) {
        super.updateItem(deliveryPriority, empty);

        if (!empty) {
            final ResourceBundle resourceBundle = PushyConsoleApplication.RESOURCE_BUNDLE;

            setText(deliveryPriority == DeliveryPriority.IMMEDIATE ?
                    resourceBundle.getString("delivery-priority.immediate") :
                    resourceBundle.getString("delivery-priority.conserve-power"));
        } else {
            setText(null);
        }
    }
}
