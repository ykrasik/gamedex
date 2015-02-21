package com.github.ykrasik.gamedex.core.service.dialog;

import com.github.ykrasik.gamedex.common.util.FileUtils;
import com.github.ykrasik.gamedex.common.util.PlatformUtils;
import com.github.ykrasik.gamedex.core.service.action.SearchResultComparators;
import com.github.ykrasik.gamedex.core.service.dialog.choice.*;
import com.github.ykrasik.gamedex.core.ui.dialog.SearchResultsDialog;
import com.github.ykrasik.gamedex.core.ui.library.CreateLibraryDialog;
import com.github.ykrasik.gamedex.core.ui.library.LibraryDef;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.provider.SearchResult;
import com.github.ykrasik.opt.Opt;
import com.gs.collections.api.list.ImmutableList;
import javafx.collections.FXCollections;
import javafx.scene.Parent;
import javafx.scene.effect.GaussianBlur;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.DialogAction;
import org.controlsfx.dialog.Dialogs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
public class DialogServiceImpl implements DialogService {
    private static final DialogAction EXCLUDE_ACTION = createExcludeAction();
    private static final DialogAction PROCEED_ANYWAY_ACTION = createProceedAnywayAction();
    private static final DialogAction NEW_NAME_ACTION = createNewNameAction();
    private static final DialogAction CHOOSE_FROM_SEARCH_RESULTS_ACTION = createChooseFromSearchResultsAction();

    @NonNull private final Stage stage;

    @Override
    @SneakyThrows
    public void showException(Throwable t) {
        log.warn("Error:", t);
        final Dialogs dialog = Dialogs.create()
            .owner(stage)
            .title("Error!")
            .message(t.getMessage());
        getUserResponse(() -> dialog.showException(t));
    }

    @Override
    @SneakyThrows
    public Opt<LibraryDef> addLibraryDialog(Opt<Path> initialDirectory) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Add Library");
        directoryChooser.setInitialDirectory(initialDirectory.map(Path::toFile).getOrElseNull());
        final File selectedDirectory = getUserResponse(() -> directoryChooser.showDialog(stage));
        return Opt.ofNullable(selectedDirectory).flatMapX(this::createLibraryFromFile);
    }

    private Opt<LibraryDef> createLibraryFromFile(File file) throws IOException {
        final Path path = Paths.get(file.toURI());
        final ImmutableList<Path> children = FileUtils.listFirstChildDirectories(path, 10).newWith(Paths.get("..."));
        return createLibraryDialog(path, children, GamePlatform.PC);
    }

    @Override
    @SneakyThrows
    public Opt<LibraryDef> createLibraryDialog(Path path, ImmutableList<Path> children, GamePlatform defaultPlatform) {
        log.info("Showing create library dialog...");
        final Opt<LibraryDef> libraryDef = getUserResponse(() -> CreateLibraryDialog.create().show(path, children, defaultPlatform));
        if (libraryDef.isPresent()) {
            log.info("Library: {}", libraryDef.get());
        } else {
            log.info("Dialog cancelled.");
        }
        return libraryDef;
    }

    @Override
    @SneakyThrows
    public DialogChoice noSearchResultsDialog(NoSearchResultsDialogParams params) {
        final String message = String.format("%s: No search results found for '%s'", params.providerName(), params.name());
        final Dialogs dialog = createDialog()
            .title(message)
            .masthead(params.path().toString())
            .message(message);

        final List<DialogAction> choices = new LinkedList<>();
        choices.add(NEW_NAME_ACTION);
        choices.add(EXCLUDE_ACTION);
        if (params.canProceedWithout()) {
            choices.add(PROCEED_ANYWAY_ACTION);
        }

        while (true) {
            log.info("Showing no {} search results dialog...", params.providerName());
            final Action action = getUserResponse(() -> dialog.showCommandLinks(choices));

            Opt<DialogChoice> choice = tryCommonDialogAction(action);
            if (choice.isPresent()) {
                return choice.get();
            }

            choice = tryNewNameDialogAction(action, params.providerName(), params.name(), params.path());
            if (choice.isPresent()) {
                return choice.get();
            }
        }
    }

    @Override
    @SneakyThrows
    public DialogChoice multipleSearchResultsDialog(MultipleSearchResultsDialogParams params) {
        final String message = String.format("%s: Found %d search results for '%s'", params.providerName(), params.searchResults().size(), params.name());
        final Dialogs dialog = createDialog()
            .title(message)
            .masthead(params.path().toString())
            .message(message);

        final List<DialogAction> choices = new LinkedList<>();
        choices.add(CHOOSE_FROM_SEARCH_RESULTS_ACTION);
        choices.add(NEW_NAME_ACTION);
        choices.add(EXCLUDE_ACTION);
        if (params.canProceedWithout()) {
            choices.add(PROCEED_ANYWAY_ACTION);
        }

        final List<SearchResult> sortedSearchResults = params.searchResults().toSortedList(SearchResultComparators.releaseDateDesc());

        while (true) {
            log.info("Showing multiple {} search result dialog...", params.providerName());
            final Action action = getUserResponse(() -> dialog.showCommandLinks(choices));

            Opt<DialogChoice> choice = tryCommonDialogAction(action);
            if (choice.isPresent()) {
                return choice.get();
            }

            choice = tryNewNameDialogAction(action, params.providerName(), params.name(), params.path());
            if (choice.isPresent()) {
                return choice.get();
            }

            choice = tryChooseFromSearchResultsDialogAction(action, params.providerName(), params.name(), params.path(), sortedSearchResults);
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
        final FutureTask<V> futureTask = new FutureTask<>(() -> {
            final Parent root = stage.getScene().getRoot();
            root.setEffect(new GaussianBlur());
            final V result = callable.call();
            root.setEffect(null);
            return result;
        });
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
