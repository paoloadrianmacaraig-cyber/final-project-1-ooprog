public class OperationCancelledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OperationCancelledException() {
        super("Cancelled. Nothing was saved.");
    }
}
