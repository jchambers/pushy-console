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
public class PasswordInputDialog extends Dialog<String> {
    private final GridPane grid;

    private final Label label;
    private final PasswordField passwordField;

    private final Label incorrectPasswordLabel;

    private final Function<String, Boolean> passwordVerificationFunction;

    public PasswordInputDialog() {
        this((password) -> true);
    }

    public PasswordInputDialog(final Function<String, Boolean> passwordVerificationFunction) {
        this.passwordVerificationFunction = passwordVerificationFunction;

        final DialogPane dialogPane = getDialogPane();
        dialogPane.contentTextProperty().addListener(o -> updateGrid());

        // TODO Localize
        setTitle(ControlResources.getString("Dialog.confirm.title"));
        dialogPane.setHeaderText(ControlResources.getString("Dialog.confirm.header"));
        dialogPane.getStyleClass().add("text-input-dialog");
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        this.passwordField = new PasswordField();
        this.passwordField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(passwordField, Priority.ALWAYS);
        GridPane.setFillWidth(passwordField, true);

        this.label = createLabel(dialogPane.getContentText());
        this.label.textProperty().bind(dialogPane.contentTextProperty());

        // TODO Localize
        this.incorrectPasswordLabel = createLabel("The password you entered is incorrect. Please try again.");
        this.incorrectPasswordLabel.setVisible(false);

        this.grid = new GridPane();
        this.grid.setHgap(10);
        this.grid.setMaxWidth(Double.MAX_VALUE);
        this.grid.setAlignment(Pos.CENTER_LEFT);

        updateGrid();

        this.passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            this.incorrectPasswordLabel.setVisible(false);
        });

        dialogPane.lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            if (!passwordVerificationFunction.apply(this.passwordField.getText())) {
                event.consume();

                this.incorrectPasswordLabel.setVisible(true);
                this.passwordField.selectAll();
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
        this.grid.getChildren().clear();

        this.grid.add(this.label, 0, 0);
        this.grid.add(this.passwordField, 1, 0);

        this.grid.add(this.incorrectPasswordLabel, 0, 1, 2, 1);
        getDialogPane().setContent(this.grid);

        Platform.runLater(this.passwordField::requestFocus);
    }
}
