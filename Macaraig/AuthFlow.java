public final class AuthFlow {

    private AuthFlow() {
    }

    public static User login(boolean staffPortal) {
        AirlineSystem system = AirlineSystem.getInstance();
        ConsoleStyle.header(staffPortal ? "Staff Portal" : "Login");
        if (staffPortal) {
            System.out.println("For airline employees only. Accounts are issued by an Admin.");
        }
        ConsoleStyle.cancelHint();
        while (true) {
            String username = InputHelper.readLine("Username: ");
            String password = InputHelper.readLine("Password: ");
            try {
                User user = system.authenticate(username, password, staffPortal);
                System.out.println();
                System.out.println("Welcome, " + user.getFullName() + " (" + user.getRole() + ")");
                return user;
            } catch (InvalidLoginException e) {
                System.out.println(ConsoleStyle.error(e.getMessage()));
                System.out.println();
            }
        }
    }

    public static void register() {
        AirlineSystem system = AirlineSystem.getInstance();
        ConsoleStyle.header("Create Account");
        ConsoleStyle.cancelHint();

        String username = readNewUsername();
        String password = readNewPassword();
        String fullName = InputHelper.read("Full name: ", User::validateFullName);

        ConsoleStyle.section("Review Account");
        System.out.println(ConsoleStyle.row("Username", username));
        System.out.println(ConsoleStyle.row("Full name", fullName));
        System.out.println(ConsoleStyle.row("Account type", "Passenger"));
        if (!InputHelper.confirm("Create this account? (Y/N): ")) {
            System.out.println("Account not created.");
            return;
        }
        system.addUser(new Passenger(username, password, fullName));
        system.save();
        System.out.println("Account created. You can now log in.");
    }

    public static void changePassword(User user) {
        ConsoleStyle.header("Change Password");
        ConsoleStyle.cancelHint();
        while (true) {
            String current = InputHelper.readLine("Current password: ");
            String newPassword = readNewPassword("New password");
            try {
                user.changePassword(current, newPassword);
                AirlineSystem.getInstance().save();
                System.out.println("Password changed.");
                return;
            } catch (InvalidInputException e) {
                System.out.println(ConsoleStyle.error(e.getMessage()));
                System.out.println();
            }
        }
    }

    public static String readNewUsername() {
        AirlineSystem system = AirlineSystem.getInstance();
        return InputHelper.read("Username (5-15 letters, numbers, _): ", raw -> {
            String username = User.validateUsername(raw);
            if (system.usernameTaken(username)) {
                throw new InvalidInputException("That username is taken.");
            }
            return username;
        });
    }

    public static String readNewPassword() {
        return readNewPassword("Password");
    }

    public static String readNewPassword(String label) {
        while (true) {
            String password = InputHelper.read(label + " (8-20, a letter and a number): ", User::validatePassword);
            String again = InputHelper.readLine("Confirm " + label.toLowerCase() + ": ");
            if (password.equals(again)) {
                return password;
            }
            System.out.println(ConsoleStyle.error("Passwords don't match. Try again."));
            System.out.println();
        }
    }
}
