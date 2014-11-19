package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.debug.DebugCommands;
import com.github.ykrasik.indexter.games.IndexterPreloader;
import com.github.ykrasik.indexter.games.config.GameCollectionConfig;
import com.github.ykrasik.indexter.games.controller.GameCollectionController;
import com.github.ykrasik.indexter.games.data.GameDataService;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.games.library.LibraryManager;
import com.github.ykrasik.jerminal.api.filesystem.ShellFileSystem;
import com.github.ykrasik.jerminal.javafx.ConsoleBuilder;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private IndexterPreloader preloader;

    @Bean
    public Parent debugConsole(List<DebugCommands> debugCommands) throws IOException {
        preloader.setMessage("Instantiating debug console...");
        final ShellFileSystem fileSystem = new ShellFileSystem();
        debugCommands.forEach(fileSystem::processAnnotationsOfObject);
        return new ConsoleBuilder(fileSystem).build();
    }

    @Qualifier("gameCollection")
    @Bean
    public Parent gameCollection(GameCollectionController controller) throws IOException {
        preloader.setMessage("Loading FXML...");
        final URL resource = Objects.requireNonNull(getClass().getResource("/com/github/ykrasik/indexter/games/ui/fxml/games.fxml"));
        final FXMLLoader loader = new FXMLLoader(resource);
        loader.setController(controller);
        return loader.load();
    }

    @Bean
    public GameCollectionController gameCollectionController(Stage stage,
                                                             GameCollectionConfig config,
                                                             LibraryManager libraryManager,
                                                             GameInfoService infoService,
                                                             GameDataService dataService) {
        preloader.setMessage("Creating controller...");
        return new GameCollectionController(stage, config, libraryManager, infoService, dataService);
    }
}
