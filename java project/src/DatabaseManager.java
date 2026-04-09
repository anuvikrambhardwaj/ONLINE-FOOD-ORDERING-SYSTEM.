package src;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:food_ordering.db";

    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL);
        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
        return conn;
    }

    public static void initializeDatabase() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS Users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT UNIQUE NOT NULL);");

            stmt.execute("CREATE TABLE IF NOT EXISTS Menu (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "name TEXT NOT NULL, " +
                         "description TEXT, " +
                         "price REAL NOT NULL, " +
                         "image_url TEXT, " +
                         "category TEXT" +
                         ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS Orders (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "user_id INTEGER, " +
                         "total_amount REAL NOT NULL, " +
                         "status TEXT DEFAULT 'PENDING', " +  
                         "delivery_address TEXT, " +
                         "delivery_boy_name TEXT, " +
                         "delivery_boy_phone TEXT, " +
                         "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                         "FOREIGN KEY(user_id) REFERENCES Users(id));");

            stmt.execute("CREATE TABLE IF NOT EXISTS OrderItems (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "order_id INTEGER, " +
                         "menu_item_id INTEGER, " +
                         "quantity INTEGER, " +
                         "FOREIGN KEY(order_id) REFERENCES Orders(id), " +
                         "FOREIGN KEY(menu_item_id) REFERENCES Menu(id));");

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Menu");
            rs.next();
            if (rs.getInt(1) == 0) {
                System.out.println("Seeding 100+ Mega Catalog data...");
                String insertMenu = "INSERT INTO Menu (name, description, price, image_url, category) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertMenu)) {
                    
                    // 20 Vegetables
                    String[] veg = {"Organic Tomato","Farm Potato","Fresh Onion","Green Spinach","Broccoli Head","Fresh Carrot","Cucumber","Capsicum Green","Red Bell Pepper","Yellow Bell Pepper","Green Chillies","Garlic Pods","Ginger Root","Fresh Coriander","Mint Leaves","Lady Finger","Cauliflower","Cabbage","Eggplant","Peas"};
                    for(int i=0; i<20; i++) {
                        pstmt.setString(1, veg[i]); pstmt.setString(2, "Freshly picked local " + veg[i]); pstmt.setDouble(3, 40.0 + (i*2)); pstmt.setString(4, "https://loremflickr.com/320/240/" + veg[i].substring(veg[i].lastIndexOf(" ") + 1).toLowerCase() + ",vegetable/all"); pstmt.setString(5, "Vegetables"); pstmt.addBatch();
                    }
                    
                    // 20 Fruits
                    String[] fruits = {"Red Apple","Fresh Banana","Alphonso Mango","Green Grapes","Black Grapes","Orange","Sweet Pomegranate","Watermelon","Papaya","Pineapple","Kiwi Box","Strawberry Pack","Blueberries","Dragon Fruit","Guava","Muskmelon","Sweet Lime","Pear","Plum","Peach"};
                    for(int i=0; i<20; i++) {
                        pstmt.setString(1, fruits[i]); pstmt.setString(2, "Premium quality juicy " + fruits[i]); pstmt.setDouble(3, 80.0 + (i*5)); pstmt.setString(4, "https://loremflickr.com/320/240/" + fruits[i].substring(fruits[i].lastIndexOf(" ") + 1).toLowerCase() + ",fruit/all"); pstmt.setString(5, "Fruits"); pstmt.addBatch();
                    }
                    
                    // 30 Non-veg
                    String[] nonveg = {"Chicken Tikka","Mutton Curry","Butter Chicken","Fish Fry","Chicken Biryani","Mutton Biryani","Prawns Curry","Chilli Chicken","Chicken 65","Tandoori Chicken","Egg Curry","Egg Biryani","Chicken Korma","Mutton Rogan Josh","Fish Tikka","Apollo Fish","Chicken Shawarma","Mutton Kebab","Chicken Kebab","Fish Curry","Crab Masala","Chicken Lollipop","Mutton Keema","Chicken Manchow","Egg Bhurji","Omelette","Chicken Wrap","Mutton Wrap","Fried Chicken","Grilled Chicken"};
                    for(int i=0; i<30; i++) {
                        pstmt.setString(1, nonveg[i]); pstmt.setString(2, "Authentic spicy and slow cooked " + nonveg[i]); pstmt.setDouble(3, 199.0 + (i*10)); pstmt.setString(4, "https://loremflickr.com/320/240/" + nonveg[i].substring(nonveg[i].lastIndexOf(" ") + 1).toLowerCase() + ",meat/all"); pstmt.setString(5, "Non-Veg"); pstmt.addBatch();
                    }

                    // 30 Under 99 Store
                    String[] store99 = {"Lays Chips","Kurkure","Maggi 2-Min","Oreo Biscuits","Good Day","Nutella Mini","Amul Butter","Bread Loaf","Eggs (6 pack)","Milk (1L)","Curd Pack","Paneer Block","Tomato Ketchup","Soy Sauce","Chilli Sauce","Salt Packet","Sugar Pack","Tea Powder","Coffee Jar","Juice Tetra","Cola Can","Sparkling Water","Energy Drink","Ice Cream Cup","Chocolate Bar","Peanuts Pack","Almonds (50g)","Cashews (50g)","Dates Pack","Honey Small"};
                    for(int i=0; i<30; i++) {
                        pstmt.setString(1, store99[i]); pstmt.setString(2, "Daily essentials: " + store99[i]); pstmt.setDouble(3, 10.0 + (i*2.5)); pstmt.setString(4, "https://loremflickr.com/320/240/" + store99[i].substring(store99[i].lastIndexOf(" ") + 1).toLowerCase() + ",snack/all"); pstmt.setString(5, "99 Store"); pstmt.addBatch();
                    }

                    pstmt.executeBatch();
                }
            }
        } catch (Exception e) {
            System.out.println("Error initializing DB: " + e.getMessage());
        }
    }
}
