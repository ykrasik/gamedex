/*
 * Copyright (C) 2014 Yevgeny Krasik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.ykrasik.indexter.games;

import com.github.ykrasik.indexter.games.debug.MetacriticDebugCommands;
import com.github.ykrasik.jerminal.api.filesystem.ShellFileSystem;
import com.github.ykrasik.jerminal.javafx.ConsoleBuilder;
import com.github.ykrasik.jerminal.javafx.ConsoleToggler;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * @author Yevgeny Krasik
 */
// FIXME: Delete this.
public class Sample extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        try {
            doStart(stage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() throws Exception {
    }

    private void doStart(Stage stage) throws IOException {
        final ShellFileSystem fileSystem = createFileSystem();

        final Parent console = new ConsoleBuilder(fileSystem).build();
        final Scene scene = new Scene(console, 1280, 720);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, new ConsoleToggler(console));

        stage.setTitle("Jerminal");
        stage.setScene(scene);
        stage.show();
    }

    private ShellFileSystem createFileSystem() {
        final ShellFileSystem fileSystem = new ShellFileSystem();
        return fileSystem.processAnnotations(MetacriticDebugCommands.class);
    }
}
