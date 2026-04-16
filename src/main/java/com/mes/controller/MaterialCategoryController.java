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

    private final MaterialCategoryService categoryService;
    private final AuthService authService;

    @FXML
    private TreeView<MaterialCategoryDTO> categoryTree;

    @FXML
    private TableView<MaterialCategoryDTO> categoryTable;

    @FXML
    private TableColumn<MaterialCategoryDTO, Long> idColumn;

    @FXML
    private TableColumn<MaterialCategoryDTO, String> codeColumn;

    @FXML
    private TableColumn<MaterialCategoryDTO, String> nameColumn;

    @FXML
    private TableColumn<MaterialCategoryDTO, String> descriptionColumn;

    @FXML
    private TableColumn<MaterialCategoryDTO, Integer> sortOrderColumn;

    @FXML
    private TableColumn<MaterialCategoryDTO, Boolean> enabledColumn;

    @FXML
    private TableColumn<MaterialCategoryDTO, Boolean> actionColumn;

    @FXML
    private TextField nameSearchField;

    @FXML
    private ComboBox<String> enabledSearchCombo;

    @FXML
    private Button addButton;

    @FXML
    private Button expandAllButton;

    @FXML
    private Button collapseAllButton;

    private ObservableList<MaterialCategoryDTO> categoryList = FXCollections.observableArrayList();

    public MaterialCategoryController(MaterialCategoryService categoryService, AuthService authService) {
        this.categoryService = categoryService;
        this.authService = authService;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        setupActionColumn();
        setupEnabledSearchCombo();
        loadCategories();
        setupPermissions();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        sortOrderColumn.setCellValueFactory(new PropertyValueFactory<>("sortOrder"));

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
        boolean canEdit = authService.hasPermission("material:edit");
        boolean canDelete = authService.hasPermission("material:delete");
        
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

    private void setupEnabledSearchCombo() {
        enabledSearchCombo.getItems().addAll("全部", "启用", "禁用");
        enabledSearchCombo.setValue("全部");
    }

    private void setupPermissions() {
        boolean canAdd = authService.hasPermission("material:add");
        boolean canEdit = authService.hasPermission("material:edit");
        boolean canDelete = authService.hasPermission("material:delete");

        addButton.setVisible(canAdd);
        addButton.setManaged(canAdd);

        if (!canEdit && !canDelete) {
            actionColumn.setVisible(false);
        }
    }

    private void loadCategories() {
        List<MaterialCategoryDTO> categories = categoryService.findAllTree();
        categoryList.setAll(flattenTree(categories));
        categoryTable.setItems(categoryList);
    }

    private List<MaterialCategoryDTO> flattenTree(List<MaterialCategoryDTO> tree) {
        List<MaterialCategoryDTO> result = new java.util.ArrayList<>();
        for (MaterialCategoryDTO dto : tree) {
            result.add(dto);
            if (dto.getChildren() != null && !dto.getChildren().isEmpty()) {
                result.addAll(flattenTree(dto.getChildren()));
            }
        }
        return result;
    }

    @FXML
    public void handleSearch() {
        String name = nameSearchField.getText();
        Boolean enabled = null;
        String enabledValue = enabledSearchCombo.getValue();
        if ("启用".equals(enabledValue)) {
            enabled = true;
        } else if ("禁用".equals(enabledValue)) {
            enabled = false;
        }
        
        List<MaterialCategoryDTO> categories = categoryService.searchTree(name, enabled);
        categoryList.setAll(flattenTree(categories));
        categoryTable.setItems(categoryList);
    }

    @FXML
    public void handleReset() {
        nameSearchField.clear();
        enabledSearchCombo.setValue("全部");
        loadCategories();
    }

    @FXML
    public void showAddDialog() {
        Dialog<MaterialCategory> dialog = createCategoryDialog("新增物料产品分类", null);
        dialog.showAndWait().ifPresent(category -> {
            try {
                categoryService.create(category);
                loadCategories();
                showSuccessAlert("新增成功", "物料产品分类新增成功");
            } catch (Exception e) {
                showErrorAlert("新增失败", e.getMessage());
            }
        });
    }

    private void showEditDialog(MaterialCategoryDTO dto) {
        MaterialCategory category = categoryService.findById(dto.getId());
        if (category == null) {
            showErrorAlert("错误", "数据不存在");
            return;
        }
        Dialog<MaterialCategory> dialog = createCategoryDialog("修改物料产品分类", category);
        dialog.showAndWait().ifPresent(updated -> {
            try {
                categoryService.update(updated);
                loadCategories();
                showSuccessAlert("修改成功", "物料产品分类修改成功");
            } catch (Exception e) {
                showErrorAlert("修改失败", e.getMessage());
            }
        });
    }

    private void deleteCategory(MaterialCategoryDTO dto) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("确认删除分类 " + dto.getName() + "?");
        alert.getDialogPane().getStylesheets().add("/css/style.css");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    categoryService.delete(dto.getId());
                    loadCategories();
                    showSuccessAlert("删除成功", "物料产品分类删除成功");
                } catch (Exception e) {
                    showErrorAlert("删除失败", e.getMessage());
                }
            }
        });
    }

    @FXML
    public void handleExpandAll() {
        expandAllRows(categoryTable);
    }

    @FXML
    public void handleCollapseAll() {
        collapseAllRows(categoryTable);
    }

    private void expandAllRows(TableView<?> table) {
        table.getSelectionModel().selectFirst();
        table.scrollTo(0);
    }

    private void collapseAllRows(TableView<?> table) {
        table.getSelectionModel().clearSelection();
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

        TextField codeField = new TextField();
        codeField.getStyleClass().add("input-field");
        codeField.setPromptText("请输入分类编码");

        TextField nameField = new TextField();
        nameField.getStyleClass().add("input-field");
        nameField.setPromptText("请输入分类名称");

        ComboBox<MaterialCategoryDTO> parentCombo = new ComboBox<>();
        parentCombo.setPromptText("请选择父级分类（可选）");
        parentCombo.setPrefWidth(200);
        List<MaterialCategoryDTO> allCategories = categoryService.findAllTree();
        parentCombo.setItems(FXCollections.observableArrayList(flattenTree(allCategories)));
        parentCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(MaterialCategoryDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String indent = "  ".repeat(getIndentLevel(item, allCategories));
                    setText(indent + item.getName());
                }
            }

            private int getIndentLevel(MaterialCategoryDTO dto, List<MaterialCategoryDTO> categories) {
                if (dto.getParentId() == null) return 0;
                for (MaterialCategoryDTO c : categories) {
                    if (c.getId().equals(dto.getParentId())) {
                        return 1 + getIndentLevel(c, categories);
                    }
                }
                return 0;
            }
        });
        parentCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(MaterialCategoryDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        TextField descField = new TextField();
        descField.getStyleClass().add("input-field");
        descField.setPromptText("请输入描述（可选）");

        TextField sortOrderField = new TextField();
        sortOrderField.getStyleClass().add("input-field");
        sortOrderField.setPromptText("请输入排序号（默认0）");
        sortOrderField.setText("0");

        CheckBox enabledCheckBox = new CheckBox("是否启用");
        enabledCheckBox.setSelected(true);

        if (category != null) {
            codeField.setText(category.getCode());
            nameField.setText(category.getName());
            descField.setText(category.getDescription());
            if (category.getSortOrder() != null) {
                sortOrderField.setText(String.valueOf(category.getSortOrder()));
            }
            enabledCheckBox.setSelected(category.isEnabled());
            if (category.getParentId() != null) {
                for (MaterialCategoryDTO dto : parentCombo.getItems()) {
                    if (dto.getId().equals(category.getParentId())) {
                        parentCombo.setValue(dto);
                        break;
                    }
                }
            }
        }

        Label codeRequired = new Label("*");
        codeRequired.setStyle("-fx-text-fill: red;");
        Label nameRequired = new Label("*");
        nameRequired.setStyle("-fx-text-fill: red;");

        grid.add(new HBox(5, new Label("分类编码:"), codeRequired), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new HBox(5, new Label("分类名称:"), nameRequired), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("父级分类:"), 0, 2);
        grid.add(parentCombo, 1, 2);
        grid.add(new Label("描述:"), 0, 3);
        grid.add(descField, 1, 3);
        grid.add(new Label("排序号:"), 0, 4);
        grid.add(sortOrderField, 1, 4);
        grid.add(new Label(""), 0, 5);
        grid.add(enabledCheckBox, 1, 5);

        dialog.getDialogPane().setContent(grid);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        codeField.textProperty().addListener((obs, oldVal, newVal) -> 
            saveButton.setDisable(newVal.trim().isEmpty() || nameField.getText().trim().isEmpty()));
        nameField.textProperty().addListener((obs, oldVal, newVal) -> 
            saveButton.setDisable(newVal.trim().isEmpty() || codeField.getText().trim().isEmpty()));

        if (category != null) {
            saveButton.setDisable(false);
        }

        Platform.runLater(codeField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                MaterialCategory result = category != null ? category : new MaterialCategory();
                result.setCode(codeField.getText().trim());
                result.setName(nameField.getText().trim());
                MaterialCategoryDTO selectedParent = parentCombo.getValue();
                result.setParentId(selectedParent != null ? selectedParent.getId() : null);
                result.setDescription(descField.getText().trim());
                try {
                    result.setSortOrder(Integer.parseInt(sortOrderField.getText().trim()));
                } catch (NumberFormatException e) {
                    result.setSortOrder(0);
                }
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
