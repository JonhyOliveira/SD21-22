package tp1.api.service.soap;

public class NotSupportedException extends Exception {

    private static final long serialVersionUID = 1L;

    public NotSupportedException() {
        super("");
    }

    public NotSupportedException(String errorMessage) {
        super(errorMessage);
    }
}
