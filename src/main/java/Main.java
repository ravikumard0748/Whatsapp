public class Main {
    public static void main(String[] args) {
        ConsoleView view = new ConsoleView();
        // Show DB connection status
        boolean mongoOk = MessagingServer.getInstance().isMongoConnected();
        if (mongoOk) System.out.println("✅ MongoDB connected (default: mongodb://127.0.0.1:27017, db: whatsapp)");
        else System.out.println("⚠️  MongoDB not connected. Use option 9 to configure DB connection.");
        AppController controller = new AppController(view, MessagingServer.getInstance());
        controller.start();
    }
}
