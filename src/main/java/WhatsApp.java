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

    // Convenience for the UI to know whether MongoDB is actually connected
    public boolean isMongoConnected() {
        return mongoService != null && mongoService.isConnected();
    }

    // Allow runtime (re)configuration of MongoDB connection from the UI
    public boolean configureMongo(String uri, String dbName) {
        if (mongoService == null) return false;
        mongoService.close();
        boolean ok = mongoService.connect(uri, dbName);
        if (ok) userManager.loadFromDB();
        return ok;
    }
}
