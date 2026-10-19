package com.scanshift.ui;

import com.scanshift.dao.ProductDAO;
import com.scanshift.dao.SaleDAO;
import com.scanshift.model.Product;
import com.scanshift.model.Sale;
import com.scanshift.model.SaleItem;
import com.scanshift.service.BillingService;
import com.scanshift.service.DailyReportExcelExporter;
import com.scanshift.service.InventoryService;
import com.scanshift.service.ReportService;
import com.scanshift.util.PriceFormatter;
import com.scanshift.util.ValidationUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.RenderingHints;
import java.awt.print.PrinterException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Color NAVY = new Color(0x17, 0x25, 0x54);
    private static final Color NAVY_LIGHT = new Color(0x23, 0x36, 0x68);
    private static final Color ORANGE = new Color(0xF9, 0x73, 0x16);
    private static final Color GREEN = new Color(0x16, 0xA3, 0x4A);
    private static final Color AMBER = new Color(0xF5, 0x9E, 0x0B);
    private static final Color RED = new Color(0xDC, 0x26, 0x26);
    private static final Color CANVAS = new Color(0xF8, 0xFA, 0xFC);
    private static final Color TEXT = new Color(0x1E, 0x29, 0x3B);
    private static final Color WHITE = Color.WHITE;
    private static final Font APP_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    static {
        UIManager.put("Panel.background", CANVAS);
        UIManager.put("OptionPane.background", CANVAS);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("TextField.background", WHITE);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("Label.foreground", TEXT);
    }

    private final ProductDAO productDAO = new ProductDAO();
    private final SaleDAO saleDAO = new SaleDAO();
    private final InventoryService inventoryService = new InventoryService();
    private final ReportService reportService = new ReportService();

    private final List<BillLine> billLines = new ArrayList<>();
    private final JTabbedPane tabs = new JTabbedPane();
    private final List<JButton> navigationButtons = new ArrayList<>();
    private final JLabel pageTitleLabel = new JLabel("Dashboard");
    private final JLabel billTotalLabel = new JLabel("₹0.00");

    private final JLabel statusLabel = new JLabel("Connecting to MySQL...");
    private final JLabel dashboardRevenueLabel = new JLabel("₹0.00");
    private final JLabel dashboardBillsLabel = new JLabel("0");
    private final JLabel dashboardItemsLabel = new JLabel("0");
    private final JLabel dashboardProductsLabel = new JLabel("0");
    private final JLabel dashboardLowStockLabel = new JLabel("0");
    private final JLabel dashboardInventoryLabel = new JLabel("0");
    private final JLabel dashboardInventoryValueLabel = new JLabel("₹0.00");
    private final DefaultTableModel recentSalesTableModel = new DefaultTableModel(
            new String[]{"Receipt", "Time", "Total"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable recentSalesTable = new JTable(recentSalesTableModel);

    private final JTextField wholesaleBarcodeField = new JTextField(18);
    private final JTextField retailBarcodeField = new JTextField(18);
    private final JSpinner wholesaleQtySpinner = new JSpinner(new javax.swing.SpinnerNumberModel(1, 1, 999, 1));
    private final JSpinner retailQtySpinner = new JSpinner(new javax.swing.SpinnerNumberModel(1, 1, 999, 1));
    private final DefaultTableModel billTableModel = new DefaultTableModel(
            new String[]{"Barcode", "Product", "Scanner", "Price", "Qty", "Line Total"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable billTable = new JTable(billTableModel);

    private final JTextField productBarcodeField = new JTextField(18);
    private final JTextField productNameField = new JTextField(18);
    private final JTextField wholesalePriceField = new JTextField(18);
    private final JTextField retailPriceField = new JTextField(18);
    private final JTextField productStockField = new JTextField(18);
    private final JTextField thresholdField = new JTextField(18);
    private final JTextField productSearchField = new JTextField(18);
    private final DefaultTableModel productTableModel = new DefaultTableModel(
            new String[]{"ID", "Barcode", "Product", "Wholesale", "Retail", "Stock", "Threshold"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable productTable = new JTable(productTableModel);

    private final JTextField stockSearchField = new JTextField(18);
    private final JTextField restockBarcodeField = new JTextField(18);
    private final JTextField restockQtyField = new JTextField(10);
    private final DefaultTableModel stockTableModel = new DefaultTableModel(
            new String[]{"Barcode", "Product", "Stock", "Threshold", "Wholesale", "Retail", "Stock Value", "Status"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable stockTable = new JTable(stockTableModel);

    private final JTextField reportDateField = new JTextField(LocalDate.now().format(DATE_FORMATTER), 12);
    private final DefaultTableModel reportTableModel = new DefaultTableModel(
            new String[]{"Bill #", "Time", "Barcode", "Product", "Scanner", "Unit Price", "Qty", "Line Total"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable reportTable = new JTable(reportTableModel);
    private final JLabel reportSelectedDateLabel = new JLabel("Selected report date: ");
    private final JLabel dailyBillsLabel = new JLabel("0");
    private final JLabel dailyQtyLabel = new JLabel("0");
    private final JLabel wholesaleTotalLabel = new JLabel("₹0.00");
    private final JLabel retailTotalLabel = new JLabel("₹0.00");
    private final JLabel grandTotalLabel = new JLabel("₹0.00");

    private final DefaultTableModel salesHistoryTableModel = new DefaultTableModel(
            new String[]{"Bill #", "Date/Time", "Total", "Items"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable salesHistoryTable = new JTable(salesHistoryTableModel);
    private final JComboBox<String> historyScannerCombo = new JComboBox<>(new String[]{"ALL", "WHOLESALE", "RETAIL"});

    public MainFrame() {
        setTitle("ScanShift — Dual-Scanner Smart Billing and Inventory");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1440, 900);
        setMinimumSize(new Dimension(1000, 680));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        tabs.addTab("Dashboard", createDashboardPanel());
        tabs.addTab("Billing", createBillingPanel());
        tabs.addTab("Product Management", createProductPanel());
        tabs.addTab("Stock Management", createStockPanel());
        tabs.addTab("Daily Sales Report", createReportPanel());
        tabs.addTab("Sales History", createHistoryPanel());
        tabs.setUI(new BasicTabbedPaneUI() {
            @Override
            protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {
                return 0;
            }

            @Override
            protected void paintTabArea(Graphics graphics, int tabPlacement, int selectedIndex) {
            }
        });

        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(CANVAS);
        JPanel pageHeader = new JPanel(new BorderLayout());
        pageHeader.setBackground(WHITE);
        pageHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(18, 28, 16, 28)));
        pageTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 23));
        pageTitleLabel.setForeground(NAVY);
        JLabel pageSubtitle = new JLabel("Dual-Scanner Smart Billing and Inventory Management");
        pageSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pageSubtitle.setForeground(new Color(100, 116, 139));
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(pageTitleLabel);
        heading.add(Box.createVerticalStrut(3));
        heading.add(pageSubtitle);
        pageHeader.add(heading, BorderLayout.WEST);
        JLabel dateBadge = new JLabel("TODAY  ·  " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        dateBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        dateBadge.setForeground(NAVY);
        dateBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        pageHeader.add(dateBadge, BorderLayout.EAST);
        workspace.add(pageHeader, BorderLayout.NORTH);
        workspace.add(tabs, BorderLayout.CENTER);

        add(createSidebar(), BorderLayout.WEST);
        add(workspace, BorderLayout.CENTER);

        add(statusLabel, BorderLayout.SOUTH);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)));
        statusLabel.setForeground(new Color(73, 91, 111));
        statusLabel.setBackground(WHITE);
        statusLabel.setOpaque(true);

        styleComponentTree(this);
        updateNavigationSelection(0);

        loadDashboard();
        loadRecentSales();
        loadProducts();
        loadStockTable();
        refreshReport();
        refreshHistory();
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBackground(NAVY);
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 14, 18, 14));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        JLabel logo = new JLabel("ScanShift");
        logo.setForeground(WHITE);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        JLabel tagline = new JLabel("SMART RETAIL DESK");
        tagline.setForeground(new Color(164, 183, 208));
        tagline.setFont(new Font("Segoe UI", Font.BOLD, 10));
        tagline.setBorder(BorderFactory.createEmptyBorder(5, 2, 24, 0));
        brand.add(logo);
        brand.add(tagline);

        JPanel navigation = new JPanel();
        navigation.setOpaque(false);
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        String[] labels = {"⌂   Dashboard", "▣   Billing", "＋   Products", "▤   Stock", "◷   Sales Report", "≡   Sales History"};
        ButtonGroup group = new ButtonGroup();
        for (int index = 0; index < labels.length; index++) {
            final int pageIndex = index;
            JButton button = new JButton(labels[index]);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            button.setPreferredSize(new Dimension(198, 46));
            button.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 10));
            button.setFont(new Font("Segoe UI", Font.BOLD, 13));
            button.setFocusPainted(false);
            button.setContentAreaFilled(false);
            button.setForeground(new Color(203, 215, 231));
            button.addActionListener(event -> setSelectedTab(pageIndex));
            group.add(button);
            navigationButtons.add(button);
            navigation.add(button);
            navigation.add(Box.createVerticalStrut(5));
        }

        JLabel footer = new JLabel("INVENTORY  •  BILLING");
        footer.setForeground(new Color(130, 151, 180));
        footer.setFont(new Font("Segoe UI", Font.BOLD, 10));
        sidebar.add(brand, BorderLayout.NORTH);
        sidebar.add(navigation, BorderLayout.CENTER);
        sidebar.add(footer, BorderLayout.SOUTH);

        tabs.addChangeListener(event -> updateNavigationSelection(tabs.getSelectedIndex()));
        return sidebar;
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(CANVAS);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 16));
        cards.setOpaque(false);
        cards.add(createMetricCard("TOTAL PRODUCTS", dashboardProductsLabel, NAVY, "Items in catalog"));
        cards.add(createMetricCard("TOTAL STOCK", dashboardInventoryLabel, ORANGE, "Available units"));
        cards.add(createMetricCard("TODAY'S SALES", dashboardRevenueLabel, GREEN, "Revenue today"));
        cards.add(createMetricCard("INVENTORY VALUE", dashboardInventoryValueLabel, NAVY_LIGHT, "At wholesale cost"));
        JPanel cardArea = new JPanel(new BorderLayout());
        cardArea.setOpaque(false);
        cardArea.add(cards, BorderLayout.NORTH);
        JPanel stockNotice = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 7));
        stockNotice.setBackground(new Color(255, 251, 235));
        stockNotice.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        JLabel lowStockTitle = new JLabel("LOW-STOCK PRODUCTS");
        lowStockTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lowStockTitle.setForeground(new Color(146, 92, 0));
        dashboardLowStockLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        dashboardLowStockLabel.setForeground(new Color(146, 92, 0));
        stockNotice.add(lowStockTitle);
        stockNotice.add(dashboardLowStockLabel);
        JLabel billsTitle = new JLabel("BILLS TODAY");
        billsTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        billsTitle.setForeground(new Color(100, 116, 139));
        dashboardBillsLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        dashboardBillsLabel.setForeground(NAVY);
        JLabel itemsTitle = new JLabel("ITEMS SOLD");
        itemsTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        itemsTitle.setForeground(new Color(100, 116, 139));
        dashboardItemsLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        dashboardItemsLabel.setForeground(NAVY);
        stockNotice.add(Box.createHorizontalStrut(20));
        stockNotice.add(billsTitle);
        stockNotice.add(dashboardBillsLabel);
        stockNotice.add(Box.createHorizontalStrut(20));
        stockNotice.add(itemsTitle);
        stockNotice.add(dashboardItemsLabel);
        cardArea.add(stockNotice, BorderLayout.SOUTH);
        panel.add(cardArea, BorderLayout.NORTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        actions.setOpaque(false);
        JButton billingButton = new JButton("Billing");
        JButton addProductButton = new JButton("Add Product");
        JButton stockButton = new JButton("Stock Management");
        JButton reportsButton = new JButton("Reports");

        billingButton.addActionListener(e -> setSelectedTab(1));
        addProductButton.addActionListener(e -> setSelectedTab(2));
        stockButton.addActionListener(e -> setSelectedTab(3));
        reportsButton.addActionListener(e -> setSelectedTab(4));

        actions.add(billingButton);
        actions.add(addProductButton);
        actions.add(stockButton);
        actions.add(reportsButton);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(evt -> {
            loadDashboard();
            loadRecentSales();
            loadProducts();
            loadStockTable();
            refreshReport();
            refreshHistory();
        });
        actions.add(refreshButton);
        JPanel quickActions = new RoundedPanel(WHITE, ORANGE);
        quickActions.setLayout(new BorderLayout());
        quickActions.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JLabel actionsTitle = new JLabel("Quick actions");
        actionsTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        actionsTitle.setForeground(NAVY);
        quickActions.add(actionsTitle, BorderLayout.NORTH);
        quickActions.add(actions, BorderLayout.CENTER);
        JPanel recentSales = new RoundedPanel(WHITE, NAVY_LIGHT);
        recentSales.setLayout(new BorderLayout(0, 10));
        recentSales.setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));
        JLabel recentTitle = new JLabel("Today's recent sales");
        recentTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        recentTitle.setForeground(NAVY);
        recentSales.add(recentTitle, BorderLayout.NORTH);
        recentSales.add(new JScrollPane(recentSalesTable), BorderLayout.CENTER);
        JPanel lower = new JPanel(new BorderLayout());
        lower.setOpaque(false);
        lower.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        lower.add(quickActions, BorderLayout.NORTH);
        lower.add(recentSales, BorderLayout.CENTER);
        panel.add(lower, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBillingPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(CANVAS);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel scanners = new JPanel(new GridLayout(1, 2, 16, 0));
        scanners.setOpaque(false);
        scanners.add(createScannerCard("SCANNER 1  ·  WHOLESALE", "Wholesale price", wholesaleBarcodeField,
                wholesaleQtySpinner, NAVY, () -> addBillRow("WHOLESALE", wholesaleBarcodeField.getText(),
                        (Integer) wholesaleQtySpinner.getValue())));
        scanners.add(createScannerCard("SCANNER 2  ·  RETAIL", "Retail price", retailBarcodeField,
                retailQtySpinner, ORANGE, () -> addBillRow("RETAIL", retailBarcodeField.getText(),
                        (Integer) retailQtySpinner.getValue())));
        panel.add(scanners, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(billTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setBackground(WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        JButton removeItemButton = new JButton("Remove Item");
        removeItemButton.addActionListener(e -> removeSelectedBillRow());
        JButton updateQuantityButton = new JButton("Change Qty");
        updateQuantityButton.addActionListener(e -> changeSelectedBillQuantity());
        JButton clearBillButton = new JButton("Clear Cart");
        clearBillButton.addActionListener(e -> clearBill());
        JButton completeSaleButton = new JButton("Checkout / Complete Sale");
        completeSaleButton.setPreferredSize(new Dimension(230, 44));
        completeSaleButton.addActionListener(e -> completeCurrentSale());

        JPanel cartActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        cartActions.setOpaque(false);
        cartActions.add(updateQuantityButton);
        cartActions.add(removeItemButton);
        cartActions.add(clearBillButton);
        JPanel totalPanel = new JPanel();
        totalPanel.setOpaque(false);
        totalPanel.setLayout(new BoxLayout(totalPanel, BoxLayout.Y_AXIS));
        JLabel totalCaption = new JLabel("GRAND TOTAL");
        totalCaption.setFont(new Font("Segoe UI", Font.BOLD, 10));
        totalCaption.setForeground(new Color(111, 126, 146));
        billTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 23));
        billTotalLabel.setForeground(NAVY);
        totalPanel.add(totalCaption);
        totalPanel.add(billTotalLabel);
        JPanel checkout = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        checkout.setOpaque(false);
        checkout.add(totalPanel);
        checkout.add(completeSaleButton);
        footer.add(cartActions, BorderLayout.WEST);
        footer.add(checkout, BorderLayout.EAST);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createScannerCard(String title, String priceDescription, JTextField barcodeField,
                                     JSpinner quantitySpinner, Color accent, Runnable scanAction) {
        JPanel card = new JPanel(new BorderLayout(10, 12));
        card.setBackground(WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(226, 232, 240)),
                        BorderFactory.createEmptyBorder(14, 16, 14, 16))));
        JLabel heading = new JLabel(title);
        heading.setForeground(accent.equals(ORANGE) ? NAVY : accent);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel pricing = new JLabel(priceDescription);
        pricing.setForeground(new Color(111, 126, 146));
        pricing.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        JPanel headingStack = new JPanel();
        headingStack.setOpaque(false);
        headingStack.setLayout(new BoxLayout(headingStack, BoxLayout.Y_AXIS));
        headingStack.add(heading);
        headingStack.add(Box.createVerticalStrut(3));
        headingStack.add(pricing);

        JPanel input = new JPanel(new GridBagLayout());
        input.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridx = 0;
        input.add(barcodeField, gbc);
        barcodeField.setToolTipText("Scan or enter a product barcode, then press Enter");
        barcodeField.addActionListener(event -> scanAction.run());
        gbc.weightx = 0;
        gbc.gridx = 1;
        input.add(quantitySpinner, gbc);
        JButton scanButton = new JButton("Add Item");
        scanButton.addActionListener(event -> scanAction.run());
        gbc.gridx = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        input.add(scanButton, gbc);
        card.add(headingStack, BorderLayout.NORTH);
        card.add(input, BorderLayout.CENTER);
        return card;
    }

    private JPanel createProductPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(CANVAS);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addFormRow(form, gbc, 0, "Barcode", productBarcodeField);
        addFormRow(form, gbc, 1, "Product Name", productNameField);
        addFormRow(form, gbc, 2, "Wholesale Price", wholesalePriceField);
        addFormRow(form, gbc, 3, "Retail Price", retailPriceField);
        addFormRow(form, gbc, 4, "Initial Stock", productStockField);
        addFormRow(form, gbc, 5, "Low Stock Threshold", thresholdField);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
        JPanel productActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        productActions.setOpaque(false);
        JPanel searchActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        searchActions.setOpaque(false);
        JButton saveButton = new JButton("Add Product");
        JButton updateButton = new JButton("Edit Product");
        JButton clearButton = new JButton("Clear");
        JButton searchButton = new JButton("Search");
        JButton refreshProductsButton = new JButton("Refresh");
        saveButton.addActionListener(e -> saveProduct());
        updateButton.addActionListener(e -> updateSelectedProduct());
        clearButton.addActionListener(e -> clearProductForm());
        searchButton.addActionListener(e -> searchProducts(productSearchField.getText()));
        productSearchField.addActionListener(e -> searchProducts(productSearchField.getText()));
        refreshProductsButton.addActionListener(e -> {
            productSearchField.setText("");
            loadProducts();
        });

        productActions.add(saveButton);
        productActions.add(updateButton);
        productActions.add(clearButton);
        searchActions.add(new JLabel("Search products:"));
        searchActions.add(productSearchField);
        searchActions.add(searchButton);
        searchActions.add(refreshProductsButton);
        actions.add(productActions);
        actions.add(searchActions);

        JPanel formAndActions = new JPanel(new BorderLayout(8, 8));
        formAndActions.setBackground(WHITE);
        formAndActions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(14, 16, 12, 16)));
        formAndActions.add(form, BorderLayout.CENTER);
        formAndActions.add(actions, BorderLayout.SOUTH);
        panel.add(formAndActions, BorderLayout.NORTH);
        panel.add(new JScrollPane(productTable), BorderLayout.CENTER);

        productTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && productTable.getSelectedRow() >= 0) {
                populateProductFormFromTable();
            }
        });
        return panel;
    }

    private JPanel createStockPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(CANVAS);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.setOpaque(false);
        controls.add(new JLabel("Search:"));
        controls.add(stockSearchField);
        JButton filterButton = new JButton("Search");
        filterButton.addActionListener(e -> loadStockTable(stockSearchField.getText()));
        controls.add(filterButton);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadStockTable());
        controls.add(refreshButton);

        JPanel restockGroup = new JPanel(new FlowLayout(FlowLayout.LEFT));
        restockGroup.setOpaque(false);
        restockGroup.add(new JLabel("Barcode:"));
        restockGroup.add(restockBarcodeField);
        restockGroup.add(new JLabel("Qty:"));
        restockGroup.add(restockQtyField);
        JButton addStockButton = new JButton("Add Stock");
        addStockButton.addActionListener(e -> addStockToProduct());
        JButton adjustCountButton = new JButton("Set Count");
        adjustCountButton.addActionListener(e -> adjustInventoryCount());
        restockGroup.add(addStockButton);
        restockGroup.add(adjustCountButton);

        JPanel tools = new JPanel();
        tools.setBackground(WHITE);
        tools.setLayout(new BoxLayout(tools, BoxLayout.Y_AXIS));
        tools.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        tools.add(controls);
        tools.add(restockGroup);
        panel.add(tools, BorderLayout.NORTH);
        panel.add(new JScrollPane(stockTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createReportPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(CANVAS);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel summary = new JPanel(new GridLayout(2, 3, 12, 12));
        summary.setOpaque(false);
        summary.add(createMetricCard("Selected Date", reportSelectedDateLabel));
        summary.add(createMetricCard("Bills", dailyBillsLabel));
        summary.add(createMetricCard("Qty Sold", dailyQtyLabel));
        summary.add(createMetricCard("Wholesale", wholesaleTotalLabel));
        summary.add(createMetricCard("Retail", retailTotalLabel));
        summary.add(createMetricCard("Grand Total", grandTotalLabel));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.setOpaque(false);
        controls.add(new JLabel("Date:"));
        controls.add(reportDateField);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshReport());
        JButton printButton = new JButton("Print");
        printButton.addActionListener(e -> printReportTable());
        JButton exportExcelButton = new JButton("Export to Excel");
        exportExcelButton.addActionListener(e -> exportDailyReportToExcel());
        controls.add(refreshButton);
        controls.add(printButton);
        controls.add(exportExcelButton);
        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(summary, BorderLayout.NORTH);
        top.add(controls, BorderLayout.SOUTH);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(reportTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(CANVAS);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel filters = new JPanel(new GridLayout(2, 1, 0, 2));
        filters.setBackground(WHITE);
        filters.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        JPanel dateFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        dateFilters.setOpaque(false);
        JPanel detailFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        detailFilters.setOpaque(false);
        JTextField startDateField = new JTextField(LocalDate.now().minusDays(7).format(DATE_FORMATTER), 10);
        dateFilters.add(new JLabel("From:"));
        dateFilters.add(startDateField);
        JTextField endDateField = new JTextField(LocalDate.now().format(DATE_FORMATTER), 10);
        dateFilters.add(new JLabel("To:"));
        dateFilters.add(endDateField);
        JTextField barcodeField = new JTextField(10);
        dateFilters.add(new JLabel("Barcode:"));
        dateFilters.add(barcodeField);
        JTextField productField = new JTextField(12);
        detailFilters.add(new JLabel("Product:"));
        detailFilters.add(productField);
        detailFilters.add(new JLabel("Scanner:"));
        detailFilters.add(historyScannerCombo);
        JButton refreshHistoryButton = new JButton("Refresh");
        refreshHistoryButton.addActionListener(e -> refreshHistory(
                startDateField.getText(), endDateField.getText(), barcodeField.getText(), productField.getText(),
                (String) historyScannerCombo.getSelectedItem()
        ));
        detailFilters.add(refreshHistoryButton);
        filters.add(dateFilters);
        filters.add(detailFilters);
        panel.add(filters, BorderLayout.NORTH);
        panel.add(new JScrollPane(salesHistoryTable), BorderLayout.CENTER);
        return panel;
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
        statusLabel.setToolTipText(text);
        String normalized = text == null ? "" : text.toLowerCase();
        if (normalized.contains("failed") || normalized.contains("error") || normalized.contains("issue")) {
            statusLabel.setForeground(new Color(185, 28, 28));
        } else if (normalized.contains("success") || normalized.contains("completed") || normalized.contains("updated")) {
            statusLabel.setForeground(new Color(22, 101, 52));
        } else if (normalized.contains("unavailable") || normalized.contains("unable")) {
            statusLabel.setForeground(new Color(146, 92, 0));
        } else {
            statusLabel.setForeground(TEXT);
        }
    }

    private void setSelectedTab(int index) {
        tabs.setSelectedIndex(index);
    }

    private void updateNavigationSelection(int selectedIndex) {
        String[] titles = {"Dashboard", "Billing", "Product Management", "Stock Management", "Daily Sales Report", "Sales History"};
        if (selectedIndex >= 0 && selectedIndex < titles.length) {
            pageTitleLabel.setText(titles[selectedIndex]);
        }
        for (int index = 0; index < navigationButtons.size(); index++) {
            JButton button = navigationButtons.get(index);
            boolean selected = index == selectedIndex;
            button.setOpaque(selected);
            button.setContentAreaFilled(selected);
            button.setBackground(selected ? ORANGE : NAVY);
            button.setForeground(selected ? NAVY : new Color(203, 215, 231));
            button.setBorder(selected
                    ? BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 3, 0, 0, NAVY),
                            BorderFactory.createEmptyBorder(0, 11, 0, 10))
                    : BorderFactory.createEmptyBorder(0, 14, 0, 10));
        }
    }

    private JPanel createMetricCard(String title, JComponent value) {
        return createMetricCard(title, value, ORANGE, "");
    }

    private JPanel createMetricCard(String title, JComponent value, Color accent, String description) {
        JPanel panel = new RoundedPanel(WHITE, accent);
        panel.setLayout(new BorderLayout(0, 7));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 15, 13, 14));
        panel.setPreferredSize(new Dimension(180, 122));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        titleLabel.setForeground(new Color(103, 119, 139));
        value.setFont(new Font("Segoe UI", Font.BOLD, 25));
        value.setForeground(accent);
        if (value instanceof JLabel) {
            ((JLabel) value).setHorizontalAlignment(SwingConstants.LEFT);
        }

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(titleLabel);
        content.add(Box.createVerticalStrut(10));
        content.add(value);
        if (!description.isBlank()) {
            JLabel detail = new JLabel(description);
            detail.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            detail.setForeground(new Color(126, 140, 158));
            content.add(Box.createVerticalStrut(4));
            content.add(detail);
        }
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private void styleComponentTree(Component component) {
        if (component instanceof JButton button) {
            if (!navigationButtons.contains(button)) {
                styleButton(button);
            }
        } else if (component instanceof JTable table) {
            styleTable(table);
        } else if (component instanceof JTextField textField) {
            textField.setFont(APP_FONT);
            textField.setPreferredSize(new Dimension(textField.getPreferredSize().width, 34));
            textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(205, 215, 227)),
                    BorderFactory.createEmptyBorder(5, 9, 5, 9)));
            textField.setBackground(WHITE);
            textField.setForeground(NAVY);
        } else if (component instanceof JComboBox<?> comboBox) {
            comboBox.setFont(APP_FONT);
            comboBox.setBackground(WHITE);
            comboBox.setForeground(NAVY);
        } else if (component instanceof JSpinner spinner) {
            spinner.setFont(APP_FONT);
            spinner.setPreferredSize(new Dimension(72, 34));
        } else if (component instanceof JLabel label
                && "Dialog".equalsIgnoreCase(label.getFont().getFamily())) {
            label.setFont(APP_FONT);
            label.setForeground(NAVY);
        }

        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                styleComponentTree(child);
            }
        }
    }

    private void styleButton(JButton button) {
        String text = button.getText() == null ? "" : button.getText().toLowerCase();
        Color background;
        if (text.contains("checkout") || text.contains("complete sale")) {
            background = GREEN;
        } else if (text.contains("add")
                && (text.contains("item") || text.contains("product") || text.contains("stock"))) {
            background = ORANGE;
        } else if (text.contains("update") || text.contains("edit")) {
            background = ORANGE;
        } else if (text.contains("clear") || text.contains("cancel") || text.contains("remove")) {
            background = RED;
        } else {
            background = NAVY_LIGHT;
        }

        button.putClientProperty("scanshift.baseColor", background);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(background.equals(ORANGE) ? NAVY : WHITE);
        button.setBackground(background);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        if (button.getPreferredSize().height < 34) {
            button.setPreferredSize(new Dimension(Math.max(button.getPreferredSize().width, 84), 36));
        }
        button.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                if (button.isEnabled()) {
                    button.setBackground(background.darker());
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                button.setBackground(background);
            }
        });
    }

    private void styleTable(JTable table) {
        table.setFont(APP_FONT);
        table.setRowHeight(30);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(232, 237, 243));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(new Color(255, 237, 213));
        table.setSelectionForeground(NAVY);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable target, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                Component cell = super.getTableCellRendererComponent(target, value, selected, focused, row, column);
                cell.setFont(APP_FONT);
                if (selected) {
                    cell.setBackground(new Color(255, 237, 213));
                    cell.setForeground(NAVY);
                } else {
                    boolean lowStock = false;
                    String status = "";
                    if (target == stockTable && row < target.getModel().getRowCount()) {
                        Object stock = target.getModel().getValueAt(row, 2);
                        Object threshold = target.getModel().getValueAt(row, 3);
                        lowStock = stock instanceof Number && threshold instanceof Number
                                && ((Number) stock).intValue() <= ((Number) threshold).intValue();
                        status = String.valueOf(target.getModel().getValueAt(row, 7));
                    }
                    cell.setBackground(lowStock ? new Color(255, 247, 237)
                            : (row % 2 == 0 ? WHITE : new Color(247, 249, 252)));
                    cell.setForeground(status.equals("OUT OF STOCK") ? RED
                            : (lowStock ? new Color(180, 83, 9) : TEXT));
                    if (target == billTable && column == 2) {
                        String scanner = String.valueOf(value);
                        cell.setForeground("RETAIL".equals(scanner) ? new Color(194, 65, 12) : NAVY);
                        cell.setFont(APP_FONT.deriveFont(Font.BOLD));
                    }
                }
                setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 9));
                return cell;
            }
        });
        if (table.getTableHeader() != null) {
            table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
            table.getTableHeader().setBackground(NAVY);
            table.getTableHeader().setForeground(WHITE);
            table.getTableHeader().setPreferredSize(new Dimension(0, 36));
            table.getTableHeader().setReorderingAllowed(false);
            table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable target, Object value, boolean selected,
                                                               boolean focused, int row, int column) {
                    Component cell = super.getTableCellRendererComponent(target, value, selected, focused, row, column);
                    cell.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    cell.setBackground(NAVY);
                    cell.setForeground(WHITE);
                    setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 9));
                    return cell;
                }
            });
        }
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent component) {
        JLabel label = new JLabel(labelText + ":");
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.2; panel.add(label, gbc);
        gbc.gridx = 1; gbc.weightx = 1; panel.add(component, gbc);
    }

    private void saveProduct() {
        try {
            String barcode = ValidationUtil.normalizeBarcode(productBarcodeField.getText());
            if (!ValidationUtil.isValidBarcode(barcode)) {
                throw new IllegalArgumentException("Barcode must contain between 3 and 50 characters.");
            }
            String name = ValidationUtil.requireText(productNameField.getText(), "Product name");
            BigDecimal wholesale = parsePrice(wholesalePriceField.getText(), "Wholesale price");
            BigDecimal retail = parsePrice(retailPriceField.getText(), "Retail price");
            int stock = parseInteger(productStockField.getText(), "Initial stock");
            int threshold = parseInteger(thresholdField.getText(), "Low stock threshold");

            if (!ValidationUtil.isPositive(wholesale) || !ValidationUtil.isPositive(retail)) {
                throw new IllegalArgumentException("Wholesale and retail prices must be greater than zero.");
            }
            if (stock < 0 || threshold < 0) {
                throw new IllegalArgumentException("Initial stock and low stock threshold cannot be negative.");
            }
            if (productDAO.findByBarcode(barcode).isPresent()) {
                throw new IllegalArgumentException("Duplicate barcode already exists.");
            }

            Product product = new Product();
            product.setBarcode(barcode);
            product.setProductName(name);
            product.setWholesalePrice(wholesale);
            product.setRetailPrice(retail);
            product.setStock(stock);
            product.setLowStockThreshold(threshold);
            inventoryService.addProduct(product);
            boolean productsRefreshed = loadProducts();
            loadStockTable();
            loadDashboard();
            clearProductForm();
            if (!productsRefreshed) {
                String message = "Product was saved, but the product table could not be refreshed. Use Refresh or check the database connection.";
                JOptionPane.showMessageDialog(this, message, "Product saved; refresh failed", JOptionPane.WARNING_MESSAGE);
                setStatus(message);
                return;
            }
            JOptionPane.showMessageDialog(this, "Product added successfully.", "Product saved", JOptionPane.INFORMATION_MESSAGE);
            setStatus("Product added successfully.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Product save failed", JOptionPane.ERROR_MESSAGE);
            setStatus("Product add failed: " + ex.getMessage());
        }
    }

    private void updateSelectedProduct() {
        try {
            int selectedRow = productTable.getSelectedRow();
            if (selectedRow < 0) {
                JOptionPane.showMessageDialog(this, "Choose a product to update.");
                return;
            }

            Long productId = Long.parseLong(productTableModel.getValueAt(selectedRow, 0).toString());
            Product product = productDAO.findById(productId).orElseThrow();
            product.setBarcode(ValidationUtil.normalizeBarcode(productBarcodeField.getText()));
            product.setProductName(ValidationUtil.requireText(productNameField.getText(), "Product name"));
            product.setWholesalePrice(parsePrice(wholesalePriceField.getText(), "Wholesale price"));
            product.setRetailPrice(parsePrice(retailPriceField.getText(), "Retail price"));
            product.setStock(parseInteger(productStockField.getText(), "Stock"));
            product.setLowStockThreshold(parseInteger(thresholdField.getText(), "Low stock threshold"));
            if (!ValidationUtil.isValidBarcode(product.getBarcode())) {
                throw new IllegalArgumentException("Barcode must contain between 3 and 50 characters.");
            }
            if (!ValidationUtil.isPositive(product.getWholesalePrice())
                    || !ValidationUtil.isPositive(product.getRetailPrice())) {
                throw new IllegalArgumentException("Wholesale and retail prices must be greater than zero.");
            }
            if (product.getStock() < 0 || product.getLowStockThreshold() < 0) {
                throw new IllegalArgumentException("Stock and threshold values cannot be negative.");
            }
            productDAO.findByBarcode(product.getBarcode()).ifPresent(existing -> {
                if (!existing.getId().equals(productId)) {
                    throw new IllegalArgumentException("Duplicate barcode already exists.");
                }
            });
            inventoryService.updateProduct(product);
            boolean productsRefreshed = loadProducts();
            loadStockTable();
            loadDashboard();
            if (!productsRefreshed) {
                String message = "Product was updated, but the product table could not be refreshed. Use Refresh or check the database connection.";
                JOptionPane.showMessageDialog(this, message, "Product updated; refresh failed", JOptionPane.WARNING_MESSAGE);
                setStatus(message);
                return;
            }
            JOptionPane.showMessageDialog(this, "Product updated successfully.", "Product updated", JOptionPane.INFORMATION_MESSAGE);
            setStatus("Product updated successfully.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Update failed", JOptionPane.ERROR_MESSAGE);
            setStatus("Product update failed: " + ex.getMessage());
        }
    }

    private BigDecimal parsePrice(String value, String fieldName) {
        String text = ValidationUtil.requireText(value, fieldName);
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.", ex);
        }
    }

    private int parseInteger(String value, String fieldName) {
        String text = ValidationUtil.requireText(value, fieldName);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a whole number.", ex);
        }
    }

    private void clearProductForm() {
        productBarcodeField.setText("");
        productNameField.setText("");
        wholesalePriceField.setText("");
        retailPriceField.setText("");
        productStockField.setText("");
        thresholdField.setText("");
    }

    private void populateProductFormFromTable() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        try {
            Long productId = Long.valueOf(productTableModel.getValueAt(selectedRow, 0).toString());
            Product product = productDAO.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Selected product is no longer available."));
            productBarcodeField.setText(product.getBarcode());
            productNameField.setText(product.getProductName());
            wholesalePriceField.setText(PriceFormatter.formatPlain(product.getWholesalePrice()));
            retailPriceField.setText(PriceFormatter.formatPlain(product.getRetailPrice()));
            productStockField.setText(String.valueOf(product.getStock()));
            thresholdField.setText(String.valueOf(product.getLowStockThreshold()));
        } catch (SQLException | IllegalArgumentException ex) {
            setStatus("Unable to load selected product: " + ex.getMessage());
        }
    }

    private boolean loadProducts() {
        try {
            List<Product> products = productDAO.findAll();
            productTableModel.setRowCount(0);
            for (Product product : products) {
                productTableModel.addRow(new Object[]{
                        product.getId(),
                        product.getBarcode(),
                        product.getProductName(),
                        PriceFormatter.formatCurrency(product.getWholesalePrice()),
                        PriceFormatter.formatCurrency(product.getRetailPrice()),
                        product.getStock(),
                        product.getLowStockThreshold()
                });
            }
            return true;
        } catch (SQLException ex) {
            setStatus("Unable to load products: " + ex.getMessage());
            return false;
        }
    }

    private void searchProducts(String keyword) {
        productTableModel.setRowCount(0);
        try {
            List<Product> products = productDAO.search(keyword);
            for (Product product : products) {
                productTableModel.addRow(new Object[]{
                        product.getId(),
                        product.getBarcode(),
                        product.getProductName(),
                        PriceFormatter.formatCurrency(product.getWholesalePrice()),
                        PriceFormatter.formatCurrency(product.getRetailPrice()),
                        product.getStock(),
                        product.getLowStockThreshold()
                });
            }
        } catch (SQLException ex) {
            setStatus("Product search failed: " + ex.getMessage());
        }
    }

    private void addBillRow(String scannerType, String barcodeText, int quantity) {
        try {
            String normalizedBarcode = ValidationUtil.normalizeBarcode(barcodeText);
            if (!ValidationUtil.isValidBarcode(normalizedBarcode)) {
                throw new IllegalArgumentException("Barcode is required.");
            }
            Product product = productDAO.findByBarcode(normalizedBarcode)
                    .orElseThrow(() -> new IllegalArgumentException("Barcode not found: " + normalizedBarcode));
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be at least 1.");
            }
            long quantityAlreadyInCart = billLines.stream()
                    .filter(line -> line.productId.equals(product.getId()))
                    .mapToLong(line -> line.quantity)
                    .sum();
            if (!BillingService.canAddQuantity(product, quantityAlreadyInCart, quantity)) {
                throw new IllegalArgumentException("Insufficient stock for " + product.getProductName() + ".");
            }
            String actualScannerType = BillingService.sanitizeScannerType(scannerType);

            boolean merged = false;
            for (BillLine line : billLines) {
                if (line.productId.equals(product.getId()) && line.scannerType.equals(actualScannerType)) {
                    line.quantity += quantity;
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                billLines.add(new BillLine(product.getId(), product.getBarcode(), product.getProductName(),
                        actualScannerType, product.getWholesalePrice(), product.getRetailPrice(), quantity));
            }
            renderBillTable();
            if ("WHOLESALE".equals(actualScannerType)) {
                wholesaleBarcodeField.setText("");
                wholesaleQtySpinner.setValue(1);
                wholesaleBarcodeField.requestFocusInWindow();
            } else {
                retailBarcodeField.setText("");
                retailQtySpinner.setValue(1);
                retailBarcodeField.requestFocusInWindow();
            }
            setStatus("Added " + product.getProductName() + " in " + actualScannerType + " mode.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Billing issue", JOptionPane.ERROR_MESSAGE);
            setStatus("Billing issue: " + ex.getMessage());
        }
    }

    private void renderBillTable() {
        billTableModel.setRowCount(0);
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (BillLine line : billLines) {
            BigDecimal unitPrice = "WHOLESALE".equals(line.scannerType) ? line.wholesalePrice : line.retailPrice;
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(line.quantity));
            grandTotal = grandTotal.add(lineTotal);
            billTableModel.addRow(new Object[]{
                    line.barcode,
                    line.productName,
                    line.scannerType,
                    PriceFormatter.formatCurrency(unitPrice),
                    line.quantity,
                    PriceFormatter.formatCurrency(lineTotal)
            });
        }
        billTotalLabel.setText(PriceFormatter.formatCurrency(grandTotal));
    }

    private void removeSelectedBillRow() {
        int selectedRow = billTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < billLines.size()) {
            billLines.remove(selectedRow);
            renderBillTable();
            setStatus("Removed selected bill line.");
        }
    }

    private void changeSelectedBillQuantity() {
        int selectedRow = billTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= billLines.size()) {
            JOptionPane.showMessageDialog(this, "Select a cart item to change its quantity.",
                    "No item selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BillLine selectedLine = billLines.get(selectedRow);
        try {
            Product product = productDAO.findById(selectedLine.productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product is no longer available."));
            JSpinner quantitySpinner = new JSpinner(new javax.swing.SpinnerNumberModel(
                    selectedLine.quantity, 1, Math.max(selectedLine.quantity, product.getStock()), 1));
            JPanel prompt = new JPanel(new BorderLayout(8, 0));
            prompt.add(new JLabel("Quantity for " + selectedLine.productName + ":"), BorderLayout.WEST);
            prompt.add(quantitySpinner, BorderLayout.EAST);
            int result = JOptionPane.showConfirmDialog(this, prompt, "Change cart quantity",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            int newQuantity = (Integer) quantitySpinner.getValue();
            long quantityForOtherLines = billLines.stream()
                    .filter(line -> line != selectedLine && line.productId.equals(selectedLine.productId))
                    .mapToLong(line -> line.quantity)
                    .sum();
            if (!BillingService.canAddQuantity(product, quantityForOtherLines, newQuantity)) {
                throw new IllegalArgumentException("The requested quantity exceeds available stock for "
                        + product.getProductName() + ".");
            }
            selectedLine.quantity = newQuantity;
            renderBillTable();
            setStatus("Updated quantity for " + product.getProductName() + ".");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Quantity update failed", JOptionPane.ERROR_MESSAGE);
            setStatus("Quantity update failed: " + ex.getMessage());
        }
    }

    private void clearBill() {
        billLines.clear();
        billTableModel.setRowCount(0);
        billTotalLabel.setText(PriceFormatter.formatCurrency(BigDecimal.ZERO));
        setStatus("Bill cleared.");
    }

    private void completeCurrentSale() {
        if (billLines.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one item before completing a sale.");
            return;
        }

        try {
            Sale sale = new Sale();
            sale.setSaleDate(LocalDateTime.now());

            List<SaleItem> items = new ArrayList<>();
            for (BillLine line : billLines) {
                BigDecimal unitPrice = "WHOLESALE".equals(line.scannerType) ? line.wholesalePrice : line.retailPrice;
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(line.quantity)).setScale(2, java.math.RoundingMode.HALF_UP);
                SaleItem item = new SaleItem();
                item.setProductId(line.productId);
                item.setBarcode(line.barcode);
                item.setProductName(line.productName);
                item.setScannerType(line.scannerType);
                item.setUnitPrice(unitPrice);
                item.setQuantity(line.quantity);
                item.setLineTotal(lineTotal);
                items.add(item);
            }

            Sale saved = saleDAO.completeSale(sale, items);
            long saleId = saved.getId();
            String receipt = "SB" + saleId;
            clearBill();
            loadDashboard();
            loadRecentSales();
            loadProducts();
            loadStockTable();
            refreshReport();
            refreshHistory();
            JOptionPane.showMessageDialog(this, "Sale completed successfully. Receipt: " + receipt, "Sale saved", JOptionPane.INFORMATION_MESSAGE);
            setStatus("Completed sale: " + receipt);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Transaction failed", JOptionPane.ERROR_MESSAGE);
            setStatus("Transaction failed: " + ex.getMessage());
        }
    }

    private void loadDashboard() {
        try {
            ReportService.DashboardSummary summary = reportService.getDashboardSummary();
            dashboardRevenueLabel.setText(PriceFormatter.formatCurrency(summary.getTodayRevenue()));
            dashboardBillsLabel.setText(String.valueOf(summary.getCompletedBills()));
            dashboardItemsLabel.setText(String.valueOf(summary.getItemsSold()));
            dashboardProductsLabel.setText(String.valueOf(summary.getProductCount()));
            dashboardLowStockLabel.setText(String.valueOf(summary.getLowStockCount()));
            dashboardInventoryLabel.setText(String.valueOf(summary.getInventoryUnits()));
            dashboardInventoryValueLabel.setText(PriceFormatter.formatCurrency(summary.getInventoryValue()));
        } catch (Exception ex) {
            setStatus("Dashboard data unavailable: " + ex.getMessage());
            return;
        }
        setStatus("Connected to MySQL · Dashboard data refreshed");
    }

    private void loadRecentSales() {
        try {
            List<Sale> sales = saleDAO.findSalesForDate(LocalDate.now());
            recentSalesTableModel.setRowCount(0);
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            for (int index = 0; index < Math.min(sales.size(), 6); index++) {
                Sale sale = sales.get(index);
                recentSalesTableModel.addRow(new Object[]{
                        "SS-" + sale.getId(),
                        sale.getSaleDate().format(timeFormatter),
                        PriceFormatter.formatCurrency(sale.getGrandTotal())
                });
            }
        } catch (SQLException ex) {
            setStatus("Recent sales unavailable: " + ex.getMessage());
        }
    }

    private void loadStockTable() {
        loadStockTable("");
    }

    private void loadStockTable(String keyword) {
        stockTableModel.setRowCount(0);
        try {
            List<Product> products = keyword == null || keyword.isBlank() ? productDAO.findAll() : productDAO.search(keyword);
            for (Product product : products) {
                stockTableModel.addRow(new Object[]{
                        product.getBarcode(),
                        product.getProductName(),
                        product.getStock(),
                        product.getLowStockThreshold(),
                        PriceFormatter.formatCurrency(product.getWholesalePrice()),
                        PriceFormatter.formatCurrency(product.getRetailPrice()),
                        PriceFormatter.formatCurrency(product.getWholesalePrice()
                                .multiply(BigDecimal.valueOf(product.getStock()))),
                        product.isOutOfStock() ? "OUT OF STOCK" : (product.isLowStock() ? "LOW STOCK" : "OK")
                });
            }
        } catch (SQLException ex) {
            setStatus("Stock table failed: " + ex.getMessage());
        }
    }

    private void addStockToProduct() {
        try {
            String barcode = restockBarcodeField.getText().trim();
            int quantity = Integer.parseInt(restockQtyField.getText().trim());
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive.");
            }
            Product product = productDAO.findByBarcode(barcode)
                    .orElseThrow(() -> new IllegalArgumentException("Barcode not found."));
            inventoryService.restockProduct(product.getId(), quantity, "Manual restock");
            loadStockTable();
            loadProducts();
            loadDashboard();
            setStatus("Stock updated for " + product.getProductName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Stock update failed", JOptionPane.ERROR_MESSAGE);
            setStatus("Stock update failed: " + ex.getMessage());
        }
    }

    private void adjustInventoryCount() {
        try {
            String barcode = restockBarcodeField.getText().trim();
            int newCount = Integer.parseInt(restockQtyField.getText().trim());
            if (newCount < 0) {
                throw new IllegalArgumentException("Inventory count cannot be negative.");
            }
            Product product = productDAO.findByBarcode(barcode)
                    .orElseThrow(() -> new IllegalArgumentException("Barcode not found."));
            int delta = newCount - product.getStock();
            inventoryService.adjustInventory(product.getId(), delta, "Physical stock count");
            loadStockTable();
            loadProducts();
            loadDashboard();
            setStatus("Inventory count set for " + product.getProductName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Inventory adjustment failed", JOptionPane.ERROR_MESSAGE);
            setStatus("Inventory adjustment failed: " + ex.getMessage());
        }
    }

    private void refreshReport() {
        try {
            LocalDate reportDate = LocalDate.parse(reportDateField.getText(), DATE_FORMATTER);
            ReportService.DailyReport report = reportService.getDailyReport(reportDate);
            reportSelectedDateLabel.setText(reportDate.toString());
            dailyBillsLabel.setText(String.valueOf(report.getCompletedBills()));
            dailyQtyLabel.setText(String.valueOf(report.getQuantitySold()));
            wholesaleTotalLabel.setText(PriceFormatter.formatCurrency(report.getWholesaleTotal()));
            retailTotalLabel.setText(PriceFormatter.formatCurrency(report.getRetailTotal()));
            grandTotalLabel.setText(PriceFormatter.formatCurrency(report.getGrandTotal()));

            reportTableModel.setRowCount(0);
            if (report.getSaleItems() != null) {
                for (SaleItem item : report.getSaleItems()) {
                    reportTableModel.addRow(new Object[]{
                            item.getSaleId(),
                            item.getSaleDate() == null ? "" : item.getSaleDate().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                            item.getBarcode(),
                            item.getProductName(),
                            item.getScannerType(),
                            PriceFormatter.formatCurrency(item.getUnitPrice()),
                            item.getQuantity(),
                            PriceFormatter.formatCurrency(item.getLineTotal())
                    });
                }
            }
        } catch (Exception ex) {
            setStatus("Report refresh failed: " + ex.getMessage());
        }
    }

    private void printReportTable() {
        try {
            boolean printed = reportTable.print(JTable.PrintMode.FIT_WIDTH,
                    new MessageFormat("ScanShift Daily Sales Report — " + reportDateField.getText()),
                    new MessageFormat("Page {0}"));
            if (printed) {
                setStatus("Report sent to printer.");
            } else {
                setStatus("Report printing cancelled.");
            }
        } catch (PrinterException ex) {
            setStatus("Print failed: " + ex.getMessage());
        }
    }

    private void exportDailyReportToExcel() {
        LocalDate reportDate;
        try {
            reportDate = LocalDate.parse(reportDateField.getText(), DATE_FORMATTER);
        } catch (java.time.format.DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid report date in YYYY-MM-DD format.",
                    "Invalid report date", JOptionPane.WARNING_MESSAGE);
            setStatus("Excel export failed: invalid report date.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export daily sales report");
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));
        fileChooser.setSelectedFile(new java.io.File("ScanShift_Daily_Report_" + reportDate + ".xlsx"));
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            setStatus("Excel export cancelled.");
            return;
        }

        Path destination = fileChooser.getSelectedFile().toPath();
        String fileName = destination.getFileName().toString();
        if (!fileName.toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")) {
            destination = destination.resolveSibling(fileName + ".xlsx");
        }

        try {
            ReportService.DailyReport report = reportService.getDailyReport(reportDate);
            List<SaleItem> items = report.getSaleItems();
            if (items == null || items.isEmpty()) {
                int result = JOptionPane.showConfirmDialog(this,
                        "There are no sales records for " + reportDate
                                + ". Export the report summary with an empty records section?",
                        "No sales records", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (result != JOptionPane.YES_OPTION) {
                    setStatus("Excel export cancelled: no sales records.");
                    return;
                }
            }

            DailyReportExcelExporter.export(report, destination);
            JOptionPane.showMessageDialog(this, "Daily report exported successfully to:\n" + destination,
                    "Excel export complete", JOptionPane.INFORMATION_MESSAGE);
            setStatus("Daily report exported to Excel: " + destination.getFileName());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Unable to load report data from MySQL:\n" + ex.getMessage(),
                    "Excel export failed", JOptionPane.ERROR_MESSAGE);
            setStatus("Excel export failed while loading report: " + ex.getMessage());
        } catch (java.io.IOException | RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Unable to write the Excel report. Check that the destination is writable and the file is not open.\n"
                            + ex.getMessage(),
                    "Excel export failed", JOptionPane.ERROR_MESSAGE);
            setStatus("Excel export failed: " + ex.getMessage());
        }
    }

    private void refreshHistory() {
        refreshHistory(LocalDate.now().minusDays(7).toString(), LocalDate.now().toString(), "", "", "ALL");
    }

    private void refreshHistory(String startText, String endText, String barcode, String productName, String scannerType) {
        try {
            LocalDate startDate = startText == null || startText.isBlank() ? null : LocalDate.parse(startText, DATE_FORMATTER);
            LocalDate endDate = endText == null || endText.isBlank() ? null : LocalDate.parse(endText, DATE_FORMATTER);
            List<Sale> sales = saleDAO.findSalesByFilter(startDate, endDate, barcode, productName,
                    "ALL".equalsIgnoreCase(scannerType) ? null : scannerType);
            salesHistoryTableModel.setRowCount(0);
            for (Sale sale : sales) {
                List<SaleItem> items = saleDAO.findItemsForSale(sale.getId());
                salesHistoryTableModel.addRow(new Object[]{
                        sale.getId(),
                        sale.getSaleDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        PriceFormatter.formatCurrency(sale.getGrandTotal()),
                        items.stream().mapToInt(SaleItem::getQuantity).sum()
                });
            }
        } catch (Exception ex) {
            setStatus("Sales history refresh failed: " + ex.getMessage());
        }
    }

    private static final class BillLine {
        private final Long productId;
        private final String barcode;
        private final String productName;
        private final String scannerType;
        private final BigDecimal wholesalePrice;
        private final BigDecimal retailPrice;
        private int quantity;

        private BillLine(Long productId, String barcode, String productName, String scannerType,
                        BigDecimal wholesalePrice, BigDecimal retailPrice, int quantity) {
            this.productId = productId;
            this.barcode = barcode;
            this.productName = productName;
            this.scannerType = scannerType;
            this.wholesalePrice = wholesalePrice;
            this.retailPrice = retailPrice;
            this.quantity = quantity;
        }
    }

    private static final class RoundedPanel extends JPanel {
        private final Color accent;

        private RoundedPanel(Color background, Color accent) {
            this.accent = accent;
            setBackground(background);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.setColor(new Color(226, 232, 240));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.setColor(accent);
            g2.fillRoundRect(1, 14, 4, Math.max(0, getHeight() - 29), 4, 4);
            g2.dispose();
        }
    }
}
