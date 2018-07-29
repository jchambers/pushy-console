/*
 * Copyright (c) 2018 Turo Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
