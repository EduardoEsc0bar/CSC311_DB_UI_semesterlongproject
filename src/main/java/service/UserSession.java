package service;

import java.util.concurrent.locks.ReentrantLock;
import java.util.prefs.Preferences;

/**
 * Thread-safe implementation of UserSession using the Singleton pattern.
 * This class manages user credentials and privileges, and provides
 * thread-safe access to the user session.
 *
 * @author eduardoescobar
 */
public class UserSession {
    private static UserSession instance;
    private static final ReentrantLock lock = new ReentrantLock();

    private String userName;
    private String password;
    private String privileges;

    /**
     * Private constructor to prevent direct instantiation.
     * @param userName The username
     * @param password The password
     * @param privileges The user privileges (USER, ADMIN, etc.)
     */
    private UserSession(String userName, String password, String privileges) {
        this.userName = userName;
        this.password = password;
        this.privileges = privileges;
    }

    /**
     * Gets the UserSession instance with the specified credentials and privileges.
     * If an instance already exists, it will be updated with the new credentials.
     * @param userName username
     * @param password password
     * @param privileges user privileges
     * @return The UserSession instance
     */
    public static UserSession getInstance(String userName, String password, String privileges) {
        lock.lock();
        try {
            if (instance == null) {
                instance = new UserSession(userName, password, privileges);
                MyLogger.makeLog("Created new UserSession for: " + userName);
            } else {
                // Update existing instance
                instance.userName = userName;
                instance.password = password;
                instance.privileges = privileges;
                MyLogger.makeLog("Updated UserSession for: " + userName);
            }
            return instance;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets the UserSession instance with the specified credentials and default privileges.
     * @param userName The username
     * @param password The password
     * @return The UserSession instance
     */
    public static UserSession getInstance(String userName, String password) {
        return getInstance(userName, password, "USER");
    }

    /**
     * Loads the UserSession from the saved preferences.
     * @return The UserSession instance, or null if no session was found
     */
    public static UserSession loadFromPreferences() {
        lock.lock();
        try {
            Preferences userPreferences = Preferences.userRoot();
            String savedUsername = userPreferences.get("USERNAME", "");
            String savedPassword = userPreferences.get("PASSWORD", "");
            String savedPrivileges = userPreferences.get("PRIVILEGES", "USER");

            if (!savedUsername.isEmpty() && !savedPassword.isEmpty()) {
                if (instance == null) {
                    instance = new UserSession(savedUsername, savedPassword, savedPrivileges);
                    MyLogger.makeLog("Loaded UserSession from preferences for: " + savedUsername);
                } else {
                    // Update existing instance
                    instance.userName = savedUsername;
                    instance.password = savedPassword;
                    instance.privileges = savedPrivileges;
                    MyLogger.makeLog("Updated UserSession from preferences for: " + savedUsername);
                }
            }
            return instance;
        } catch (Exception e) {
            MyLogger.makeLog("Error loading UserSession from preferences: " + e.getMessage());
            return null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets the username.
     * @return The username
     */
    public String getUserName() {
        lock.lock();
        try {
            return this.userName;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets the password.
     * @return The password
     */
    public String getPassword() {
        lock.lock();
        try {
            return this.password;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets the user privileges.
     * @return The user privileges
     */
    public String getPrivileges() {
        lock.lock();
        try {
            return this.privileges;
        } finally {
            lock.unlock();
        }
    }

    /**
     * "Cleans" the user session by removing all user data.
     */
    public void cleanUserSession() {
        lock.lock();
        try {
            this.userName = "";
            this.password = "";
            this.privileges = "";

            // Clear preferences
            Preferences userPreferences = Preferences.userRoot();
            userPreferences.remove("USERNAME");
            userPreferences.remove("PASSWORD");
            userPreferences.remove("PRIVILEGES");

            MyLogger.makeLog("Cleaned UserSession");

            // Reset the singleton instance
            instance = null;
        } catch (Exception e) {
            MyLogger.makeLog("Error cleaning UserSession: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns a string representation of the UserSession.
     * @return A string representation of the UserSession
     */
    @Override
    public String toString() {
        lock.lock();
        try {
            return "UserSession{" +
                    "userName='" + this.userName + '\'' +
                    ", privileges=" + this.privileges +
                    '}';
        } finally {
            lock.unlock();
        }
    }
}