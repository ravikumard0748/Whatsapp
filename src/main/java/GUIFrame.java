import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * A simple but more professional Swing-based GUI for the Mini-WhatsApp app.
 * - Left: contacts list
 * - Center: chat view + message composer
 * - Top: toolbar with Login / Logout / Settings
 * Uses the existing MessagingServer API.
 */
public class GUIFrame extends JFrame {
    private final MessagingServer server;
    private String activeUser = null;
    private NotificationObserver observer = null;

    private final DefaultListModel<String> contactsModel = new DefaultListModel<>();
    private final JList<String> contactsList = new JList<>(contactsModel);

    private final JTextArea chatArea = new JTextArea();
    private final JTextField messageField = new JTextField();
    private final JButton sendButton = new JButton("Send");

    private final JLabel statusBar = new JLabel("Not logged in");

    private String selectedContact = null;

    public GUIFrame(MessagingServer server) {
        super("Mini-WhatsApp");
        this.server = server;
        initLookAndFeel();
        initComponents();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    private void initLookAndFeel() {
        try { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); }
        catch (Exception ignored) {}
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JButton loginBtn = new JButton("Login / Register");
        JButton logoutBtn = new JButton("Logout");
        JButton settingsBtn = new JButton("DB Settings");
        toolBar.add(loginBtn);
        toolBar.add(logoutBtn);
        toolBar.addSeparator();
        toolBar.add(settingsBtn);
        add(toolBar, BorderLayout.NORTH);

        // Left: contacts
        contactsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(new EmptyBorder(8,8,8,8));
        left.add(new JLabel("Contacts"), BorderLayout.NORTH);
        left.add(new JScrollPane(contactsList), BorderLayout.CENTER);
        JButton refreshBtn = new JButton("Refresh");
        left.add(refreshBtn, BorderLayout.SOUTH);
        left.setPreferredSize(new Dimension(230, 0));
        add(left, BorderLayout.WEST);

        // Center: chat + composer
        chatArea.setEditable(false);
        chatArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(new EmptyBorder(8,8,8,8));
        center.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel composer = new JPanel(new BorderLayout(8,8));
        composer.add(messageField, BorderLayout.CENTER);
        composer.add(sendButton, BorderLayout.EAST);
        composer.setBorder(new EmptyBorder(8,0,0,0));
        center.add(composer, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        // Status bar
        statusBar.setBorder(new EmptyBorder(4,8,4,8));
        add(statusBar, BorderLayout.SOUTH);

        // Event wiring
        loginBtn.addActionListener(e -> showLoginDialog());
        logoutBtn.addActionListener(e -> doLogout());
        settingsBtn.addActionListener(e -> showSettingsDialog());
        refreshBtn.addActionListener(e -> reloadContacts());

        contactsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String sel = contactsList.getSelectedValue();
                if (sel != null) {
                    selectedContact = sel.split(" ")[0]; // username is first token
                    loadChatWith(selectedContact);
                }
            }
        });

        sendButton.addActionListener(e -> doSendMessage());
        messageField.addActionListener(e -> doSendMessage());

        // initial state
        refreshContactsModel();
    }

    private void showLoginDialog() {
        JPanel p = new JPanel(new GridLayout(0,1,8,8));
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        p.add(new JLabel("Username")); p.add(username);
        p.add(new JLabel("Password")); p.add(password);
        int option = JOptionPane.showOptionDialog(this, p, "Login or Register", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, new Object[]{"Login","Register","Cancel"}, "Login");
        if (option == 0 || option == 1) {
            String u = username.getText().trim();
            String pwd = new String(password.getPassword());
            try {
                if (option == 1) server.registerUser(u, pwd);
                // create observer that updates the chat area and status bar
                observer = new NotificationObserver() {
                    @Override
                    public void update(Notification notification) {
                        SwingUtilities.invokeLater(() -> {
                            if (notification.getType() == NotificationType.NEW_MESSAGE) {
                                Message m = notification.getMessage();
                                appendMessageToChat(m);
                                // optionally show toast
                                Toolkit.getDefaultToolkit().beep();
                            } else if (notification.getType() == NotificationType.USER_ONLINE) {
                                statusBar.setText(notification.getNote());
                            } else if (notification.getType() == NotificationType.MESSAGE_STATUS_UPDATE) {
                                // show in status bar briefly
                                statusBar.setText(notification.getNote());
                            }
                        });
                    }
                };
                server.loginUser(u, pwd, observer);
                activeUser = u;
                statusBar.setText("Logged in as: " + u);
                refreshContactsModel();
                loadChatWith(selectedContact);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Auth error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void doLogout() {
        if (activeUser == null) return;
        server.logoutUser(activeUser, observer);
        activeUser = null; observer = null; selectedContact = null;
        chatArea.setText("");
        statusBar.setText("Not logged in");
        refreshContactsModel();
    }

    private void showSettingsDialog() {
        JPanel p = new JPanel(new GridLayout(0,1,8,8));
        JTextField uri = new JTextField("mongodb://127.0.0.1:27017");
        JTextField db = new JTextField("whatsapp");
        p.add(new JLabel("MongoDB URI")); p.add(uri);
        p.add(new JLabel("Database name")); p.add(db);
        int ok = JOptionPane.showConfirmDialog(this, p, "DB Settings", JOptionPane.OK_CANCEL_OPTION);
        if (ok == JOptionPane.OK_OPTION) {
            boolean res = server.configureMongo(uri.getText().trim(), db.getText().trim());
            statusBar.setText(res ? "DB configured" : "DB connect failed");
            refreshContactsModel();
        }
    }

    private void reloadContacts() { refreshContactsModel(); }

    private void refreshContactsModel() {
        contactsModel.clear();
        for (User u : server.listUsers()) {
            contactsModel.addElement(u.getUsername() + " (" + u.getStatus() + ")");
        }
    }

    private void loadChatWith(String username) {
        chatArea.setText("");
        if (username == null) return;
        List<Message> history = server.getMessageHistory(username);
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Message m : history) {
            chatArea.append("[" + m.getTimestamp().format(f) + "] ");
            chatArea.append(m.getSender() + " -> " + m.getReceiver() + ": " + m.getContent() + " (" + m.getStatus() + ")\n");
        }
    }

    private void appendMessageToChat(Message m) {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        chatArea.append("[" + m.getTimestamp().format(f) + "] " + m.getSender() + " -> " + m.getReceiver() + ": " + m.getContent() + " (" + m.getStatus() + ")\n");
    }

    private void doSendMessage() {
        if (activeUser == null) { JOptionPane.showMessageDialog(this, "Please login first."); return; }
        if (selectedContact == null) { JOptionPane.showMessageDialog(this, "Select a contact first."); return; }
        String msg = messageField.getText().trim();
        if (msg.isEmpty()) return;
        try {
            server.sendMessage(activeUser, selectedContact, msg);
            // locally append message for immediate feedback
            Message m = MessageFactory.createTextMessage(activeUser, selectedContact, msg);
            m.setStatus(MessageStatus.SENT);
            appendMessageToChat(m);
            messageField.setText("");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Send failed: " + e.getMessage());
        }
    }
}
