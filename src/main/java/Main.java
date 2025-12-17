import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Launch GUI by default; use --console argument to run CLI
        if (args.length > 0 && "--console".equals(args[0])) {
            ConsoleView view = new ConsoleView();
            boolean mongoOk = MessagingServer.getInstance().isMongoConnected();
            if (mongoOk) System.out.println("✅ MongoDB connected (default: mongodb://127.0.0.1:27017, db: whatsapp)");
            else System.out.println("⚠️  MongoDB not connected. Use option 9 to configure DB connection.");
            AppController controller = new AppController(view, MessagingServer.getInstance());
            controller.start();
        } else {
            SwingUtilities.invokeLater(() -> {
                GUIFrame frame = new GUIFrame(MessagingServer.getInstance());
                frame.setVisible(true);
            });
        }
    }
}
