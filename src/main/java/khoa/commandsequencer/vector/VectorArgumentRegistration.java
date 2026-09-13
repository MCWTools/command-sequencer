package khoa.commandsequencer.vector;

import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.BuiltInRegistries;

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
 * VectorArgument không phụ thuộc CommandBuildContext (không cần registry
 * access để tạo instance) nên dùng {@code SingletonArgumentInfo.contextFree},
 * giống hệt cách vanilla đăng ký Vec3Argument.
 */
public final class VectorArgumentRegistration {

    private VectorArgumentRegistration() {
    }

    public static void register() {
        ArgumentTypeInfos.register(
                BuiltInRegistries.COMMAND_ARGUMENT_TYPE,
                "command-sequencer:vector",
                VectorArgument.class,
                SingletonArgumentInfo.contextFree(VectorArgument::vector)
        );
    }
}
