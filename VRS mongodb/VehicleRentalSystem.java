import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;
import java.util.HexFormat; 

public class VehicleRentalSystem {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                MongoManager dbManager = new MongoManager();
                new AuthSystem(dbManager).showWelcome();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Critical Error: " + e.getMessage());
            }
        });
    }
}

class MongoManager {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DB_NAME = "rental_db";

    private final MongoClient mongoClient;
    private final MongoDatabase database;

    // Collections
    public final MongoCollection<Document> admins;
    public final MongoCollection<Document> customers;
    public final MongoCollection<Document> vehicles;
    public final MongoCollection<Document> bookings;
    public final MongoCollection<Document> payments;
    public final MongoCollection<Document> counters;

    public MongoManager() {
        mongoClient = MongoClients.create(CONNECTION_STRING);
        database = mongoClient.getDatabase(DB_NAME);

        admins = database.getCollection("admins");
        customers = database.getCollection("customers");
        vehicles = database.getCollection("vehicles");
        bookings = database.getCollection("bookings");
        payments = database.getCollection("payments");
        counters = database.getCollection("counters");
    }

    // Auto-Increment Helper
    public int getNextSequence(String collectionId) {
        Document filter = new Document("_id", collectionId);
        Document update = new Document("$inc", new Document("seq", 1));
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
                .returnDocument(ReturnDocument.AFTER)
                .upsert(true);
        Document result = counters.findOneAndUpdate(filter, update, options);
        return (result != null) ? result.getInteger("seq") : 1;
    }
}

class AuthSystem {
    private final MongoManager db;

    public AuthSystem(MongoManager db) {
        this.db = db;
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

        JPanel btnPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
        JButton loginBtn = new JButton("Login");
        JButton signupBtn = new JButton("Create New Account");
        btnPanel.add(loginBtn);
        btnPanel.add(signupBtn);
        frame.add(btnPanel, BorderLayout.CENTER);

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
        frame.setSize(400, 250);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel fields = new JPanel(new GridLayout(2, 2, 10, 10));
        fields.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        fields.add(new JLabel("Username:"));
        fields.add(userField);
        fields.add(new JLabel("Password:"));
        fields.add(passField);

        JLabel status = new JLabel(" ", SwingConstants.CENTER);
        status.setForeground(Color.RED);

        JPanel btns = new JPanel(new FlowLayout());
        JButton login = new JButton("Login");
        JButton back = new JButton("Back");
        btns.add(login);
        btns.add(back);

        frame.add(fields, BorderLayout.CENTER);
        frame.add(status, BorderLayout.NORTH);
        frame.add(btns, BorderLayout.SOUTH);

        login.addActionListener(e -> {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword()).trim();
            if (u.isEmpty() || p.isEmpty()) {
                status.setText("Enter creds.");
                return;
            }

            String hash = sha256Hex(p);
            // Admin Check
            if (db.admins.find(new Document("Username", u).append("HashedPassword", hash)).first() != null) {
                frame.dispose();
                new AdminDashboard(db).show();
                return;
            }
            // Customer Check
            Document cust = db.customers.find(new Document("Username", u).append("HashedPassword", hash)).first();
            if (cust != null) {
                frame.dispose();
                new CustomerDashboard(db, cust.getInteger("CustomerID"), cust.getString("Name")).show();
                return;
            }
            status.setText("Invalid credentials.");
        });

