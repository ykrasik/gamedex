package com.github.ykrasik.indexter.games.logic;

import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.GameRawBriefInfo;
import com.github.ykrasik.indexter.util.FileUtils;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.DialogAction;
import org.controlsfx.dialog.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.FutureTask;

/**
 * @author Yevgeny Krasik
 */
public class DialogChoiceProvider implements ChoiceProvider {
    private static final Logger LOG = LoggerFactory.getLogger(DialogChoiceProvider.class);

    private final Stage stage;

    public DialogChoiceProvider(Stage stage) {
        this.stage = Objects.requireNonNull(stage);
    }

    @Override
    public NoSearchResultsChoice getNoMetacriticSearchResultsChoice(Path path, String name, GamePlatform platform) throws Exception {
        final DialogAction newName = new DialogAction("New name");
        newName.setLongText("Retry with a new name");

        final DialogAction exclude = new DialogAction("Exclude");
        exclude.setLongText("Exclude directory from further processing");

        final Dialogs dialog = Dialogs.create()
            .owner(stage)
            .title("No Metacritic search results found!")
            .masthead(path.toString())
            .message(String.format("No Metacritic search results found: '%s'", name));

        // Dialog must be displayed on JavaFx thread.
        LOG.debug("Showing no metacritic search results dialog...");
        final FutureTask<Action> futureChoice = new FutureTask<>(() -> dialog.showCommandLinks(newName, exclude));
        Platform.runLater(futureChoice);
        final Action choice = futureChoice.get();

        if (choice == Dialog.ACTION_CANCEL) {
            LOG.debug("Dialog cancelled.");
            return NoSearchResultsChoice.SKIP;
        }
        if (choice == newName) {
            LOG.debug("New name requested.");
            return NoSearchResultsChoice.NEW_NAME;
        }
        if (choice == exclude) {
            LOG.debug("Exclude requested.");
            return NoSearchResultsChoice.EXCLUDE;
        }
        throw new IndexterException("Invalid choice: %s", choice);
    }

    @Override
    public MultipleSearchResultsChoice getMultipleMetacriticSearchResultsChoice(Path path,
                                                                                String name,
                                                                                GamePlatform platform,
                                                                                List<GameRawBriefInfo> briefInfos) throws Exception {
        final DialogAction chooseOne = new DialogAction("Choose");
        chooseOne.setLongText("Choose from the search results");

        final DialogAction newName = new DialogAction("New name");
        newName.setLongText("Retry with a new name");

        final DialogAction exclude = new DialogAction("Exclude");
        exclude.setLongText("Exclude directory from further processing");

        final DialogAction designateSubLibrary = new DialogAction("Designate as sub-library");
        designateSubLibrary.setLongText("Scan this directory's children");

        final List<DialogAction> choices = new ArrayList<>();
        choices.add(chooseOne);
        choices.add(newName);
        choices.add(exclude);
        if (FileUtils.hasChildDirectories(path)) {
            choices.add(designateSubLibrary);
        }

        final Dialogs dialog = Dialogs.create()
            .owner(stage)
            .title("Too many Metacritic search results!")
            .masthead(path.toString())
            .message(String.format("Found %d Metacritic search results for '%s':", briefInfos.size(), name));

        // Dialog must be displayed on JavaFx thread.
        LOG.debug("Showing multiple search result dialog...");
        final FutureTask<Action> futureChoice = new FutureTask<>(() -> dialog.showCommandLinks(choices));
        Platform.runLater(futureChoice);
        final Action choice = futureChoice.get();

        if (choice == Dialog.ACTION_CANCEL) {
            LOG.debug("Dialog cancelled.");
            return MultipleSearchResultsChoice.SKIP;
        }
        if (choice == chooseOne) {
            LOG.debug("Choose one result requested.");
            return MultipleSearchResultsChoice.CHOOSE;
        }
        if (choice == newName) {
            LOG.debug("New name requested.");
            return MultipleSearchResultsChoice.NEW_NAME;
        }
        if (choice == exclude) {
            LOG.debug("Exclude requested.");
            return MultipleSearchResultsChoice.EXCLUDE;
        }
        if (choice == designateSubLibrary) {
            LOG.debug("Designate sub-library requested.");
            return MultipleSearchResultsChoice.SUB_LIBRARY;
        }
        throw new IndexterException("Invalid choice: %s", choice);
    }

