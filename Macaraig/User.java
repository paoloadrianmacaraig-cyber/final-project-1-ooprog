package Macaraig;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

public abstract class User implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int MAX_FAILED_LOGINS = 5;

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*$");
    private static final Pattern NAME = Pattern.compile("^\\p{L}[\\p{L} .'-]*$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String username;
    private String fullName;
    private String salt;
    private String passwordHash;
    private boolean active = true;
    private int failedLogins;
    private boolean locked;

    protected User(String username, String password, String fullName) {
        this.username = username;
        this.fullName = fullName;
        setPassword(password);
    }

    public abstract String getRole();

    public abstract void showMenu();

    public abstract boolean isEmployee();

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean checkPassword(String password) {
        return passwordHash.equals(hash(salt, password));
    }

    public void changePassword(String currentPassword, String newPassword) throws InvalidInputException {
        if (!checkPassword(currentPassword)) {
            throw new InvalidInputException("Current password is incorrect.");
        }
        if (checkPassword(newPassword)) {
            throw new InvalidInputException("The new password must be different from the current one.");
        }
        setPassword(newPassword);
    }

    public final void setPassword(String password) {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        salt = Base64.getEncoder().encodeToString(bytes);
        passwordHash = hash(salt, password);
    }

    public boolean recordFailedLogin() {
        failedLogins++;
        if (failedLogins >= MAX_FAILED_LOGINS) {
            locked = true;
            return true;
        }
        return false;
    }

    public void resetFailedLogins() {
        failedLogins = 0;
    }

    public void unlock() {
        locked = false;
        failedLogins = 0;
    }

    public String getStatusLabel() {
        if (!active) {
            return "Deactivated";
        }
        return locked ? "Locked" : "Active";
    }

    private static String hash(String salt, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] result = digest.digest((salt + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : result) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is missing from this Java installation.", e);
        }
    }

    public static String validateUsername(String raw) throws InvalidInputException {
        if (raw.length() < 5 || raw.length() > 15) {
            throw new InvalidInputException("Username must be 5-15 characters.");
        }
        if (!USERNAME.matcher(raw).matches()) {
            throw new InvalidInputException("Username can only have letters, numbers, and underscores,"
                    + " and must start with a letter.");
        }
        return raw;
    }

    public static String validatePassword(String raw) throws InvalidInputException {
        if (raw.length() < 8 || raw.length() > 20) {
            throw new InvalidInputException("Password must be 8-20 characters.");
        }
        if (raw.contains(" ")) {
            throw new InvalidInputException("Password can't contain spaces.");
        }
        if (!raw.matches(".*[A-Za-z].*")) {
            throw new InvalidInputException("Password needs at least one letter.");
        }
        if (!raw.matches(".*\\d.*")) {
            throw new InvalidInputException("Password needs at least one number.");
        }
        return raw;
    }

    public static String validateFullName(String raw) throws InvalidInputException {
        String name = raw.replaceAll("\\s+", " ").trim();
        if (name.isEmpty()) {
            throw new InvalidInputException("Name can't be empty.");
        }
        if (!NAME.matcher(name).matches()) {
            throw new InvalidInputException("Name can only have letters, spaces, periods, hyphens, and apostrophes.");
        }
        if (name.length() < 2 || name.length() > 50) {
            throw new InvalidInputException("Name must be 2-50 characters.");
        }
        return name;
    }
}