        back.addActionListener(e -> {
            frame.dispose();
            showWelcome();
        });
        frame.setVisible(true);
    }

    public void showSignup() {
        JFrame frame = new JFrame("Create Account");
        frame.setSize(450, 350);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel fields = new JPanel(new GridLayout(5, 2, 10, 10));
        fields.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JTextField nameF = new JTextField();
        JTextField mobF = new JTextField();
        JTextField userF = new JTextField();
        JPasswordField passF = new JPasswordField();
        JPasswordField confF = new JPasswordField();

        fields.add(new JLabel("Name:"));
        fields.add(nameF);
        fields.add(new JLabel("Mobile:"));
        fields.add(mobF);
        fields.add(new JLabel("Username:"));
        fields.add(userF);
        fields.add(new JLabel("Password:"));
        fields.add(passF);
        fields.add(new JLabel("Confirm:"));
        fields.add(confF);

        JLabel status = new JLabel(" ", SwingConstants.CENTER);
        status.setForeground(Color.RED);

        JPanel btns = new JPanel(new FlowLayout());
        JButton create = new JButton("Create");
        JButton back = new JButton("Back");
        btns.add(create);
        btns.add(back);

        frame.add(fields, BorderLayout.CENTER);
        frame.add(status, BorderLayout.NORTH);
        frame.add(btns, BorderLayout.SOUTH);

        create.addActionListener(e -> {
            String n = nameF.getText();
            String m = mobF.getText();
            String u = userF.getText();
            String p = new String(passF.getPassword());
            if (n.isEmpty() || u.isEmpty() || p.isEmpty()) {
                status.setText("Fill all fields.");
                return;
            }
            if (!p.equals(new String(confF.getPassword()))) {
                status.setText("Mismatch.");
                return;
            }

            if (db.customers.countDocuments(new Document("Username", u)) > 0) {
                status.setText("Username taken.");
                return;
            }

            int id = db.getNextSequence("userid");
            db.customers.insertOne(new Document("CustomerID", id).append("Name", n)
                    .append("MobileNumber", m).append("Username", u).append("HashedPassword", sha256Hex(p)));

            JOptionPane.showMessageDialog(frame, "Created!");
            frame.dispose();
            showLogin();
        });

        back.addActionListener(e -> {
            frame.dispose();
            showWelcome();
        });
        frame.setVisible(true);
    }
}

class CustomerDashboard {
    private final MongoManager db;
    private final int cid;
    private final String cname;
    private JFrame frame;
    private DefaultTableModel model;
    private JTable table;

    public CustomerDashboard(MongoManager db, int cid, String cname) {
        this.db = db;
        this.cid = cid;
        this.cname = cname;
    }

