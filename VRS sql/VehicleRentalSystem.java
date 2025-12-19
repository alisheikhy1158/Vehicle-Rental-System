import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.HexFormat;

public class VehicleRentalSystem {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DatabaseManager dbManager = new DatabaseManager();
            Connection conn = dbManager.connect();

            if (conn != null) {
                new AuthenticationSystem(conn).showWelcome();
            }
        });
    }
}

class DatabaseManager {
    private static final String DB_URL = "jdbc:oracle:thin:@//localhost:1521/orclpdb";
    private static final String DB_USER = "rental";
    private static final String DB_PASS = "rental123";

    public Connection connect() {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database Connection Failed: " + e.getMessage());
            System.exit(1);
            return null;
        }
    }
}

class AuthenticationSystem {
    private final Connection conn;

    public AuthenticationSystem(Connection conn) {
        this.conn = conn;
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void showWelcome() {
        JFrame frame = new JFrame("Vehicle Rental System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 250);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("Welcome to Vehicle Rental System", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        frame.add(title, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));

        JButton loginBtn = new JButton("Login");
        JButton signupBtn = new JButton("Create New Account");

        buttonPanel.add(loginBtn);
        buttonPanel.add(signupBtn);
        frame.add(buttonPanel, BorderLayout.CENTER);

        loginBtn.addActionListener(e -> {
            frame.dispose();
            showLogin();
        });

        signupBtn.addActionListener(e -> {
            frame.dispose();
            showSignup();
        });

        frame.setVisible(true);
    }

    public void showLogin() {
        JFrame frame = new JFrame("Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 250);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        JPanel fields = new JPanel(new GridLayout(2, 2, 10, 10));
        fields.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        fields.add(new JLabel("Username:"));
        fields.add(usernameField);
        fields.add(new JLabel("Password:"));
        fields.add(passwordField);

        JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(Color.RED);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton loginBtn = new JButton("Login");
        JButton backBtn = new JButton("Back");
        buttonPanel.add(loginBtn);
        buttonPanel.add(backBtn);

        frame.add(fields, BorderLayout.CENTER);
        frame.add(statusLabel, BorderLayout.NORTH);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        loginBtn.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            if (username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Enter username and password.");
                return;
            }

            try {
                String hashedPassword = sha256Hex(password);

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT AdminID FROM Admins WHERE TRIM(Username) = ? AND HashedPassword = ?")) {
                    ps.setString(1, username);
                    ps.setString(2, hashedPassword);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            frame.dispose();
                            new AdminDashboard(conn).show(); // OOP: Instantiate Admin Object
                            return;
                        }
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT CustomerID, Name FROM Customers WHERE TRIM(Username) = ? AND HashedPassword = ?")) {
                    ps.setString(1, username);
                    ps.setString(2, hashedPassword);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int customerId = rs.getInt("CustomerID");
                            String customerName = rs.getString("Name");
                            frame.dispose();
                            new CustomerDashboard(conn, customerId, customerName).show(); 
                                                                                          
                        }
                    }
                }

                statusLabel.setText("Invalid credentials.");
            } catch (Exception ex) {
                ex.printStackTrace();
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        backBtn.addActionListener(e -> {
            frame.dispose();
            showWelcome();
        });

        frame.setVisible(true);
    }

    public void showSignup() {
        JFrame frame = new JFrame("Create New Account");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(450, 350);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        JPanel fields = new JPanel(new GridLayout(5, 2, 10, 10));
        fields.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField nameField = new JTextField();
        JTextField mobileField = new JTextField();
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmPasswordField = new JPasswordField();

        fields.add(new JLabel("Full Name:"));
        fields.add(nameField);
        fields.add(new JLabel("Mobile:"));
        fields.add(mobileField);
        fields.add(new JLabel("Username:"));
        fields.add(usernameField);
        fields.add(new JLabel("Password:"));
        fields.add(passwordField);
        fields.add(new JLabel("Confirm:"));
        fields.add(confirmPasswordField);

        JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(Color.RED);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton createBtn = new JButton("Create Account");
        JButton backBtn = new JButton("Back");
        buttonPanel.add(createBtn);
        buttonPanel.add(backBtn);

        frame.add(fields, BorderLayout.CENTER);
        frame.add(statusLabel, BorderLayout.NORTH);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        createBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String mobile = mobileField.getText().trim();
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String confirm = new String(confirmPasswordField.getPassword());

            if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Fill all fields.");
                return;
            }
            if (!password.equals(confirm)) {
                statusLabel.setText("Passwords mismatch.");
                return;
            }

