package com.github.ykrasik.indexter.games.manager.flow.choice;

import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.info.giantbomb.GiantBombSearchResult;
import com.github.ykrasik.indexter.games.datamodel.info.metacritic.MetacriticSearchResult;
import com.github.ykrasik.indexter.games.manager.flow.choice.type.Choice;
import com.github.ykrasik.indexter.games.manager.flow.choice.type.ChoiceData;
import com.github.ykrasik.indexter.games.manager.flow.choice.type.ChoiceType;
import com.github.ykrasik.indexter.util.FileUtils;
import com.github.ykrasik.indexter.util.ListUtils;
import com.github.ykrasik.indexter.util.PlatformUtils;
import com.github.ykrasik.indexter.util.exception.SupplierThrows;
import javafx.stage.Stage;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.DialogAction;
import org.controlsfx.dialog.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.LinkedList;
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

    @Override
    public Choice onNoMetacriticSearchResults(String name, GamePlatform platform, Path path) throws Exception {
        return onNoSearchResults("Metacritic", name, path, () -> getNoSearchResultsChoice("Metacritic", name, platform, path, false));
    }

    @Override
    public Choice onNoGiantBombSearchResults(String name, GamePlatform platform, Path path) throws Exception {
        return onNoSearchResults("GiantBomb", name, path, () -> getNoSearchResultsChoice("GiantBomb", name, platform, path, true));
    }

    private Choice onNoSearchResults(String gameInfoServiceName,
                                     String name,
                                     Path path,
                                     SupplierThrows<ChoiceType> choiceSupplier) throws Exception {
        Optional<? extends Choice> choice = Optional.empty();
        while (!choice.isPresent()) {
            final ChoiceType choiceType = choiceSupplier.get();
            switch (choiceType) {
                case NEW_NAME:
                    choice = selectNewName(gameInfoServiceName, name, path);
                    break;

                case EXCLUDE: choice = Optional.of(Choice.EXCLUDE); break;
                case PROCEED_ANYWAY: choice = Optional.of(Choice.PROCEED_ANYWAY); break;
                default: choice = Optional.of(Choice.SKIP); break;
            }
        }
        return choice.get();
    }

    private ChoiceType getNoSearchResultsChoice(String gameInfoServiceName,
                                                String name,
                                                GamePlatform platform,
                                                Path path,
                                                boolean canProceedAnyway) throws Exception {
        final Dialogs dialog = createDialog()
            .title(String.format("No %s search results found!", gameInfoServiceName))
            .masthead(path.toString())
            .message(String.format("No %s search results found: '%s'[%s]", gameInfoServiceName, name, platform));

        final DialogAction newName = new DialogAction("New name");
        newName.setLongText("Retry with a new name");

        final DialogAction exclude = new DialogAction("Exclude");
        exclude.setLongText("Exclude directory from further processing");

        final DialogAction proceedAnyway = new DialogAction("Proceed Anyway");
        exclude.setLongText("Proceed anyway");

        final List<DialogAction> choices = new LinkedList<>();
        choices.add(newName);
        choices.add(exclude);
        if (canProceedAnyway) {
            choices.add(proceedAnyway);
        }

        LOG.debug("Showing no {} search results dialog...", gameInfoServiceName);
        final Action choice = getUserResponse(() -> dialog.showCommandLinks(choices));

        if (choice == Dialog.ACTION_CANCEL) {
            LOG.debug("Dialog cancelled.");
            return ChoiceType.SKIP;
        }
        if (choice == newName) {
            LOG.debug("New name requested.");
            return ChoiceType.NEW_NAME;
        }
        if (choice == exclude) {
            LOG.debug("Exclude requested.");
            return ChoiceType.EXCLUDE;
        }
        if (choice == proceedAnyway) {
            LOG.debug("Proceed anyway requested.");
            return ChoiceType.PROCEED_ANYWAY;
        }
        throw new IndexterException("Invalid choice: %s", choice);
    }

    private Optional<ChoiceData> selectNewName(String gameInfoServiceName, String prevName, Path path) throws Exception {
        final Dialogs dialog = createDialog()
            .title(String.format("%s: Select new name instead of '%s'", gameInfoServiceName, prevName))
            .masthead(path.toString());

        LOG.debug("Showing new name dialog...");
        final Optional<String> newName = getUserResponse(() -> dialog.showTextInput(prevName));
        if (newName.isPresent()) {
            LOG.debug("New name chosen: '{}'", newName.get());
            return Optional.of(new ChoiceData(ChoiceType.NEW_NAME, newName.get()));
        } else {
            LOG.debug("Dialog cancelled.");
            return Optional.empty();
        }
    }

    @Override
    public Choice onMultipleMetacriticSearchResults(String name, GamePlatform platform, Path path, List<MetacriticSearchResult> searchResults) throws Exception {
        return onMultipleSearchResults(
            "Metacritic",
            name,
            path,
            () -> getMultipleSearchResultsChoice("Metacritic", name, platform, path, searchResults.size(), false),
            () -> chooseFromMultipleMetacriticSearchResults(name, path, platform, searchResults)
        );
    }

    @Override
    public Choice onMultipleGiantBombSearchResults(String name, GamePlatform platform, Path path, List<GiantBombSearchResult> searchResults) throws Exception {
        return onMultipleSearchResults(
            "GiantBomb",
            name,
            path,
            () -> getMultipleSearchResultsChoice("GiantBomb", name, platform, path, searchResults.size(), true),
            () -> chooseFromMultipleGiantBombSearchResults(name, path, platform, searchResults)
        );
    }

    private Choice onMultipleSearchResults(String gameInfoServiceName,
                                           String name,
                                           Path path,
                                           SupplierThrows<ChoiceType> choiceSupplier,
                                           SupplierThrows<Optional<ChoiceData>> multipleChoiceSupplier) throws Exception {
        Optional<? extends Choice> choice = Optional.empty();
        while (!choice.isPresent()) {
            final ChoiceType choiceType = choiceSupplier.get();
            switch (choiceType) {
                case NEW_NAME:
                    choice = selectNewName(gameInfoServiceName, name, path);
                    break;

                case CHOOSE:
                    choice = multipleChoiceSupplier.get();
                    break;

                case EXCLUDE: choice = Optional.of(Choice.EXCLUDE); break;
                case PROCEED_ANYWAY: choice = Optional.of(Choice.PROCEED_ANYWAY); break;
                default: choice = Optional.of(Choice.SKIP); break;
            }
        }
        return choice.get();
    }

    private ChoiceType getMultipleSearchResultsChoice(String gameInfoServiceName,
                                                      String name,
                                                      GamePlatform platform,
                                                      Path path,
                                                      int numSearchResults,
                                                      boolean canProceedAnyway) throws Exception {
        final Dialogs dialog = createDialog()
            .title(String.format("Too many %s search results!", gameInfoServiceName))
            .masthead(path.toString())
            .message(String.format("Found %d %s search results for '%s'[%s]:", numSearchResults, gameInfoServiceName, name, platform));

        final DialogAction chooseOne = new DialogAction("Choose");
        chooseOne.setLongText("Choose from the search results");

        final DialogAction newName = new DialogAction("New name");
        newName.setLongText("Retry with a new name");

        final DialogAction exclude = new DialogAction("Exclude");
        exclude.setLongText("Exclude directory from further processing");

        final DialogAction proceedAnyway = new DialogAction("Proceed Anyway");
        exclude.setLongText("Proceed anyway");

        final List<DialogAction> choices = new LinkedList<>();
        choices.add(chooseOne);
        choices.add(newName);
        choices.add(exclude);
        if (canProceedAnyway) {
            choices.add(proceedAnyway);
        }

        LOG.debug("Showing multiple search result dialog...");
        final Action choice = getUserResponse(() -> dialog.showCommandLinks(choices));

        if (choice == Dialog.ACTION_CANCEL) {
            LOG.debug("Dialog cancelled.");
            return ChoiceType.SKIP;
        }
        if (choice == chooseOne) {
            LOG.debug("Choose one result requested.");
            return ChoiceType.CHOOSE;
        }
        if (choice == newName) {
            LOG.debug("New name requested.");
            return ChoiceType.NEW_NAME;
        }
        if (choice == exclude) {
            LOG.debug("Exclude requested.");
            return ChoiceType.EXCLUDE;
        }
        if (choice == proceedAnyway) {
            LOG.debug("Proceed anyway requested.");
            return ChoiceType.PROCEED_ANYWAY;
        }
        throw new IndexterException("Invalid choice: %s", choice);
    }

    private Optional<ChoiceData> chooseFromMultipleMetacriticSearchResults(String name,
                                                                           Path path,
                                                                           GamePlatform platform,
                                                                           List<MetacriticSearchResult> searchResults) throws Exception {
        final Dialogs dialog = createDialog()
            .title(String.format("Metacritic search results for: '%s'[%s]", name, platform))
            .masthead(path.toString());

        final List<MetacriticSearchResultChoice> choices = ListUtils.map(searchResults, MetacriticSearchResultChoice::new);

        LOG.debug("Showing all search results...");
        final Optional<MetacriticSearchResultChoice> choice = getUserResponse(() -> dialog.showChoices(choices));
        if (choice.isPresent()) {
            LOG.debug("Choice from multiple results: '{}'", choice.get());
            return Optional.of(new ChoiceData(ChoiceType.CHOOSE, choice.get().getSearchResult()));
        } else {
            LOG.debug("Dialog cancelled.");
            return Optional.empty();
        }
    }

    private Optional<ChoiceData> chooseFromMultipleGiantBombSearchResults(String name,
                                                                          Path path,
                                                                          GamePlatform platform,
                                                                          List<GiantBombSearchResult> searchResults) throws Exception {
        final Dialogs dialog = createDialog()
            .title(String.format("GiantBomb search results for: '%s'[%s]", name, platform))
            .masthead(path.toString());

        final List<GiantBombSearchResultChoice> choices = ListUtils.map(searchResults, GiantBombSearchResultChoice::new);

        LOG.debug("Showing all search results...");
        final Optional<GiantBombSearchResultChoice> choice = getUserResponse(() -> dialog.showChoices(choices));
        if (choice.isPresent()) {
            LOG.debug("Choice from multiple results: '{}'", choice.get());
            return Optional.of(new ChoiceData(ChoiceType.CHOOSE, choice.get().getSearchResult()));
        } else {
            LOG.debug("Dialog cancelled.");
            return Optional.empty();
        }
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
