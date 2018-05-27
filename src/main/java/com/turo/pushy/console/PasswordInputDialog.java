package com.turo.pushy.console;

import com.sun.javafx.scene.control.skin.resources.ControlResources;
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
 */
class PasswordInputDialog extends Dialog<String> {
    private final GridPane grid;

    private final Label label;
    private final PasswordField passwordField;

    private final Label incorrectPasswordLabel;

    PasswordInputDialog(final Function<String, Boolean> passwordVerificationFunction) {
        final DialogPane dialogPane = getDialogPane();
        dialogPane.contentTextProperty().addListener(o -> updateGrid());

        setTitle(ControlResources.getString("Dialog.confirm.title"));
        dialogPane.setHeaderText(ControlResources.getString("Dialog.confirm.header"));
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
