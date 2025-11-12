package com.javafx.demo;

import com.javafx.demo.app.Session;
import com.javafx.demo.dao.LocationDao;
import com.javafx.demo.dao.ProductDao;
import com.javafx.demo.dao.ProductStockDao;
import com.javafx.demo.service.InventoryService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

public class TransfersController {

    @FXML private ComboBox<LocationDao.Location> fromLocationCombo;
    @FXML private ComboBox<LocationDao.Location> toLocationCombo;
    @FXML private TextField fromSearchField;
    @FXML private TableView<LocationProductRow> fromTable;
    @FXML private TableColumn<LocationProductRow, String> fromProductColumn;
    @FXML private TableColumn<LocationProductRow, Integer> fromQtyColumn;
    @FXML private TableView<LocationProductRow> toTable;
    @FXML private TableColumn<LocationProductRow, String> toProductColumn;
    @FXML private TableColumn<LocationProductRow, Integer> toQtyColumn;
    @FXML private Label messageLabel;

    private final LocationDao locationDao = new LocationDao();
    private final ProductStockDao stockDao = new ProductStockDao();
    private final ProductDao productDao = new ProductDao();
    private final InventoryService inventoryService = new InventoryService();

    @FXML
    private void initialize() {
        fromProductColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        fromQtyColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        toProductColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        toQtyColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        var locs = FXCollections.observableArrayList(locationDao.findAll());
        fromLocationCombo.setItems(locs);
        toLocationCombo.setItems(FXCollections.observableArrayList(locs));
        // renderers for name only
        java.util.function.Consumer<ComboBox<LocationDao.Location>> decorate = cb -> {
            cb.setCellFactory(list -> new ListCell<>() {
                @Override protected void updateItem(LocationDao.Location loc, boolean empty) {
                    super.updateItem(loc, empty);
                    setText(empty || loc == null ? null : loc.name());
                }
            });
            cb.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(LocationDao.Location loc, boolean empty) {
                    super.updateItem(loc, empty);
                    setText(empty || loc == null ? "Select location" : loc.name());
                }
            });
        };
        decorate.accept(fromLocationCombo);
        decorate.accept(toLocationCombo);
        if (!locs.isEmpty()) {
            fromLocationCombo.getSelectionModel().select(0);
            if (locs.size() > 1) toLocationCombo.getSelectionModel().select(1);
        }
        loadFromTable();
        loadToTable();

        fromLocationCombo.setOnAction(e -> loadFromTable());
        toLocationCombo.setOnAction(e -> loadToTable());
        fromSearchField.textProperty().addListener((obs, o, n) -> loadFromTable());

        // Enable multi-select
        fromTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private void loadFromTable() {
        var loc = fromLocationCombo.getSelectionModel().getSelectedItem();
        if (loc == null) return;
        ObservableList<LocationProductRow> list = FXCollections.observableArrayList();
        var rows = stockDao.findByLocation(loc.id());
        String q = fromSearchField != null && fromSearchField.getText() != null ? fromSearchField.getText().trim().toLowerCase() : "";
        for (var r : rows) {
            if (!q.isBlank() && !r.name().toLowerCase().contains(q)) continue;
            list.add(new LocationProductRow(r.productId(), r.name(), r.quantity()));
        }
        fromTable.getSelectionModel().clearSelection();
        fromTable.setItems(list);
    }

    private void loadToTable() {
        var loc = toLocationCombo.getSelectionModel().getSelectedItem();
        if (loc == null) return;
        ObservableList<LocationProductRow> list = FXCollections.observableArrayList();
        for (var r : stockDao.findByLocation(loc.id())) {
            list.add(new LocationProductRow(r.productId(), r.name(), r.quantity()));
        }
        toTable.getSelectionModel().clearSelection();
        toTable.setItems(list);
    }

    @FXML
    private void onTransferClick(ActionEvent event) {
        var fromLoc = fromLocationCombo.getSelectionModel().getSelectedItem();
        var toLoc = toLocationCombo.getSelectionModel().getSelectedItem();
        var selected = fromTable.getSelectionModel().getSelectedItems();
        if (fromLoc == null || toLoc == null || selected == null || selected.isEmpty()) {
            showError("Select from/to locations and at least one product to transfer");
            return;
        }
        if (fromLoc.id() == toLoc.id()) {
            showError("From and To must be different");
            return;
        }
        if (selected.size() == 1) {
            var row = selected.get(0);
            var dlg = new TextInputDialog();
            dlg.setTitle("Transfer");
            dlg.setHeaderText("Transfer " + row.getProductName() + " from " + fromLoc.name() + " to " + toLoc.name());
            dlg.setContentText("Quantity (available " + row.getQuantity() + "):");
            dlg.showAndWait().ifPresent(s -> {
                try {
                    int qty = Integer.parseInt(s.trim());
                    var user = Session.getInstance().getCurrentUser();
                    inventoryService.transfer(row.productId(), fromLoc.id(), toLoc.id(), user.id(), qty, "");
                    showSuccess("Transferred " + qty + " " + row.getProductName());
                    loadFromTable();
                    // re-apply search filter automatically
                    loadToTable();
                } catch (NumberFormatException ex) {
                    showError("Quantity must be a number");
                } catch (Exception ex) {
                    showError(ex.getMessage());
                }
            });
        } else {
            var dlg = new TextInputDialog();
            dlg.setTitle("Bulk Transfer");
            dlg.setHeaderText("Transfer " + selected.size() + " products from " + fromLoc.name() + " to " + toLoc.name());
            dlg.setContentText("Quantity to transfer for each selected item:");
            dlg.showAndWait().ifPresent(s -> {
                try {
                    int qtyEach = Integer.parseInt(s.trim());
                    var user = Session.getInstance().getCurrentUser();
                    for (var row : selected) {
                        inventoryService.transfer(row.productId(), fromLoc.id(), toLoc.id(), user.id(), qtyEach, "");
                    }
                    showSuccess("Transferred " + qtyEach + " units for " + selected.size() + " products");
                    loadFromTable();
                    // re-apply to table view
                    loadToTable();
                } catch (NumberFormatException ex) {
                    showError("Quantity must be a number");
                } catch (Exception ex) {
                    showError(ex.getMessage());
                }
            });
        }
    }

    @FXML
    private void onQuickTransfer(ActionEvent event) {
        var dialog = new Dialog<Void>();
        dialog.setTitle("Quick Transfer");

        var productCombo = new ComboBox<com.javafx.demo.model.Product>();
        productCombo.setItems(FXCollections.observableArrayList(productDao.findAll()));
        productCombo.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(com.javafx.demo.model.Product p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : p.name());
            }
        });
        productCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(com.javafx.demo.model.Product p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "Select product" : p.name());
            }
        });

        var locs = FXCollections.observableArrayList(locationDao.findAll());
        var fromLocCombo = new ComboBox<LocationDao.Location>(locs);
        var toLocCombo = new ComboBox<LocationDao.Location>(FXCollections.observableArrayList(locs));
        // apply renderers for dialog combos
        java.util.function.Consumer<ComboBox<LocationDao.Location>> decorate = cb -> {
            cb.setCellFactory(list -> new ListCell<>() {
                @Override protected void updateItem(LocationDao.Location loc, boolean empty) {
                    super.updateItem(loc, empty);
                    setText(empty || loc == null ? null : loc.name());
                }
            });
            cb.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(LocationDao.Location loc, boolean empty) {
                    super.updateItem(loc, empty);
                    setText(empty || loc == null ? "Select location" : loc.name());
                }
            });
        };
        decorate.accept(fromLocCombo);
        decorate.accept(toLocCombo);
        var qtyField = new TextField();
        qtyField.setPromptText("Quantity");
        var notesField = new TextField();
        notesField.setPromptText("Notes (optional)");
        var keepOpenCheckbox = new CheckBox("Keep dialog open");

        var grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8);
        grid.addRow(0, new Label("Product:"), productCombo);
        grid.addRow(1, new Label("From:"), fromLocCombo);
        grid.addRow(2, new Label("To:"), toLocCombo);
        grid.addRow(3, new Label("Quantity:"), qtyField);
        grid.addRow(4, new Label("Notes:"), notesField);
        grid.addRow(5, keepOpenCheckbox);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Keep-open behavior and validation
        var okBtn = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(ActionEvent.ACTION, evt -> {
            try {
                var p = productCombo.getSelectionModel().getSelectedItem();
                var f = fromLocCombo.getSelectionModel().getSelectedItem();
                var t = toLocCombo.getSelectionModel().getSelectedItem();
                int q = Integer.parseInt(qtyField.getText().trim());
                var user = Session.getInstance().getCurrentUser();
                if (p == null || f == null || t == null) {
                    throw new IllegalArgumentException("Product, From, and To locations are required.");
                }
                inventoryService.transfer(p.id(), f.id(), t.id(), user.id(), q, notesField.getText() != null ? notesField.getText().trim() : "");
                showSuccess("Quick transferred " + q + " " + p.name());
                loadFromTable(); loadToTable();
                if (keepOpenCheckbox.isSelected()) {
                    qtyField.clear();
                    Platform.runLater(qtyField::requestFocus);
                    evt.consume(); // keep dialog open
                }
            } catch (NumberFormatException ex) {
                showError("Quantity must be a number.");
                evt.consume();
            } catch (IllegalArgumentException ex) {
                showError(ex.getMessage());
                evt.consume();
            } catch (Exception ex) {
                showError("Error: " + ex.getMessage());
                evt.consume();
            }
        });
        dialog.setResultConverter(bt -> null);
        dialog.showAndWait();
    }

    private void showError(String msg) {
        if (messageLabel != null) {
            messageLabel.setTextFill(javafx.scene.paint.Color.RED);
            messageLabel.setText(msg);
        }
    }

    private void showSuccess(String msg) {
        if (messageLabel != null) {
            messageLabel.setTextFill(javafx.scene.paint.Color.GREEN);
            messageLabel.setText(msg);
        }
    }

    public static class LocationProductRow {
        private final int productId;
        private final String productName;
        private final Integer quantity;
        public LocationProductRow(int productId, String productName, Integer quantity) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
        }
        public int productId(){ return productId; }
        public String getProductName(){ return productName; }
        public Integer getQuantity(){ return quantity; }
    }
}


