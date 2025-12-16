import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;



public class WhatsApp {
    public static void main(String[] args) {
        ConsoleView view = new ConsoleView();
        AppController controller = new AppController(view, MessagingServer.getInstance());
        controller.start();
    }
}

enum UserStatus { ONLINE, OFFLINE }
enum MessageStatus { SENT, DELIVERED, READ }
enum NotificationType { NEW_MESSAGE, USER_ONLINE, MESSAGE_STATUS_UPDATE }
enum MessageType { TEXT }


class User {
    private final String username;
    private String password;
    private UserStatus status;
    private final Queue<Message> offlineQueue; // store pending messages
    private final List<Message> messageHistory; // both sent and received

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.status = UserStatus.OFFLINE;
        this.offlineQueue = new LinkedList<>();
        this.messageHistory = new ArrayList<>();
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public void enqueueOfflineMessage(Message m) { offlineQueue.add(m); }
    public Queue<Message> drainOfflineMessages() {
        Queue<Message> drained = new LinkedList<>(offlineQueue);
        offlineQueue.clear();
        return drained;
    }

    public void addToHistory(Message m) { messageHistory.add(m); }
    public List<Message> getMessageHistory() { return messageHistory; }

    @Override
    public String toString() {
        return username + " (" + status + ")";
    }
}

class Message {
    private final String id;
    private final String sender;
    private final String receiver;
    private final String content;
    private final LocalDateTime timestamp;
    private MessageStatus status;

    public Message(String id, String sender, String receiver, String content, LocalDateTime timestamp) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.timestamp = timestamp;
        this.status = MessageStatus.SENT;
    }

    public String getId() { return id; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }

    public void markDelivered() { this.status = MessageStatus.DELIVERED; }
    public void markRead() { this.status = MessageStatus.READ; }

    @Override
    public String toString() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "[" + timestamp.format(f) + "] from: " + sender + " -> " + receiver + " | " + content + " (" + status + ")";
    }
}

/* ===========================
   Factory Pattern for Message
   =========================== */
class MessageFactory {
    // Factory method for creating messages
    public static Message createTextMessage(String sender, String receiver, String content) {
        String id = UUID.randomUUID().toString();
        return new Message(id, sender, receiver, content, LocalDateTime.now());
    }
}

/* ===========================
   Observer Pattern Interfaces
   =========================== */
interface NotificationObserver {
    void update(Notification notification);
}

class Notification {
    private final NotificationType type;
    private final String username; // who caused the notification (sender or online user)
    private final Message message; // optional
    private final String note;

    public Notification(NotificationType type, String username, Message message, String note) {
        this.type = type;
        this.username = username;
        this.message = message;
        this.note = note;
    }

    public NotificationType getType() { return type; }
    public String getUsername() { return username; }
    public Message getMessage() { return message; }
    public String getNote() { return note; }
}

/* ===========================
   Notification Manager (Observer)
   =========================== */
class NotificationManager {
    // Map username -> list of observers (clients listening for notifications)
    private final Map<String, List<NotificationObserver>> observers;

    public NotificationManager() {
        this.observers = new HashMap<>();
    }

    public synchronized void registerObserver(String username, NotificationObserver observer) {
        observers.computeIfAbsent(username, k -> new ArrayList<>()).add(observer);
    }

    public synchronized void removeObserver(String username, NotificationObserver observer) {
        List<NotificationObserver> list = observers.get(username);
        if (list != null) {
            list.remove(observer);
            if (list.isEmpty()) observers.remove(username);
        }
    }

    // Notify all observers of a particular user
    public synchronized void notifyUser(String username, Notification notification) {
        List<NotificationObserver> list = observers.get(username);
        if (list != null) {
            for (NotificationObserver o : new ArrayList<>(list)) { // clone to avoid CME
                o.update(notification);
            }
        }
    }

    // Broadcast a user-online notification to all online users (observers map keys)
    public synchronized void broadcastUserOnline(String username) {
        Notification n = new Notification(NotificationType.USER_ONLINE, username, null, username + " is now online");
        for (String usr : observers.keySet()) {
            if (!usr.equals(username)) { // don't notify the user who came online
                notifyUser(usr, n);
            }
        }
    }
}

