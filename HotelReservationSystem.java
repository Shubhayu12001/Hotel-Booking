import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import com.toedter.calendar.JDateChooser;   // <-- import JDateChooser
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;

public class HotelReservationSystem extends JFrame implements ActionListener {

    // ==== Models ====
    static class Room {
        int id;
        String category;
        double pricePerNight;

        Room(int id, String category, double pricePerNight) {
            this.id = id;
            this.category = category;
            this.pricePerNight = pricePerNight;
        }
    }

    static class Reservation {
        String reservationId;
        int roomId;
        String guestName;
        String guestPhone;
        LocalDate checkIn;
        LocalDate checkOut;
        double amount;
        boolean paid;
        String paymentRef;

        Reservation(String reservationId, int roomId, String guestName, String guestPhone,
                    LocalDate checkIn, LocalDate checkOut, double amount, boolean paid, String paymentRef) {
            this.reservationId = reservationId;
            this.roomId = roomId;
            this.guestName = guestName;
            this.guestPhone = guestPhone;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.amount = amount;
            this.paid = paid;
            this.paymentRef = paymentRef;
        }
    }

    // ==== Service (data + business logic) ====
    static class HotelService {
        private final List<Room> rooms = new ArrayList<>();
        private final List<Reservation> reservations = new ArrayList<>();
        private final String ROOMS_PATH = "rooms.csv";
        private final String RES_PATH = "reservations.csv";

        HotelService() {
            try {
                loadRooms();
                loadReservations();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<Room> getRooms() { return rooms; }
        List<Reservation> getReservations() { return reservations; }

        List<Room> searchAvailable(String category, LocalDate in, LocalDate out) {
            List<Room> result = new ArrayList<>();
            for (Room r : rooms) {
                if (!category.equals("Any") && !r.category.equalsIgnoreCase(category)) continue;
                if (isRoomAvailable(r.id, in, out)) result.add(r);
            }
            return result;
        }

        boolean isRoomAvailable(int roomId, LocalDate in, LocalDate out) {
            for (Reservation res : reservations) {
                if (res.roomId == roomId) {
                    if (overlaps(in, out, res.checkIn, res.checkOut)) return false;
                }
            }
            return true;
        }

        static boolean overlaps(LocalDate aStart, LocalDate aEnd, LocalDate bStart, LocalDate bEnd) {
            return !(aEnd.compareTo(bStart) <= 0 || aStart.compareTo(bEnd) >= 0);
        }

        Reservation makeReservation(Room room, String name, String phone, LocalDate in, LocalDate out, boolean simulatePay) throws IOException {
            long nights = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(in, out));
            double amount = nights * room.pricePerNight;
            String id = "RES-" + System.currentTimeMillis();
            String paymentRef = "";
            boolean paid = false;
            if (simulatePay) {
                paid = true;
                paymentRef = "TXN" + (int)(Math.random()*900000 + 100000);
            }
            Reservation res = new Reservation(id, room.id, name, phone, in, out, amount, paid, paymentRef);
            reservations.add(res);
            saveReservations();
            return res;
        }

        boolean cancelReservation(String reservationId) throws IOException {
            Iterator<Reservation> it = reservations.iterator();
            while (it.hasNext()) {
                Reservation r = it.next();
                if (r.reservationId.equalsIgnoreCase(reservationId)) {
                    it.remove();
                    saveReservations();
                    return true;
                }
            }
            return false;
        }

        Room findRoomById(int id) {
            for (Room r : rooms) if (r.id == id) return r;
            return null;
        }

        private void loadRooms() throws IOException {
            if (!Files.exists(Paths.get(ROOMS_PATH))) {
                rooms.add(new Room(101, "Standard", 2000));
                rooms.add(new Room(102, "Standard", 2000));
                rooms.add(new Room(201, "Deluxe", 3500));
                rooms.add(new Room(202, "Deluxe", 3600));
                rooms.add(new Room(301, "Suite", 6000));
                saveRooms();
            } else {
                try (BufferedReader br = Files.newBufferedReader(Paths.get(ROOMS_PATH))) {
                    String line;
                    rooms.clear();
                    while ((line = br.readLine()) != null) {
                        if (line.trim().isEmpty() || line.startsWith("#")) continue;
                        String[] a = line.split(",");
                        rooms.add(new Room(Integer.parseInt(a[0].trim()), a[1].trim(),
                                Double.parseDouble(a[2].trim())));
                    }
                }
            }
        }

        private void saveRooms() throws IOException {
            try (PrintWriter pw = new PrintWriter(new FileWriter(ROOMS_PATH))) {
                pw.println("#id,category,pricePerNight");
                for (Room r : rooms) {
                    pw.println(r.id + "," + r.category + "," + r.pricePerNight);
                }
            }
        }

        private void loadReservations() throws IOException {
            if (!Files.exists(Paths.get(RES_PATH))) {
                saveReservations();
                return;
            }
            try (BufferedReader br = Files.newBufferedReader(Paths.get(RES_PATH))) {
                String line;
                reservations.clear();
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty() || line.startsWith("#")) continue;
                    String[] a = line.split(",");
                    Reservation r = new Reservation(
                            a[0], Integer.parseInt(a[1]), a[2], a[3],
                            LocalDate.parse(a[4]), LocalDate.parse(a[5]),
                            Double.parseDouble(a[6]),
                            Boolean.parseBoolean(a[7]),
                            a.length > 8 ? a[8] : ""
                    );
                    reservations.add(r);
                }
            }
        }

