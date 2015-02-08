package com.github.ykrasik.indexter.games.manager.flow.dialog;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.info.SearchResult;
import com.github.ykrasik.indexter.games.manager.flow.dialog.choice.*;
import com.github.ykrasik.indexter.games.ui.SearchResultDialog;
import com.github.ykrasik.indexter.util.FileUtils;
import com.github.ykrasik.indexter.util.PlatformUtils;
import javafx.collections.FXCollections;
import javafx.stage.Stage;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.DialogAction;
import org.controlsfx.dialog.Dialogs;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * @author Yevgeny Krasik
 */
@Slf4j
@RequiredArgsConstructor
public class DialogManagerImpl implements DialogManager {
    private static final DialogAction EXCLUDE_ACTION = createExcludeAction();
    private static final DialogAction PROCEED_ANYWAY_ACTION = createProceedAnywayAction();
    private static final DialogAction NEW_NAME_ACTION = createNewNameAction();
    private static final DialogAction CHOOSE_FROM_SEARCH_RESULTS_ACTION = createChooseFromSearchResultsAction();

    @NonNull private final Stage stage;

    @Override
    public boolean shouldCreateLibraryDialog(Path path) throws Exception {
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

        log.debug("Showing create library confirmation dialog...");
        final Action action = getUserResponse(dialog::showConfirm);
        final boolean yes = action == Dialog.ACTION_YES;
        if (yes) {
            log.info("Library creation requested: '{}'", path);
        } else {
            log.debug("Dialog cancelled.");
        }
        return yes;
    }

    @Override
    public Optional<String> libraryNameDialog(Path path, GamePlatform platform) throws Exception {
        final Dialogs dialog = createDialog()
            .title("Enter library name")
            .masthead(String.format("%s\nPlatform: %s\n", path.toString(), platform));

        log.debug("Showing library name dialog...");
        final String defaultName = path.getFileName().toString();
        final Optional<String> libraryName = getUserResponse(() -> dialog.showTextInput(defaultName));

        if (libraryName.isPresent()) {
            log.info("Library name: '{}'", libraryName.get());
        } else {
            log.debug("Dialog cancelled.");
        }
        return libraryName;
    }

    @Override
    @SneakyThrows
    public DialogChoice noSearchResultsDialog(NoSearchResultsDialogParams params) {
        final String providerName = params.getProviderName();

        final Dialogs dialog = createDialog()
            .title(String.format("No %s search results found!", providerName))
            .masthead(params.getPath().toString())
            .message(String.format("No %s search results found: '%s'[%s]", providerName, params.getName(), params.getPlatform()));

        final List<DialogAction> choices = new LinkedList<>();
        choices.add(NEW_NAME_ACTION);
        choices.add(EXCLUDE_ACTION);
        if (params.isCanProceedWithout()) {
            choices.add(PROCEED_ANYWAY_ACTION);
        }

        while (true) {
            log.debug("Showing no {} search results dialog...", providerName);
            final Action action = getUserResponse(() -> dialog.showCommandLinks(choices));

            Optional<DialogChoice> choice = tryCommonDialogAction(action);
            if (choice.isPresent()) {
                return choice.get();
            }

            choice = tryNewNameDialogAction(action, providerName, params.getName(), params.getPath());
            if (choice.isPresent()) {
                return choice.get();
            }
        }
    }

    @Override
    @SneakyThrows
    public DialogChoice multipleSearchResultsDialog(MultipleSearchResultsDialogParams params) {
        final String providerName = params.getProviderName();

        final Dialogs dialog = createDialog()
            .title(String.format("Too many %s search results!", providerName))
            .masthead(params.getPath().toString())
            .message(String.format("Found %d %s search results for '%s'[%s]:", params.getSearchResults().size(), providerName, params.getName(), params.getPlatform()));

        final List<DialogAction> choices = new LinkedList<>();
        choices.add(CHOOSE_FROM_SEARCH_RESULTS_ACTION);
        choices.add(NEW_NAME_ACTION);
        choices.add(EXCLUDE_ACTION);
        if (params.isCanProceedWithout()) {
            choices.add(PROCEED_ANYWAY_ACTION);
        }

        while (true) {
            log.debug("Showing multiple search result dialog...");
            final Action action = getUserResponse(() -> dialog.showCommandLinks(choices));

            Optional<DialogChoice> choice = tryCommonDialogAction(action);
            if (choice.isPresent()) {
                return choice.get();
            }

            choice = tryNewNameDialogAction(action, providerName, params.getName(), params.getPath());
            if (choice.isPresent()) {
                return choice.get();
            }

            choice = tryChooseFromSearchResultsDialogAction(action, providerName, params.getName(), params.getPlatform(), params.getSearchResults());
            if (choice.isPresent()) {
                return choice.get();
            }
        }
    }

