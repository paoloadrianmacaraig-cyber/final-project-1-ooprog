public class Admin extends Staff {

    private static final long serialVersionUID = 1L;

    public Admin(String username, String password, String fullName) {
        super(username, password, fullName);
    }

    @Override
    public String getRole() {
        return "Admin";
    }

    @Override
    public void showMenu() {
        ConsoleStyle.enter("Admin");
        try {
            runAdminMenu();
        } finally {
            ConsoleStyle.leave();
        }
    }

    @Override
    protected String getMenuLabel() {
        return "Ticketing Tools";
    }

    private void runAdminMenu() {
        while (true) {
            ConsoleStyle.menuHeader("Admin Menu");
            System.out.println("  1 - Manage Flights");
            System.out.println("  2 - Manage Users");
            System.out.println("  3 - Ticketing Tools");
            System.out.println("  4 - All Bookings");
            System.out.println("  5 - Passenger List");
            System.out.println("  6 - Revenue Report");
            System.out.println("  7 - Change Password");
            System.out.println("  8 - Logout");
            int choice = InputHelper.readMenuChoice("Choice: ", 1, 8);
            if (choice == 8) {
                System.out.println("Logged out.");
                return;
            }
            try {
                switch (choice) {
                    case 1:
                        FlightManagementFlow.run();
                        break;
                    case 2:
                        UserManagementFlow.run(this);
                        break;
                    case 3:
                        super.showMenu();
                        break; // the Staff ticketing menu
                    case 4:
                        ReportFlow.allBookings();
                        break;
                    case 5:
                        ReportFlow.passengerList();
                        break;
                    case 6:
                        ReportFlow.revenueReport();
                        break;
                    default:
                        AuthFlow.changePassword(this);
                        break;
                }
            } catch (OperationCancelledException e) {
                System.out.println();
                System.out.println(ConsoleStyle.error(e.getMessage()));
            }
        }
    }

    @Override
    protected String getExitLabel() {
        return "Back to Admin Menu";
    }

    @Override
    protected String getExitMessage() {
        return "Back to the Admin Menu.";
    }
}