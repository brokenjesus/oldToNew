package by.lupach.oldtonew2.exceptions;

public class PatientCreationException extends ImportException {
    public PatientCreationException(String message) {
        super(message);
    }

    public PatientCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
