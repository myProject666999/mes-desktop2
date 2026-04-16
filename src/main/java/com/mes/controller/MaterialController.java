package com.mes.controller;

import com.mes.dto.BomDTO;
import com.mes.dto.MaterialDTO;
import com.mes.entity.Bom;
import com.mes.entity.Material;
import com.mes.entity.MaterialCategory;
import com.mes.entity.UnitOfMeasure;
import com.mes.service.AuthService;
import com.mes.service.BomService;
import com.mes.service.MaterialCategoryService;
import com.mes.service.MaterialService;
import com.mes.service.UnitOfMeasureService;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MaterialController {

    private final MaterialService materialService;
    private final MaterialCategoryService materialCategoryService;
    private final BomService bomService;
    private final AuthService authService;

    @FXML
    private TreeView<Object> categoryTreeView;

    private java.util.Map<String, Long> categoryIdMap = new java.util.HashMap<>();

    @FXML
    private TableView<MaterialDTO> materialTable;

    @FXML
    private TableColumn<MaterialDTO, Long> idColumn;

    @FXML
    private TableColumn<MaterialDTO, String> codeColumn;

    @FXML
    private TableColumn<MaterialDTO, String> nameColumn;

    @FXML
    private TableColumn<MaterialDTO, String> specColumn;

    @FXML
    private TableColumn<MaterialDTO, String> unitColumn;

    @FXML
    private TableColumn<MaterialDTO, String> categoryColumn;

    @FXML
    private TableColumn<MaterialDTO, BigDecimal> minStockColumn;

    @FXML
    private TableColumn<MaterialDTO, BigDecimal> maxStockColumn;

    @FXML
    private TableColumn<MaterialDTO, Boolean> enabledColumn;

    @FXML
    private TableColumn<MaterialDTO, Boolean> actionColumn;

    @FXML
    private TextField codeSearchField;

    @FXML
    private TextField nameSearchField;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private TabPane detailTabPane;

    @FXML
    private TableView<BomDTO> bomTable;

    @FXML
    private TableColumn<BomDTO, String> bomCodeColumn;

    @FXML
    private TableColumn<BomDTO, String> bomNameColumn;

    @FXML
    private TableColumn<BomDTO, BigDecimal> bomQtyColumn;

    @FXML
    private TableColumn<BomDTO, String> bomRemarkColumn;

    @FXML
    private TableColumn<BomDTO, Boolean> bomActionColumn;

    @FXML
    private Button addBomButton;

    private ObservableList<MaterialDTO> materialList = FXCollections.observableArrayList();
    private ObservableList<BomDTO> bomList = FXCollections.observableArrayList();
    private Material currentMaterial;
    private final UnitOfMeasureService unitOfMeasureService;

    public MaterialController(MaterialService materialService,
                              MaterialCategoryService materialCategoryService,
                              BomService bomService,
                              UnitOfMeasureService unitOfMeasureService,
                              AuthService authService) {
        this.materialService = materialService;
        this.materialCategoryService = materialCategoryService;
        this.bomService = bomService;
        this.unitOfMeasureService = unitOfMeasureService;
        this.authService = authService;
    }

    @FXML
    public void initialize() {
        setupCategoryTree();
        setupMaterialTable();
        setupBomTable();
        setupPermissions();
        loadMaterials();
    }

    private void setupCategoryTree() {
        TreeItem<Object> rootItem = new TreeItem<>("全部物料");
        rootItem.setExpanded(true);
        buildTree(rootItem, materialCategoryService.buildTree());
        categoryTreeView.setRoot(rootItem);
        categoryTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadMaterialsByCategory(newVal);
            }
        });
    }

    private void buildTree(TreeItem<Object> parent, List<MaterialCategory> categories) {
        for (MaterialCategory category : categories) {
            TreeItem<Object> item = new TreeItem<>(category.getName());
            categoryIdMap.put(category.getName(), category.getId());
            parent.getChildren().add(item);
            if (category.getChildren() != null && !category.getChildren().isEmpty()) {
                buildTree(item, category.getChildren());
            }
        }
    }

    private void loadMaterialsByCategory(TreeItem<Object> selectedItem) {
        String categoryName = (String) selectedItem.getValue();
        if ("全部物料".equals(categoryName)) {
            loadMaterials();
        } else {
            Long categoryId = categoryIdMap.get(categoryName);
            if (categoryId != null) {
                List<Long> allCategoryIds = materialCategoryService.getAllChildIds(categoryId);
                loadMaterials(allCategoryIds);
            } else {
                loadMaterials();
            }
        }
    }

    private void setupMaterialTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        specColumn.setCellValueFactory(new PropertyValueFactory<>("specification"));
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        minStockColumn.setCellValueFactory(new PropertyValueFactory<>("minStock"));
        maxStockColumn.setCellValueFactory(new PropertyValueFactory<>("maxStock"));

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

        setupMaterialActionColumn();

        materialTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentMaterial = materialService.findById(newVal.getId());
                loadBomData();
            }
        });
    }

    private void setupMaterialActionColumn() {
        boolean canEdit = authService.hasPermission("material:edit");
        boolean canDelete = authService.hasPermission("material:delete");

        Callback<TableColumn<MaterialDTO, Boolean>, TableCell<MaterialDTO, Boolean>> cellFactory =
                param -> new TableCell<>() {
                    final Button editBtn = new Button("修改");
                    final Button deleteBtn = new Button("删除");
                    final HBox pane = new HBox(5);

                    {
                        editBtn.getStyleClass().addAll("action-button", "edit-button");
                        deleteBtn.getStyleClass().addAll("action-button", "delete-button");

                        editBtn.setOnAction(event -> {
                            MaterialDTO dto = getTableView().getItems().get(getIndex());
                            showEditDialog(dto);
                        });

                        deleteBtn.setOnAction(event -> {
                            MaterialDTO dto = getTableView().getItems().get(getIndex());
                            deleteMaterial(dto);
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

    private void setupBomTable() {
        bomCodeColumn.setCellValueFactory(new PropertyValueFactory<>("childMaterialCode"));
        bomNameColumn.setCellValueFactory(new PropertyValueFactory<>("childMaterialName"));
        bomQtyColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        bomRemarkColumn.setCellValueFactory(new PropertyValueFactory<>("remarks"));

        boolean canEdit = authService.hasPermission("material:bom:edit");
        boolean canDelete = authService.hasPermission("material:bom:delete");

        Callback<TableColumn<BomDTO, Boolean>, TableCell<BomDTO, Boolean>> cellFactory =
                param -> new TableCell<>() {
                    final Button deleteBtn = new Button("删除");
                    final HBox pane = new HBox(5);

                    {
                        deleteBtn.getStyleClass().addAll("action-button", "delete-button");
                        deleteBtn.setOnAction(event -> {
                            BomDTO dto = getTableView().getItems().get(getIndex());
                            deleteBom(dto);
                        });
                    }

                    @Override
                    protected void updateItem(Boolean item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            pane.getChildren().clear();
                            if (canDelete) pane.getChildren().add(deleteBtn);
                            setGraphic(pane);
                        }
                    }
                };

        bomActionColumn.setCellFactory(cellFactory);
        bomActionColumn.setCellValueFactory(cellData -> new SimpleBooleanProperty(true));
    }

    private void setupPermissions() {
        boolean canAdd = authService.hasPermission("material:add");
        boolean canEdit = authService.hasPermission("material:edit");
        boolean canDelete = authService.hasPermission("material:delete");
        boolean canAddBom = authService.hasPermission("material:bom:add");

        addButton.setVisible(canAdd);
        addButton.setManaged(canAdd);
        editButton.setVisible(canEdit);
        editButton.setManaged(canEdit);
        deleteButton.setVisible(canDelete);
        deleteButton.setManaged(canDelete);
        addBomButton.setVisible(canAddBom);
        addBomButton.setManaged(canAddBom);

        if (!canEdit && !canDelete) {
            actionColumn.setVisible(false);
        }
    }

    private void loadMaterials() {
        loadMaterials(null);
    }

    private void loadMaterials(List<Long> categoryIds) {
        String code = codeSearchField.getText();
        String name = nameSearchField.getText();

        List<MaterialDTO> materials = materialService.search(code, name, categoryIds).stream()
                .map(MaterialDTO::fromEntity)
                .collect(Collectors.toList());
        materialList.setAll(materials);
        materialTable.setItems(materialList);
    }

    private void loadBomData() {
        if (currentMaterial != null) {
            List<BomDTO> boms = bomService.findByParentMaterialId(currentMaterial.getId()).stream()
                    .map(BomDTO::fromEntity)
                    .collect(Collectors.toList());
            bomList.setAll(boms);
            bomTable.setItems(bomList);
        } else {
            bomList.clear();
            bomTable.setItems(bomList);
        }
    }

    @FXML
    public void handleSearch() {
        TreeItem<Object> selected = categoryTreeView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            String categoryName = (String) selected.getValue();
            if (!"全部物料".equals(categoryName)) {
                Long categoryId = categoryIdMap.get(categoryName);
                if (categoryId != null) {
                    List<Long> allCategoryIds = materialCategoryService.getAllChildIds(categoryId);
                    loadMaterials(allCategoryIds);
                    return;
                }
            }
        }
        loadMaterials();
    }

    @FXML
    public void handleReset() {
        codeSearchField.clear();
        nameSearchField.clear();
        categoryTreeView.getSelectionModel().selectFirst();
        loadMaterials();
    }

    @FXML
    public void showAddDialog() {
        Dialog<Material> dialog = createMaterialDialog("新增物料产品", null);
        dialog.showAndWait().ifPresent(material -> {
            try {
                materialService.create(material);
                loadMaterials();
                showSuccessAlert("新增成功", "物料产品新增成功");
            } catch (Exception e) {
                showErrorAlert("新增失败", e.getMessage());
            }
        });
    }

    @FXML
    public void handleBatchEdit() {
        ObservableList<MaterialDTO> selectedItems = materialTable.getSelectionModel().getSelectedItems();
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

    private void showEditDialog(MaterialDTO dto) {
        Material material = materialService.findById(dto.getId());
        if (material == null) {
            showErrorAlert("错误", "数据不存在");
            return;
        }
        Dialog<Material> dialog = createMaterialDialog("修改物料产品", material);
        dialog.showAndWait().ifPresent(updated -> {
            try {
                materialService.update(updated);
                loadMaterials();
                showSuccessAlert("修改成功", "物料产品修改成功");
            } catch (Exception e) {
                showErrorAlert("修改失败", e.getMessage());
            }
        });
    }

    @FXML
    public void handleBatchDelete() {
        ObservableList<MaterialDTO> selectedItems = materialTable.getSelectionModel().getSelectedItems();
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
                            .map(MaterialDTO::getId)
                            .collect(Collectors.toList());
                    materialService.deleteByIds(ids);
                    loadMaterials();
                    showSuccessAlert("删除成功", "物料产品删除成功");
                } catch (Exception e) {
                    showErrorAlert("删除失败", e.getMessage());
                }
            }
        });
    }

    private void deleteMaterial(MaterialDTO dto) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("确认删除物料产品 " + dto.getName() + "?");
        alert.getDialogPane().getStylesheets().add("/css/style.css");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    materialService.delete(dto.getId());
                    loadMaterials();
                    showSuccessAlert("删除成功", "物料产品删除成功");
                } catch (Exception e) {
                    showErrorAlert("删除失败", e.getMessage());
                }
            }
        });
    }

    @FXML
    public void showAddBomDialog() {
        if (currentMaterial == null) {
            showWarningAlert("提示", "请先选择一个物料产品");
            return;
        }

        Dialog<Bom> dialog = createBomDialog();
        dialog.showAndWait().ifPresent(bom -> {
            try {
                bom.setParentMaterialId(currentMaterial.getId());
                bomService.create(bom);
                loadBomData();
                showSuccessAlert("新增成功", "BOM组成新增成功");
            } catch (Exception e) {
                showErrorAlert("新增失败", e.getMessage());
            }
        });
    }

    private void deleteBom(BomDTO dto) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("确认删除该BOM组成?");
        alert.getDialogPane().getStylesheets().add("/css/style.css");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    bomService.delete(dto.getId());
                    loadBomData();
                    showSuccessAlert("删除成功", "BOM组成删除成功");
                } catch (Exception e) {
                    showErrorAlert("删除失败", e.getMessage());
                }
            }
        });
    }

    private Dialog<Material> createMaterialDialog(String title, Material material) {
        Dialog<Material> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getStylesheets().add("/css/style.css");
        dialog.setResizable(true);

        ButtonType saveButtonType = new ButtonType("提交", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField codeField = new TextField();
        codeField.getStyleClass().add("input-field");
        codeField.setPromptText("请输入物料编码");

        Button generateCodeBtn = new Button("自动生成");
        generateCodeBtn.setOnAction(e -> codeField.setText(materialService.generateCode(null)));
        HBox codeBox = new HBox(5, codeField, generateCodeBtn);

        TextField nameField = new TextField();
        nameField.getStyleClass().add("input-field");
        nameField.setPromptText("请输入物料名称");

        TextField specField = new TextField();
        specField.getStyleClass().add("input-field");
        specField.setPromptText("请输入规格型号（可选）");

        ComboBox<UnitOfMeasure> unitComboBox = new ComboBox<>();
        unitComboBox.getItems().addAll(unitOfMeasureService.findAll());
        unitComboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(UnitOfMeasure u) {
                return u == null ? "" : u.getName();
            }

            @Override
            public UnitOfMeasure fromString(String s) {
                return null;
            }
        });

        ComboBox<MaterialCategory> categoryComboBox = new ComboBox<>();
        categoryComboBox.getItems().addAll(materialCategoryService.findAll());
        categoryComboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(MaterialCategory c) {
                return c == null ? "" : c.getName();
            }

            @Override
            public MaterialCategory fromString(String s) {
                return null;
            }
        });

        TextField minStockField = new TextField("0");
        minStockField.getStyleClass().add("input-field");
        TextField maxStockField = new TextField("0");
        maxStockField.getStyleClass().add("input-field");

        TextArea remarksArea = new TextArea();
        remarksArea.getStyleClass().add("input-field");
        remarksArea.setPromptText("备注（可选）");
        remarksArea.setPrefRowCount(3);

        CheckBox enabledCheckBox = new CheckBox("是否启用");
        enabledCheckBox.setSelected(true);

        if (material != null) {
            codeField.setText(material.getCode());
            nameField.setText(material.getName());
            specField.setText(material.getSpecification());
            UnitOfMeasure currentUnit = unitOfMeasureService.findAll().stream()
                    .filter(u -> u.getName().equals(material.getUnit()))
                    .findFirst()
                    .orElse(null);
            unitComboBox.setValue(currentUnit);
            MaterialCategory category = materialCategoryService.findById(material.getCategoryId());
            categoryComboBox.setValue(category);
            minStockField.setText(material.getMinStock().toString());
            maxStockField.setText(material.getMaxStock().toString());
            remarksArea.setText(material.getRemarks());
            enabledCheckBox.setSelected(material.isEnabled());
        }

        Label required1 = new Label("*");
        required1.setStyle("-fx-text-fill: red;");
        Label required2 = new Label("*");
        required2.setStyle("-fx-text-fill: red;");
        Label required3 = new Label("*");
        required3.setStyle("-fx-text-fill: red;");
        Label required4 = new Label("*");
        required4.setStyle("-fx-text-fill: red;");

        HBox codeLabelBox = new HBox(5, new Label("物料编码:"), required1);
        HBox nameLabelBox = new HBox(5, new Label("物料名称:"), required2);
        HBox unitLabelBox = new HBox(5, new Label("单位:"), required3);
        HBox categoryLabelBox = new HBox(5, new Label("物料分类:"), required4);

        grid.add(codeLabelBox, 0, 0);
        grid.add(codeBox, 1, 0);
        grid.add(nameLabelBox, 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("规格型号:"), 0, 2);
        grid.add(specField, 1, 2);
        grid.add(unitLabelBox, 0, 3);
        grid.add(unitComboBox, 1, 3);
        grid.add(categoryLabelBox, 0, 4);
        grid.add(categoryComboBox, 1, 4);
        grid.add(new Label("最小库存:"), 0, 5);
        grid.add(minStockField, 1, 5);
        grid.add(new Label("最大库存:"), 0, 6);
        grid.add(maxStockField, 1, 6);
        grid.add(new Label("备注:"), 0, 7);
        grid.add(remarksArea, 1, 7);
        grid.add(new Label("是否启用:"), 0, 8);
        grid.add(enabledCheckBox, 1, 8);

        dialog.getDialogPane().setContent(grid);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        Runnable checkFields = () -> {
            boolean disable = codeField.getText().trim().isEmpty() ||
                    nameField.getText().trim().isEmpty() ||
                    unitComboBox.getValue() == null ||
                    categoryComboBox.getValue() == null;
            saveButton.setDisable(disable);
        };

        codeField.textProperty().addListener((obs, oldVal, newVal) -> checkFields.run());
        nameField.textProperty().addListener((obs, oldVal, newVal) -> checkFields.run());
        unitComboBox.valueProperty().addListener((obs, oldVal, newVal) -> checkFields.run());
        categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> checkFields.run());

        checkFields.run();
        Platform.runLater(codeField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Material result = material != null ? material : new Material();
                result.setCode(codeField.getText().trim());
                result.setName(nameField.getText().trim());
                result.setSpecification(specField.getText().trim());
                if (unitComboBox.getValue() != null) {
                    result.setUnit(unitComboBox.getValue().getName());
                }
                if (categoryComboBox.getValue() != null) {
                    result.setCategoryId(categoryComboBox.getValue().getId());
                }
                try {
                    result.setMinStock(new BigDecimal(minStockField.getText().trim()));
                } catch (Exception e) {
                    result.setMinStock(BigDecimal.ZERO);
                }
                try {
                    result.setMaxStock(new BigDecimal(maxStockField.getText().trim()));
                } catch (Exception e) {
                    result.setMaxStock(BigDecimal.ZERO);
                }
                result.setRemarks(remarksArea.getText().trim());
                result.setEnabled(enabledCheckBox.isSelected());
                return result;
            }
            return null;
        });

        return dialog;
    }

    private Dialog<Bom> createBomDialog() {
        Dialog<Bom> dialog = new Dialog<>();
        dialog.setTitle("选择BOM物料");
        dialog.getDialogPane().getStylesheets().add("/css/style.css");

        ButtonType saveButtonType = new ButtonType("确认", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        List<Material> availableMaterials = materialService.findAll().stream()
                .filter(m -> !m.getId().equals(currentMaterial.getId()))
                .collect(Collectors.toList());

        TableView<Material> materialSelectTable = new TableView<>();
        materialSelectTable.setPrefHeight(300);
        TableColumn<Material, String> selectCodeCol = new TableColumn<>("物料编码");
        selectCodeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        selectCodeCol.setPrefWidth(120);
        TableColumn<Material, String> selectNameCol = new TableColumn<>("物料名称");
        selectNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        selectNameCol.setPrefWidth(150);
        TableColumn<Material, String> selectUnitCol = new TableColumn<>("单位");
        selectUnitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
        selectUnitCol.setPrefWidth(80);
        materialSelectTable.getColumns().addAll(selectCodeCol, selectNameCol, selectUnitCol);
        materialSelectTable.getItems().addAll(availableMaterials);

        TextField qtyField = new TextField("1");
        qtyField.getStyleClass().add("input-field");
        qtyField.setPromptText("数量");

        TextArea remarkArea = new TextArea();
        remarkArea.getStyleClass().add("input-field");
        remarkArea.setPromptText("备注（可选）");
        remarkArea.setPrefRowCount(2);

        HBox qtyBox = new HBox(10, new Label("数量:"), qtyField);

        content.getChildren().addAll(new Label("请选择子物料:"), materialSelectTable, qtyBox, new Label("备注:"), remarkArea);
        dialog.getDialogPane().setContent(content);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        materialSelectTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) ->
                saveButton.setDisable(newVal == null));

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Material selected = materialSelectTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Bom bom = new Bom();
                    bom.setChildMaterialId(selected.getId());
                    try {
                        bom.setQuantity(new BigDecimal(qtyField.getText().trim()));
                    } catch (Exception e) {
                        bom.setQuantity(BigDecimal.ONE);
                    }
                    bom.setRemarks(remarkArea.getText().trim());
                    return bom;
                }
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