            try {
                // Check Username
                try (PreparedStatement ps = conn
                        .prepareStatement("SELECT COUNT(*) FROM Customers WHERE Username = ?")) {
                    ps.setString(1, username);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        statusLabel.setText("Username exists.");
                        return;
                    }
                }

                // Insert
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO Customers (Name, MobileNumber, Username, HashedPassword) VALUES (?, ?, ?, ?)")) {
                    ps.setString(1, name);
                    ps.setString(2, mobile);
                    ps.setString(3, username);
                    ps.setString(4, sha256Hex(password));
                    ps.executeUpdate();
                }

                JOptionPane.showMessageDialog(frame, "Created! Please login.");
                frame.dispose();
                showLogin();

            } catch (Exception ex) {
                ex.printStackTrace();
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        backBtn.addActionListener(e -> {
            frame.dispose();
            showWelcome();
        });

        frame.setVisible(true);
    }
}

class CustomerDashboard {
    private final Connection conn;
    private final int customerId;
    private final String customerName;
    private JFrame frame;
    private DefaultTableModel model;
    private JTable table;

    public CustomerDashboard(Connection conn, int customerId, String customerName) {
        this.conn = conn;
        this.customerId = customerId;
        this.customerName = customerName;
    }

    public void show() {
        frame = new JFrame("Customer Dashboard - " + customerName);
        frame.setSize(900, 500);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        model = new DefaultTableModel(new Object[] { "Vehicle ID", "Model", "Category", "Rent/Day", "Available" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(model);

        // Custom Cell Renderer
        table.getColumnModel().getColumn(4).setCellRenderer((tbl, value, isSelected, hasFocus, row, col) -> {
            JLabel cell = new JLabel(value.toString());
            cell.setOpaque(true);
            int vehicleId = (int) model.getValueAt(row, 0);

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Bookings WHERE VehicleID=? AND CustomerID=? AND Status='Booked'")) {
                ps.setInt(1, vehicleId);
                ps.setInt(2, customerId);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0)
                    cell.setForeground(Color.BLUE);
                else if ("No".equals(value))
                    cell.setForeground(Color.RED);
                else
                    cell.setForeground(Color.BLACK);
            } catch (Exception e) {
                cell.setForeground(Color.BLACK);
            }

            cell.setBackground(isSelected ? tbl.getSelectionBackground() : tbl.getBackground());
            return cell;
        });
        table.setRowHeight(25);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel statusLabel = new JLabel("Welcome, " + customerName + "!", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        frame.add(statusLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton bookBtn = new JButton("Book Vehicle");
        JButton returnBtn = new JButton("Return Vehicle");
        JButton historyBtn = new JButton("My Bookings");
        JButton paymentsBtn = new JButton("My Payments");
        JButton logoutBtn = new JButton("Logout");

        buttonPanel.add(bookBtn);
        buttonPanel.add(returnBtn);
        buttonPanel.add(historyBtn);
        buttonPanel.add(paymentsBtn);
        buttonPanel.add(logoutBtn);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        loadVehicles();

        bookBtn.addActionListener(e -> bookVehicle());
        returnBtn.addActionListener(e -> returnVehicle());
        historyBtn.addActionListener(e -> showHistory());
        paymentsBtn.addActionListener(e -> showPayments());
        logoutBtn.addActionListener(e -> {
            frame.dispose();
            new AuthenticationSystem(conn).showWelcome();
        });

        frame.setVisible(true);
    }

    private void loadVehicles() {
        model.setRowCount(0);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT VehicleID, Model, Category, RentPerDay, Available FROM Vehicles ORDER BY VehicleID");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getInt("VehicleID"), rs.getString("Model"),
                        rs.getString("Category"), rs.getDouble("RentPerDay"),
                        "Y".equals(rs.getString("Available")) ? "Yes" : "No"
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void bookVehicle() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(frame, "Select a vehicle.");
            return;
        }

        int vid = (int) model.getValueAt(row, 0);
        if ("No".equals(model.getValueAt(row, 4))) {
            JOptionPane.showMessageDialog(frame, "Unavailable.");
            return;
        }

        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn
                    .prepareStatement("INSERT INTO Bookings (CustomerID, VehicleID, Status) VALUES (?, ?, 'Booked')")) {
                ps.setInt(1, customerId);
                ps.setInt(2, vid);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE Vehicles SET Available='N' WHERE VehicleID=?")) {
                ps.setInt(1, vid);
                ps.executeUpdate();
            }
            conn.commit();
            JOptionPane.showMessageDialog(frame, "Booked!");
            loadVehicles();
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception ignored) {
            }
            e.printStackTrace();
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (Exception ignored) {
            }
        }
    }

    private void returnVehicle() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(frame, "Select a vehicle.");
            return;
        }
        int vid = (int) model.getValueAt(row, 0);

        try {
            conn.setAutoCommit(false);
            Integer bid = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT BookingID FROM Bookings WHERE VehicleID=? AND CustomerID=? AND Status='Booked'")) {
                ps.setInt(1, vid);
                ps.setInt(2, customerId);
                ResultSet rs = ps.executeQuery();
                if (rs.next())
                    bid = rs.getInt(1);
            }
            if (bid == null) {
                JOptionPane.showMessageDialog(frame, "No active booking.");
                conn.rollback();
                return;
            }

            try (PreparedStatement ps = conn
                    .prepareStatement("UPDATE Bookings SET Status='Returned', ReturnDate=SYSDATE WHERE BookingID=?")) {
                ps.setInt(1, bid);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE Vehicles SET Available='Y' WHERE VehicleID=?")) {
                ps.setInt(1, vid);
                ps.executeUpdate();
            }
            conn.commit();
            JOptionPane.showMessageDialog(frame, "Returned!");
            loadVehicles();
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception ignored) {
            }
            e.printStackTrace();
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (Exception ignored) {
            }
        }
    }

    private void showHistory() {
        DefaultTableModel hModel = new DefaultTableModel(
                new Object[] { "ID", "Vehicle", "Status", "Booked", "Returned" }, 0);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT b.BookingID, v.Model, b.Status, b.BookingDate, b.ReturnDate FROM Bookings b JOIN Vehicles v ON b.VehicleID=v.VehicleID WHERE b.CustomerID=? ORDER BY b.BookingID DESC")) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                hModel.addRow(new Object[] { rs.getInt(1), rs.getString(2), rs.getString(3), rs.getTimestamp(4),
                        rs.getTimestamp(5) });
        } catch (Exception e) {
            e.printStackTrace();
        }
        JOptionPane.showMessageDialog(frame, new JScrollPane(new JTable(hModel)), "History", JOptionPane.PLAIN_MESSAGE);
    }

    private void showPayments() {
        DefaultTableModel pModel = new DefaultTableModel(new Object[] { "ID", "Vehicle", "Amount", "Method", "Date" },
                0);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT p.PaymentID, v.Model, p.Amount, p.PaymentMethod, p.PaymentDate FROM Payments p JOIN Bookings b ON p.BookingID=b.BookingID JOIN Vehicles v ON b.VehicleID=v.VehicleID WHERE b.CustomerID=? ORDER BY p.PaymentDate DESC")) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                pModel.addRow(new Object[] { rs.getInt(1), rs.getString(2), rs.getDouble(3), rs.getString(4),
                        rs.getTimestamp(5) });
        } catch (Exception e) {
            e.printStackTrace();
        }
        JOptionPane.showMessageDialog(frame, new JScrollPane(new JTable(pModel)), "Payments",
                JOptionPane.PLAIN_MESSAGE);
    }
}

