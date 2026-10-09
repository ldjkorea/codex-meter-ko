package dev.bennett.codexmeter;

/** Presentation only: never changes a rank, quota, saved theme, or observation. */
public final class WidgetTierPalette {
    private WidgetTierPalette() {}

    private static final int[] TOP = {
        0xff343e4b, 0xff503628, 0xff3e4c5c, 0xff554323, 0xff214652,
        0xff174e3d, 0xff224868, 0xff483464, 0xff542d3c, 0xff5e4927
    };
    private static final int[] BOTTOM = {
        0xff111820, 0xff211711, 0xff171f29, 0xff251d10, 0xff102329,
        0xff0b251e, 0xff102130, 0xff1d142c, 0xff28131d, 0xff251c0e
    };
    private static final int[] ACCENTS = {
        0xffa9b3bd, 0xffdfa778, 0xffd5e1eb, 0xffe8ce91, 0xffaee4ee,
        0xff68e1b1, 0xff9bdcfa, 0xffc7a7f4, 0xfff1a0ac, 0xffffdfa1
    };

    public static boolean active(boolean crest, int tier, int opacity) {
        return crest && tier >= 0 && tier < TOP.length && opacity > 0;
    }
    public static int top(int tier) { return TOP[tier]; }
    public static int bottom(int tier) { return BOTTOM[tier]; }
    public static int accent(int tier) { return ACCENTS[tier]; }
    /** Same three opacity steps as the existing widget settings. */
    public static int opacity(int value) {
        if (value <= 0) return 0;
        if (value < 72) return 56;
        if (value < 94) return 88;
        return 100;
    }
    /** Scale gauges and long countdown labels together when the center crest is shown. */
    public static boolean scalableRings(boolean crest) {
        return crest;
    }
}
