package khoa.commandsequencer.vector;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;

/**
 * Một trục toạ độ đơn (giống {@code WorldCoordinate} của vanilla), giữ lại
 * đúng cú pháp {@code ~1.5} / {@code ~} / {@code -3.2}, nhưng KHÔNG tự cộng
 * vào trục world ở bước parse — việc "cộng vào đâu" (trục world hay theo
 * hướng nhìn) do {@link VectorCoordinates} quyết định sau khi đã parse xong
 * cả 3 trục.
 *
 * @param relative true nếu người dùng gõ dấu {@code ~}
 * @param value    giá trị số đi kèm (offset nếu relative, giá trị tuyệt đối nếu không)
 */
public record VectorCoordinate(boolean relative, double value) {

    private static final char PREFIX_RELATIVE = '~';

    public static final SimpleCommandExceptionType ERROR_EXPECTED_DOUBLE =
            new SimpleCommandExceptionType(Component.translatable("argument.pos.missing.double"));
    public static final SimpleCommandExceptionType ERROR_EXPECTED_INT =
            new SimpleCommandExceptionType(Component.translatable("argument.pos.missing.int"));

    public double get(double base) {
        return this.relative ? this.value + base : this.value;
    }

    public static boolean isRelative(StringReader reader) {
        return reader.canRead() && reader.peek() == PREFIX_RELATIVE;
    }

    /**
     * Copy logic parse của {@code WorldCoordinate.parseDouble} (vanilla):
     * dấu {@code ~} là optional, số theo sau cũng optional (mặc định 0 nếu
     * relative). Nếu không phải relative thì số là bắt buộc và phải là số
     * tuyệt đối — giữ nguyên hành vi cũ (vanilla) của lệnh gốc.
     */
    public static VectorCoordinate parseDouble(StringReader reader, boolean centerCorrect) throws CommandSyntaxException {
        if (!reader.canRead()) {
            throw ERROR_EXPECTED_DOUBLE.createWithContext(reader);
        }

        boolean relative = isRelative(reader);
        int cursor = reader.getCursor();
        if (relative) {
            reader.skip();
        }

        if (reader.canRead() && reader.peek() != ' ') {
            double value = centerCorrect && !relative
                    ? readCoordinate(reader)
                    : reader.readDouble();
            return new VectorCoordinate(relative, value);
        }

        if (relative) {
            return new VectorCoordinate(true, 0.0D);
        }

        reader.setCursor(cursor);
        throw ERROR_EXPECTED_DOUBLE.createWithContext(reader);
    }

    public static VectorCoordinate parseInt(StringReader reader) throws CommandSyntaxException {
        if (!reader.canRead()) {
            throw ERROR_EXPECTED_INT.createWithContext(reader);
        }

        boolean relative = isRelative(reader);
        int cursor = reader.getCursor();
        if (relative) {
            reader.skip();
        }

        if (reader.canRead() && reader.peek() != ' ') {
            double value = relative ? reader.readDouble() : (double) reader.readInt();
            return new VectorCoordinate(relative, value);
        }

        if (relative) {
            return new VectorCoordinate(true, 0.0D);
        }

        reader.setCursor(cursor);
        throw ERROR_EXPECTED_INT.createWithContext(reader);
    }

    // Vanilla center-correct: toạ độ tuyệt đối kiểu block (không dấu ~) sẽ
    // được cộng thêm 0.5 nếu người dùng gõ số nguyên (căn giữa block), giữ
    // hành vi giống hệt Vec3Argument gốc.
    private static double readCoordinate(StringReader reader) throws CommandSyntaxException {
        int cursor = reader.getCursor();
        boolean hasDecimal = false;
        while (reader.canRead() && isAllowedNumberChar(reader.peek())) {
            if (reader.peek() == '.') {
                hasDecimal = true;
            }
            reader.skip();
        }
        String raw = reader.getString().substring(cursor, reader.getCursor());
        if (raw.isEmpty()) {
            throw ERROR_EXPECTED_DOUBLE.createWithContext(reader);
        }
        double value = Double.parseDouble(raw);
        return hasDecimal ? value : value + 0.5D;
    }

    private static boolean isAllowedNumberChar(char c) {
        return (c >= '0' && c <= '9') || c == '-' || c == '.';
    }
}
