package Macaraig;

public class Staff extends User {

    private static final long serialVersionUID = 1L;

    public Staff(String username, String password, String fullName) {
        super(username, password, fullName);
    }

    @Override
    public String getRole() {
        return "Staff";
    }

    @Override
    public boolean isEmployee() {
        return true;
    }

    @Override
    public void showMenu() {
        ConsoleStyle.enter(getMenuLabel());
        try {
            runTicketingMenu();
        } finally {
            ConsoleStyle.leave();
        }
    }

    protected String getMenuLabel() {
        return "Staff";
    }

    private void runTicketingMenu() {
        while (true) {
            ConsoleStyle.menuHeader("Ticketing Menu");
            System.out.println("  1 - Search Flights");
            System.out.println("  2 - Book for a Walk-in");
            System.out.println("  3 - Manage Booking");
            System.out.println("  4 - Check In");
            System.out.println("  5 - Cancel Booking");
            System.out.println("  6 - Change Password");
            System.out.println("  7 - " + getExitLabel());
            int choice = InputHelper.readMenuChoice("Choice: ", 1, 7);
            if (choice == 7) {
                System.out.println(getExitMessage());
                return;
            }
            try {
                switch (choice) {
                    case 1:
                        SearchFlow.searchOnly();
                        break;
                    case 2:
                        BookingFlow.run(getUsername());
                        break;
                    case 3:
                        ManageBookingFlow.run();
                        break;
                    case 4:
                        CheckInFlow.run();
                        break;
                    case 5:
                        CancelFlow.run();
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

    protected String getExitLabel() {
        return "Logout";
    }

    protected String getExitMessage() {
        return "Logged out.";
    }
}
