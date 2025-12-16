import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.*;

/**
 * MongoDB-backed implementation of MongoDBService using mongodb-driver-sync.
 * Stores users in collection `users` and messages in `messages` in the configured database.
 */
public class MongoDBService {
    private MongoClient client;
    private MongoDatabase db;
    private MongoCollection<Document> usersColl;
    private MongoCollection<Document> messagesColl;
    private boolean connected = false;

    public boolean connect(String connectionString, String dbName) {
        try {
            if (connectionString == null || connectionString.trim().isEmpty()) {
                connected = false;
                return false;
            }
            client = MongoClients.create(connectionString);
            db = client.getDatabase(dbName);
            usersColl = db.getCollection("users");
            messagesColl = db.getCollection("messages");

            // create useful indexes: username unique, messages by receiver+status, messages by timestamp
            usersColl.createIndex(new Document("username", 1), new IndexOptions().unique(true));
            messagesColl.createIndex(new Document("receiver", 1).append("status", 1));
            messagesColl.createIndex(new Document("timestamp", 1));

            connected = true;
            System.out.println("Connected to MongoDB: " + connectionString + " (db: " + dbName + ")");
            return true;
        } catch (Exception e) {
            System.err.println("Failed to connect to MongoDB: " + e.getMessage());
            connected = false;
            return false;
        }
    }

    public boolean isConnected() {
        return connected && client != null;
    }

    public void close() {
        if (client != null) {
            client.close();
            client = null;
            db = null;
            usersColl = null;
            messagesColl = null;
        }
        connected = false;
    }

    // USERS
    public void saveOrUpdateUser(User u) {
        if (!isConnected() || u == null) return;
        Document doc = new Document("username", u.getUsername())
                .append("password", u.getPassword())
                .append("status", u.getStatus().name());
        usersColl.updateOne(Filters.eq("username", u.getUsername()), new Document("$set", doc), new UpdateOptions().upsert(true));
    }

    public List<User> loadAllUsers() {
        List<User> out = new ArrayList<>();
        if (!isConnected()) return out;
        for (Document d : usersColl.find()) {
            String username = d.getString("username");
            String password = d.getString("password");
            String status = d.getString("status");
            User u = new User(username, password);
            if (status != null) u.setStatus(UserStatus.valueOf(status));
            out.add(u);
        }
        return out;
    }

    // MESSAGES
    public void saveMessage(Message m) {
        if (!isConnected() || m == null) return;
        Document doc = new Document("id", m.getId())
                .append("sender", m.getSender())
                .append("receiver", m.getReceiver())
                .append("content", m.getContent())
                .append("timestamp", m.getTimestamp().toString())
                .append("status", m.getStatus().name());
        messagesColl.insertOne(doc);
    }

    public void updateMessageStatus(String messageId, MessageStatus newStatus) {
        if (!isConnected() || messageId == null) return;
        messagesColl.updateOne(Filters.eq("id", messageId), Updates.set("status", newStatus.name()));
    }

    public List<Message> getUndeliveredMessages(String receiver) {
        List<Message> out = new ArrayList<>();
        if (!isConnected() || receiver == null) return out;
        for (Document d : messagesColl.find(Filters.and(Filters.eq("receiver", receiver), Filters.eq("status", MessageStatus.SENT.name())))) {
            Message m = docToMessage(d);
            out.add(m);
        }
        out.sort(Comparator.comparing(Message::getTimestamp));
        return out;
    }

    public List<Message> getMessageHistory(String username) {
        List<Message> out = new ArrayList<>();
        if (!isConnected() || username == null) return out;
        for (Document d : messagesColl.find(Filters.or(Filters.eq("sender", username), Filters.eq("receiver", username)))) {
            out.add(docToMessage(d));
        }
        out.sort(Comparator.comparing(Message::getTimestamp));
        return out;
    }

    private Message docToMessage(Document d) {
        String id = d.getString("id");
        String sender = d.getString("sender");
        String receiver = d.getString("receiver");
        String content = d.getString("content");
        String ts = d.getString("timestamp");
        LocalDateTime timestamp = ts == null ? LocalDateTime.now() : LocalDateTime.parse(ts);
        MessageStatus status = MessageStatus.valueOf(d.getString("status"));
        Message m = new Message(id, sender, receiver, content, timestamp);
        m.setStatus(status);
        return m;
    }
}
