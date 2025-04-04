package dataaccess;

import java.sql.*;
import java.util.Properties;

public class DatabaseManager {
    // static final cannot be changed once set, and are shared across whole class.
    private static final String DATABASE_NAME;
    private static final String USER;
    private static final String PASSWORD;
    private static final String CONNECTION_URL;

    /*
     * Big Picture: preparing program to talk to database by loading config details. kinda like setting up a GPS before a road trip.
     */

    // this static block runs ONCE when the DatabaseManager class is loaded into memory.
    static {
        try {
            // pulling from file "db.properties" and setting it as propStream, uses try catch w resources to auto-close.
            try (var propStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("db.properties")) {
                if (propStream == null) {
                    throw new Exception("Unable to load db.properties");
                }
                // built in properties class.
                Properties props = new Properties();
                props.load(propStream);
                DATABASE_NAME = props.getProperty("db.name");
                USER = props.getProperty("db.user");
                PASSWORD = props.getProperty("db.password");

                // builds the connection url.
                var host = props.getProperty("db.host"); // string
                var port = Integer.parseInt(props.getProperty("db.port")); // int
                CONNECTION_URL = String.format("jdbc:mysql://%s:%d", host, port);
            }
        } catch (Exception ex) {
            throw new RuntimeException("unable to process db.properties. " + ex.getMessage());
        }
    }

    /**
     * Creates the database if it does not already exist.
     */
    public static void createDatabase() throws DataAccessException {
        try {
            var statement = "CREATE DATABASE IF NOT EXISTS " + DATABASE_NAME;
            var conn = DriverManager.getConnection(CONNECTION_URL, USER, PASSWORD);
            try (var preparedStatement = conn.prepareStatement(statement)) {
                preparedStatement.executeUpdate();
            }

            // selects the database before adding tables.
            conn.setCatalog(DATABASE_NAME);

            // creates the user table
            var createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                    "username VARCHAR(255) PRIMARY KEY, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "EMAIL VARCHAR(255) NOT NULL)";

            try (var preparedStatement2 = conn.prepareStatement(createUsersTable)) {
                preparedStatement2.executeUpdate();
            }

            // creates the game table.
            var createGamesTable = "CREATE TABLE IF NOT EXISTS games (" +
                    "game_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "game_name VARCHAR(255) NOT NULL, " +
                    "white_username VARCHAR(255), " +
                    "black_username VARCHAR(255), " +
                    "game_data TEXT)";

            try (var preparedStatement3 = conn.prepareStatement(createGamesTable)) {
                preparedStatement3.executeUpdate();
            }

            // creates the auth table
            var createAuthTable = "CREATE TABLE IF NOT EXISTS auth_tokens (" +
                    "auth_token VARCHAR(255) PRIMARY KEY, " +
                    "username VARCHAR(255) NOT NULL)";

            try (var preparedStatement4 = conn.prepareStatement(createAuthTable)) {
                preparedStatement4.executeUpdate();
            }

        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage());
        }
    }

    /**
     * Create a connection to the database and sets the catalog based upon the
     * properties specified in db.properties. Connections to the database should
     * be short-lived, and you must close the connection when you are done with it.
     * The easiest way to do that is with a try-with-resource block.
     * <br/>
     * <code>
     * try (var conn = DbInfo.getConnection(databaseName)) {
     * // execute SQL statements.
     * }
     * </code>
     */
    static Connection getConnection() throws DataAccessException {
        try {
            var conn = DriverManager.getConnection(CONNECTION_URL, USER, PASSWORD);
            conn.setCatalog(DATABASE_NAME);
            return conn;
        } catch (SQLException e) {
            throw new DataAccessException(e.getMessage());
        }
    }
}
