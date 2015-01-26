package com.github.ykrasik.indexter.games.manager.flow.choice;

import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.GameRawBriefInfo;
import com.github.ykrasik.indexter.util.FileUtils;
import com.github.ykrasik.indexter.util.PlatformUtils;
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
import java.util.concurrent.Callable;
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

        final Dialogs dialog = createDialog()
            .title("No Metacritic search results found!")
            .masthead(path.toString())
            .message(String.format("No Metacritic search results found: '%s'", name));

        LOG.debug("Showing no metacritic search results dialog...");
        final Action choice = getUserResponse(() -> dialog.showCommandLinks(newName, exclude));

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

        final List<DialogAction> choices = new ArrayList<>();
        choices.add(chooseOne);
        choices.add(newName);
        choices.add(exclude);

        final Dialogs dialog = createDialog()
            .title("Too many Metacritic search results!")
            .masthead(path.toString())
            .message(String.format("Found %d Metacritic search results for '%s':", briefInfos.size(), name));

        LOG.debug("Showing multiple search result dialog...");
        final Action choice = getUserResponse(() -> dialog.showCommandLinks(choices));

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

        final Dialogs dialog = createDialog()
            .title("Too many GiantBomb search results!")
            .masthead(path.toString())
            .message(String.format("Found %d GiantBomb search results for '%s':", briefInfos.size(), name));

        LOG.debug("Showing multiple search result dialog...");
        final Action choice = getUserResponse(() -> dialog.showCommandLinks(choices));

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
        final Dialogs dialog = createDialog()
            .title(String.format("Search results for: '%s'", name))
            .masthead(path.toString());

        LOG.debug("Showing all search results...");
        final Optional<GameRawBriefInfo> choice = getUserResponse(() -> dialog.showChoices(briefInfos));
        if (choice.isPresent()) {
            LOG.info("Choice from multiple results: '{}'", choice.get());
        } else {
            LOG.debug("Dialog cancelled.");
        }
        return choice;
    }

    @Override
    public Optional<String> selectNewName(Path path, String name, GamePlatform platform) throws Exception {
        final Dialogs dialog = createDialog()
            .title(String.format("Select new name for: '%s'", name))
            .masthead(path.toString());

        LOG.debug("Showing new name dialog...");
        final Optional<String> newName = getUserResponse(() -> dialog.showTextInput(name));
        if (newName.isPresent()) {
            LOG.info("New name chosen: '{}'", newName.get());
        } else {
            LOG.debug("Dialog cancelled.");
        }
        return newName;
    }

    @Override
    public boolean shouldCreateLibrary(Path path) throws Exception {
        final StringBuilder sb = new StringBuilder("This path has sub-directories. Would you like to create a library out of it?\n");
        final List<Path> childDirectories = FileUtils.listChildDirectories(path);
        for (Path childDirectory : childDirectories) {
            sb.append('\t');
            sb.append(childDirectory.toString());
            sb.append('\n');
        }

        final Dialogs dialog = createDialog()
            .title("Create library?")
            .masthead(path.toString())
            .message(sb.toString());

        LOG.debug("Showing create library confirmation dialog...");
        final Action response = getUserResponse(dialog::showConfirm);
        final boolean yes = response == Dialog.ACTION_YES;
        if (yes) {
            LOG.info("Library creation requested: '{}'", path);
        } else {
            LOG.debug("Dialog cancelled.");
        }
        return yes;
    }

    @Override
    public Optional<String> getLibraryName(Path path, GamePlatform platform) throws Exception {
        final Dialogs dialog = createDialog()
            .title("Enter library name")
            .masthead(String.format("%s\nPlatform: %s\n", path.toString(), platform));

        LOG.debug("Showing library name dialog...");
        final String defaultName = path.getFileName().toString();
        final Optional<String> libraryName = getUserResponse(() -> dialog.showTextInput(defaultName));

        if (libraryName.isPresent()) {
            LOG.info("Library name: '{}'", libraryName.get());
        } else {
            LOG.debug("Dialog cancelled.");
        }
        return libraryName;
    }

    private Dialogs createDialog() {
        return Dialogs.create().owner(stage);
    }

    private <V> V getUserResponse(Callable<V> callable) throws Exception {
        // Dialog must be displayed on JavaFx thread.
        final FutureTask<V> futureTask = new FutureTask<>(callable);
        PlatformUtils.runLater(futureTask);
        return futureTask.get();
    }
}