        private void saveReservations() throws IOException {
            try (PrintWriter pw = new PrintWriter(new FileWriter(RES_PATH))) {
                pw.println("#reservationId,roomId,name,phone,checkIn,checkOut,amount,paid,paymentRef");
                for (Reservation r : reservations) {
                    pw.println(String.join(",",
                            r.reservationId,
                            String.valueOf(r.roomId),
                            r.guestName,
                            r.guestPhone,
                            r.checkIn.toString(),
                            r.checkOut.toString(),
                            String.valueOf(r.amount),
                            String.valueOf(r.paid),
                            r.paymentRef
                    ));
                }
            }
        }
    }

    // ==== UI components ====
    private final HotelService service = new HotelService();

    private JComboBox<String> categoryBox;
    private JDateChooser inDateChooser, outDateChooser;  // Calendar pickers
    private JTextField nameField, phoneField;
    private DefaultTableModel searchModel;
    private JTable searchTable;
    private JButton btnSearch, btnBook, btnClear;

    private DefaultTableModel resModel;
    private JTable resTable;
    private JTextField cancelIdField;
    private JButton btnCancel, btnRefresh;

    public HotelReservationSystem() {
        setTitle("Hotel Reservation System - (Student Edition)");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Welcome to Hotel Californa", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(title, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Search & Book", buildSearchPanel());
        tabs.add("Manage Bookings", buildManagePanel());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildSearchPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JPanel form = new JPanel(new GridLayout(3, 4, 10, 8));
        form.setBorder(BorderFactory.createTitledBorder("Search Criteria / Guest Details"));

        form.add(new JLabel("Category:"));
        categoryBox = new JComboBox<>(new String[]{"Any", "Standard", "Deluxe", "Suite"});
        form.add(categoryBox);

        form.add(new JLabel("Check-In:"));
        inDateChooser = new JDateChooser();
        inDateChooser.setDateFormatString("yyyy-MM-dd");
        form.add(inDateChooser);

        form.add(new JLabel("Check-Out:"));
        outDateChooser = new JDateChooser();
        outDateChooser.setDateFormatString("yyyy-MM-dd");
        form.add(outDateChooser);

        form.add(new JLabel("Guest Name:"));
        nameField = new JTextField();
        form.add(nameField);

        form.add(new JLabel("Phone:"));
        phoneField = new JTextField();
        form.add(phoneField);

        btnSearch = new JButton("Search Rooms");
        btnSearch.addActionListener(this);
        form.add(btnSearch);

        btnBook = new JButton("Book Selected (with Payment)");
        btnBook.addActionListener(this);
        form.add(btnBook);

        btnClear = new JButton("Clear");
        btnClear.addActionListener(this);
        form.add(btnClear);

        p.add(form, BorderLayout.NORTH);

        searchModel = new DefaultTableModel(new String[]{"Room ID", "Category", "Price/Night"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        searchTable = new JTable(searchModel);
        p.add(new JScrollPane(searchTable), BorderLayout.CENTER);

        return p;
    }

    private JPanel buildManagePanel() {
        JPanel p = new JPanel(new BorderLayout(10,10));
        p.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        resModel = new DefaultTableModel(new String[]{
                "Reservation ID", "Room", "Category", "Guest", "Phone", "Check-In", "Check-Out", "Amount", "Paid", "Payment Ref"
        }, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        resTable = new JTable(resModel);
        refreshReservationsTable();
        p.add(new JScrollPane(resTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setBorder(BorderFactory.createTitledBorder("Actions"));

        actions.add(new JLabel("Reservation ID:"));
        cancelIdField = new JTextField(20);
        actions.add(cancelIdField);

        btnCancel = new JButton("Cancel Reservation");
        btnCancel.addActionListener(this);
        actions.add(btnCancel);

        btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(this);
        actions.add(btnRefresh);

        p.add(actions, BorderLayout.SOUTH);
        return p;
    }

    private void refreshReservationsTable() {
        resModel.setRowCount(0);
        for (Reservation r : service.getReservations()) {
            Room room = service.findRoomById(r.roomId);
            String cat = room != null ? room.category : "?";
            resModel.addRow(new Object[]{
                    r.reservationId, r.roomId, cat, r.guestName, r.guestPhone,
                    r.checkIn, r.checkOut, r.amount, r.paid ? "Yes" : "No", r.paymentRef
            });
        }
    }

    private void performSearch() {
        searchModel.setRowCount(0);

        String cat = (String) categoryBox.getSelectedItem();
        Date inRaw = inDateChooser.getDate();
        Date outRaw = outDateChooser.getDate();

        if (inRaw == null || outRaw == null) {
            JOptionPane.showMessageDialog(this, "Please select both Check-In and Check-Out dates.");
            return;
        }

        LocalDate inDate = new java.sql.Date(inRaw.getTime()).toLocalDate();
        LocalDate outDate = new java.sql.Date(outRaw.getTime()).toLocalDate();

        if (!outDate.isAfter(inDate)) {
            JOptionPane.showMessageDialog(this, "Check-Out must be after Check-In.");
            return;
        }

        List<Room> available = service.searchAvailable(cat, inDate, outDate);
        for (Room r : available) {
            searchModel.addRow(new Object[]{r.id, r.category, r.pricePerNight});
        }

        if (available.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No rooms available for the selected dates.");
        }
    }

    private void performBooking() {
        int row = searchTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a room to book from the table.");
            return;
        }
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        if (name.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter guest name and phone.");
            return;
        }

        Date inRaw = inDateChooser.getDate();
        Date outRaw = outDateChooser.getDate();
        if (inRaw == null || outRaw == null) {
            JOptionPane.showMessageDialog(this, "Please select valid dates.");
            return;
        }

        LocalDate inDate = new java.sql.Date(inRaw.getTime()).toLocalDate();
        LocalDate outDate = new java.sql.Date(outRaw.getTime()).toLocalDate();

        if (!outDate.isAfter(inDate)) {
            JOptionPane.showMessageDialog(this, "Check-Out must be after Check-In.");
            return;
        }

        int roomId = Integer.parseInt(String.valueOf(searchModel.getValueAt(row, 0)));
        Room room = service.findRoomById(roomId);

        String card = JOptionPane.showInputDialog(this,
                "Payment Simulation:\nEnter last 4 digits of card to proceed:", "0000");
        if (card == null) return;
        if (!card.matches("\\d{4}")) {
            JOptionPane.showMessageDialog(this, "Invalid card number (must be 4 digits).");
            return;
        }

        try {
            Reservation res = service.makeReservation(room, name, phone, inDate, outDate, true);
            JOptionPane.showMessageDialog(this,
                    "Booking Confirmed!\nReservation ID: " + res.reservationId +
                            "\nAmount: " + res.amount +
                            "\nPaid: Yes\nPayment Ref: " + res.paymentRef);
            refreshReservationsTable();
            performSearch();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error saving reservation: " + ex.getMessage());
        }
    }

    private void performCancel() {
        String id = cancelIdField.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a Reservation ID to cancel.");
            return;
        }
        try {
            boolean ok = service.cancelReservation(id);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Reservation cancelled.");
                refreshReservationsTable();
            } else {
                JOptionPane.showMessageDialog(this, "Reservation ID not found.");
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error updating file: " + ex.getMessage());
        }
    }

    private void clearForm() {
        categoryBox.setSelectedIndex(0);
        inDateChooser.setDate(null);
        outDateChooser.setDate(null);
        nameField.setText("");
        phoneField.setText("");
        searchModel.setRowCount(0);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnSearch) performSearch();
        else if (src == btnBook) performBooking();
        else if (src == btnClear) clearForm();
        else if (src == btnCancel) performCancel();
        else if (src == btnRefresh) refreshReservationsTable();
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new HotelReservationSystem().setVisible(true));
    }
}
