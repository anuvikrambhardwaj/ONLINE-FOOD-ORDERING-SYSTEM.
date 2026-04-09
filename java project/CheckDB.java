import java.sql.*;

public class CheckDB {
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:food_ordering.db");
        Statement stmt = conn.createStatement();
        
        System.out.println("=========================================");
        System.out.println("       REGISTERED USERS (Login Data)");
        System.out.println("=========================================");
        System.out.println("ID  |  Name           |  Phone/Gmail");
        System.out.println("----|-----------------|------------------");
        ResultSet users = stmt.executeQuery("SELECT * FROM Users");
        while(users.next()) {
            System.out.printf("%-4d|  %-15s|  %s%n", users.getInt("id"), users.getString("name"), users.getString("phone"));
        }
        
        System.out.println();
        System.out.println("=========================================");
        System.out.println("       ALL ORDERS HISTORY");
        System.out.println("=========================================");
        System.out.println("Order# | User | Amount   | Status    | Date");
        System.out.println("-------|------|----------|-----------|--------------------");
        ResultSet orders = stmt.executeQuery(
            "SELECT o.id, u.name, o.total_amount, o.status, o.created_at, o.delivery_address, o.delivery_boy_name " +
            "FROM Orders o JOIN Users u ON o.user_id = u.id ORDER BY o.id DESC"
        );
        while(orders.next()) {
            System.out.printf("%-7d| %-5s| Rs%-7.2f| %-10s| %s%n",
                orders.getInt("id"), orders.getString("name"),
                orders.getDouble("total_amount"), orders.getString("status"),
                orders.getString("created_at"));
            System.out.println("       Address: " + orders.getString("delivery_address"));
            System.out.println("       Agent: " + orders.getString("delivery_boy_name"));
            System.out.println("       ---");
        }
        
        System.out.println();
        System.out.println("=========================================");
        System.out.println("  Total Users: ");
        ResultSet countU = stmt.executeQuery("SELECT COUNT(*) FROM Users");
        if(countU.next()) System.out.println("  " + countU.getInt(1) + " users registered");
        ResultSet countO = stmt.executeQuery("SELECT COUNT(*) FROM Orders");
        if(countO.next()) System.out.println("  " + countO.getInt(1) + " total orders");
        ResultSet countC = stmt.executeQuery("SELECT COUNT(*) FROM Orders WHERE status='CANCELLED'");
        if(countC.next()) System.out.println("  " + countC.getInt(1) + " cancelled orders");
        System.out.println("=========================================");
        
        conn.close();
    }
}
