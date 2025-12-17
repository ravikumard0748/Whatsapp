import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import spark.Filter;
import spark.Request;
import spark.Response;

import java.util.*;

import static spark.Spark.*;

public class RESTServer {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void start(MessagingServer server, int port) {
        port(port);
        enableCORS("*", "GET,POST,OPTIONS", "Content-Type,Authorization");

        get("/api/health", (req, res) -> {
            res.type("application/json");
            return gson.toJson(Map.of("ok", true));
        });

        post("/api/register", (req, res) -> {
            res.type("application/json");
            Map body = gson.fromJson(req.body(), Map.class);
            String username = (String) body.get("username");
            String password = (String) body.get("password");
            try {
                server.registerUser(username, password);
                return gson.toJson(Map.of("ok", true, "username", username));
            } catch (Exception e) {
                res.status(400);
                return gson.toJson(Map.of("ok", false, "error", e.getMessage()));
            }
        });

        post("/api/login", (req, res) -> {
            res.type("application/json");
            Map body = gson.fromJson(req.body(), Map.class);
            String username = (String) body.get("username");
            String password = (String) body.get("password");
            try {
                // Use a no-op observer — notifications are printed to server console
                server.loginUser(username, password, notification -> System.out.println("[notify] " + notification.getNote()));
                return gson.toJson(Map.of("ok", true, "username", username));
            } catch (Exception e) {
                res.status(400);
                return gson.toJson(Map.of("ok", false, "error", e.getMessage()));
            }
        });

        post("/api/logout", (req, res) -> {
            res.type("application/json");
            Map body = gson.fromJson(req.body(), Map.class);
            String username = (String) body.get("username");
            try {
                server.logoutUser(username, null);
                return gson.toJson(Map.of("ok", true));
            } catch (Exception e) {
                res.status(400);
                return gson.toJson(Map.of("ok", false, "error", e.getMessage()));
            }
        });

        get("/api/users", (req, res) -> {
            res.type("application/json");
            List<Map<String, Object>> out = new ArrayList<>();
            for (User u : server.listUsers()) {
                out.add(Map.of("username", u.getUsername(), "status", u.getStatus().name()));
            }
            return gson.toJson(out);
        });

        post("/api/message", (req, res) -> {
            res.type("application/json");
            Map body = gson.fromJson(req.body(), Map.class);
            String sender = (String) body.get("sender");
            String receiver = (String) body.get("receiver");
            String content = (String) body.get("content");
            try {
                Message m = server.sendMessage(sender, receiver, content);
                return gson.toJson(Map.of("ok", true, "messageId", m.getId()));
            } catch (Exception e) {
                res.status(400);
                return gson.toJson(Map.of("ok", false, "error", e.getMessage()));
            }
        });

        get("/api/messages/:username", (req, res) -> {
            res.type("application/json");
            String username = req.params(":username");
            List<Message> messages = server.getMessageHistory(username);
            List<Map<String, Object>> out = new ArrayList<>();
            for (Message m : messages) {
                out.add(Map.of(
                        "id", m.getId(),
                        "sender", m.getSender(),
                        "receiver", m.getReceiver(),
                        "content", m.getContent(),
                        "timestamp", m.getTimestamp().toString(),
                        "status", m.getStatus().name()
                ));
            }
            return gson.toJson(out);
        });

        post("/api/configure-db", (req, res) -> {
            res.type("application/json");
            Map body = gson.fromJson(req.body(), Map.class);
            String uri = (String) body.get("uri");
            String dbName = (String) body.get("db");
            boolean ok = server.configureMongo(uri, dbName);
            return gson.toJson(Map.of("ok", ok));
        });

        exception(Exception.class, (e, req, res) -> {
            res.type("application/json");
            res.status(500);
            res.body(gson.toJson(Map.of("ok", false, "error", e.getMessage())));
        });

        // simple CORS handling
        before((request, response) -> response.type("application/json"));
    }

    private static void enableCORS(final String origin, final String methods, final String headers) {
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", origin);
            response.header("Access-Control-Allow-Methods", methods);
            response.header("Access-Control-Allow-Headers", headers);
            // Note: allow credentials if needed
        });
    }
}