    public void show() {
        frame = new JFrame("Customer: " + cname);
        frame.setSize(900, 500);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        model = new DefaultTableModel(new Object[] { "ID", "Model", "Category", "Rent", "Available" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(model);

        table.getColumnModel().getColumn(4).setCellRenderer((tbl, value, isSelected, hasFocus, row, col) -> {
            JLabel cell = new JLabel(value.toString());
            cell.setOpaque(true);
            int vid = (int) model.getValueAt(row, 0);
            long booked = db.bookings.countDocuments(new Document("VehicleID", vid)
                    .append("CustomerID", cid).append("Status", "Booked"));

            if (booked > 0)
                cell.setForeground(Color.BLUE);
            else if ("No".equals(value))
                cell.setForeground(Color.RED);
            else
                cell.setForeground(Color.BLACK);

            cell.setBackground(isSelected ? tbl.getSelectionBackground() : tbl.getBackground());
            return cell;
        });
        table.setRowHeight(25);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        frame.add(new JLabel("Welcome, " + cname, SwingConstants.CENTER), BorderLayout.NORTH);

        JPanel pnl = new JPanel(new FlowLayout());
        JButton book = new JButton("Book");
        JButton ret = new JButton("Return");
        JButton hist = new JButton("History");
        JButton pay = new JButton("Payments");
        JButton out = new JButton("Logout");
        pnl.add(book);
        pnl.add(ret);
        pnl.add(hist);
        pnl.add(pay);
        pnl.add(out);
        frame.add(pnl, BorderLayout.SOUTH);

        loadVehicles();

        book.addActionListener(e -> bookVehicle());
        ret.addActionListener(e -> returnVehicle());
        hist.addActionListener(e -> showHistory());
        pay.addActionListener(e -> showPayments());
        out.addActionListener(e -> {
            frame.dispose();
            new AuthSystem(db).showWelcome();
        });

        frame.setVisible(true);
    }

    private void loadVehicles() {
        model.setRowCount(0);
        for (Document d : db.vehicles.find().sort(new Document("VehicleID", 1))) {
            String avail = "No";
            if ("Y".equalsIgnoreCase(d.getString("Available")))
                avail = "Yes";

            double rent = 0.0;
            if (d.get("RentPerDay") instanceof Number)
                rent = ((Number) d.get("RentPerDay")).doubleValue();

            model.addRow(new Object[] { d.getInteger("VehicleID"), d.getString("Model"),
                    d.getString("Category"), rent, avail });
        }
    }

    private void bookVehicle() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(frame, "Select vehicle.");
            return;
        }
        if ("No".equals(model.getValueAt(r, 4))) {
            JOptionPane.showMessageDialog(frame, "Unavailable.");
            return;
        }

        int vid = (int) model.getValueAt(r, 0);
        if (JOptionPane.showConfirmDialog(frame, "Confirm Booking?", "Confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            int bid = db.getNextSequence("bookingid");
            db.bookings.insertOne(new Document("BookingID", bid).append("CustomerID", cid)
                    .append("VehicleID", vid).append("BookingDate", new Date()).append("Status", "Booked"));
            db.vehicles.updateOne(new Document("VehicleID", vid), new Document("$set", new Document("Available", "N")));
            JOptionPane.showMessageDialog(frame, "Booked!");
            loadVehicles();
        }
    }

    private void returnVehicle() {
        int r = table.getSelectedRow();
        if (r < 0)
            return;
        int vid = (int) model.getValueAt(r, 0);

        Document b = db.bookings
                .find(new Document("VehicleID", vid).append("CustomerID", cid).append("Status", "Booked")).first();
        if (b == null) {
            JOptionPane.showMessageDialog(frame, "Not booked by you.");
            return;
        }

        db.bookings.updateOne(new Document("BookingID", b.getInteger("BookingID")),
                new Document("$set", new Document("Status", "Returned").append("ReturnDate", new Date())));
        db.vehicles.updateOne(new Document("VehicleID", vid), new Document("$set", new Document("Available", "Y")));

        JOptionPane.showMessageDialog(frame, "Returned!");
        loadVehicles();
    }

    private void showHistory() {
        DefaultTableModel hm = new DefaultTableModel(new Object[] { "ID", "Vehicle", "Status", "Date" }, 0);
        List<Bson> pipe = Arrays.asList(
                Aggregates.match(Filters.eq("CustomerID", cid)),
                Aggregates.lookup("vehicles", "VehicleID", "VehicleID", "v"),
                Aggregates.unwind("$v"),
                Aggregates.sort(Sorts.descending("BookingDate")));
        for (Document d : db.bookings.aggregate(pipe)) {
            Document v = (Document) d.get("v");
            hm.addRow(new Object[] { d.getInteger("BookingID"), v.getString("Model"), d.getString("Status"),
                    d.getDate("BookingDate") });
        }
        JOptionPane.showMessageDialog(frame, new JScrollPane(new JTable(hm)));
    }

    private void showPayments() {
        DefaultTableModel pm = new DefaultTableModel(new Object[] { "ID", "Amt", "Method", "Date" }, 0);
        List<Integer> bids = new ArrayList<>();
        for (Document b : db.bookings.find(new Document("CustomerID", cid)))
            bids.add(b.getInteger("BookingID"));

        if (!bids.isEmpty()) {
            for (Document p : db.payments.find(Filters.in("BookingID", bids))) {
                pm.addRow(new Object[] { p.getInteger("PaymentID"), p.getDouble("Amount"), p.getString("PaymentMethod"),
                        p.getDate("PaymentDate") });
            }
        }
        JOptionPane.showMessageDialog(frame, new JScrollPane(new JTable(pm)));
    }
}

// === ADMIN DASHBOARD ===
class AdminDashboard {
    private final MongoManager db;
    private JFrame frame;
    private DefaultTableModel model;
    private JTable table;
    private JTextField mf, cf, rf;

    public AdminDashboard(MongoManager db) {
        this.db = db;
    }

    private void showTopCustomers() {
        DefaultTableModel model = new DefaultTableModel(new Object[] { "Rank", "Customer Name", "Total Bookings" }, 0);
        JTable table = new JTable(model);

        List<Bson> pipeline = Arrays.asList(
                Aggregates.group("$CustomerID", Accumulators.sum("total_bookings", 1)),

                Aggregates.sort(Sorts.descending("total_bookings")),

                Aggregates.lookup("customers", "_id", "CustomerID", "cust_info"),

                Aggregates.unwind("$cust_info"),

                Aggregates.limit(5));

        int rank = 1;
        for (Document doc : db.bookings.aggregate(pipeline)) {
            Document custInfo = (Document) doc.get("cust_info");
            model.addRow(new Object[] {
                    rank++,
                    custInfo.getString("Name"),
                    doc.getInteger("total_bookings")
            });
        }

        JOptionPane.showMessageDialog(frame, new JScrollPane(table), "Top 5 Customers (Aggregation)",
                JOptionPane.PLAIN_MESSAGE);
    }

