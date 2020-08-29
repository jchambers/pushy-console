module pushy.console {
    requires java.prefs;

    requires bcprov.jdk15on;
    requires bcpkix.jdk15on;

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;

    requires gson;

    requires org.apache.commons.lang3;

    requires pushy;

    exports com.eatthepath.pushy.console;
}
