import java.time.LocalDateTime;
import java.util.*;

/**
 * Lightweight stub of MongoDBService to allow compiling/running without MongoDB driver.
 * This provides the same methods used by the app but stores data in memory.
 * Replace with the real driver-backed implementation when you add the MongoDB jars.
 */
public class MongoDBService {
    private boolean connected = false;
    private final Map<String, User> users = new HashMap<>();
    private final Map<String, Message> messages = new LinkedHashMap<>();

    public boolean connect(String connectionString, String dbName) {
        // Very permissive: consider connectionString valid if non-empty
        if (connectionString == null || connectionString.trim().isEmpty()) {
            connected = false;
            return false;
        }
        connected = true;
        System.out.println("(stub) Connected to MongoDB: " + connectionString + " (db: " + dbName + ")");
        return true;
    }

    public boolean isConnected() { return connected; }

    public void close() { connected = false; }

    // USERS
    public void saveOrUpdateUser(User u) {
        if (!connected || u == null) return;
        // store a simple copy
        User copy = new User(u.getUsername(), u.getPassword());
        copy.setStatus(u.getStatus());
        users.put(copy.getUsername(), copy);
    }

    public List<User> loadAllUsers() {
        if (!connected) return Collections.emptyList();
        List<User> out = new ArrayList<>();
        for (User u : users.values()) {
            User copy = new User(u.getUsername(), u.getPassword());
            copy.setStatus(u.getStatus());
            out.add(copy);
        }
        return out;
    }

    // MESSAGES
    public void saveMessage(Message m) {
        if (!connected || m == null) return;
        messages.put(m.getId(), m);
    }

    public void updateMessageStatus(String messageId, MessageStatus newStatus) {
        if (!connected) return;
        Message m = messages.get(messageId);
        if (m != null) m.setStatus(newStatus);
    }

    public List<Message> getUndeliveredMessages(String receiver) {
        List<Message> out = new ArrayList<>();
        if (!connected) return out;
        for (Message m : messages.values()) {
            if (m.getReceiver().equals(receiver) && m.getStatus() == MessageStatus.SENT) {
                out.add(m);
            }
        }
        out.sort(Comparator.comparing(Message::getTimestamp));
        return out;
    }

    public List<Message> getMessageHistory(String username) {
        List<Message> out = new ArrayList<>();
        if (!connected) return out;
        for (Message m : messages.values()) {
            if (m.getSender().equals(username) || m.getReceiver().equals(username)) out.add(m);
        }
        out.sort(Comparator.comparing(Message::getTimestamp));
        return out;
    }
}