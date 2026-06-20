package dev.leonetic.util.render;

public class GuiFade {
    public static float alpha = 1f;

    public static int apply(int color) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        return (color & 0x00FFFFFF) | (a << 24);
    }
}
