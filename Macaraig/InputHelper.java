import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Scanner;
import java.util.regex.Pattern;

public final class InputHelper {

    public static final String CANCEL_WORD = "cancel";

    private static final Scanner SCANNER = new Scanner(System.in, consoleCharset());

    private static final Pattern WHOLE_NUMBER = Pattern.compile("^[+-]?\\d{1,9}$");

    private static final Pattern AMOUNT = Pattern.compile("^(\\d{1,9}(\\.\\d{1,2})?|\\.\\d{1,2})$");

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);

    private InputHelper() {
    }

    private static String consoleCharset() {
        String name = System.getProperty("stdin.encoding");
        if (name == null) {
            name = System.getProperty("stdout.encoding", System.getProperty("sun.stdout.encoding"));
        }
        try {
            return (name != null && Charset.isSupported(name)) ? name : Charset.defaultCharset().name();
        } catch (IllegalArgumentException e) {
            return Charset.defaultCharset().name();
        }
    }

    public static String readRaw(String prompt) {
        System.out.print(prompt);
        if (!SCANNER.hasNextLine()) {
            System.out.println();
            System.out.println("No more input. Exiting.");
            System.exit(0);
        }
        return SCANNER.nextLine().trim();
    }

    public static String readLine(String prompt) {
        String input = readRaw(prompt);
        if (input.equalsIgnoreCase(CANCEL_WORD)) {
            throw new OperationCancelledException();
        }
        return input;
    }

    public static <T> T read(String prompt, InputRule<T> rule) {
        while (true) {
            String raw = readLine(prompt);
            try {
                return rule.apply(raw);
            } catch (InvalidInputException e) {
                System.out.println(ConsoleStyle.error(e.getMessage()));
                System.out.println();
            }
        }
    }

    public static int readMenuChoice(String prompt, int min, int max) {
        while (true) {
            spaceBeforeChoice(prompt);
            String raw = readRaw(prompt);
            if (WHOLE_NUMBER.matcher(raw).matches()) {
                int value = Integer.parseInt(raw);
                if (value >= min && value <= max) {
                    return value;
                }
            }
            System.out.println(ConsoleStyle.error("Invalid choice. Enter a number from " + min + " to " + max + "."));
            if (!isChoicePrompt(prompt)) {
                System.out.println();
            }
        }
    }

    public static int readChoice(String prompt, int min, int max) {
        while (true) {
            spaceBeforeChoice(prompt);
            String raw = readLine(prompt);
            if (WHOLE_NUMBER.matcher(raw).matches()) {
                int value = Integer.parseInt(raw);
                if (value >= min && value <= max) {
                    return value;
                }
            }
            System.out.println(ConsoleStyle.error("Invalid choice. Enter a number from " + min + " to " + max + "."));
            if (!isChoicePrompt(prompt)) {
                System.out.println();
            }
        }
    }

    public static boolean readYesNo(String prompt) {
        System.out.println();
        while (true) {
            String raw = readLine(prompt).toLowerCase();
            if (raw.equals("y") || raw.equals("yes")) {
                return true;
            }
            if (raw.equals("n") || raw.equals("no")) {
                return false;
            }
            System.out.println(ConsoleStyle.error("Please enter Y or N."));
            System.out.println();
        }
    }

    public static boolean confirm(String prompt) {
        boolean answer = readYesNo(prompt);
        System.out.println();
        return answer;
    }

    private static boolean isChoicePrompt(String prompt) {
        return prompt.startsWith("Choice");
    }

    private static void spaceBeforeChoice(String prompt) {
        if (isChoicePrompt(prompt)) {
            System.out.println();
        }
    }

    public static int parseWholeNumber(String raw, int min, int max, String field) throws InvalidInputException {
        if (!WHOLE_NUMBER.matcher(raw).matches()) {
            throw new InvalidInputException(field + " must be a whole number.");
        }
        int value = Integer.parseInt(raw);
        if (value < min || value > max) {
            throw new InvalidInputException(field + " must be from " + min + " to " + max + ".");
        }
        return value;
    }

    public static double parseAmount(String raw, double maxValue, String field) throws InvalidInputException {
        if (!AMOUNT.matcher(raw).matches()) {
            throw new InvalidInputException(field + " must be a number with at most 2 decimal places.");
        }
        double value = Double.parseDouble(raw);
        if (value <= 0) {
            throw new InvalidInputException(field + " must be above 0.");
        }
        if (value > maxValue) {
            throw new InvalidInputException(field + " can't be more than " + ConsoleStyle.number(maxValue) + ".");
        }
        return value;
    }

    public static LocalDate parseDate(String raw) throws InvalidInputException {
        try {
            return LocalDate.parse(raw, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new InvalidInputException("Not a real date. Use YYYY-MM-DD, e.g. 2026-11-20.");
        }
    }

    public static LocalTime parseTime(String raw) throws InvalidInputException {
        try {
            return LocalTime.parse(raw, TIME_FORMAT);
        } catch (DateTimeParseException e) {
            throw new InvalidInputException("Use 24-hour HH:MM, e.g. 08:30.");
        }
    }

    public static String requireText(String raw, String emptyMessage) throws InvalidInputException {
        if (raw.isEmpty()) {
            throw new InvalidInputException(emptyMessage);
        }
        return raw;
    }

    public static int readWholeNumber(String prompt, int min, int max, String field) {
        return read(prompt, raw -> parseWholeNumber(raw, min, max, field));
    }

    public static LocalDate readDate(String prompt) {
        return read(prompt, InputHelper::parseDate);
    }

    public static LocalTime readTime(String prompt) {
        return read(prompt, InputHelper::parseTime);
    }
}
