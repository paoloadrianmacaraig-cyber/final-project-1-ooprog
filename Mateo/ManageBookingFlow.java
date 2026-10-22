

public final class ManageBookingFlow {

    private ManageBookingFlow() {
    }

    public static Booking findBooking() {
        AirlineSystem system = AirlineSystem.getInstance();
        ConsoleStyle.cancelHint();
        while (true) {
            String pnr = InputHelper.read("PNR: ", Booking::validatePnr);
            String lastName = InputHelper.read("Last name of the first traveler: ",
                    raw -> InputHelper.requireText(raw, "Enter the last name."));
            try {
                return system.findBooking(pnr, lastName);
            } catch (BookingNotFoundException e) {
                System.out.println(ConsoleStyle.error(e.getMessage()));
                System.out.println();
            }
        }
    }

    public static void run() {
        ConsoleStyle.header("Manage Booking");
        Booking booking = findBooking();

        boolean firstTime = true;
        while (true) {

            if (firstTime) {
                System.out.println();
                firstTime = false;
            } else {
                ConsoleStyle.header("Manage Booking");
            }
            BookingFlow.printBookingSummary(booking);
            System.out.println();
            System.out.println("  1 - View e-ticket");
            System.out.println("  2 - Save e-ticket as a text file");
            System.out.println("  3 - Change seat");
            System.out.println("  4 - Change flight");
            System.out.println("  5 - Add checked baggage");
            System.out.println("  6 - Add a pet in the cabin");
            System.out.println("  7 - Check in");
            System.out.println("  8 - Cancel booking");
            System.out.println("  9 - Back");
            int choice = InputHelper.readMenuChoice("Choice: ", 1, 9);
            if (choice == 9) {
                return;
            }
            try {
                switch (choice) {
                    case 1: ETicket.print(booking); break;
                    case 2: ETicket.save(booking); break;
                    case 3: ChangeSeatFlow.run(booking); break;
                    case 4: ChangeFlightFlow.run(booking); break;
                    case 5: BaggageFlow.addLater(booking); break;
                    case 6: PetFlow.addLater(booking); break;
                    case 7: CheckInFlow.checkIn(booking); break;
                    default: CancelFlow.cancel(booking); break;
                }
            } catch (OperationCancelledException e) {
                System.out.println();
                System.out.println(ConsoleStyle.error(e.getMessage()));
            }
        }
    }
}
