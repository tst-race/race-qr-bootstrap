package race;

abstract class Time {

    static double getTimestampSeconds() {
        return ((double) System.currentTimeMillis()) / 1000;
    }

    static String getTimestampString(double timestamp) {
        return String.format("%.3f", timestamp);
    }
}