class AdminDashboard {
    private final Connection conn;
    private JFrame frame;
    private DefaultTableModel model;
    private JTable table;
    private JTextField modelField, categoryField, rentField;

    public AdminDashboard(Connection conn) {
        this.conn = conn;
    }

    private void showTopCustomers() {
        DefaultTableModel model = new DefaultTableModel(
                new Object[] { "Rank", "Customer Name", "Mobile", "Total Bookings" }, 0);
        JTable table = new JTable(model);

        String sql = "SELECT c.Name, c.MobileNumber, COUNT(b.BookingID) as BookingCount " +
                "FROM Bookings b " +
                "JOIN Customers c ON b.CustomerID = c.CustomerID " +
                "GROUP BY c.Name, c.MobileNumber " +
                "ORDER BY BookingCount DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            int rank = 1;
            while (rs.next()) {
                model.addRow(new Object[] {
                        rank++,
                        rs.getString("Name"),
                        rs.getString("MobileNumber"),
                        rs.getInt("BookingCount")
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage());
        }

        JOptionPane.showMessageDialog(frame, new JScrollPane(table), "Top 5 Loyal Customers",
                JOptionPane.PLAIN_MESSAGE);
    }

    public void show() {
        frame = new JFrame("Admin Dashboard");
        frame.setSize(1100, 600);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        model = new DefaultTableModel(new Object[] { "Vehicle ID", "Model", "Category", "Rent/Day", "Available" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(model);
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if ("No".equals(value))
                    c.setForeground(Color.RED);
                else
                    c.setForeground(Color.BLACK);
                return c;
            }
        });
        table.setRowHeight(25);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                modelField.setText(String.valueOf(table.getValueAt(row, 1)));
                categoryField.setText(String.valueOf(table.getValueAt(row, 2)));
                rentField.setText(String.valueOf(table.getValueAt(row, 3)));
            }
        });

        setupFormsAndButtons();
        loadVehicles();
        frame.setVisible(true);
    }

    private void setupFormsAndButtons() {
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Vehicle Management", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        topPanel.add(titleLabel, BorderLayout.CENTER);

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        modelField = new JTextField();
        categoryField = new JTextField();
        rentField = new JTextField();
        formPanel.add(new JLabel("Model:"));
        formPanel.add(modelField);
        formPanel.add(new JLabel("Category:"));
        formPanel.add(categoryField);
        formPanel.add(new JLabel("Rent Per Day:"));
        formPanel.add(rentField);
        topPanel.add(formPanel, BorderLayout.SOUTH);
        frame.add(topPanel, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton topCustBtn = new JButton("Top Customers");
        JButton addBtn = new JButton("Add");
        JButton updateBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");
        JButton reportBtn = new JButton("Report");
        JButton custBtn = new JButton("Customers");
        JButton delCustBtn = new JButton("Del Customer");
        JButton bookBtn = new JButton("Bookings");
        JButton payBtn = new JButton("Rec. Payment");
        JButton logoutBtn = new JButton("Logout");

        btnPanel.add(topCustBtn);
        btnPanel.add(addBtn);
        btnPanel.add(updateBtn);
        btnPanel.add(deleteBtn);
        btnPanel.add(reportBtn);
        btnPanel.add(custBtn);
        btnPanel.add(delCustBtn);
        btnPanel.add(bookBtn);
        btnPanel.add(payBtn);
        btnPanel.add(logoutBtn);
        frame.add(btnPanel, BorderLayout.SOUTH);

        // Listeners
        addBtn.addActionListener(e -> addVehicle());
        updateBtn.addActionListener(e -> updateVehicle());
        deleteBtn.addActionListener(e -> deleteVehicle());
        reportBtn.addActionListener(e -> generateReport());
        custBtn.addActionListener(e -> viewCustomers());
        delCustBtn.addActionListener(e -> deleteCustomer());
        bookBtn.addActionListener(e -> viewBookings());
        payBtn.addActionListener(e -> recordPayment());
        topCustBtn.addActionListener(e -> showTopCustomers());
        logoutBtn.addActionListener(e -> {
            frame.dispose();
            new AuthenticationSystem(conn).showWelcome();
        });
    }

    private void loadVehicles() {
        model.setRowCount(0);
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT VehicleID, Model, Category, RentPerDay, Available FROM Vehicles ORDER BY VehicleID");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getInt(1), rs.getString(2), rs.getString(3), rs.getDouble(4),
                        "Y".equals(rs.getString(5)) ? "Yes" : "No"
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addVehicle() {
        try {
            double rent = Double.parseDouble(rentField.getText());
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Vehicles (Model, Category, RentPerDay, Available) VALUES (?, ?, ?, 'Y')")) {
                ps.setString(1, modelField.getText());
                ps.setString(2, categoryField.getText());
                ps.setDouble(3, rent);
                ps.executeUpdate();
            }
            loadVehicles();
            JOptionPane.showMessageDialog(frame, "Added!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage());
        }
    }

    private void updateVehicle() {
        int row = table.getSelectedRow();
        if (row < 0)
            return;
        try {
            int vid = (int) model.getValueAt(row, 0);
            try (PreparedStatement ps = conn
                    .prepareStatement("UPDATE Vehicles SET Model=?, Category=?, RentPerDay=? WHERE VehicleID=?")) {
                ps.setString(1, modelField.getText());
                ps.setString(2, categoryField.getText());
                ps.setDouble(3, Double.parseDouble(rentField.getText()));
                ps.setInt(4, vid);
                ps.executeUpdate();
            }
            loadVehicles();
            JOptionPane.showMessageDialog(frame, "Updated!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage());
        }
    }

    private void deleteVehicle() {
        int row = table.getSelectedRow();
        if (row < 0)
            return;
        int vid = (int) model.getValueAt(row, 0);
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Vehicles WHERE VehicleID=?")) {
            ps.setInt(1, vid);
            ps.executeUpdate();
            loadVehicles();
            JOptionPane.showMessageDialog(frame, "Deleted!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage());
        }
    }

    private void generateReport() {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) AS Total, SUM(CASE WHEN Available='Y' THEN 1 ELSE 0 END) AS Avail, SUM(CASE WHEN Available='N' THEN 1 ELSE 0 END) AS Rented FROM Vehicles");
                ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                JOptionPane.showMessageDialog(frame,
                        "Total: " + rs.getInt(1) + "\nAvailable: " + rs.getInt(2) + "\nRented: " + rs.getInt(3));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void viewCustomers() {
        DefaultTableModel cModel = new DefaultTableModel(new Object[] { "ID", "Name", "Mobile", "User" }, 0);
        try (PreparedStatement ps = conn
                .prepareStatement("SELECT CustomerID, Name, MobileNumber, Username FROM Customers");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                cModel.addRow(new Object[] { rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4) });
        } catch (Exception e) {
            e.printStackTrace();
        }
        JOptionPane.showMessageDialog(frame, new JScrollPane(new JTable(cModel)));
    }

    private void deleteCustomer() {
        String idStr = JOptionPane.showInputDialog(frame, "Enter Cust ID:");
        if (idStr == null)
            return;
        try {
            int cid = Integer.parseInt(idStr);
            int conf = JOptionPane.showConfirmDialog(frame, "Delete Customer & History?", "Warning",
                    JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                conn.setAutoCommit(false);
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM Payments WHERE BookingID IN (SELECT BookingID FROM Bookings WHERE CustomerID=?)")) {
                    ps.setInt(1, cid);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Bookings WHERE CustomerID=?")) {
                    ps.setInt(1, cid);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM Customers WHERE CustomerID=?")) {
                    ps.setInt(1, cid);
                    ps.executeUpdate();
                }
                conn.commit();
                JOptionPane.showMessageDialog(frame, "Deleted.");
            }
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception ignored) {
            }
            JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage());
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (Exception ignored) {
            }
        }
    }

    private void viewBookings() {
        JDialog dialog = new JDialog(frame, "Booking Management", true);
        dialog.setSize(900, 500);
        dialog.setLocationRelativeTo(frame);
        dialog.setLayout(new BorderLayout());

        // Create the table
        DefaultTableModel bModel = new DefaultTableModel(
                new Object[] { "ID", "Customer", "Vehicle", "Status", "Date" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable table = new JTable(bModel);
        table.setRowHeight(25);

        JPanel filterPanel = new JPanel(new FlowLayout());
        JButton btnAll = new JButton("Show All");
        JButton btnBooked = new JButton("Show Active (Booked)");
        JButton btnReturned = new JButton("Show Returned");

        btnBooked.setForeground(Color.BLUE);

        filterPanel.add(btnAll);
        filterPanel.add(btnBooked);
        filterPanel.add(btnReturned);

      
        // 1. Show All
        btnAll.addActionListener(e -> loadBookingData(bModel, "ALL"));

        // 2. Show Only Booked
        btnBooked.addActionListener(e -> loadBookingData(bModel, "Booked"));

        // 3. Show Only Returned
        btnReturned.addActionListener(e -> loadBookingData(bModel, "Returned"));

        // Initial load (Show All by default)
        loadBookingData(bModel, "ALL");

        // Add everything to dialog
        dialog.add(filterPanel, BorderLayout.NORTH);
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void loadBookingData(DefaultTableModel model, String statusFilter) {
        model.setRowCount(0); 

        StringBuilder sql = new StringBuilder(
                "SELECT b.BookingID, c.Name, v.Model, b.Status, b.BookingDate " +
                        "FROM Bookings b " +
                        "JOIN Customers c ON b.CustomerID = c.CustomerID " +
                        "JOIN Vehicles v ON b.VehicleID = v.VehicleID ");

        if (!"ALL".equals(statusFilter)) {
            sql.append("WHERE b.Status = ? ");
        }

        sql.append("ORDER BY b.BookingID DESC");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (!"ALL".equals(statusFilter)) {
                ps.setString(1, statusFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[] {
                            rs.getInt("BookingID"),
                            rs.getString("Name"),
                            rs.getString("Model"),
                            rs.getString("Status"),
                            rs.getTimestamp("BookingDate")
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error loading filtered data: " + e.getMessage());
        }
    }

    private void recordPayment() {
        String bidStr = JOptionPane.showInputDialog(frame, "Enter Booking ID:");
        if (bidStr == null)
            return;
        try {
            int bid = Integer.parseInt(bidStr);
            boolean exists = false;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM Bookings WHERE BookingID=?")) {
                ps.setInt(1, bid);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0)
                    exists = true;
            }
            if (!exists) {
                JOptionPane.showMessageDialog(frame, "Invalid Booking ID.");
                return;
            }

            String amt = JOptionPane.showInputDialog(frame, "Amount:");
            String method = JOptionPane.showInputDialog(frame, "Method:");

            try (PreparedStatement ps = conn
                    .prepareStatement("INSERT INTO Payments (BookingID, Amount, PaymentMethod) VALUES (?, ?, ?)")) {
                ps.setInt(1, bid);
                ps.setDouble(2, Double.parseDouble(amt));
                ps.setString(3, method);
                ps.executeUpdate();
            }
            JOptionPane.showMessageDialog(frame, "Recorded!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage());
        }
    }
}