/* ===========================
   UserManager
   =========================== */
class UserManager {
    private final Map<String, User> users = new HashMap<>();
    private final MongoDBService mongo;

    public UserManager() { this.mongo = null; }
    public UserManager(MongoDBService mongo) { this.mongo = mongo; }

    public synchronized User register(String username, String password) throws RuntimeException {
        if (username == null || username.trim().isEmpty()) throw new RuntimeException("Invalid username");
        if (users.containsKey(username)) throw new RuntimeException("Username already exists");
        User u = new User(username, password);
        users.put(username, u);
        if (mongo != null && mongo.isConnected()) mongo.saveOrUpdateUser(u);
        return u;
    }

    public synchronized User login(String username, String password) throws RuntimeException {
        User u = users.get(username);
        if (u == null) throw new RuntimeException("No such user");
        if (!u.getPassword().equals(password)) throw new RuntimeException("Incorrect password");
        u.setStatus(UserStatus.ONLINE);
        if (mongo != null && mongo.isConnected()) mongo.saveOrUpdateUser(u);
        return u;
    }

    public synchronized void logout(String username) {
        User u = users.get(username);
        if (u != null) {
            u.setStatus(UserStatus.OFFLINE);
            if (mongo != null && mongo.isConnected()) mongo.saveOrUpdateUser(u);
        }
    }

    public synchronized boolean isOnline(String username) {
        User u = users.get(username);
        return u != null && u.getStatus() == UserStatus.ONLINE;
    }

    public synchronized User getUser(String username) {
        return users.get(username);
    }

    public synchronized Collection<User> listAllUsers() {
        return Collections.unmodifiableCollection(users.values());
    }

    public synchronized void loadFromDB() {
        if (mongo == null || !mongo.isConnected()) return;
        List<User> fromDb = mongo.loadAllUsers();
        for (User u : fromDb) {
            if (!users.containsKey(u.getUsername())) users.put(u.getUsername(), u);
        }
    }
}

/* ===========================
   MessageManager
   =========================== */
class MessageManager {
    private final UserManager userManager;
    private final NotificationManager notificationManager;
    private final MongoDBService mongo; // optional persistence

    public MessageManager(UserManager userManager, NotificationManager notificationManager, MongoDBService mongo) {
        this.userManager = userManager;
        this.notificationManager = notificationManager;
        this.mongo = mongo;
    }

    // Send message from sender -> receiver with proper status updates
    public synchronized Message sendMessage(String sender, String receiver, String content) {
        User s = userManager.getUser(sender);
        User r = userManager.getUser(receiver);
        if (s == null) throw new RuntimeException("Sender does not exist");
        if (r == null) throw new RuntimeException("Receiver does not exist");

        Message m = MessageFactory.createTextMessage(sender, receiver, content);
        m.setStatus(MessageStatus.SENT);

        // persist message if possible
        if (mongo != null && mongo.isConnected()) mongo.saveMessage(m);

        // Add to sender history
        s.addToHistory(m);

        if (userManager.isOnline(receiver)) {
            // deliver immediately
            m.markDelivered();
            if (mongo != null && mongo.isConnected()) mongo.updateMessageStatus(m.getId(), MessageStatus.DELIVERED);
            r.addToHistory(m); // recipient's history contains message now as delivered
            // notify recipient about new message
            notificationManager.notifyUser(receiver, new Notification(NotificationType.NEW_MESSAGE, sender, m, "New message"));
            // notify sender that message was delivered
            notificationManager.notifyUser(sender, new Notification(NotificationType.MESSAGE_STATUS_UPDATE, receiver, m, "Message delivered"));
        } else {
            // queue offline
            r.enqueueOfflineMessage(m);
            // notify sender that message was sent but not delivered
            notificationManager.notifyUser(sender, new Notification(NotificationType.MESSAGE_STATUS_UPDATE, receiver, m, "Message sent and queued (recipient offline)"));
        }
        return m;
    }

