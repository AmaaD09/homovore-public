package dev.leonetic.util.render;

import dev.leonetic.util.traits.Util;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class MatrixCapture {
    public static Matrix4f projection;
    public static Matrix4f view;

    public static float[] worldToScreen(double worldX, double worldY, double worldZ) {
        if (projection == null || view == null) return null;

        Vec3 camPos = Util.mc.gameRenderer.getMainCamera().position();
        Matrix4f mvp = projection.mul(view, new Matrix4f());

        Vector4f point = new Vector4f(
                (float) (worldX - camPos.x()),
                (float) (worldY - camPos.y()),
                (float) (worldZ - camPos.z()),
                1.0f
        );
        mvp.transform(point);

        if (point.w <= 0.0f) return null;

        float ndcX = point.x / point.w;
        float ndcY = point.y / point.w;

        if (ndcX < -1.1f || ndcX > 1.1f || ndcY < -1.1f || ndcY > 1.1f) return null;

        int guiW = Util.mc.getWindow().getGuiScaledWidth();
        int guiH = Util.mc.getWindow().getGuiScaledHeight();

        return new float[]{
                (ndcX + 1.0f) / 2.0f * guiW,
                (1.0f - ndcY) / 2.0f * guiH
        };
    }
}
