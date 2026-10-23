public class Main {

    public static void main(String[] args) {
        AirlineSystem system = AirlineSystem.getInstance();
        ConsoleStyle.titleScreen();
        if (system.hasSampleData()) {
            System.out.println("Sample data loaded. Test accounts are listed in TEST-ACCOUNTS.txt.");
        }

        while (true) {
            ConsoleStyle.mainMenuHeader();
            System.out.println("  1 - Login");
            System.out.println("  2 - Create Account");
            System.out.println("  3 - Flight Status");
            System.out.println("  4 - Staff Portal");
            System.out.println("  5 - Exit");
            int choice = InputHelper.readMenuChoice("Choice: ", 1, 5);
            try {
                if (choice == 1) {
                    User user = AuthFlow.login(false);
                    ConsoleStyle.setUser(user.getFullName());
                    user.showMenu();
                    ConsoleStyle.setUser("");
                } else if (choice == 2) {
                    AuthFlow.register();
                } else if (choice == 3) {
                    FlightStatusFlow.run();
                } else if (choice == 4) {
                    User user = AuthFlow.login(true);
                    ConsoleStyle.setUser(user.getFullName());
                    user.showMenu();
                    ConsoleStyle.setUser("");
                } else {
                    system.save();
                    System.out.println();
                    ConsoleStyle.divider();
                    System.out.println("Thank you for flying with us. Goodbye!");
                    ConsoleStyle.divider();
                    return;
                }
            } catch (OperationCancelledException e) {
                System.out.println();
                System.out.println("Back to the main menu.");
            }
        }
    }
}