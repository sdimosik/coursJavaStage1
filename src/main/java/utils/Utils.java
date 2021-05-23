package utils;

public class Utils {
    public static int generateInt(int low, int height) {
        return low + (int) (Math.random() * (height - low) - 1);
    }
}
