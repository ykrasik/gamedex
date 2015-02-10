package com.github.ykrasik.gamedex.core;

import com.github.ykrasik.gamedex.common.preloader.Preloader;
import com.github.ykrasik.gamedex.ui.preloader.PreloaderImpl;
import com.github.ykrasik.jerminal.javafx.SceneToggler;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import java.io.IOException;

/**
 * @author Yevgeny Krasik
 */
@Slf4j
public class Main extends Application {
    private Preloader preloader;
    private AbstractApplicationContext context;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        preloader = new PreloaderImpl();
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
        if (context != null) {
            context.stop();
        }
    }

    private void doStart(final Stage mainStage) throws IOException {
        final Task<AbstractApplicationContext> task = new Task<AbstractApplicationContext>() {
            @Override
            protected AbstractApplicationContext call() throws InterruptedException {
                return createContext(mainStage, preloader);
            }
        };

        // FIXME: Should create a new stage on the background thread, while the preloader is loading.
        preloader.start(task, context -> {
            this.context = context;
            final Scene mainScene = new Scene(context.getBean("gameCollection", Parent.class));
            final Parent debugConsole = context.getBean("debugConsole", Parent.class);
            initStage(mainStage, mainScene, debugConsole);
            mainStage.show();
        });
    }

    private void initStage(Stage stage, Scene mainScene, Parent debugConsole) {
        mainScene.getStylesheets().add("com/github/ykrasik/gamedex/ui/css/main.css");

        // FIXME: Should be done in spring, inject configurations with a properties object.
        stage.setWidth(1280);
        stage.setHeight(720);
        stage.setMaximized(true);
        stage.setTitle("GameDex");
        stage.setScene(mainScene);

        SceneToggler.register(stage, debugConsole);
    }

    private AbstractApplicationContext createContext(Stage stage, Preloader preloader) {
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getBeanFactory().registerSingleton("stage", stage);
        context.getBeanFactory().registerSingleton("preloader", preloader);
        context.scan("com.github.ykrasik.gamedex");
        context.refresh();
        return context;
    }
}
