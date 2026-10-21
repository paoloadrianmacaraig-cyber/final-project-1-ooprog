public final class UserManagementFlow {

    private UserManagementFlow() {
    }

    public static void run(Admin admin) {
        ConsoleStyle.enter("Manage Users");
        try {
            runMenu(admin);
        } finally {
            ConsoleStyle.leave();
        }
    }

    private static void runMenu(Admin admin) {
        while (true) {
            ConsoleStyle.menuHeader("Manage Users");
            System.out.println("  1 - View all accounts");
            System.out.println("  2 - Add Staff account");
            System.out.println("  3 - Deactivate or reactivate an account");
            System.out.println("  4 - Unlock an account");
            System.out.println("  5 - Back");
            int choice = InputHelper.readMenuChoice("Choice: ", 1, 5);
            if (choice == 5) {
                return;
            }
            try {
                switch (choice) {
                    case 1:
                        viewAll();
                        break;
                    case 2:
                        addStaff();
                        break;
                    case 3:
                        toggleActive(admin);
                        break;
                    default:
                        unlock();
                        break;
                }
            } catch (OperationCancelledException e) {
                System.out.println();
                System.out.println(ConsoleStyle.error(e.getMessage()));
            }
        }
    }

    private static void viewAll() {
        ConsoleStyle.header("All Accounts");
        System.out.printf("%-16s %-26s %-10s %s%n", "Username", "Full name", "Role", "Status");
        System.out.println("-".repeat(ConsoleStyle.WIDTH));
        for (User u : AirlineSystem.getInstance().getUsers()) {
            System.out.printf("%-16s %-26s %-10s %s%n", u.getUsername(), u.getFullName(), u.getRole(),
                    u.getStatusLabel());
        }
    }

    private static void addStaff() {
        AirlineSystem system = AirlineSystem.getInstance();
        ConsoleStyle.header("Add Staff Account");
        ConsoleStyle.cancelHint();
        String username = AuthFlow.readNewUsername();
        String password = AuthFlow.readNewPassword();
        String fullName = InputHelper.read("Full name: ", User::validateFullName);
        if (!InputHelper.confirm("Create Staff account " + username + " for " + fullName + "? (Y/N): ")) {
            System.out.println("Account not created.");
            return;
        }
        system.addUser(new Staff(username, password, fullName));
        system.save();
        System.out.println("Staff account " + username + " created. They log in through the Staff Portal.");
    }

    private static void toggleActive(Admin admin) {
        AirlineSystem system = AirlineSystem.getInstance();
        ConsoleStyle.header("Deactivate or Reactivate");
        ConsoleStyle.cancelHint();
        User user = readExistingUser();
        if (user.getUsername().equalsIgnoreCase(admin.getUsername())) {
            System.out.println(ConsoleStyle.error("You can't deactivate your own account."));
            return;
        }
        boolean deactivating = user.isActive();
        if (deactivating && user instanceof Admin && system.countActiveAdmins() <= 1) {
            System.out.println(ConsoleStyle.error("You can't deactivate the last active Admin."));
            return;
        }
        String action = deactivating ? "Deactivate" : "Reactivate";
        if (!InputHelper.confirm(action + " " + user.getUsername() + " (" + user.getFullName() + ")? (Y/N): ")) {
            System.out.println("Nothing changed.");
            return;
        }
        user.setActive(!deactivating);
        system.save();
        System.out.println(user.getUsername() + " is now " + (deactivating ? "deactivated" : "active") + ".");
    }

    private static void unlock() {
        AirlineSystem system = AirlineSystem.getInstance();
        ConsoleStyle.header("Unlock Account");
        ConsoleStyle.cancelHint();
        User user = readExistingUser();
        if (!user.isLocked()) {
            System.out.println(user.getUsername() + " isn't locked.");
            return;
        }
        if (!InputHelper.confirm("Unlock " + user.getUsername() + "? (Y/N): ")) {
            System.out.println("Nothing changed.");
            return;
        }
        user.unlock();
        system.save();
        System.out.println(user.getUsername() + " is unlocked.");
    }

    private static User readExistingUser() {
        AirlineSystem system = AirlineSystem.getInstance();
        return InputHelper.read("Username: ", raw -> {
            User user = system.findUser(raw);
            if (user == null) {
                throw new InvalidInputException("No account with that username.");
            }
            return user;
        });
    }
}
