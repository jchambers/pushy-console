module pushy.console {
    requires java.prefs;

    requires bcprov.jdk15on;
    requires bcpkix.jdk15on;

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;

    requires org.apache.commons.lang3;

    requires pushy;

    opens com.eatthepath.pushy.console to javafx.fxml;
    exports com.eatthepath.pushy.console;
}
