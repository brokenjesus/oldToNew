package by.lupach.oldtonew2.exceptions;

public class NoteProcessingException extends ImportException {
    public NoteProcessingException(String message) {
        super(message);
    }

    public NoteProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