    @Override
    public MultipleSearchResultsChoice getMultipleGiantBombSearchResultsChoice(Path path,
                                                                               String name,
                                                                               GamePlatform platform,
                                                                               List<GameRawBriefInfo> briefInfos) throws Exception {
        final DialogAction chooseOne = new DialogAction("Choose");
        chooseOne.setLongText("Choose from the search results");

        final DialogAction newName = new DialogAction("New name");
        newName.setLongText("Retry with a new name");

        final List<DialogAction> choices = new ArrayList<>();
        choices.add(chooseOne);
        choices.add(newName);

        final Dialogs dialog = Dialogs.create()
            .owner(stage)
            .title("Too many GiantBomb search results!")
            .masthead(path.toString())
            .message(String.format("Found %d GiantBomb search results for '%s':", briefInfos.size(), name));

        // Dialog must be displayed on JavaFx thread.
        LOG.debug("Showing multiple search result dialog...");
        final FutureTask<Action> futureChoice = new FutureTask<>(() -> dialog.showCommandLinks(choices));
        Platform.runLater(futureChoice);
        final Action choice = futureChoice.get();

        if (choice == Dialog.ACTION_CANCEL) {
            LOG.debug("Dialog cancelled.");
            return MultipleSearchResultsChoice.SKIP;
        }
        if (choice == chooseOne) {
            LOG.debug("Choose one result requested.");
            return MultipleSearchResultsChoice.CHOOSE;
        }
        if (choice == newName) {
            LOG.debug("New name requested.");
            return MultipleSearchResultsChoice.NEW_NAME;
        }
        throw new IndexterException("Invalid choice: %s", choice);
    }

    @Override
    public Optional<GameRawBriefInfo> chooseFromMultipleResults(Path path,
                                                                String name,
                                                                GamePlatform platform,
                                                                List<GameRawBriefInfo> briefInfos) throws Exception {
        final Dialogs dialog = Dialogs.create()
            .owner(stage)
            .title(String.format("Search results for: '%s'", name))
            .masthead(path.toString());

        // Dialog must be displayed on JavaFx thread.
        LOG.debug("Showing all search results...");
        final FutureTask<Optional<GameRawBriefInfo>> futureChoice = new FutureTask<>(() -> dialog.showChoices(briefInfos));
        Platform.runLater(futureChoice);

        final Optional<GameRawBriefInfo> choice = futureChoice.get();
        if (choice.isPresent()) {
            LOG.info("Choice from multiple results: '{}'", choice.get());
        } else {
            LOG.debug("Dialog cancelled.");
        }
        return choice;
    }

    @Override
    public Optional<String> selectNewName(Path path, String name, GamePlatform platform) throws Exception {
        final Dialogs dialog = Dialogs.create()
            .owner(stage)
            .title(String.format("Select new name for: '%s'", name))
            .masthead(path.toString());

        // Dialog must be displayed on JavaFx thread.
        LOG.debug("Showing new name dialog...");
        final FutureTask<Optional<String>> futureChoice = new FutureTask<>(() -> dialog.showTextInput(name));
        Platform.runLater(futureChoice);
        final Optional<String> newName = futureChoice.get();
        if (newName.isPresent()) {
            LOG.info("New name chosen: '{}'", newName.get());
        } else {
            LOG.debug("Dialog cancelled.");
        }
        return newName;
    }

    @Override
    public Optional<String> getSubLibraryName(Path path, String name, GamePlatform platform) throws Exception {
        final Dialogs dialog = Dialogs.create()
            .owner(stage)
            .title("Enter sub-library name")
            .masthead(String.format("%s\nPlatform: %s\n", path.toString(), platform));

        // Dialog must be displayed on JavaFx thread.
        LOG.debug("Showing designate sub-library dialog...");
        final FutureTask<Optional<String>> futureChoice = new FutureTask<>(() -> dialog.showTextInput(name));
        Platform.runLater(futureChoice);
        final Optional<String> libraryName = futureChoice.get();

        if (libraryName.isPresent()) {
            LOG.info("Sub-library name: '%s'", libraryName.get());
        } else {
            LOG.debug("Dialog cancelled.");
        }
        return libraryName;
    }
}
