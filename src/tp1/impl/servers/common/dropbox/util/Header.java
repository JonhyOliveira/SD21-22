package tp1.impl.service.common.dropbox.util;

public record Header(String property, String value) {

    public static final String DROPBOX_ARG_HEADER = "Dropbox-API-Arg";

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Header header = (Header) o;

        return property.equals(header.property);
    }

    @Override
    public int hashCode() {
        return property.hashCode();
    }
}
