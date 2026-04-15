package com.mes.controller;

import com.mes.dto.MaterialCategoryDTO;
import com.mes.entity.MaterialCategory;
import com.mes.service.AuthService;
import com.mes.service.MaterialCategoryService;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MaterialCategoryController {

    private final MaterialCategoryService materialCategoryService;
    private final AuthService authService;

    @FXML
    private TableView<MaterialCategoryDTO> categoryTable;

    @FXML
    private TableColumn<MaterialCategoryDTO, Long> idColumn;

    @FXML
    private TableColumn<MaterialCategoryDTO, String> nameColumn;

    @FXML
    private TableColumn<MaterialCategoryDTO, String> descriptionColumn;

    @FXML
    private TableColumn<MaterialCategoryDTO, String> parentColumn;

    @FXML
    private TableColumn<MaterialCategoryDTO, Boolean> enabledColumn;

    @FXML
    private TableColumn<MaterialCategoryDTO, Boolean> actionColumn;

    @FXML
    private TextField nameSearchField;

    @FXML
    private ComboBox<String> enabledComboBox;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    private ObservableList<MaterialCategoryDTO> categoryList = FXCollections.observableArrayList();

    public MaterialCategoryController(MaterialCategoryService materialCategoryService, AuthService authService) {
        this.materialCategoryService = materialCategoryService;
        this.authService = authService;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        setupActionColumn();
        setupSearchCombo();
        loadCategories();
        setupPermissions();
    }

    private void setupSearchCombo() {
        enabledComboBox.getItems().addAll("全部", "启用", "禁用");
        enabledComboBox.setValue("全部");
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        parentColumn.setCellValueFactory(new PropertyValueFactory<>("parentName"));

        enabledColumn.setCellValueFactory(cellData -> new SimpleBooleanProperty(cellData.getValue().isEnabled()));
        enabledColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item ? "启用" : "禁用");
                }
            }
        });
    }

    private void setupActionColumn() {
        boolean canEdit = authService.hasPermission("material:category:edit");
        boolean canDelete = authService.hasPermission("material:category:delete");

        Callback<TableColumn<MaterialCategoryDTO, Boolean>, TableCell<MaterialCategoryDTO, Boolean>> cellFactory =
                param -> new TableCell<>() {
                    final Button editBtn = new Button("修改");
                    final Button deleteBtn = new Button("删除");
                    final HBox pane = new HBox(5);

                    {
                        editBtn.getStyleClass().addAll("action-button", "edit-button");
                        deleteBtn.getStyleClass().addAll("action-button", "delete-button");

                        editBtn.setOnAction(event -> {
                            MaterialCategoryDTO dto = getTableView().getItems().get(getIndex());
                            showEditDialog(dto);
                        });

                        deleteBtn.setOnAction(event -> {
                            MaterialCategoryDTO dto = getTableView().getItems().get(getIndex());
                            deleteCategory(dto);
                        });
                    }

                    @Override
                    protected void updateItem(Boolean item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            pane.getChildren().clear();
                            if (canEdit) pane.getChildren().add(editBtn);
                            if (canDelete) pane.getChildren().add(deleteBtn);
                            setGraphic(pane);
                        }
                    }
                };

        actionColumn.setCellFactory(cellFactory);
        actionColumn.setCellValueFactory(cellData -> new SimpleBooleanProperty(true));
    }

    private void setupPermissions() {
        boolean canAdd = authService.hasPermission("material:category:add");
        boolean canEdit = authService.hasPermission("material:category:edit");
        boolean canDelete = authService.hasPermission("material:category:delete");

        addButton.setVisible(canAdd);
        addButton.setManaged(canAdd);
        editButton.setVisible(canEdit);
        editButton.setManaged(canEdit);
        deleteButton.setVisible(canDelete);
        deleteButton.setManaged(canDelete);

        if (!canEdit && !canDelete) {
            actionColumn.setVisible(false);
        }
    }

    private void loadCategories() {
        List<MaterialCategoryDTO> categories = materialCategoryService.findAll().stream()
                .map(category -> {
                    MaterialCategoryDTO dto = MaterialCategoryDTO.fromEntity(category);
                    if (category.getParentId() != null) {
                        MaterialCategory parent = materialCategoryService.findById(category.getParentId());
                        if (parent != null) {
                            dto.setParentName(parent.getName());
                        }
                    }
                    return dto;
                })
                .collect(Collectors.toList());
        categoryList.setAll(categories);
        categoryTable.setItems(categoryList);
    }

    @FXML
    public void handleSearch() {
        String name = nameSearchField.getText();
        String enabledValue = enabledComboBox.getValue();
        Boolean enabled = null;
        if ("启用".equals(enabledValue)) enabled = true;
        if ("禁用".equals(enabledValue)) enabled = false;

        List<MaterialCategoryDTO> categories = materialCategoryService.search(name, enabled).stream()
                .map(category -> {
                    MaterialCategoryDTO dto = MaterialCategoryDTO.fromEntity(category);
                    if (category.getParentId() != null) {
                        MaterialCategory parent = materialCategoryService.findById(category.getParentId());
                        if (parent != null) {
                            dto.setParentName(parent.getName());
                        }
                    }
                    return dto;
                })
                .collect(Collectors.toList());
        categoryList.setAll(categories);
        categoryTable.setItems(categoryList);
    }

    @FXML
    public void handleReset() {
        nameSearchField.clear();
        enabledComboBox.setValue("全部");
        loadCategories();
    }

    @FXML
    public void showAddDialog() {
        Dialog<MaterialCategory> dialog = createCategoryDialog("新增物料分类", null);
        dialog.showAndWait().ifPresent(category -> {
            try {
                materialCategoryService.create(category);
                loadCategories();
                showSuccessAlert("新增成功", "物料分类新增成功");
            } catch (Exception e) {
                showErrorAlert("新增失败", e.getMessage());
            }
        });
    }

    @FXML
    public void handleBatchEdit() {
        ObservableList<MaterialCategoryDTO> selectedItems = categoryTable.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            showWarningAlert("提示", "请先选择要修改的数据");
            return;
        }
        if (selectedItems.size() > 1) {
            showWarningAlert("提示", "一次只能修改一条数据");
            return;
        }
        showEditDialog(selectedItems.get(0));
    }

    private void showEditDialog(MaterialCategoryDTO dto) {
        MaterialCategory category = materialCategoryService.findById(dto.getId());
        if (category == null) {
            showErrorAlert("错误", "数据不存在");
            return;
        }
        Dialog<MaterialCategory> dialog = createCategoryDialog("修改物料分类", category);
        dialog.showAndWait().ifPresent(updated -> {
            try {
                materialCategoryService.update(updated);
                loadCategories();
                showSuccessAlert("修改成功", "物料分类修改成功");
            } catch (Exception e) {
                showErrorAlert("修改失败", e.getMessage());
            }
        });
    }

    @FXML
    public void handleBatchDelete() {
        ObservableList<MaterialCategoryDTO> selectedItems = categoryTable.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            showWarningAlert("提示", "请先选择要删除的数据");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("确认删除选中的 " + selectedItems.size() + " 条数据?");
        alert.getDialogPane().getStylesheets().add("/css/style.css");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    List<Long> ids = selectedItems.stream()
                            .map(MaterialCategoryDTO::getId)
                            .collect(Collectors.toList());
                    materialCategoryService.deleteByIds(ids);
                    loadCategories();
                    showSuccessAlert("删除成功", "物料分类删除成功");
                } catch (Exception e) {
                    showErrorAlert("删除失败", e.getMessage());
                }
            }
        });
    }

    private void deleteCategory(MaterialCategoryDTO dto) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("确认删除物料分类 " + dto.getName() + "?");
        alert.getDialogPane().getStylesheets().add("/css/style.css");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    materialCategoryService.delete(dto.getId());
                    loadCategories();
                    showSuccessAlert("删除成功", "物料分类删除成功");
                } catch (Exception e) {
                    showErrorAlert("删除失败", e.getMessage());
                }
            }
        });
    }

    private Dialog<MaterialCategory> createCategoryDialog(String title, MaterialCategory category) {
        Dialog<MaterialCategory> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getStylesheets().add("/css/style.css");

        ButtonType saveButtonType = new ButtonType("提交", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.getStyleClass().add("input-field");
        nameField.setPromptText("请输入分类名称");

        TextField descField = new TextField();
        descField.getStyleClass().add("input-field");
        descField.setPromptText("请输入描述（可选）");

        ComboBox<MaterialCategory> parentComboBox = new ComboBox<>();
        parentComboBox.getItems().add(null);
        parentComboBox.getItems().addAll(materialCategoryService.findAll());
        parentComboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(MaterialCategory c) {
                return c == null ? "无（顶级分类）" : c.getName();
            }

            @Override
            public MaterialCategory fromString(String s) {
                return null;
            }
        });

        CheckBox enabledCheckBox = new CheckBox("是否启用");
        enabledCheckBox.setSelected(true);

        if (category != null) {
            nameField.setText(category.getName());
            descField.setText(category.getDescription());
            if (category.getParentId() != null) {
                MaterialCategory parent = materialCategoryService.findById(category.getParentId());
                parentComboBox.setValue(parent);
            }
            enabledCheckBox.setSelected(category.isEnabled());
        }

        Label nameRequired = new Label("*");
        nameRequired.setStyle("-fx-text-fill: red;");

        HBox nameBox = new HBox(5, new Label("分类名称:"), nameRequired);

        grid.add(nameBox, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("上级分类:"), 0, 1);
        grid.add(parentComboBox, 1, 1);
        grid.add(new Label("描述:"), 0, 2);
        grid.add(descField, 1, 2);
        grid.add(new Label("是否启用:"), 0, 3);
        grid.add(enabledCheckBox, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        nameField.textProperty().addListener((obs, oldVal, newVal) ->
                saveButton.setDisable(newVal.trim().isEmpty()));

        if (category != null) {
            saveButton.setDisable(false);
        }

        Platform.runLater(nameField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                MaterialCategory result = category != null ? category : new MaterialCategory();
                result.setName(nameField.getText().trim());
                result.setDescription(descField.getText().trim());
                MaterialCategory selectedParent = parentComboBox.getValue();
                result.setParentId(selectedParent != null ? selectedParent.getId() : null);
                result.setEnabled(enabledCheckBox.isSelected());
                return result;
            }
            return null;
        });

        return dialog;
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add("/css/style.css");
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add("/css/style.css");
        alert.showAndWait();
    }

    private void showWarningAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add("/css/style.css");
        alert.showAndWait();
    }
}
