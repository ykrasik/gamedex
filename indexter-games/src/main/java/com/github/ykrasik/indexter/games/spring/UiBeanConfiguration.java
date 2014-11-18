package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.debug.DebugCommands;
import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.indexter.games.controller.GameCollectionController;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.games.data.GameDataService;
import com.github.ykrasik.indexter.games.library.LibraryManager;
import com.github.ykrasik.jerminal.api.filesystem.ShellFileSystem;
import com.github.ykrasik.jerminal.javafx.ConsoleBuilder;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
public class UiBeanConfiguration {
    @Bean
    public Parent debugConsole(List<DebugCommands> debugCommands) throws IOException {
        final ShellFileSystem fileSystem = new ShellFileSystem();
        debugCommands.forEach(fileSystem::processAnnotationsOfObject);
        return new ConsoleBuilder(fileSystem).build();
    }

    @Qualifier("gameCollection")
    @Bean
    public Parent gameCollection(GameCollectionController controller) throws IOException {
        final URL resource = Objects.requireNonNull(getClass().getResource("/com/github/ykrasik/indexter/games/ui/fxml/games.fxml"));
        final FXMLLoader loader = new FXMLLoader(resource);
        loader.setController(controller);
        return loader.load();
    }

    // TODO: Is this too much?
    @Qualifier("gameCollectionScene")
    @Bean
    public Scene gameCollectionScene(@Qualifier("gameCollection") Parent gameCollection) {
        return new Scene(gameCollection);
    }

    @Bean
    public GameCollectionController gameCollectionController(Stage stage,
                                                             GameCollectionConfig config,
                                                             LibraryManager libraryManager,
                                                             GameInfoService infoService,
                                                             GameDataService dataService) {
        return new GameCollectionController(stage, config, libraryManager, infoService, dataService);
    }
}