    // Deliver all pending offline messages to user (called when user logs in)
    public synchronized void deliverOfflineMessages(String username) {
        User u = userManager.getUser(username);
        if (u == null) return;
        // First, deliver in-memory queued messages
        Queue<Message> pending = u.drainOfflineMessages();
        while (!pending.isEmpty()) {
            Message m = pending.poll();
            m.markDelivered();
            if (mongo != null && mongo.isConnected()) mongo.updateMessageStatus(m.getId(), MessageStatus.DELIVERED);
            u.addToHistory(m);
            // notify recipient
            notificationManager.notifyUser(username, new Notification(NotificationType.NEW_MESSAGE, m.getSender(), m, "Delivered offline message"));
            // notify sender that message was delivered now
            notificationManager.notifyUser(m.getSender(), new Notification(NotificationType.MESSAGE_STATUS_UPDATE, username, m, "Message delivered (recipient came online)"));
        }
        // Then, deliver messages stored in DB (if connected)
        if (mongo != null && mongo.isConnected()) {
            List<Message> dbPending = mongo.getUndeliveredMessages(username);
            for (Message m : dbPending) {
                m.markDelivered();
                mongo.updateMessageStatus(m.getId(), MessageStatus.DELIVERED);
                u.addToHistory(m);
                notificationManager.notifyUser(username, new Notification(NotificationType.NEW_MESSAGE, m.getSender(), m, "Delivered offline message"));
                notificationManager.notifyUser(m.getSender(), new Notification(NotificationType.MESSAGE_STATUS_UPDATE, username, m, "Message delivered (recipient came online)"));
            }
        }
    }

    // When recipient views inbox, mark messages as READ and notify senders
    public synchronized void markMessagesRead(String username, List<Message> messagesRead) {
        for (Message m : messagesRead) {
            if (m.getReceiver().equals(username) && m.getStatus() != MessageStatus.READ) {
                m.markRead();
                if (mongo != null && mongo.isConnected()) mongo.updateMessageStatus(m.getId(), MessageStatus.READ);
                // notify sender
                notificationManager.notifyUser(m.getSender(), new Notification(NotificationType.MESSAGE_STATUS_UPDATE, username, m, "Message read"));
            }
        }
    }
}

/* ===========================
   MessagingServer (Singleton)
   =========================== */
/*
 * Singleton pattern: central server instance which holds UserManager, MessageManager, NotificationManager
 */
class MessagingServer {
    private static MessagingServer instance;
    private final UserManager userManager;
    private final NotificationManager notificationManager;
    private final MessageManager messageManager;
    private final MongoDBService mongoService;

    private static final String DEFAULT_MONGO_URI = "mongodb://127.0.0.1:27017";
    private static final String DEFAULT_DB = "whatsapp";

    private MessagingServer() {
        // Attempt to connect to MongoDB (safe to fail — app keeps running in memory-only mode)
        this.mongoService = new MongoDBService();
        this.mongoService.connect(DEFAULT_MONGO_URI, DEFAULT_DB);

        this.userManager = new UserManager(mongoService);
        this.notificationManager = new NotificationManager();
        this.messageManager = new MessageManager(userManager, notificationManager, mongoService);

        // Load any users from DB into memory
        this.userManager.loadFromDB();
    }

    public static synchronized MessagingServer getInstance() {
        if (instance == null) instance = new MessagingServer();
        return instance;
    }

    public UserManager getUserManager() { return userManager; }
    public NotificationManager getNotificationManager() { return notificationManager; }
    public MessageManager getMessageManager() { return messageManager; }

    /* High-level operations */
    public User registerUser(String username, String password) {
        return userManager.register(username, password);
    }

    public User loginUser(String username, String password, NotificationObserver observer) {
        User u = userManager.login(username, password);
        // Register observer to receive notifications for this user
        notificationManager.registerObserver(username, observer);
        // Broadcast to other online users that this user came online
        notificationManager.broadcastUserOnline(username);
        // Deliver offline messages
        messageManager.deliverOfflineMessages(username);
        return u;
    }

    public void logoutUser(String username, NotificationObserver observer) {
        notificationManager.removeObserver(username, observer);
        userManager.logout(username);
        // Optional: broadcast offline to others (not required)
    }

    public Message sendMessage(String sender, String receiver, String content) {
        return messageManager.sendMessage(sender, receiver, content);
    }

    public void markMessagesRead(String username, List<Message> messagesRead) {
        messageManager.markMessagesRead(username, messagesRead);
    }

