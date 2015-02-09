package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.debug.DebugCommands;
import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.indexter.games.controller.GameController;
import com.github.ykrasik.indexter.games.manager.exclude.ExcludedPathManager;
import com.github.ykrasik.indexter.games.manager.flow.FlowManager;
import com.github.ykrasik.indexter.games.manager.game.GameManager;
import com.github.ykrasik.indexter.games.manager.library.LibraryManager;
import com.github.ykrasik.jerminal.api.filesystem.ShellFileSystem;
import com.github.ykrasik.jerminal.javafx.ConsoleBuilder;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class UiBeanConfiguration extends AbstractBeanConfiguration {
    @Bean
    public Parent debugConsole(List<DebugCommands> debugCommands) throws IOException {
        preloader.setMessage("Loading debug console...");
        final ShellFileSystem fileSystem = new ShellFileSystem();
        debugCommands.forEach(fileSystem::processAnnotationsOfObject);
        return new ConsoleBuilder(fileSystem).build();
    }

    @Qualifier("gameCollection")
    @Bean
    public Parent gameCollection(GameController controller) throws IOException {
        preloader.setMessage("Loading FXML...");
        final URL resource = Objects.requireNonNull(getClass().getResource("/com/github/ykrasik/indexter/games/ui/fxml/games.fxml"));
        final FXMLLoader loader = new FXMLLoader(resource);
        loader.setController(controller);
        return loader.load();
    }

    @Bean
    public GameController gameCollectionController(Stage stage,
                                                   GameCollectionConfig config,
                                                   FlowManager flowManager,
                                                   GameManager gameManager,
                                                   LibraryManager libraryManager,
                                                   ExcludedPathManager excludedPathManager) {
        return new GameController(stage, config, flowManager, gameManager, libraryManager, excludedPathManager);
    }
}
