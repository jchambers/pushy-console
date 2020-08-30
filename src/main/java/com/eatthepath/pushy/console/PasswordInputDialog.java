/*
 * Copyright (c) 2020 Jon Chambers.
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

package com.eatthepath.pushy.console;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.function.Function;

/**
 * A simple password input dialog. Shamelessly borrowed from TextInputField.
 *
 * @author <a href="https://github.com/jchambers/">Jon Chambers</a>
 */
class PasswordInputDialog extends Dialog<String> {
    private final GridPane grid;

    private final Label label;
    private final PasswordField passwordField;

    private final Label incorrectPasswordLabel;

    PasswordInputDialog(final Function<String, Boolean> passwordVerificationFunction) {
        final DialogPane dialogPane = getDialogPane();
        dialogPane.contentTextProperty().addListener(o -> updateGrid());

        setTitle(PushyConsoleApplication.RESOURCE_BUNDLE.getString("password-dialog.title"));
        dialogPane.setHeaderText(PushyConsoleApplication.RESOURCE_BUNDLE.getString("password-dialog.header"));
        dialogPane.getStyleClass().add("text-input-dialog");
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        passwordField = new PasswordField();
        passwordField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(passwordField, Priority.ALWAYS);
        GridPane.setFillWidth(passwordField, true);

        label = createLabel(dialogPane.getContentText());
        label.textProperty().bind(dialogPane.contentTextProperty());

        incorrectPasswordLabel = createLabel(PushyConsoleApplication.RESOURCE_BUNDLE.getString("password-dialog.incorrect-password"));
        incorrectPasswordLabel.setVisible(false);
        incorrectPasswordLabel.textProperty().addListener(o -> updateGrid());

        grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.setAlignment(Pos.CENTER_LEFT);

        updateGrid();

        passwordField.textProperty().addListener((observable, oldValue, newValue) -> incorrectPasswordLabel.setVisible(false));

        dialogPane.lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            if (!passwordVerificationFunction.apply(passwordField.getText())) {
                event.consume();

                incorrectPasswordLabel.setVisible(true);
                passwordField.selectAll();
            }
        });

        setResultConverter((dialogButton) -> {
            final ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonBar.ButtonData.OK_DONE ? passwordField.getText() : null;
        });
    }

    private static Label createLabel(final String labelText) {
        final Label label = new Label(labelText);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMaxHeight(Double.MAX_VALUE);
        label.getStyleClass().add("content");
        label.setWrapText(true);
        label.setPrefWidth(360);
        label.setPrefWidth(Region.USE_COMPUTED_SIZE);

        return label;
    }

    private void updateGrid() {
        grid.getChildren().clear();

        grid.add(label, 0, 0);
        grid.add(passwordField, 1, 0);

        grid.add(incorrectPasswordLabel, 0, 1, 2, 1);
        getDialogPane().setContent(grid);

        Platform.runLater(passwordField::requestFocus);
    }
}
