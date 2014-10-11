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

import com.github.ykrasik.indexter.games.data.GameDataService;
import com.github.ykrasik.indexter.games.debug.DataServiceDebugCommands;
import com.github.ykrasik.indexter.games.debug.GiantBombDebugCommands;
import com.github.ykrasik.indexter.games.debug.MetacriticDebugCommands;
import com.github.ykrasik.indexter.games.debug.UiDebugCommands;
import com.github.ykrasik.indexter.games.spring.BeanConfiguration;
import com.github.ykrasik.indexter.games.spring.DebugBeanConfiguration;
import com.github.ykrasik.jerminal.api.filesystem.ShellFileSystem;
import com.github.ykrasik.jerminal.javafx.ConsoleBuilder;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
public class IndexterGamesMain extends Application {
    private AbstractApplicationContext context;

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
        context.stop();
    }

    private void doStart(Stage stage) throws IOException {
        final URL resource = Objects.requireNonNull(getClass().getResource("main.fxml"));
        final FXMLLoader loader = new FXMLLoader(resource);
        final Parent mainWindow = loader.load();
        final MainWindowController controller = loader.getController();
        final TilePane gameWall = controller.getGameWall();
        final VBox gameDetails = controller.getGameDetails();
        final Scene mainScene = new Scene(mainWindow);

        populateGameWall(gameWall);

        context = new AnnotationConfigApplicationContext(BeanConfiguration.class, DebugBeanConfiguration.class);

        final BorderPane debugConsole = createDebugConsole(gameWall);
        final Scene debugScene = new Scene(debugConsole);

        stage.setWidth(1280);
        stage.setHeight(720);
        stage.setTitle("inDexter");
        stage.setScene(mainScene);
        stage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.BACK_QUOTE) {
                final Scene currentScene = stage.getScene();
                final Scene nextScene;
                if (currentScene == mainScene) {
                    nextScene = debugScene;
                } else {
                    nextScene = mainScene;
                }
                stage.setScene(nextScene);
            }
        });

        stage.show();
    }

    private BorderPane createDebugConsole(TilePane gameWall) throws IOException {
        final GameDataService dataService = context.getBean(GameDataService.class);

        final ShellFileSystem fileSystem = new ShellFileSystem();
        fileSystem.processAnnotationsOfObject(context.getBean(MetacriticDebugCommands.class));
        fileSystem.processAnnotationsOfObject(context.getBean(DataServiceDebugCommands.class));
        fileSystem.processAnnotationsOfObject(context.getBean(GiantBombDebugCommands.class));
        fileSystem.processAnnotationsOfObject(new UiDebugCommands(dataService, gameWall));

        return (BorderPane) new ConsoleBuilder(fileSystem).build();
    }

    private void populateGameWall(TilePane gameWall) {
        final ObservableList<Node> children = gameWall.getChildren();
        for (int i = 0; i < 1; i++) {
            children.addAll(
                new ImageView(new Image(getClass().getResourceAsStream("9afe8989b56577f6b27528c9b65dbde3-98.jpg"))),
                new ImageView(new Image(getClass().getResourceAsStream("c02ae1470f8b5ab5a0ee91a77c7aa7b8-98.jpg")))
            );
        }
    }
}
