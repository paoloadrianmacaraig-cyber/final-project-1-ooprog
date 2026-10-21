/**
 * Thrown when a login fails: wrong username or password, a locked account,
 * or a deactivated account.
 */
public class InvalidLoginException extends AirlineException {

    private static final long serialVersionUID = 1L;

    public InvalidLoginException(String message) {
        super(message);
    }
}