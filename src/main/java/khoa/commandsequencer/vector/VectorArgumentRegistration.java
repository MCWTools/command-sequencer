package khoa.commandsequencer.vector;

import khoa.commandsequencer.CommandSequencer;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import java.lang.reflect.Method;

/**
 * Đăng ký {@link VectorArgument} vào registry ArgumentTypeInfo — BẮT BUỘC,
 * nếu bỏ bước này thì:
 *  - Lúc server gửi command tree xuống client (ClientboundCommandsPacket),
 *    server không biết serialize node dùng VectorArgument như thế nào ->
 *    hoặc crash, hoặc node đó bị lược bỏ khỏi tab-complete phía client.
 *  - Nếu server và client hiểu argument type khác nhau (desync), sẽ gây ra
 *    đúng loại lỗi "client gõ/tab được nhưng server báo invalid syntax" mà
 *    plan gốc đã cảnh báo.
 *
 * LƯU Ý QUAN TRỌNG (khác với lần viết đầu): {@code ArgumentTypeInfos.register}
 * là {@code private static} trong 26.1.2 — không gọi trực tiếp được. Nó làm
 * 2 việc: (1) put vào map nội bộ {@code BY_CLASSES} mà {@code byClass()} cần
 * để tra cứu Class -> ArgumentTypeInfo khi serialize, và (2)
 * {@code Registry.register(...)}. Map {@code BY_CLASSES} là field private
 * không có setter công khai nào khác, nên chỉ gọi {@code Registry.register}
 * suông (bỏ qua ArgumentTypeInfos.register) sẽ THIẾU bước (1) và gây đúng
 * lỗi desync client/server mà comment ở trên cảnh báo.
 *
 * => Dùng reflection gọi thẳng {@code ArgumentTypeInfos.register(...)} —
 * chữ ký đã verify chính xác từ bytecode 26_1_2_Fabric.jar, đây là cách duy
 * nhất công khai-tương đương để làm đúng cả 2 việc mà không tự ý sửa field
 * private của Mojang.
 */
public final class VectorArgumentRegistration {

    private static final Method REGISTER;

    static {
        try {
            REGISTER = ArgumentTypeInfos.class.getDeclaredMethod(
                    "register",
                    Registry.class,
                    String.class,
                    Class.class,
                    ArgumentTypeInfo.class
            );
            REGISTER.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(
                    "khoa.commandsequencer.vector: không tìm thấy ArgumentTypeInfos#register"
                            + " với chữ ký đã verify cho 26.1.2 — kiểm tra lại mapping trước khi build: " + e);
        }
    }

    private VectorArgumentRegistration() {
    }

    public static void register() {
        SingletonArgumentInfo<VectorArgument> info = SingletonArgumentInfo.contextFree(VectorArgument::vector);
        try {
            REGISTER.invoke(null, BuiltInRegistries.COMMAND_ARGUMENT_TYPE, "command-sequencer:vector", VectorArgument.class, info);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(
                    "khoa.commandsequencer.vector: lỗi khi đăng ký VectorArgument vào ArgumentTypeInfos qua reflection", e);
        }
        CommandSequencer.LOGGER.info("Đã đăng ký VectorArgument (vectortp/vectorsummon)");
    }
}
