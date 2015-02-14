package com.github.ykrasik.gamedex.core.dialog;

import com.github.ykrasik.gamedex.common.util.FileUtils;
import com.github.ykrasik.gamedex.common.util.PlatformUtils;
import com.github.ykrasik.gamedex.core.dialog.choice.*;
import com.github.ykrasik.gamedex.core.ui.dialog.SearchResultsDialog;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.provider.SearchResult;
import com.github.ykrasik.opt.Opt;
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

        log.info("Showing create library confirmation dialog...");
        final Action action = getUserResponse(dialog::showConfirm);
        final boolean yes = action == Dialog.ACTION_YES;
        if (yes) {
            log.info("Library creation requested: '{}'", path);
        } else {
            log.info("Dialog cancelled.");
        }
        return yes;
    }

    @Override
    public Opt<String> libraryNameDialog(Path path, GamePlatform platform) throws Exception {
        final Dialogs dialog = createDialog()
            .title("Enter library name")
            .masthead(String.format("%s\nPlatform: %s\n", path.toString(), platform));

        log.info("Showing library name dialog...");
        final String defaultName = path.getFileName().toString();
        final Optional<String> libraryName = getUserResponse(() -> dialog.showTextInput(defaultName));
        if (libraryName.isPresent()) {
            log.info("Library name: '{}'", libraryName.get());
        } else {
            log.info("Dialog cancelled.");
        }
        return Opt.fromOptional(libraryName);
    }

    @Override
    @SneakyThrows
    public DialogChoice noSearchResultsDialog(NoSearchResultsDialogParams params) {
        final String providerName = params.getProviderName();

        final String message = String.format("%s: No search results found for '%s'", providerName, params.getName());
        final Dialogs dialog = createDialog()
            .title(message)
            .masthead(params.getPath().toString())
            .message(message);

        final List<DialogAction> choices = new LinkedList<>();
        choices.add(NEW_NAME_ACTION);
        choices.add(EXCLUDE_ACTION);
        if (params.isCanProceedWithout()) {
            choices.add(PROCEED_ANYWAY_ACTION);
        }

        while (true) {
            log.info("Showing no {} search results dialog...", providerName);
            final Action action = getUserResponse(() -> dialog.showCommandLinks(choices));

            Opt<DialogChoice> choice = tryCommonDialogAction(action);
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

        final String message = String.format("%s: Found %d search results for '%s'", providerName, params.getSearchResults().size(), params.getName());
        final Dialogs dialog = createDialog()
            .title(message)
            .masthead(params.getPath().toString())
            .message(message);

        final List<DialogAction> choices = new LinkedList<>();
        choices.add(CHOOSE_FROM_SEARCH_RESULTS_ACTION);
        choices.add(NEW_NAME_ACTION);
        choices.add(EXCLUDE_ACTION);
        if (params.isCanProceedWithout()) {
            choices.add(PROCEED_ANYWAY_ACTION);
        }

        while (true) {
            log.info("Showing multiple {} search result dialog...", providerName);
            final Action action = getUserResponse(() -> dialog.showCommandLinks(choices));

            Opt<DialogChoice> choice = tryCommonDialogAction(action);
            if (choice.isPresent()) {
                return choice.get();
            }

            choice = tryNewNameDialogAction(action, providerName, params.getName(), params.getPath());
            if (choice.isPresent()) {
                return choice.get();
            }

            choice = tryChooseFromSearchResultsDialogAction(action, providerName, params.getName(), params.getPath(), params.getSearchResults());
            if (choice.isPresent()) {
                return choice.get();
            }
        }
    }

    private Opt<DialogChoice> tryCommonDialogAction(Action action) {
        if (action == Dialog.ACTION_CANCEL) {
            log.info("Dialog cancelled.");
            return Opt.of(SkipDialogChoice.instance());
        }
        if (action == EXCLUDE_ACTION) {
            log.info("Exclude requested.");
            return Opt.of(ExcludeDialogChoice.instance());
        }
        if (action == PROCEED_ANYWAY_ACTION) {
            log.info("Proceed anyway requested.");
            return Opt.of(ProceedAnywayDialogChoice.instance());
        }
        return Opt.absent();
    }

    private Opt<DialogChoice> tryNewNameDialogAction(Action action, String providerName, String prevName, Path path) throws Exception {
        if (action == NEW_NAME_ACTION) {
            log.info("New name requested.");
            final Opt<String> chosenName = newNameDialog(providerName, prevName, path);
            if (chosenName.isPresent()) {
                return Opt.of(new NewNameDialogChoice(chosenName.get()));
            }
        }
        return Opt.absent();
    }

    private Opt<DialogChoice> tryChooseFromSearchResultsDialogAction(Action action,
                                                                     String providerName,
                                                                     String name,
                                                                     Path path,
                                                                     List<SearchResult> searchResults) throws Exception {
        if (action == CHOOSE_FROM_SEARCH_RESULTS_ACTION) {
            log.info("Choose from search results requested.");
            final Opt<SearchResult> searchResult = chooseFromSearchResults(providerName, name, path, searchResults);
            return searchResult.map(ChooseFromSearchResultsChoice::new);
        }
        return Opt.absent();
    }

    private Opt<String> newNameDialog(String gameInfoServiceName, String prevName, Path path) throws Exception {
        final Dialogs dialog = createDialog()
            .title(String.format("%s: Select new name instead of '%s'", gameInfoServiceName, prevName))
            .masthead(path.toString());

        log.info("Showing new name dialog...");
        final Optional<String> newName = getUserResponse(() -> dialog.showTextInput(prevName));
        if (newName.isPresent()) {
            log.info("New name chosen: '{}'", newName.get());
        } else {
            log.info("Dialog cancelled.");
        }
        return Opt.fromOptional(newName);
    }

    private Opt<SearchResult> chooseFromSearchResults(String providerName,
                                                      String name,
                                                      Path path,
                                                      List<SearchResult> searchResults) throws Exception {
        final SearchResultsDialog dialog = new SearchResultsDialog(providerName, name, path);

        log.info("Showing all search results...");
        final Opt<SearchResult> choice = getUserResponse(() -> dialog.show(FXCollections.observableArrayList(searchResults)));
        if (choice.isPresent()) {
            log.info("Choice from multiple results: '{}'", choice.get());
        } else {
            log.info("Dialog cancelled.");
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