    public void show() {
        frame = new JFrame("Admin Dashboard (Mongo OOP)");
        frame.setSize(1100, 600);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        model = new DefaultTableModel(new Object[] { "ID", "Model", "Category", "Rent", "Available" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(model);
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                if ("No".equals(v))
                    comp.setForeground(Color.RED);
                else
                    comp.setForeground(Color.BLACK);
                return comp;
            }
        });
        table.setRowHeight(25);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) {
                mf.setText(table.getValueAt(r, 1).toString());
                cf.setText(table.getValueAt(r, 2).toString());
                rf.setText(table.getValueAt(r, 3).toString());
            }
        });

        setupUI();
        loadVehicles();
        frame.setVisible(true);
    }

    private void setupUI() {
        JPanel top = new JPanel(new BorderLayout());
        JPanel form = new JPanel(new GridLayout(3, 2, 5, 5));
        mf = new JTextField();
        cf = new JTextField();
        rf = new JTextField();
        form.add(new JLabel("Model:"));
        form.add(mf);
        form.add(new JLabel("Category:"));
        form.add(cf);
        form.add(new JLabel("Rent:"));
        form.add(rf);
        top.add(new JLabel("Vehicle Management", SwingConstants.CENTER), BorderLayout.NORTH);
        top.add(form, BorderLayout.SOUTH);
        frame.add(top, BorderLayout.NORTH);

        JPanel btns = new JPanel(new FlowLayout());
        JButton topCustBtn = new JButton("Top Customers");
        JButton add = new JButton("Add Vehicle");
        JButton upd = new JButton("Update");
        JButton del = new JButton("Delete");
        JButton rep = new JButton("Report");
        JButton cust = new JButton("Customers");
        JButton dcust = new JButton("Delete Customer");
        JButton book = new JButton(" view Bookings");
        JButton pay = new JButton("Rec. Payment");
        JButton out = new JButton("Logout");

        btns.add(topCustBtn);
        btns.add(add);
        btns.add(upd);
        btns.add(del);
        btns.add(rep);
        btns.add(cust);
        btns.add(dcust);
        btns.add(book);
        btns.add(pay);
        btns.add(out);
        frame.add(btns, BorderLayout.SOUTH);

        topCustBtn.addActionListener(e -> showTopCustomers());
        add.addActionListener(e -> addV());
        upd.addActionListener(e -> updV());
        del.addActionListener(e -> delV());
        rep.addActionListener(e -> showRep());
        cust.addActionListener(e -> viewC());
        dcust.addActionListener(e -> delC());
        book.addActionListener(e -> viewBookings()); // THE NEW SORTING FEATURE
        pay.addActionListener(e -> recPay());
        out.addActionListener(e -> {
            frame.dispose();
            new AuthSystem(db).showWelcome();
        });
    }

    private void loadVehicles() {
        model.setRowCount(0);
        for (Document d : db.vehicles.find().sort(new Document("VehicleID", 1))) {
            String avail = "Y".equalsIgnoreCase(d.getString("Available")) ? "Yes" : "No";
            double rent = 0.0;
            if (d.get("RentPerDay") instanceof Number)
                rent = ((Number) d.get("RentPerDay")).doubleValue();
            model.addRow(new Object[] { d.getInteger("VehicleID"), d.getString("Model"), d.getString("Category"), rent,
                    avail });
        }
    }

    private void addV() {
        try {
            int id = db.getNextSequence("vehicleid");
            db.vehicles.insertOne(new Document("VehicleID", id).append("Model", mf.getText())
                    .append("Category", cf.getText()).append("RentPerDay", Double.parseDouble(rf.getText()))
                    .append("Available", "Y"));
            loadVehicles();
            JOptionPane.showMessageDialog(frame, "Added!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error!");
        }
    }

    private void updV() {
        int r = table.getSelectedRow();
        if (r < 0)
            return;
        int vid = (int) model.getValueAt(r, 0);
        db.vehicles.updateOne(new Document("VehicleID", vid), new Document("$set", new Document("Model", mf.getText())
                .append("Category", cf.getText()).append("RentPerDay", Double.parseDouble(rf.getText()))));
        loadVehicles();
    }

    private void delV() {
        int r = table.getSelectedRow();
        if (r < 0)
            return;
        db.vehicles.deleteOne(new Document("VehicleID", (int) model.getValueAt(r, 0)));
        loadVehicles();
    }

    private void showRep() {
        long tot = db.vehicles.countDocuments();
        long av = db.vehicles.countDocuments(new Document("Available", "Y"));
        JOptionPane.showMessageDialog(frame, "Total: " + tot + "\nAvail: " + av + "\nRented: " + (tot - av));
    }

    private void viewC() {
        DefaultTableModel cm = new DefaultTableModel(new Object[] { "ID", "Name", "Mobile", "User" }, 0);
        for (Document d : db.customers.find())
            cm.addRow(new Object[] { d.get("CustomerID"), d.get("Name"), d.get("MobileNumber"), d.get("Username") });
        JOptionPane.showMessageDialog(frame, new JScrollPane(new JTable(cm)));
    }

    private void delC() {
        String s = JOptionPane.showInputDialog("ID:");
        if (s == null)
            return;
        int id = Integer.parseInt(s);
        db.bookings.deleteMany(new Document("CustomerID", id));
        db.customers.deleteOne(new Document("CustomerID", id));
        JOptionPane.showMessageDialog(frame, "Deleted");
    }

    // --- NEW: BOOKING VIEW WITH SORTING ---
    private void viewBookings() {
        JDialog dlg = new JDialog(frame, "Bookings", true);
        dlg.setSize(900, 500);
        dlg.setLocationRelativeTo(frame);
        dlg.setLayout(new BorderLayout());

        DefaultTableModel bm = new DefaultTableModel(new Object[] { "ID", "Cust", "Veh", "Status", "Date" }, 0);
        JTable bt = new JTable(bm);
        bt.setRowHeight(25);

        JPanel p = new JPanel(new FlowLayout());
        JButton b1 = new JButton("Show All");
        JButton b2 = new JButton("Show Active");
        JButton b3 = new JButton("Show Returned");
        b2.setForeground(Color.BLUE);
        p.add(b1);
        p.add(b2);
        p.add(b3);

        b1.addActionListener(e -> loadBData(bm, "ALL"));
        b2.addActionListener(e -> loadBData(bm, "Booked"));
        b3.addActionListener(e -> loadBData(bm, "Returned"));

        loadBData(bm, "ALL"); // Load default

        dlg.add(p, BorderLayout.NORTH);
        dlg.add(new JScrollPane(bt), BorderLayout.CENTER);
        dlg.setVisible(true);
    }

    private void loadBData(DefaultTableModel mod, String filter) {
        mod.setRowCount(0);
        List<Bson> pipe = new ArrayList<>();
        pipe.add(Aggregates.lookup("customers", "CustomerID", "CustomerID", "c"));
        pipe.add(Aggregates.lookup("vehicles", "VehicleID", "VehicleID", "v"));
        pipe.add(Aggregates.unwind("$c"));
        pipe.add(Aggregates.unwind("$v"));

        if (!filter.equals("ALL")) {
            pipe.add(Aggregates.match(Filters.eq("Status", filter)));
        }
        pipe.add(Aggregates.sort(Sorts.descending("BookingID")));

        for (Document d : db.bookings.aggregate(pipe)) {
            Document c = (Document) d.get("c");
            Document v = (Document) d.get("v");
            mod.addRow(new Object[] { d.getInteger("BookingID"), c.getString("Name"), v.getString("Model"),
                    d.getString("Status"), d.getDate("BookingDate") });
        }
    }

    private void recPay() {
        String s = JOptionPane.showInputDialog("Booking ID:");
        if (s == null)
            return;
        int bid = Integer.parseInt(s);
        if (db.bookings.countDocuments(new Document("BookingID", bid)) == 0) {
            JOptionPane.showMessageDialog(frame, "Invalid ID");
            return;
        }
        String amt = JOptionPane.showInputDialog("Amount:");
        String meth = JOptionPane.showInputDialog("Method:");
        db.payments.insertOne(new Document("PaymentID", db.getNextSequence("paymentid"))
                .append("BookingID", bid).append("Amount", Double.parseDouble(amt))
                .append("PaymentMethod", meth).append("PaymentDate", new Date()));
        JOptionPane.showMessageDialog(frame, "Recorded!");
    }
}