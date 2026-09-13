package khoa.commandsequencer.vector;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * Điểm khởi tạo cho /vectortp và /vectorsummon.
 *
 * QUAN TRỌNG: dùng {@code CommandRegistrationCallback} (fabric-api,
 * server-side), KHÔNG PHẢI {@code ClientCommandRegistrationCallback}.
 * Lệnh client-side chỉ hoạt động khi gõ trong chat lúc đang kết nối tới
 * server có mod này cài trên client — sẽ KHÔNG hoạt động trong command
 * block (vì command block được server thực thi trực tiếp, không qua client
 * dispatcher), và sẽ "biến mất" khi vào multiplayer nếu server không đồng
 * bộ được command tree xuống (đây đúng là bug plan gốc lo: singleplayer
 * chạy được vì integrated server nằm chung process với client, nhưng
 * multiplayer thì client chỉ thấy lệnh nào SERVER thật sự có).
 */
public final class VectorCommands {

    private VectorCommands() {
    }

    public static void register() {
        VectorArgumentRegistration.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            VectorTeleportCommand.register(dispatcher);
            VectorSummonCommand.register(dispatcher, registryAccess);
        });
    }
}
