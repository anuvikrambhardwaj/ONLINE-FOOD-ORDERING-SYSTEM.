package src;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

public class Server {
    
    // In-memory OTP storage: Phone -> OTP
    static ConcurrentHashMap<String, String> otpStorage = new ConcurrentHashMap<>();
    
    public static void main(String[] args) throws Exception {
        DatabaseManager.initializeDatabase();
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/menu", new MenuHandler());
        
        server.createContext("/api/login", new LoginHandler());
        
        server.createContext("/api/order", new OrderHandler());
        server.createContext("/api/orders", new GetOrdersHandler());
        server.createContext("/api/cancel", new CancelOrderHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("V3 Mega Server started on port 8000...");
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }
            File file = new File("public" + path);
            if (!file.exists()) {
                String response = "404 File Not Found";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }
            exchange.sendResponseHeaders(200, file.length());
            try (OutputStream os = exchange.getResponseBody(); FileInputStream fs = new FileInputStream(file)) {
                final byte[] buffer = new byte[0x10000];
                int count;
                while ((count = fs.read(buffer)) >= 0) {
                    os.write(buffer, 0, count);
                }
            }
        }
    }

    static class MenuHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "[";
            try (Connection conn = DatabaseManager.connect();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM Menu")) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) response += ",";
                    response += String.format(
                        "{\"id\":%d, \"name\":\"%s\", \"description\":\"%s\", \"price\":%.2f, \"image_url\":\"%s\", \"category\":\"%s\"}",
                        rs.getInt("id"), rs.getString("name"),
                        rs.getString("description").replace("\"", "\\\""),
                        rs.getDouble("price"), rs.getString("image_url"),
                        rs.getString("category")
                    );
                    first = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            response += "]";
            sendResponse(exchange, 200, response);
        }
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) return;
            String body = new String(exchange.getRequestBody().readAllBytes(), "UTF-8");
            
            String name = "", phone = "";
            Matcher mName = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
            if (mName.find()) name = mName.group(1);
            Matcher mPhone = Pattern.compile("\"phone\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
            if (mPhone.find()) phone = mPhone.group(1);

            int userId = -1;
            String dbName = name;
            
            try (Connection conn = DatabaseManager.connect()) {
                PreparedStatement psCheck = conn.prepareStatement("SELECT * FROM Users WHERE phone = ?");
                psCheck.setString(1, phone);
                ResultSet rs = psCheck.executeQuery();
                if (rs.next()) {
                    userId = rs.getInt("id");
                    dbName = rs.getString("name"); 
                } else {
                    PreparedStatement psInsert = conn.prepareStatement("INSERT INTO Users (name, phone) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
                    psInsert.setString(1, name);
                    psInsert.setString(2, phone);
                    psInsert.executeUpdate();
                    ResultSet rsKey = psInsert.getGeneratedKeys();
                    if (rsKey.next()) userId = rsKey.getInt(1);
                }
            } catch(Exception e) {
                 sendResponse(exchange, 500, "{\"success\":false}"); return;
            }
            sendResponse(exchange, 200, "{\"success\":true, \"userId\":" + userId + ", \"name\":\"" + dbName + "\"}");
        }
    }

    static class OrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) return;
            String body = new String(exchange.getRequestBody().readAllBytes(), "UTF-8");
            
            int userId = 0;
            double total = 0.0;
            String address = "Not Provided";
            
            Matcher userMatcher = Pattern.compile("\"userId\"\\s*:\\s*(\\d+)").matcher(body);
            if (userMatcher.find()) userId = Integer.parseInt(userMatcher.group(1));

            Matcher totalMatcher = Pattern.compile("\"total\"\\s*:\\s*([0-9.]+)").matcher(body);
            if (totalMatcher.find()) total = Double.parseDouble(totalMatcher.group(1));
            
            Matcher addrMatcher = Pattern.compile("\"address\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
            if (addrMatcher.find()) address = addrMatcher.group(1);

            if (userId == 0) {
               sendResponse(exchange, 400, "{\"success\":false, \"message\":\"Not logged in\"}"); return;
            }

            // Assign fake delivery boy
            String[] agents = {"Ramesh Kumar", "Suresh Das", "Vinod Singh", "Agent 47"};
            String dboyName = agents[new Random().nextInt(agents.length)];
            String dboyPhone = "+91 9" + (100000000 + new Random().nextInt(900000000));

            int orderId = -1;
            try (Connection conn = DatabaseManager.connect()) {
                String insertOrder = "INSERT INTO Orders (user_id, total_amount, status, delivery_address, delivery_boy_name, delivery_boy_phone) VALUES (?, ?, 'PENDING', ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertOrder, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, userId);
                    pstmt.setDouble(2, total);
                    pstmt.setString(3, address);
                    pstmt.setString(4, dboyName);
                    pstmt.setString(5, dboyPhone);
                    pstmt.executeUpdate();
                    ResultSet rsKey = pstmt.getGeneratedKeys();
                    if (rsKey.next()) orderId = rsKey.getInt(1);
                }

                Matcher itemMatcher = Pattern.compile("\\{\\s*\"id\"\\s*:\\s*(\\d+)\\s*,\\s*\"quantity\"\\s*:\\s*(\\d+)\\s*}").matcher(body);
                String insertItem = "INSERT INTO OrderItems (order_id, menu_item_id, quantity) VALUES (?, ?, ?)";
                try (PreparedStatement pItem = conn.prepareStatement(insertItem)) {
                    while (itemMatcher.find()) {
                        int itemId = Integer.parseInt(itemMatcher.group(1));
                        int qty = Integer.parseInt(itemMatcher.group(2));
                        pItem.setInt(1, orderId);
                        pItem.setInt(2, itemId);
                        pItem.setInt(3, qty);
                        pItem.addBatch();
                    }
                    pItem.executeBatch();
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"success\":false, \"message\":\"DB Error\"}"); return;
            }
            sendResponse(exchange, 200, "{\"success\":true, \"orderId\":" + orderId + "}");
        }
    }

    static class GetOrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            int userId = 0;
            if (query != null && query.contains("userId=")) {
                userId = Integer.parseInt(query.split("userId=")[1].split("&")[0]);
            }
            
            String response = "[";
            boolean first = true;
            try (Connection conn = DatabaseManager.connect();
                 PreparedStatement stmt = conn.prepareStatement("SELECT id, total_amount, status, created_at, delivery_address, delivery_boy_name, delivery_boy_phone FROM Orders WHERE user_id = ? ORDER BY id DESC")) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    if (!first) response += ",";
                    
                    int orderId = rs.getInt("id");
                    
                    String itemsDesc = "";
                    PreparedStatement itemsStmt = conn.prepareStatement("SELECT m.name, oi.quantity FROM OrderItems oi JOIN Menu m ON oi.menu_item_id = m.id WHERE oi.order_id = ?");
                    itemsStmt.setInt(1, orderId);
                    ResultSet rsItems = itemsStmt.executeQuery();
                    boolean firstItem = true;
                    while(rsItems.next()) {
                        if (!firstItem) itemsDesc += ", ";
                        itemsDesc += rsItems.getInt("quantity") + "x " + rsItems.getString("name");
                        firstItem = false;
                    }

                    response += String.format(
                        "{\"id\":%d, \"total\":%.2f, \"status\":\"%s\", \"date\":\"%s\", \"items\":\"%s\", \"address\":\"%s\", \"boyName\":\"%s\", \"boyPhone\":\"%s\"}",
                        orderId, rs.getDouble("total_amount"),
                        rs.getString("status"), rs.getString("created_at"), itemsDesc,
                        rs.getString("delivery_address"), rs.getString("delivery_boy_name"), rs.getString("delivery_boy_phone")
                    );
                    first = false;
                }
            } catch (Exception e) { e.printStackTrace(); }
            response += "]";
            sendResponse(exchange, 200, response);
        }
    }

    static class CancelOrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) return;
            String body = new String(exchange.getRequestBody().readAllBytes(), "UTF-8");
            Matcher mOrder = Pattern.compile("\"orderId\"\\s*:\\s*(\\d+)").matcher(body);
            int orderId = 0;
            if (mOrder.find()) orderId = Integer.parseInt(mOrder.group(1));

            try (Connection conn = DatabaseManager.connect();
                 PreparedStatement stmt = conn.prepareStatement("UPDATE Orders SET status = 'CANCELLED' WHERE id = ?")) {
                stmt.setInt(1, orderId);
                stmt.executeUpdate();
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"success\":false}"); return;
            }
            sendResponse(exchange, 200, "{\"success\":true}");
        }
    }
}
