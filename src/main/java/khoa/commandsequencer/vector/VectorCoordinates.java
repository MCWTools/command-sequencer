package khoa.commandsequencer.vector;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Bản thay thế cho {@code WorldCoordinates} (vanilla).
 *
 * Khác biệt duy nhất so với vanilla: khi trục nào đó có dấu {@code ~},
 * offset của trục đó được cộng theo VECTOR HƯỚNG NHÌN (yaw/pitch) của
 * entity gọi lệnh, thay vì cộng thẳng vào trục world cố định.
 *
 * Quy ước (theo plan gốc):
 *  - ~z  -> tiến/lùi theo hướng nhìn ngang (forward vector, chỉ dùng yaw)
 *  - ~x  -> lệch phải/trái theo hướng nhìn (right vector = forward xoay 90°)
 *  - ~y  -> vẫn là world Y bình thường (không bị ảnh hưởng bởi pitch)
 *
 * Nếu người dùng không gõ ~ ở trục nào thì trục đó là số tuyệt đối, hành vi
 * y hệt vanilla — không có gì thay đổi.
 */
public record VectorCoordinates(VectorCoordinate x, VectorCoordinate y, VectorCoordinate z) implements Coordinates {

    public static final VectorCoordinates ZERO_ROTATION = new VectorCoordinates(
            new VectorCoordinate(true, 0.0D),
            new VectorCoordinate(true, 0.0D),
            new VectorCoordinate(true, 0.0D)
    );

    @Override
    public Vec3 getPosition(CommandSourceStack source) {
        Vec3 basePos = source.getPosition();

        // Không trục nào relative -> y hệt tuyệt đối, không cần tính vector.
        if (!x.relative() && !y.relative() && !z.relative()) {
            return new Vec3(x.get(basePos.x), y.get(basePos.y), z.get(basePos.z));
        }

        float yaw = source.getRotation().y;
        double yawRad = Math.toRadians(yaw);

        // forward: hướng nhìn ngang (theo quy ước vanilla, yaw=0 nhìn về -Z)
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);

        // right: forward xoay 90° theo chiều kim đồng hồ nhìn từ trên xuống
        double rightX = forwardZ;
        double rightZ = -forwardX;

        double offsetZ = z.relative() ? z.value() : 0.0D;
        double offsetX = x.relative() ? x.value() : 0.0D;

        double vecX = basePos.x + offsetZ * forwardX + offsetX * rightX;
        double vecZ = basePos.z + offsetZ * forwardZ + offsetX * rightZ;

        // Trục nào KHÔNG relative thì vẫn là tuyệt đối bình thường
        // (không cộng vector) - dấu ~ chỉ áp dụng cho trục có dấu ~.
        double finalX = x.relative() ? vecX : x.value();
        double finalZ = z.relative() ? vecZ : z.value();
        double finalY = y.get(basePos.y);

        return new Vec3(finalX, finalY, finalZ);
    }

    @Override
    public Vec2 getRotation(CommandSourceStack source) {
        // Rotation không liên quan tới vị trí -> giữ hành vi vanilla nguyên vẹn.
        Vec2 sourceRot = source.getRotation();
        double x = this.x.get(sourceRot.x);
        double y = this.y.get(sourceRot.y);
        return new Vec2((float) x, (float) y);
    }

    @Override
    public boolean isXRelative() {
        return x.relative();
    }

    @Override
    public boolean isYRelative() {
        return y.relative();
    }

    @Override
    public boolean isZRelative() {
        return z.relative();
    }

    public static VectorCoordinates parseDouble(StringReader reader, boolean centerCorrect) throws CommandSyntaxException {
        int cursor = reader.getCursor();
        VectorCoordinate x = VectorCoordinate.parseDouble(reader, centerCorrect);
        if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();
            VectorCoordinate y = VectorCoordinate.parseDouble(reader, centerCorrect);
            if (reader.canRead() && reader.peek() == ' ') {
                reader.skip();
                VectorCoordinate z = VectorCoordinate.parseDouble(reader, centerCorrect);
                return new VectorCoordinates(x, y, z);
            }
            reader.setCursor(cursor);
            throw VectorCoordinate.ERROR_EXPECTED_DOUBLE.createWithContext(reader);
        }
        reader.setCursor(cursor);
        throw VectorCoordinate.ERROR_EXPECTED_DOUBLE.createWithContext(reader);
    }

    public static VectorCoordinates parseInt(StringReader reader) throws CommandSyntaxException {
        int cursor = reader.getCursor();
        VectorCoordinate x = VectorCoordinate.parseInt(reader);
        if (reader.canRead() && reader.peek() == ' ') {
            reader.skip();
            VectorCoordinate y = VectorCoordinate.parseInt(reader);
            if (reader.canRead() && reader.peek() == ' ') {
                reader.skip();
                VectorCoordinate z = VectorCoordinate.parseInt(reader);
                return new VectorCoordinates(x, y, z);
            }
            reader.setCursor(cursor);
            throw VectorCoordinate.ERROR_EXPECTED_INT.createWithContext(reader);
        }
        reader.setCursor(cursor);
        throw VectorCoordinate.ERROR_EXPECTED_INT.createWithContext(reader);
    }

    public static VectorCoordinates absolute(double x, double y, double z) {
        return new VectorCoordinates(
                new VectorCoordinate(false, x),
                new VectorCoordinate(false, y),
                new VectorCoordinate(false, z)
        );
    }
}
