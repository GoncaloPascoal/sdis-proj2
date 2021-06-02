package utils;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Utils {
    // Prevent class from being instantiated
    private Utils() {}

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static List<byte[]> splitMessage(byte[] message) {
        List<byte[]> components = new ArrayList<>();

        for (int i = 0; i < message.length - 3; ++i) {
            if (message[i] == 0xD && message[i + 1] == 0xA && message[i + 2] == 0xD && message[i + 3] == 0xA) {
                components.add(Arrays.copyOfRange(message, 0, i));
                components.add(Arrays.copyOfRange(message, i + 4, message.length));
                break;
            }
        }

        return components;
    }

    // Adapted from https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
    public static String byteArrayToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; ++i) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_ARRAY[v >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String calculateFileId(File file) throws NoSuchAlgorithmException {
        /* This function should theoretically never throw a NoSuchAlgorithmException, but the getInstance method
        of MessageDigest needs handling of this exception */

        String generatorString = file.getAbsolutePath() + file.lastModified();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return byteArrayToHex(digest.digest(generatorString.getBytes()));
    }
}
