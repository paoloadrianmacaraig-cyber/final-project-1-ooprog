package Macaraig;

public class Passenger extends User {

    private static final long serialVersionUID = 1L;

    public Passenger(String username, String password, String fullName) {
        super(username, password, fullName);
    }

    @Override
    public String getRole() {
        return "Passenger";
    }

    @Override
    public boolean isEmployee() {
        return false;
    }

    @Override
    public void showMenu() {
        ConsoleStyle.enter("Passenger");
        try {
            runMenu();
        } finally {
            ConsoleStyle.leave();
        }
    }

    private void runMenu() {
        while (true) {
            ConsoleStyle.menuHeader("Passenger Menu");
            System.out.println("  1 - Search Flights");
            System.out.println("  2 - Book a Flight");
            System.out.println("  3 - My Bookings");
            System.out.println("  4 - Manage Booking");
            System.out.println("  5 - Check In");
            System.out.println("  6 - Cancel Booking");
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
                        SearchFlow.searchOnly();
                        break;
                    case 2:
                        BookingFlow.run(getUsername());
                        break;
                    case 3:
                        BookingFlow.showMyBookings(getUsername());
                        break;
                    case 4:
                        ManageBookingFlow.run();
                        break;
                    case 5:
                        CheckInFlow.run();
                        break;
                    case 6:
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
}
