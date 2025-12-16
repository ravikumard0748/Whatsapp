import java.util.*;

public class AppController {
    private final ConsoleView view;
    private final MessagingServer server;
    private String activeUser = null;
    private NotificationObserver observer = null;

    public AppController(ConsoleView view, MessagingServer server) {
        this.view = view;
        this.server = server;
    }

    public void start() {
        view.printHeader();
        while (true) {
            view.showMenu(activeUser);
            int choice = view.promptInt("Enter choice:");
            switch (choice) {
                case 1: register(); break;
                case 2: login(); break;
                case 3: sendMessage(); break;
                case 4: viewInbox(); break;
                case 5: logout(); break;
                case 6: listUsers(); break;
                case 7: switchActiveUser(); break;
                case 8: exit(); return;
                case 9: configureDB(); break;
                default: view.println("Invalid choice");
            }
        }
    }

    private void register() {
        String u = view.promptLine("Username:");
        String p = view.promptLine("Password:");
        try {
            server.registerUser(u, p);
            view.println("Registered: " + u);
        } catch (Exception e) { view.println("Register failed: " + e.getMessage()); }
    }

    private void login() {
        String u = view.promptLine("Username:");
        String p = view.promptLine("Password:");
        try {
            // create a simple observer that prints notifications
            observer = new NotificationObserver() {
                @Override
                public void update(Notification notification) {
                    System.out.println("\n[notification] " + notification.getNote());
                }
            };
            server.loginUser(u, p, observer);
            activeUser = u;
            view.println("Logged in as: " + u);
        } catch (Exception e) { view.println("Login failed: " + e.getMessage()); }
    }

    private void sendMessage() {
        if (activeUser == null) { view.println("No active user. Please login."); return; }
        String to = view.promptLine("Send to (username):");
        String content = view.promptLine("Message:");
        try {
            server.sendMessage(activeUser, to, content);
            view.println("Message sent.");
        } catch (Exception e) { view.println("Send failed: " + e.getMessage()); }
    }

    private void viewInbox() {
        if (activeUser == null) { view.println("No active user. Please login."); return; }
        List<Message> messages = server.getMessageHistory(activeUser);
        view.printMessages(messages);
        // mark unread messages as read
        List<Message> toMark = new ArrayList<>();
        for (Message m : messages) {
            if (m.getReceiver().equals(activeUser) && m.getStatus() != MessageStatus.READ) toMark.add(m);
        }
        if (!toMark.isEmpty()) server.markMessagesRead(activeUser, toMark);
    }

    private void logout() {
        if (activeUser == null) { view.println("No active user"); return; }
        server.logoutUser(activeUser, observer);
        view.println("Logged out: " + activeUser);
        activeUser = null;
        observer = null;
    }

    private void listUsers() {
        view.printUsers(server.listUsers());
    }

    private void switchActiveUser() {
        String u = view.promptLine("Switch to username:");
        if (server.getUser(u) != null) {
            activeUser = u;
            view.println("Active user: " + u);
        } else view.println("User not found: " + u);
    }

    private void configureDB() {
        String uri = view.promptLine("MongoDB URI (e.g. mongodb://127.0.0.1:27017):");
        String db = view.promptLine("Database name (e.g. whatsapp):");
        boolean ok = server.configureMongo(uri, db);
        view.println(ok ? "DB configured and loaded" : "DB connect failed");
    }

    private void exit() {
        view.println("Goodbye!");
        System.exit(0);
    }
}