package com.example.demo.dao.impl;

import com.example.demo.dao.UserDao;
import com.example.demo.connection.ConnectionPool;
import com.example.demo.dao.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class UserDaoImpl implements UserDao {

    private static final Logger logger = LogManager.getLogger(UserDaoImpl.class);

    private static final String INSERT_USER_QUERY =
            "INSERT INTO users (username, password, email, phone, role, created_at) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SELECT_USER_BY_USERNAME_QUERY =
            "SELECT * FROM users WHERE username = ?";
    private static final String SELECT_PASSWORD_BY_USERNAME_QUERY =
            "SELECT password FROM users WHERE username = ?";

    private final ConnectionPool connectionPool = ConnectionPool.getInstance();

    @Override
    public boolean registerUser(String username, String password, String email, String phone, String role) {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        LocalDateTime createdAt = LocalDateTime.now();

        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_USER_QUERY)) {
            statement.setString(1, username);
            statement.setString(2, hashedPassword);
            statement.setString(3, email);
            statement.setString(4, phone);
            statement.setString(5, role);
            statement.setObject(6, createdAt);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error while registering user with username: {}", username, e);
            return false;
        }
    }

    @Override
    public User getUserByUsername(String username) {
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_USER_BY_USERNAME_QUERY)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return extractUserFromResultSet(resultSet);
            }
        } catch (SQLException e) {
            logger.error("Error while fetching user with username: {}", username, e);
        }
        return null;
    }

    @Override
    public String getHashedPassword(String username) {
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PASSWORD_BY_USERNAME_QUERY)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("password");
            }
        } catch (SQLException e) {
            logger.error("Error while fetching hashed password for username: {}", username, e);
        }
        return null;
    }

    private User extractUserFromResultSet(ResultSet resultSet) throws SQLException {
        int userId = resultSet.getInt("user_id");
        String username = resultSet.getString("username");
        String password = resultSet.getString("password");
        String email = resultSet.getString("email");
        String phone = resultSet.getString("phone");
        String role = resultSet.getString("role");
        LocalDateTime createdAt = resultSet.getObject("created_at", LocalDateTime.class);
        return new User(userId, username, password, email, phone, role, createdAt);
    }

}