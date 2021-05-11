package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Utils {
    // Prevent class from being instantiated
    private Utils() {}

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
}
