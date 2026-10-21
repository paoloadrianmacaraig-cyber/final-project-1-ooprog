import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.time.format.DateTimeFormatter;

public final class ConsoleStyle {

    public static final int WIDTH = 72;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TODAY = DateTimeFormatter.ofPattern("EEE, d MMM yyyy");
    private static final Deque<String> PATH = new ArrayDeque<>();
    private static String user = "";

    private ConsoleStyle() {
    }

    public static void titleScreen() {
        divider();
        System.out.println(center(AirlineSystem.AIRLINE_NAME.toUpperCase()));
        System.out.println(center(AirlineSystem.TAGLINE));
        divider();
    }

    public static void setUser(String name) {
        user = name;
    }

    public static void enter(String area) {
        PATH.addLast(area);
    }

    public static void leave() {
        PATH.pollLast();
    }

    public static void mainMenuHeader() {
        bar(null);
    }

    public static void menuHeader(String menuName) {
        bar(null);
    }

    public static void header(String title) {
        bar(title);
    }

    public static void section(String title) {
        System.out.println();
        System.out.println("-- " + title + " --");
        System.out.println();
    }

    public static void cancelHint() {
        System.out.println("  (Type 'cancel' at any prompt to go back)");
        System.out.println();
    }

    public static String error(String message) {
        return "[!] " + message;
    }

    private static void bar(String title) {
        String right = user.isEmpty() ? LocalDate.now().format(TODAY) : user;
        System.out.println();
        System.out.println("=".repeat(WIDTH));
        System.out.println(spread(" " + path(title), right + " "));
        System.out.println("-".repeat(WIDTH));
        System.out.println();
    }

    private static String path(String title) {
        StringBuilder sb = new StringBuilder();
        for (String part : PATH) {
            sb.append(sb.length() == 0 ? "" : " > ").append(part);
        }
        if (title != null) {
            sb.append(sb.length() == 0 ? "" : " > ").append(title);
        }
        return sb.length() == 0 ? "Home" : sb.toString();
    }

    private static String spread(String left, String right) {
        int gap = Math.max(2, WIDTH - left.length() - right.length());
        return left + " ".repeat(gap) + right;
    }

    public static void divider() {
        System.out.println("=".repeat(WIDTH));
    }

    public static String row(String label, String value) {
        return String.format("  %-18s %s", label, value);
    }

    public static String row(String label, double amount) {
        return row(label, peso(amount));
    }

    public static String peso(double amount) {
        if (amount < 0) {
            return "-Php " + number(-amount);
        }
        return "Php " + number(amount);
    }

    public static String number(double amount) {
        return String.format("%,.2f", amount);
    }

    public static String date(LocalDate date) {
        return date.format(DATE);
    }

    public static String time(LocalDateTime dateTime) {
        return dateTime.format(TIME);
    }

    public static String dateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME);
    }

    private static String center(String text) {
        int padding = Math.max(0, (WIDTH - text.length()) / 2);
        return " ".repeat(padding) + text;
    }
}
