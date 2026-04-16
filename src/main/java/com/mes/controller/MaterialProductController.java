package com.mes.controller;

import com.mes.dto.BomItemDTO;
import com.mes.dto.MaterialCategoryDTO;
import com.mes.dto.MaterialProductDTO;
import com.mes.entity.BomItem;
import com.mes.entity.MaterialCategory;
import com.mes.entity.MaterialProduct;
import com.mes.service.AuthService;
import com.mes.service.MaterialCategoryService;
import com.mes.service.MaterialProductService;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.Callback;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MaterialProductController {

    private final MaterialProductService productService;
    private final MaterialCategoryService categoryService;
    private final AuthService authService;

    @FXML
    private TreeView<MaterialCategoryDTO> categoryTree;

    @FXML
    private TableView<MaterialProductDTO> productTable;

    @FXML
    private TableColumn<MaterialProductDTO, Long> idColumn;

    @FXML
    private TableColumn<MaterialProductDTO, String> codeColumn;

    @FXML
    private TableColumn<MaterialProductDTO, String> nameColumn;

    @FXML
    private TableColumn<MaterialProductDTO, String> categoryColumn;

    @FXML
    private TableColumn<MaterialProductDTO, String> specificationColumn;

    @FXML
    private TableColumn<MaterialProductDTO, String> unitColumn;

    @FXML
    private TableColumn<MaterialProductDTO, String> typeColumn;

    @FXML
    private TableColumn<MaterialProductDTO, Boolean> enabledColumn;

    @FXML
    private TableColumn<MaterialProductDTO, Boolean> actionColumn;

    @FXML
    private TextField codeSearchField;

    @FXML
    private TextField nameSearchField;

    @FXML
    private ComboBox<String> enabledSearchCombo;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    private ObservableList<MaterialProductDTO> productList = FXCollections.observableArrayList();
    private Long selectedCategoryId = null;

    private TextField codeField;
    private TextField nameField;
    private ComboBox<MaterialCategoryDTO> categoryCombo;
    private TextField specificationField;
    private TextField modelField;
    private TextField unitField;
    private TextField descField;
    private ComboBox<String> typeCombo;
    private TextField minStockField;
    private TextField maxStockField;
    private CheckBox enabledCheckBox;
    private Long editingProductId;

    public MaterialProductController(MaterialProductService productService,
                                     MaterialCategoryService categoryService,
                                     AuthService authService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.authService = authService;
    }

    @FXML
    public void initialize() {
        setupCategoryTree();
        setupTableColumns();
        setupActionColumn();
        setupEnabledSearchCombo();
        loadProducts();
        setupPermissions();
    }

    private void setupCategoryTree() {
        List<MaterialCategoryDTO> categories = categoryService.findAllEnabledTree();
        TreeItem<MaterialCategoryDTO> rootItem = new TreeItem<>(createRootCategory());
        rootItem.setExpanded(true);
        buildTreeItems(rootItem, categories);
        categoryTree.setRoot(rootItem);
        categoryTree.setCellFactory(param -> new TreeCell<>() {
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
        categoryTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                if (newVal.getValue().getId() == null) {
                    selectedCategoryId = null;
                } else {
                    selectedCategoryId = newVal.getValue().getId();
                }
                loadProducts();
            }
        });
    }

    private MaterialCategoryDTO createRootCategory() {
        MaterialCategoryDTO root = new MaterialCategoryDTO();
        root.setName("全部分类");
        return root;
    }

    private void buildTreeItems(TreeItem<MaterialCategoryDTO> parent, List<MaterialCategoryDTO> categories) {
        for (MaterialCategoryDTO category : categories) {
            TreeItem<MaterialCategoryDTO> item = new TreeItem<>(category);
            item.setExpanded(true);
            parent.getChildren().add(item);
            if (category.getChildren() != null && !category.getChildren().isEmpty()) {
                buildTreeItems(item, category.getChildren());
            }
        }
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        specificationColumn.setCellValueFactory(new PropertyValueFactory<>("specification"));
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));

        typeColumn.setCellValueFactory(cellData -> {
            MaterialProduct.MaterialType type = cellData.getValue().getMaterialType();
            String typeName = type != null ? getTypeName(type) : "";
            return new javafx.beans.property.SimpleStringProperty(typeName);
        });

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

    private String getTypeName(MaterialProduct.MaterialType type) {
        switch (type) {
            case MATERIAL: return "原材料";
            case SEMI_FINISHED: return "半成品";
            case FINISHED_PRODUCT: return "产成品";
            default: return "";
        }
    }

    private MaterialProduct.MaterialType getTypeFromName(String name) {
        switch (name) {
            case "原材料": return MaterialProduct.MaterialType.MATERIAL;
            case "半成品": return MaterialProduct.MaterialType.SEMI_FINISHED;
            case "产成品": return MaterialProduct.MaterialType.FINISHED_PRODUCT;
            default: return MaterialProduct.MaterialType.MATERIAL;
        }
    }

    private void setupActionColumn() {
        boolean canEdit = authService.hasPermission("material:edit");
        boolean canDelete = authService.hasPermission("material:delete");
        
        Callback<TableColumn<MaterialProductDTO, Boolean>, TableCell<MaterialProductDTO, Boolean>> cellFactory =
                param -> new TableCell<>() {
                    final Button editBtn = new Button("修改");
                    final Button deleteBtn = new Button("删除");
                    final HBox pane = new HBox(5);

                    {
                        editBtn.getStyleClass().addAll("action-button", "edit-button");
                        deleteBtn.getStyleClass().addAll("action-button", "delete-button");

                        editBtn.setOnAction(event -> {
                            MaterialProductDTO dto = getTableView().getItems().get(getIndex());
                            showEditDialog(dto);
                        });

                        deleteBtn.setOnAction(event -> {
                            MaterialProductDTO dto = getTableView().getItems().get(getIndex());
                            deleteProduct(dto);
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
        editButton.setVisible(canEdit);
        editButton.setManaged(canEdit);
        deleteButton.setVisible(canDelete);
        deleteButton.setManaged(canDelete);

        if (!canEdit && !canDelete) {
            actionColumn.setVisible(false);
        }
    }

    private void loadProducts() {
        List<MaterialProductDTO> products = productService.findByCategoryIdDTO(selectedCategoryId);
        productList.setAll(products);
        productTable.setItems(productList);
    }

    @FXML
    public void handleSearch() {
        String code = codeSearchField.getText();
        String name = nameSearchField.getText();
        Boolean enabled = null;
        String enabledValue = enabledSearchCombo.getValue();
        if ("启用".equals(enabledValue)) {
            enabled = true;
        } else if ("禁用".equals(enabledValue)) {
            enabled = false;
        }
        
        List<MaterialProductDTO> products = productService.searchDTO(code, name, selectedCategoryId, enabled);
        productList.setAll(products);
        productTable.setItems(productList);
    }

    @FXML
    public void handleReset() {
        codeSearchField.clear();
        nameSearchField.clear();
        enabledSearchCombo.setValue("全部");
        loadProducts();
    }

    @FXML
    public void showAddDialog() {
        editingProductId = null;
        Dialog<MaterialProduct> dialog = createProductDialog("新增物料产品", null);
        dialog.showAndWait().ifPresent(product -> {
            try {
                productService.create(product);
                loadProducts();
                showSuccessAlert("新增成功", "物料产品新增成功");
            } catch (Exception e) {
                showErrorAlert("新增失败", e.getMessage());
            }
        });
    }

    @FXML
    public void handleBatchEdit() {
        ObservableList<MaterialProductDTO> selectedItems = productTable.getSelectionModel().getSelectedItems();
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

    private void showEditDialog(MaterialProductDTO dto) {
        MaterialProduct product = productService.findById(dto.getId());
        if (product == null) {
            showErrorAlert("错误", "数据不存在");
            return;
        }
        editingProductId = product.getId();
        Dialog<MaterialProduct> dialog = createProductDialog("修改物料产品", product);
        dialog.showAndWait().ifPresent(updated -> {
            try {
                productService.update(updated);
                loadProducts();
                showSuccessAlert("修改成功", "物料产品修改成功");
            } catch (Exception e) {
                showErrorAlert("修改失败", e.getMessage());
            }
        });
    }

    @FXML
    public void handleBatchDelete() {
        ObservableList<MaterialProductDTO> selectedItems = productTable.getSelectionModel().getSelectedItems();
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
                            .map(MaterialProductDTO::getId)
                            .collect(Collectors.toList());
                    productService.deleteByIds(ids);
                    loadProducts();
                    showSuccessAlert("删除成功", "物料产品删除成功");
                } catch (Exception e) {
                    showErrorAlert("删除失败", e.getMessage());
                }
            }
        });
    }

    private void deleteProduct(MaterialProductDTO dto) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("确认删除物料产品 " + dto.getName() + "?");
        alert.getDialogPane().getStylesheets().add("/css/style.css");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    productService.delete(dto.getId());
                    loadProducts();
                    showSuccessAlert("删除成功", "物料产品删除成功");
                } catch (Exception e) {
                    showErrorAlert("删除失败", e.getMessage());
                }
            }
        });
    }

    private Dialog<MaterialProduct> createProductDialog(String title, MaterialProduct product) {
        Dialog<MaterialProduct> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getStylesheets().add("/css/style.css");
        dialog.getDialogPane().setPrefWidth(800);
        dialog.setResizable(true);

        ButtonType saveButtonType = new ButtonType("提交", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab basicTab = new Tab("基本信息");
        basicTab.setContent(createBasicInfoPane(product));

        Tab bomTab = new Tab("BOM组成");
        bomTab.setContent(createBomPane(product));

        tabPane.getTabs().addAll(basicTab, bomTab);

        dialog.getDialogPane().setContent(tabPane);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        Runnable validateForm = () -> {
            boolean valid = !codeField.getText().trim().isEmpty() 
                    && !nameField.getText().trim().isEmpty() 
                    && !unitField.getText().trim().isEmpty()
                    && categoryCombo.getValue() != null;
            saveButton.setDisable(!valid);
        };

        codeField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        nameField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        unitField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        categoryCombo.valueProperty().addListener((obs, old, newVal) -> validateForm.run());

        if (product != null) {
            saveButton.setDisable(false);
        }

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return collectFormData();
            }
            return null;
        });

        return dialog;
    }

    private MaterialProduct collectFormData() {
        MaterialProduct product = new MaterialProduct();
        if (editingProductId != null) {
            product.setId(editingProductId);
        }
        product.setCode(codeField.getText().trim());
        product.setName(nameField.getText().trim());
        if (categoryCombo.getValue() != null) {
            product.setCategoryId(categoryCombo.getValue().getId());
        }
        product.setSpecification(specificationField.getText().trim());
        product.setModel(modelField.getText().trim());
        product.setUnit(unitField.getText().trim());
        product.setDescription(descField.getText().trim());
        product.setMaterialType(getTypeFromName(typeCombo.getValue()));
        try {
            product.setMinStock(new BigDecimal(minStockField.getText().trim()));
        } catch (NumberFormatException e) {
            product.setMinStock(BigDecimal.ZERO);
        }
        try {
            product.setMaxStock(new BigDecimal(maxStockField.getText().trim()));
        } catch (NumberFormatException e) {
            product.setMaxStock(BigDecimal.ZERO);
        }
        product.setEnabled(enabledCheckBox.isSelected());
        return product;
    }

    private VBox createBasicInfoPane(MaterialProduct product) {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        codeField = new TextField();
        codeField.getStyleClass().add("input-field");
        codeField.setPromptText("请输入物料编码");
        codeField.setPrefWidth(200);

        Button generateCodeBtn = new Button("自动生成");
        generateCodeBtn.getStyleClass().add("default-button");
        generateCodeBtn.setOnAction(e -> {
            String prefix = "M";
            if (categoryCombo.getValue() != null && categoryCombo.getValue().getCode() != null) {
                prefix = categoryCombo.getValue().getCode();
            }
            codeField.setText(productService.generateCode(prefix));
        });

        nameField = new TextField();
        nameField.getStyleClass().add("input-field");
        nameField.setPromptText("请输入物料名称");
        nameField.setPrefWidth(200);

        categoryCombo = new ComboBox<>();
        categoryCombo.setPromptText("请选择分类");
        categoryCombo.setPrefWidth(200);
        List<MaterialCategoryDTO> categories = categoryService.findAllEnabledTree();
        categoryCombo.setItems(FXCollections.observableArrayList(flattenTree(categories)));
        categoryCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(MaterialCategoryDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String indent = "  ".repeat(getIndentLevel(item, categories));
                    setText(indent + item.getName());
                }
            }

            private int getIndentLevel(MaterialCategoryDTO dto, List<MaterialCategoryDTO> cats) {
                if (dto.getParentId() == null) return 0;
                for (MaterialCategoryDTO c : cats) {
                    if (c.getId().equals(dto.getParentId())) {
                        return 1 + getIndentLevel(c, cats);
                    }
                }
                return 0;
            }
        });
        categoryCombo.setButtonCell(new ListCell<>() {
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

        specificationField = new TextField();
        specificationField.getStyleClass().add("input-field");
        specificationField.setPromptText("请输入规格（可选）");
        specificationField.setPrefWidth(200);

        modelField = new TextField();
        modelField.getStyleClass().add("input-field");
        modelField.setPromptText("请输入型号（可选）");
        modelField.setPrefWidth(200);

        unitField = new TextField();
        unitField.getStyleClass().add("input-field");
        unitField.setPromptText("请输入单位");
        unitField.setPrefWidth(200);

        descField = new TextField();
        descField.getStyleClass().add("input-field");
        descField.setPromptText("请输入描述（可选）");
        descField.setPrefWidth(200);

        typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("原材料", "半成品", "产成品");
        typeCombo.setValue("原材料");
        typeCombo.setPrefWidth(200);

        minStockField = new TextField();
        minStockField.getStyleClass().add("input-field");
        minStockField.setPromptText("最小库存量");
        minStockField.setText("0");
        minStockField.setPrefWidth(200);

        maxStockField = new TextField();
        maxStockField.getStyleClass().add("input-field");
        maxStockField.setPromptText("最大库存量");
        maxStockField.setText("0");
        maxStockField.setPrefWidth(200);

        enabledCheckBox = new CheckBox("是否启用");
        enabledCheckBox.setSelected(true);

        if (product != null) {
            codeField.setText(product.getCode());
            nameField.setText(product.getName());
            specificationField.setText(product.getSpecification());
            modelField.setText(product.getModel());
            unitField.setText(product.getUnit());
            descField.setText(product.getDescription());
            if (product.getMaterialType() != null) {
                typeCombo.setValue(getTypeName(product.getMaterialType()));
            }
            if (product.getMinStock() != null) {
                minStockField.setText(product.getMinStock().toString());
            }
            if (product.getMaxStock() != null) {
                maxStockField.setText(product.getMaxStock().toString());
            }
            enabledCheckBox.setSelected(product.isEnabled());
            if (product.getCategoryId() != null) {
                for (MaterialCategoryDTO dto : categoryCombo.getItems()) {
                    if (dto.getId().equals(product.getCategoryId())) {
                        categoryCombo.setValue(dto);
                        break;
                    }
                }
            }
        }

        Label codeRequired = new Label("*");
        codeRequired.setStyle("-fx-text-fill: red;");
        Label nameRequired = new Label("*");
        nameRequired.setStyle("-fx-text-fill: red;");
        Label unitRequired = new Label("*");
        unitRequired.setStyle("-fx-text-fill: red;");
        Label categoryRequired = new Label("*");
        categoryRequired.setStyle("-fx-text-fill: red;");

        int row = 0;
        grid.add(new HBox(5, new Label("物料编码:"), codeRequired), 0, row);
        grid.add(new HBox(5, codeField, generateCodeBtn), 1, row++);

        grid.add(new HBox(5, new Label("物料名称:"), nameRequired), 0, row);
        grid.add(nameField, 1, row++);

        grid.add(new HBox(5, new Label("物料分类:"), categoryRequired), 0, row);
        grid.add(categoryCombo, 1, row++);

        grid.add(new Label("规格:"), 0, row);
        grid.add(specificationField, 1, row++);

        grid.add(new Label("型号:"), 0, row);
        grid.add(modelField, 1, row++);

        grid.add(new HBox(5, new Label("单位:"), unitRequired), 0, row);
        grid.add(unitField, 1, row++);

        grid.add(new Label("描述:"), 0, row);
        grid.add(descField, 1, row++);

        grid.add(new Label("物料类型:"), 0, row);
        grid.add(typeCombo, 1, row++);

        grid.add(new Label("最小库存:"), 0, row);
        grid.add(minStockField, 1, row++);

        grid.add(new Label("最大库存:"), 0, row);
        grid.add(maxStockField, 1, row++);

        grid.add(new Label(""), 0, row);
        grid.add(enabledCheckBox, 1, row);

        vbox.getChildren().add(grid);
        return vbox;
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

    private VBox createBomPane(MaterialProduct product) {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(20));

        Label infoLabel = new Label();
        if (product == null || product.getId() == null) {
            infoLabel.setText("请先保存物料产品基本信息后再配置BOM");
            infoLabel.setStyle("-fx-text-fill: #888;");
            vbox.getChildren().add(infoLabel);
            return vbox;
        }

        Long currentProductId = product.getId();
        infoLabel.setText("当前产品: " + product.getName() + " (" + product.getCode() + ")");
        infoLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        TableView<BomItemDTO> bomTable = new TableView<>();
        bomTable.setPrefHeight(300);

        TableColumn<BomItemDTO, Long> bomIdCol = new TableColumn<>("ID");
        bomIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        bomIdCol.setPrefWidth(50);

        TableColumn<BomItemDTO, String> materialCodeCol = new TableColumn<>("物料编码");
        materialCodeCol.setCellValueFactory(new PropertyValueFactory<>("materialCode"));
        materialCodeCol.setPrefWidth(120);

        TableColumn<BomItemDTO, String> materialNameCol = new TableColumn<>("物料名称");
        materialNameCol.setCellValueFactory(new PropertyValueFactory<>("materialName"));
        materialNameCol.setPrefWidth(150);

        TableColumn<BomItemDTO, String> materialUnitCol = new TableColumn<>("单位");
        materialUnitCol.setCellValueFactory(new PropertyValueFactory<>("materialUnit"));
        materialUnitCol.setPrefWidth(60);

        TableColumn<BomItemDTO, BigDecimal> quantityCol = new TableColumn<>("数量");
        quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        quantityCol.setPrefWidth(80);

        TableColumn<BomItemDTO, String> remarkCol = new TableColumn<>("备注");
        remarkCol.setCellValueFactory(new PropertyValueFactory<>("remark"));
        remarkCol.setPrefWidth(150);

        TableColumn<BomItemDTO, Boolean> bomActionCol = new TableColumn<>("操作");
        bomActionCol.setPrefWidth(150);
        bomActionCol.setCellFactory(param -> new TableCell<>() {
            final Button editBtn = new Button("修改");
            final Button deleteBtn = new Button("删除");
            final HBox pane = new HBox(5);

            {
                editBtn.getStyleClass().addAll("action-button", "edit-button");
                deleteBtn.getStyleClass().addAll("action-button", "delete-button");

                editBtn.setOnAction(event -> {
                    BomItemDTO dto = getTableView().getItems().get(getIndex());
                    showEditBomDialog(dto, bomTable, currentProductId);
                });

                deleteBtn.setOnAction(event -> {
                    BomItemDTO dto = getTableView().getItems().get(getIndex());
                    deleteBomItem(dto, bomTable, currentProductId);
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });

        bomTable.getColumns().addAll(bomIdCol, materialCodeCol, materialNameCol, materialUnitCol, quantityCol, remarkCol, bomActionCol);

        loadBomItems(bomTable, currentProductId);

        Button addBomBtn = new Button("新增BOM项");
        addBomBtn.getStyleClass().add("primary-button");
        addBomBtn.setOnAction(e -> showAddBomDialog(bomTable, currentProductId));

        HBox buttonBox = new HBox(10, addBomBtn);

        vbox.getChildren().addAll(infoLabel, buttonBox, bomTable);
        return vbox;
    }

    private void loadBomItems(TableView<BomItemDTO> table, Long productId) {
        if (productId == null) return;
        List<BomItem> items = productService.getBomItems(productId);
        List<BomItemDTO> dtos = items.stream()
                .map(BomItemDTO::fromEntity)
                .collect(Collectors.toList());
        table.setItems(FXCollections.observableArrayList(dtos));
    }

    private void showAddBomDialog(TableView<BomItemDTO> bomTable, Long productId) {
        Dialog<BomItem> dialog = new Dialog<>();
        dialog.setTitle("新增BOM项");
        dialog.getDialogPane().getStylesheets().add("/css/style.css");

        ButtonType saveButtonType = new ButtonType("提交", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<MaterialProductDTO> materialCombo = new ComboBox<>();
        materialCombo.setPromptText("请选择物料");
        materialCombo.setPrefWidth(300);
        List<MaterialProduct> materials = productService.findAvailableMaterialsForBom(productId);
        materialCombo.setItems(FXCollections.observableArrayList(
                materials.stream().map(MaterialProductDTO::fromEntity).collect(Collectors.toList())
        ));
        materialCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(MaterialProductDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getCode() + " - " + item.getName());
                }
            }
        });
        materialCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(MaterialProductDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getCode() + " - " + item.getName());
                }
            }
        });

        TextField quantityField = new TextField();
        quantityField.getStyleClass().add("input-field");
        quantityField.setPromptText("请输入数量");
        quantityField.setText("1");
        quantityField.setPrefWidth(100);

        TextField remarkField = new TextField();
        remarkField.getStyleClass().add("input-field");
        remarkField.setPromptText("请输入备注（可选）");
        remarkField.setPrefWidth(200);

        Label materialRequired = new Label("*");
        materialRequired.setStyle("-fx-text-fill: red;");
        Label quantityRequired = new Label("*");
        quantityRequired.setStyle("-fx-text-fill: red;");

        grid.add(new HBox(5, new Label("物料:"), materialRequired), 0, 0);
        grid.add(materialCombo, 1, 0);
        grid.add(new HBox(5, new Label("数量:"), quantityRequired), 0, 1);
        grid.add(quantityField, 1, 1);
        grid.add(new Label("备注:"), 0, 2);
        grid.add(remarkField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        materialCombo.valueProperty().addListener((obs, oldVal, newVal) -> 
            saveButton.setDisable(newVal == null || quantityField.getText().trim().isEmpty()));
        quantityField.textProperty().addListener((obs, oldVal, newVal) -> 
            saveButton.setDisable(materialCombo.getValue() == null || newVal.trim().isEmpty()));

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                BomItem item = new BomItem();
                item.setProductId(productId);
                item.setMaterialId(materialCombo.getValue().getId());
                try {
                    item.setQuantity(new BigDecimal(quantityField.getText().trim()));
                } catch (NumberFormatException e) {
                    item.setQuantity(BigDecimal.ONE);
                }
                item.setRemark(remarkField.getText().trim());
                return item;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(item -> {
            try {
                productService.addBomItem(productId, item.getMaterialId(), item.getQuantity(), item.getRemark());
                loadBomItems(bomTable, productId);
                showSuccessAlert("新增成功", "BOM项新增成功");
            } catch (Exception e) {
                showErrorAlert("新增失败", e.getMessage());
            }
        });
    }

    private void showEditBomDialog(BomItemDTO dto, TableView<BomItemDTO> bomTable, Long productId) {
        Dialog<BomItem> dialog = new Dialog<>();
        dialog.setTitle("修改BOM项");
        dialog.getDialogPane().getStylesheets().add("/css/style.css");

        ButtonType saveButtonType = new ButtonType("提交", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        Label materialLabel = new Label(dto.getMaterialCode() + " - " + dto.getMaterialName());

        TextField quantityField = new TextField();
        quantityField.getStyleClass().add("input-field");
        quantityField.setText(dto.getQuantity() != null ? dto.getQuantity().toString() : "1");
        quantityField.setPrefWidth(100);

        TextField remarkField = new TextField();
        remarkField.getStyleClass().add("input-field");
        remarkField.setText(dto.getRemark());
        remarkField.setPrefWidth(200);

        grid.add(new Label("物料:"), 0, 0);
        grid.add(materialLabel, 1, 0);
        grid.add(new Label("数量:"), 0, 1);
        grid.add(quantityField, 1, 1);
        grid.add(new Label("备注:"), 0, 2);
        grid.add(remarkField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                BomItem item = new BomItem();
                item.setId(dto.getId());
                item.setProductId(dto.getProductId());
                item.setMaterialId(dto.getMaterialId());
                try {
                    item.setQuantity(new BigDecimal(quantityField.getText().trim()));
                } catch (NumberFormatException e) {
                    item.setQuantity(BigDecimal.ONE);
                }
                item.setRemark(remarkField.getText().trim());
                return item;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(item -> {
            try {
                productService.updateBomItem(dto.getId(), item.getQuantity(), item.getRemark());
                loadBomItems(bomTable, productId);
                showSuccessAlert("修改成功", "BOM项修改成功");
            } catch (Exception e) {
                showErrorAlert("修改失败", e.getMessage());
            }
        });
    }

    private void deleteBomItem(BomItemDTO dto, TableView<BomItemDTO> bomTable, Long productId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("确认删除该BOM项?");
        alert.getDialogPane().getStylesheets().add("/css/style.css");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    productService.deleteBomItem(dto.getId());
                    loadBomItems(bomTable, productId);
                    showSuccessAlert("删除成功", "BOM项删除成功");
                } catch (Exception e) {
                    showErrorAlert("删除失败", e.getMessage());
                }
            }
        });
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