    public Collection<User> listUsers() { return userManager.listAllUsers(); }

    public User getUser(String username) { return userManager.getUser(username); }

    // Return message history, prefer DB-backed history when available
    public List<Message> getMessageHistory(String username) {
        if (mongoService != null && mongoService.isConnected()) return mongoService.getMessageHistory(username);
        User u = userManager.getUser(username);
        return u == null ? Collections.emptyList() : new ArrayList<>(u.getMessageHistory());
    }

    public boolean reconnectMongo(String uri, String dbName) {
        if (mongoService != null) mongoService.close();
        boolean ok = mongoService.connect(uri, dbName);
        if (ok) userManager.loadFromDB();
        return ok;
    }

    public boolean isMongoConnected() {
        return mongoService != null && mongoService.isConnected();
    }
}

/* ===========================
   Controller & View (MVC)
   =========================== */
class AppController {
    private final ConsoleView view;
    private final MessagingServer server;
    // Track currently "active" user for console interaction
    private String activeUsername = null;

    // Keep an observer instance per logged-in user to handle notifications
    private final Map<String, NotificationObserver> observers = new HashMap<>();

    public AppController(ConsoleView view, MessagingServer server) {
        this.view = view;
        this.server = server;
    }

    public void start() {
        view.showWelcome();
        boolean running = true;
        while (running) {
            view.showMenu(activeUsername);
            int choice = view.promptInt("Enter choice");
            try {
                switch (choice) {
                    case 1: // Register
                        handleRegister();
                        break;
                    case 2: // Login
                        handleLogin();
                        break;
                    case 3: // Send Message
                        handleSendMessage();
                        break;
                    case 4: // View Inbox
                        handleViewInbox();
                        break;
                    case 5: // Logout
                        handleLogout();
                        break;
                    case 6: // List Users
                        handleListUsers();
                        break;
                    case 7: // Switch Active User
                        handleSwitchActiveUser();
                        break;
                    case 8: // Exit
                        running = false;
                        view.showInfo("Exiting. Goodbye!");
                        break;
                    case 9: // Configure DB
                        handleConfigureDB();
                        break;
                    default:
                        view.showError("Unknown option");
                }
            } catch (RuntimeException ex) {
                view.showError("Error: " + ex.getMessage());
            }
        }
    }

    private void handleRegister() {
        String username = view.promptString("Choose username");
        String password = view.promptString("Choose password");
        server.registerUser(username, password);
        view.showSuccess("Registered user: " + username);
    }

    private void handleLogin() {
        String username = view.promptString("Username");
        String password = view.promptString("Password");
        // Create a NotificationObserver that displays notifications to console for this user
        NotificationObserver observer = new NotificationObserver() {
            @Override
            public void update(Notification notification) {
                // Print notification distinctly for the target user
                String prefix = "[Notification to " + username + "] ";
                switch (notification.getType()) {
                    case NEW_MESSAGE:
                        Message m = notification.getMessage();
                        System.out.println(prefix + "New message from " + notification.getUsername() + ": \"" + m.getContent() + "\" [" + m.getTimestamp() + "]");
                        break;
                    case USER_ONLINE:
                        System.out.println(prefix + notification.getNote());
                        break;
                    case MESSAGE_STATUS_UPDATE:
                        Message msg = notification.getMessage();
                        System.out.println(prefix + "Status update related to message ID " + msg.getId() + " -> " + notification.getNote() + " (status now: " + msg.getStatus() + ")");
                        break;
                }
            }
        };

        User u = server.loginUser(username, password, observer);
        observers.put(username, observer);
        activeUsername = username;
        view.showSuccess("Logged in as: " + username);
    }

    private void handleSendMessage() {
        if (!ensureActiveUser()) return;
        String to = view.promptString("Send to (username)");
        String content = view.promptString("Message text");
        server.sendMessage(activeUsername, to, content);
        view.showSuccess("Message sent (may be queued if recipient offline).");
    }

