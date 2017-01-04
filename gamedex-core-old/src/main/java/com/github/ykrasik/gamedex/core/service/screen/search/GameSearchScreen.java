package com.github.ykrasik.gamedex.core.service.screen.search;

import com.github.ykrasik.gamedex.core.manager.stage.StageManager;
import com.github.ykrasik.gamedex.core.ui.UIResources;
import com.github.ykrasik.yava.javafx.JavaFxUtils;
import com.gitlab.ykrasik.gamedex.provider.DataProviderInfo;
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult;
import com.gs.collections.api.list.ImmutableList;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.nio.file.Path;

import static com.github.ykrasik.gamedex.core.util.StringUtils.toStringOrUnavailable;

/**
 * @author Yevgeny Krasik
 */
public class GameSearchScreen {
    @FXML private Label pathLabel;
    @FXML private ImageView logoImageView;
    @FXML private Label errorLabel;

    @FXML private TextField searchTextField;
    @FXML private Button searchButton;
    @FXML private Label searchResultsCountLabel;

    @FXML private CheckBox multipleResultsCheckBox;
    @FXML private CheckBox autoContinueCheckBox;

    @FXML private TableView<ProviderSearchResult> searchResultsTable;
    @FXML private TableColumn<ProviderSearchResult, Boolean> checkColumn;
    @FXML private TableColumn<ProviderSearchResult, String> nameColumn;
    @FXML private TableColumn<ProviderSearchResult, String> scoreColumn;
    @FXML private TableColumn<ProviderSearchResult, String> releaseDateColumn;

    @FXML private Button proceedAnywayButton;
    @FXML private Button okButton;

    private final Stage stage = new Stage();
    private final ListProperty<ProviderSearchResult> providerSearchResultsProperty = new SimpleListProperty<>(FXCollections.emptyObservableList());

    private final StageManager stageManager;

    private GameSearchChoice result;

    @SneakyThrows
    public GameSearchScreen(@NonNull StageManager stageManager) {
        this.stageManager = stageManager;

        final FXMLLoader loader = new FXMLLoader(UIResources.gameSearchScreenFxml());
        loader.setController(this);
        final BorderPane root = loader.load();

        final Scene scene = new Scene(root, Color.TRANSPARENT);
        scene.getStylesheets().addAll(UIResources.mainCss(), UIResources.gameSearchScreenCss());

        stage.initStyle(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);

        // Make the stage draggable by clicking anywhere.
        JavaFxUtils.makeStageDraggable(stage, root);
    }

    @FXML
    private void initialize() {
        initSearchResultsTable();

        searchResultsCountLabel.textProperty().bind(providerSearchResultsProperty.sizeProperty().asString("Results: %d"));

        searchButton.defaultButtonProperty().bind(searchTextField.focusedProperty());
        okButton.defaultButtonProperty().bind(searchButton.defaultButtonProperty().not());
        okButton.disableProperty().bind(searchResultsTable.getSelectionModel().selectedItemProperty().isNull());
        okButton.setOnAction(e -> setResultFromSelection());
    }

    private void initSearchResultsTable() {
        checkColumn.setCellFactory(param -> new CheckBoxTableCell<>());
        nameColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getName()));
        releaseDateColumn.setCellValueFactory(e -> new SimpleStringProperty(toStringOrUnavailable(e.getValue().getReleaseDate())));
        scoreColumn.setCellValueFactory(e -> new SimpleStringProperty(toStringOrUnavailable(e.getValue().getScore())));

        searchResultsTable.itemsProperty().bind(providerSearchResultsProperty);
        searchResultsTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                setResultFromSelection();
            }
        });

        searchButton.setOnAction(e -> {
            final String newName = searchTextField.getText();
            setResult(GameSearchChoice.newName(newName));
        });
    }

    public GameSearchChoice show(String searchedName, Path path, DataProviderInfo info, ImmutableList<ProviderSearchResult> searchResults) {
        logoImageView.setImage(info.getLogo());
        pathLabel.setText(path.toString());
        searchTextField.setText(searchedName);
        errorLabel.setText(getErrorLabel(searchedName, searchResults));
        setSearchResults(searchResults);
        proceedAnywayButton.setDisable(info.isRequired());

        stageManager.runWithBlur(stage::showAndWait);
        return result;
    }

    private String getErrorLabel(String searchedName, ImmutableList<ProviderSearchResult> searchResults) {
        if (searchResults.isEmpty()) {
            return String.format("No search results for '%s'!", searchedName);
        } else {
            return String.format("Too many search results for '%s'!", searchedName);
        }
    }

    private void setSearchResults(ImmutableList<ProviderSearchResult> searchResults) {
        providerSearchResultsProperty.set(FXCollections.observableArrayList(searchResults.castToList()));
    }

    private void setResultFromSelection() {
        final ProviderSearchResult selectedItem = searchResultsTable.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            setResult(GameSearchChoice.select(selectedItem));
        }
    }

    @FXML
    private void skip() {
        setResult(GameSearchChoice.skip());
    }

    @FXML
    private void exclude() {
        setResult(GameSearchChoice.exclude());
    }

    @FXML
    private void proceedAnyway() {
        setResult(GameSearchChoice.proceedAnyway());
    }

    private void setResult(GameSearchChoice choice) {
        this.result = choice;
        stage.hide();
    }
}
