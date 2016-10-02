package com.github.ykrasik.gamedex.core;

import com.github.ykrasik.gamedex.common.debug.DebugCommands;
import com.github.ykrasik.gamedex.common.preloader.Preloader;
import com.github.ykrasik.gamedex.core.controller.ControllerProvider;
import com.github.ykrasik.gamedex.core.preloader.PreloaderImpl;
import com.github.ykrasik.gamedex.core.service.dialog.DialogService;
import com.github.ykrasik.gamedex.core.ui.UIResources;
import com.github.ykrasik.jaci.cli.javafx.JavaFxCliBuilder;
import com.github.ykrasik.jaci.cli.javafx.SceneToggler;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import java.util.Map;

/**
 * @author Yevgeny Krasik
 */
@Slf4j
public class Main extends Application {
    private static final int ELEMENTS_TO_LOAD = 8;

    private AbstractApplicationContext context;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage stage) throws Exception {
        final Preloader preloader = new PreloaderImpl(ELEMENTS_TO_LOAD);

        final Task<AbstractApplicationContext> task = new Task<AbstractApplicationContext>() {
            @Override
            protected AbstractApplicationContext call() throws InterruptedException {
                final AbstractApplicationContext context = createContext(stage, preloader);
                // A hack, but this message doesn't appear otherwise.
                preloader.message("Loading UI...");
                Thread.sleep(20);
                return context;
            }
        };

        preloader.start(task, context -> initStage(context, stage));
    }

    @Override
    public void stop() throws Exception {
        if (context != null) {
            context.stop();
        }
    }

    private AbstractApplicationContext createContext(Stage stage, Preloader preloader) {
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getBeanFactory().registerSingleton("stage", stage);
        context.getBeanFactory().registerSingleton("preloader", preloader);
        context.scan("com.github.ykrasik.gamedex");
        context.refresh();
        return context;
    }

    @SneakyThrows
    private void initStage(AbstractApplicationContext context, Stage stage) {
        this.context = context;
        final DialogService dialogService = context.getBean(DialogService.class);

        try {
            final ControllerProvider controllerProvider = context.getBean(ControllerProvider.class);
            final FXMLLoader loader = new FXMLLoader(UIResources.mainFxml());
            loader.setControllerFactory(controllerProvider::getController);
            final Parent root = loader.load();
            final Scene mainScene = new Scene(root);
            mainScene.getStylesheets().add(UIResources.mainCss());

            final Map<String, DebugCommands> debugCommands = context.getBeansOfType(DebugCommands.class);
            final JavaFxCliBuilder cliBuilder = new JavaFxCliBuilder();
            debugCommands.values().forEach(cliBuilder::process);
            final Parent debugConsole = cliBuilder.build();

            SceneToggler.register(stage, debugConsole);

            final Rectangle2D screen = Screen.getPrimary().getVisualBounds();

            stage.setWidth(screen.getWidth() * 0.9);
            stage.setHeight(screen.getHeight() * 0.8);
//            stage.setMaximized(true);
            stage.setTitle("GameDex");
            stage.setScene(mainScene);
            stage.show();
        } catch (Exception e) {
            dialogService.showException(e);
            stage.close();
            stop();
        }
    }
}
