import java.sql.*;

public class DataBaseManager {
         // database url
        String url = "jdbc:sqlite:chatapp.db";
    DataBaseManager() {
        // SQL statement to create the users table
        String createTableSQL = "CREATE TABLE IF NOT EXISTS users (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "username TEXT NOT NULL UNIQUE" +
                                ");";

        // tries to connect to the database and create the users table if it doesn't exist
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            // create table
            stmt.execute(createTableSQL);
            System.out.println("Database connected and created users table");

        } catch (Exception e) {
            System.out.println("Error connecting to database: " + e.getMessage());
        }
    }

    public String getUsername(){
        String queryUser = "SELECT username FROM users LIMIT 1";
        String username = null;
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
             ResultSet rs = stmt.executeQuery(queryUser);
             if(rs.next()){
                username = rs.getString("username");
             }

                
            // get user 
            System.out.println("Username retrieved: " + username);
            

        } catch (Exception e) {
            System.out.println("Error getting username: " + e.getMessage());
        }

        return(username);
           
    }

    public void setUsername(String username){
        String insertUser = "INSERT INTO users (username) VALUES (?)";
        try (Connection conn = DriverManager.getConnection(url);    
             PreparedStatement stmt = conn.prepareStatement(insertUser)) {
            // insert user \
            stmt.setString(1, username);
            stmt.executeUpdate();
            System.out.println("Username set: " + username);
            

        } catch (Exception e) {
            System.out.println("Error setting username: " + e.getMessage());
        }

    }

}