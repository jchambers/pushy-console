package com.turo.pushy.console;

import javafx.application.Platform;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * A test rule that ensures that JavaFX tests run on the main UI thread.
 *
 * @see <a href="http://andrewtill.blogspot.com/2012/10/junit-rule-for-javafx-controller-testing.html">WONTFIX: JUnit
 * Rule for JavaFX Controller Testing</a>
 */
public class JavaFXThreadRule implements TestRule {

    @Override
    public Statement apply(final Statement statement, final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                final CompletableFuture<Void> statementFuture = new CompletableFuture<>();

                Platform.runLater(() -> {
                    try {
                        statement.evaluate();
                        statementFuture.complete(null);
                    } catch (final Throwable throwable) {
                        statementFuture.completeExceptionally(throwable);
                    }
                });

                try {
                    statementFuture.get();
                } catch (final ExecutionException e) {
                    throw e.getCause();
                }
            }
        };
    }
}
