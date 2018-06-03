/*
 * Copyright (c) 2018 Turo
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
