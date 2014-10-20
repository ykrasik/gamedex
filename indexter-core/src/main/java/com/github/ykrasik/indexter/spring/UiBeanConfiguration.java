package com.github.ykrasik.indexter.spring;

import com.github.ykrasik.indexter.debug.DebugCommands;
import com.github.ykrasik.jerminal.api.filesystem.ShellFileSystem;
import com.github.ykrasik.jerminal.javafx.ConsoleBuilder;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
@Configuration
public class UiBeanConfiguration {
    @Qualifier("mainScene")
    @Bean
    public Scene mainScene(@Qualifier("gameCollection") Parent mainWindow) throws IOException {
        return new Scene(mainWindow);
    }

    @Qualifier("debugConsoleScene")
    @Bean
    public Scene debugConsole(List<DebugCommands> debugCommands) throws IOException {
        final ShellFileSystem fileSystem = new ShellFileSystem();
        debugCommands.forEach(fileSystem::processAnnotationsOfObject);
        final Parent debugConsole = new ConsoleBuilder(fileSystem).build();
        return new Scene(debugConsole);
    }
}
