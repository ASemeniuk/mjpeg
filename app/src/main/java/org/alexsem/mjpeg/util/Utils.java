package org.alexsem.mjpeg.util;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class Utils {

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    /**
     * Generate URL to camera providing host
     * @param host Mjpeg camera host
     * @return generated URL
     */
    public static String generateMpegUrl(String host) {
        return String.format("http://%s:8080/videofeed", host);
    }

    /**
     * Generate unique view identifier
     * @return Generated ID
     */
    public static int generateViewId() {
        for (;;) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

}
