package uk.co.visad.util;

import org.springframework.core.io.ByteArrayResource;

/**
 * ByteArrayResource that returns a user-visible filename from getFilename().
 * Needed when controllers use resource.getFilename() to build Content-Disposition.
 */
public class NamedByteArrayResource extends ByteArrayResource {

    private final String filename;

    public NamedByteArrayResource(byte[] bytes, String filename) {
        super(bytes);
        this.filename = filename;
    }

    @Override
    public String getFilename() {
        return filename;
    }
}
