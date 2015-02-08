package com.github.ykrasik.indexter.games;

import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.indexter.games.controller.GameController;
import com.github.ykrasik.indexter.games.manager.exclude.ExcludedPathManager;
import com.github.ykrasik.indexter.games.manager.flow.FlowManager;
import com.github.ykrasik.indexter.games.manager.game.GameManager;
import com.github.ykrasik.indexter.games.manager.library.LibraryManager;
import com.github.ykrasik.jerminal.javafx.SceneToggler;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import java.io.IOException;

/**
 * @author Yevgeny Krasik
 */
public class IndexterGamesMain extends Application {
    private IndexterPreloader preloader;
    private AbstractApplicationContext context;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        preloader = new IndexterPreloader();
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
        final Task<AbstractApplicationContext> loadContextTask = new Task<AbstractApplicationContext>() {
            @Override
            protected AbstractApplicationContext call() throws InterruptedException {
                final AbstractApplicationContext context = createContext(mainStage, preloader);
                final GameController controller = context.getBean(GameController.class);

                // TODO: what a hack... This is me performing work that should be done by spring :/
                controller.setDependencies(
                    context.getBean(Stage.class),
                    context.getBean(GameCollectionConfig.class),
                    context.getBean(FlowManager.class),
                    context.getBean(GameManager.class),
                    context.getBean(LibraryManager.class),
                    context.getBean(ExcludedPathManager.class)
                );
                return context;
            }
        };

        // FIXME: Should create a new stage on the background thread, while the preloader is loading.
        preloader.start(loadContextTask, context -> {
            this.context = context;
            final Scene mainScene = new Scene(context.getBean("gameCollection", Parent.class));
            final Parent debugConsole = context.getBean("debugConsole", Parent.class);
            initStage(mainStage, mainScene, debugConsole);
            mainStage.show();
        });
    }

    private void initStage(Stage stage, Scene mainScene, Parent debugConsole) {
        mainScene.getStylesheets().add("com/github/ykrasik/indexter/games/ui/css/games.css");

        // FIXME: Should be done in spring, inject configurations with a properties object.
        stage.setWidth(1280);
        stage.setHeight(720);
        stage.setMaximized(true);
        stage.setTitle("inDexter");
        stage.setScene(mainScene);

        SceneToggler.register(stage, debugConsole);
    }

    private AbstractApplicationContext createContext(Stage stage, IndexterPreloader preloader) {
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getBeanFactory().registerSingleton("stage", stage);
        context.getBeanFactory().registerSingleton("preloader", preloader);
        context.scan("com.github.ykrasik.indexter");
        context.refresh();
        return context;
    }
}
