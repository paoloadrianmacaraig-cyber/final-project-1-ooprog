@FunctionalInterface
public interface InputRule<T> {

    T apply(String raw) throws InvalidInputException;
}