    private Optional<DialogChoice> tryCommonDialogAction(Action action) {
        if (action == Dialog.ACTION_CANCEL) {
            log.debug("Dialog cancelled.");
            return Optional.of(SkipDialogChoice.instance());
        }
        if (action == EXCLUDE_ACTION) {
            log.debug("Exclude requested.");
            return Optional.of(ExcludeDialogChoice.instance());
        }
        if (action == PROCEED_ANYWAY_ACTION) {
            log.debug("Proceed anyway requested.");
            return Optional.of(ProceedAnywayDialogChoice.instance());
        }
        return Optional.empty();
    }

    private Optional<DialogChoice> tryNewNameDialogAction(Action action, String providerName, String prevName, Path path) throws Exception {
        if (action == NEW_NAME_ACTION) {
            log.debug("New name requested.");
            final Optional<String> chosenName = newNameDialog(providerName, prevName, path);
            if (chosenName.isPresent()) {
                return Optional.of(new NewNameDialogChoice(chosenName.get()));
            }
        }
        return Optional.empty();
    }

    private Optional<DialogChoice> tryChooseFromSearchResultsDialogAction(Action action,
                                                                          String providerName,
                                                                          String name,
                                                                          GamePlatform platform,
                                                                          List<SearchResult> searchResults) throws Exception {
        if (action == CHOOSE_FROM_SEARCH_RESULTS_ACTION) {
            log.debug("Choose from search results requested.");
            final Optional<SearchResult> searchResult = chooseFromSearchResults(providerName, name, platform, searchResults);
            return searchResult.map(ChooseFromSearchResultsChoice::new);
        }
        return Optional.empty();
    }

    private Optional<String> newNameDialog(String gameInfoServiceName, String prevName, Path path) throws Exception {
        final Dialogs dialog = createDialog()
            .title(String.format("%s: Select new name instead of '%s'", gameInfoServiceName, prevName))
            .masthead(path.toString());

        log.debug("Showing new name dialog...");
        final Optional<String> newName = getUserResponse(() -> dialog.showTextInput(prevName));
        if (newName.isPresent()) {
            log.debug("New name chosen: '{}'", newName.get());
        } else {
            log.debug("Dialog cancelled.");
        }
        return newName;
    }

    private Optional<SearchResult> chooseFromSearchResults(String providerName,
                                                           String name,
                                                           GamePlatform platform,
                                                           List<SearchResult> searchResults) throws Exception {
        final SearchResultDialog<SearchResult> dialog = new SearchResultDialog<>()
            .owner(stage)
            .title(String.format("%s search results for: '%s'[%s]", providerName, name, platform));

        log.debug("Showing all search results...");
        final Optional<SearchResult> choice = getUserResponse(() -> dialog.show(FXCollections.observableArrayList(searchResults)));
        if (choice.isPresent()) {
            log.debug("Choice from multiple results: '{}'", choice.get());
        } else {
            log.debug("Dialog cancelled.");
        }
        return choice;
    }

    private Dialogs createDialog() {
        return Dialogs.create().owner(stage);
    }

    private <V> V getUserResponse(Callable<V> callable) throws Exception {
        // Dialog must be displayed on JavaFx thread.
        final FutureTask<V> futureTask = new FutureTask<>(callable);
        PlatformUtils.runLaterIfNecessary(futureTask);
        return futureTask.get();
    }

    private static DialogAction createExcludeAction() {
        final DialogAction choice = new DialogAction("Exclude");
        choice.setLongText("Exclude directory from further processing");
        return choice;
    }

    private static DialogAction createProceedAnywayAction() {
        final DialogAction choice = new DialogAction("Proceed Anyway");
        choice.setLongText("Proceed anyway");
        return choice;
    }

    private static DialogAction createNewNameAction() {
        final DialogAction choice = new DialogAction("New name");
        choice.setLongText("Retry with a new name");
        return choice;
    }

    private static DialogAction createChooseFromSearchResultsAction() {
        final DialogAction action = new DialogAction("Choose");
        action.setLongText("Choose from the search results");
        return action;
    }
}
