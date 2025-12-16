import java.util.*;

public class ConsoleView {
    private final Scanner scanner;

    public ConsoleView() {
        this.scanner = new Scanner(System.in);
    }

    public void printHeader() {
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

    public int promptInt(String prompt) {
        System.out.print(prompt + " ");
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (Exception e) {
            return -1;
        }
    }

    public String promptLine(String prompt) {
        System.out.print(prompt + " ");
        return scanner.nextLine();
    }

    public void println(String s) {
        System.out.println(s);
    }

    public void printUsers(Collection<User> users) {
        System.out.println("\nUsers:");
        for (User u : users) System.out.println(" - " + u.getUsername() + " (" + u.getStatus() + ")");
    }

    public void printMessages(List<Message> messages) {
        System.out.println("\nMessages:");
        for (Message m : messages) System.out.println(m.toString());
    }
}