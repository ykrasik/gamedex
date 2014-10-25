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

import com.github.ykrasik.indexter.games.spring.DataBeanConfiguration;
import com.github.ykrasik.indexter.games.spring.ProviderBeanConfiguration;
import com.github.ykrasik.indexter.games.spring.DebugBeanConfiguration;
import com.github.ykrasik.indexter.games.spring.UiBeanConfiguration;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import java.io.IOException;

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
        context = createContext(stage);

        final Scene mainScene = context.getBean("gameCollectionScene", Scene.class);
        final Scene debugConsoleScene = context.getBean("debugConsoleScene", Scene.class);

        initStage(stage, mainScene, debugConsoleScene);

        stage.show();
    }

    private void initStage(Stage stage, Scene mainScene, Scene debugConsoleScene) {
        mainScene.getStylesheets().add("com/github/ykrasik/indexter/games/ui/games.css");

        // FIXME: Should be done in spring, inject configurations with a properties object.
        stage.setWidth(1280);
        stage.setHeight(720);
        stage.setTitle("inDexter");
        stage.setScene(mainScene);
        stage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.BACK_QUOTE) {
                final Scene currentScene = stage.getScene();
                final Scene nextScene;
                if (currentScene == mainScene) {
                    nextScene = debugConsoleScene;
                } else {
                    nextScene = mainScene;
                }
                stage.setScene(nextScene);
            }
        });
    }

    private AbstractApplicationContext createContext(Stage stage) {
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getBeanFactory().registerSingleton("stage", stage);
        context.register(
            DataBeanConfiguration.class,
            ProviderBeanConfiguration.class,
            UiBeanConfiguration.class,
            DebugBeanConfiguration.class
        );
        context.refresh();
        return context;
    }
}