    private void handleViewInbox() {
        if (!ensureActiveUser()) return;
        // Prefer DB-backed message history when available
        List<Message> history = server.getMessageHistory(activeUsername);
        if (history.isEmpty()) {
            view.showInfo("No messages.");
            return;
        }
        view.showInfo("Message History for " + activeUsername + ":");
        List<Message> unreadToMarkRead = new ArrayList<>();
        int idx = 1;
        for (Message m : history) {
            boolean incoming = m.getReceiver().equals(activeUsername);
            String dir = incoming ? "IN" : "OUT";
            System.out.println(idx + ". [" + dir + "] " + m);
            if (incoming && m.getStatus() != MessageStatus.READ) {
                unreadToMarkRead.add(m);
            }
            idx++;
        }
        // Mark unread incoming messages as READ and notify senders
        if (!unreadToMarkRead.isEmpty()) {
            server.markMessagesRead(activeUsername, unreadToMarkRead);
            view.showSuccess("Marked " + unreadToMarkRead.size() + " messages as READ.");
        }
    }

    private void handleLogout() {
        if (!ensureActiveUser()) return;
        NotificationObserver ob = observers.get(activeUsername);
        server.logoutUser(activeUsername, ob);
        observers.remove(activeUsername);
        view.showSuccess("Logged out: " + activeUsername);
        activeUsername = null;
    }

    private void handleListUsers() {
        Collection<User> users = server.listUsers();
        view.showInfo("All users:");
        for (User u : users) {
            System.out.println("- " + u);
        }
    }

    private void handleSwitchActiveUser() {
        Collection<User> users = server.listUsers();
        if (users.isEmpty()) {
            view.showInfo("No users registered yet.");
            return;
        }
        List<String> loggedIn = new ArrayList<>();
        for (User u : users) {
            if (u.getStatus() == UserStatus.ONLINE) loggedIn.add(u.getUsername());
        }
        if (loggedIn.isEmpty()) {
            view.showInfo("No users are logged in. Please log in first.");
            return;
        }
        view.showInfo("Logged in users: " + loggedIn);
        String choice = view.promptString("Enter username to switch active");
        if (loggedIn.contains(choice)) {
            activeUsername = choice;
            view.showSuccess("Active user set to " + activeUsername);
        } else {
            view.showError("User not logged in or doesn't exist");
        }
    }

    private boolean ensureActiveUser() {
        if (activeUsername == null) {
            view.showError("No active user. Please login first or switch active user.");
            return false;
        }
        return true;
    }

    // Configure MongoDB connection at runtime
    private void handleConfigureDB() {
        String uri = view.promptString("MongoDB URI (e.g. mongodb://127.0.0.1:27017)");
        String db = view.promptString("Database name (e.g. whatsapp)");
        boolean ok = MessagingServer.getInstance().reconnectMongo(uri, db);
        if (ok) view.showSuccess("Connected to MongoDB at " + uri + " (db: " + db + ")");
        else view.showError("Failed to connect to MongoDB. Check URI and try again.");
    }
}

class ConsoleView {
    private final Scanner scanner = new Scanner(System.in);

    public void showWelcome() {
        System.out.println("==== Welcome to Mini-WhatsApp (Console) ====");
        System.out.println("Pure Java, menu-driven app. Patterns: Singleton, Observer, Factory, MVC.");
        System.out.println();
    }

    public void showMenu(String activeUser) {
        System.out.println("\n--- Main Menu ---");
        System.out.println("Active user: " + (activeUser == null ? "None" : activeUser));
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("3. Send Message");
        System.out.println("4. View Inbox / Message History");
        System.out.println("5. Logout");
        System.out.println("6. List Users");
        System.out.println("7. Switch Active User (among logged-in users)");
        System.out.println("8. Exit");
        System.out.println("9. Configure DB Connection (MongoDB)");
    }

    public String promptString(String prompt) {
        System.out.print(prompt + ": ");
        return scanner.nextLine().trim();
    }

    public int promptInt(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            String s = scanner.nextLine().trim();
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ex) {
                System.out.println("Please enter a number.");
            }
        }
    }

    public void showError(String msg) {
        System.out.println("⚠️  ERROR: " + msg);
    }

    public void showSuccess(String msg) {
        System.out.println("✅ " + msg);
    }

    public void showInfo(String msg) {
        System.out.println("ℹ️  " + msg);
    }
}
