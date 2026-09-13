package khoa.commandsequencer.vector;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.LocalCoordinates;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Thay thế {@code Vec3Argument} (vanilla) — copy gần như y hệt cấu trúc gốc
 * (kể cả nhánh {@code ^} local coordinates, giữ nguyên để cú pháp tương thích
 * ngược nếu ai đó gõ {@code ^ ^ ^}), chỉ khác ở chỗ khi gặp {@code ~} thì trả
 * về {@link VectorCoordinates} thay vì {@code WorldCoordinates}.
 */
public class VectorArgument implements ArgumentType<Coordinates> {

    private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "^ ^ ^", "~1 ~-2 ~5");

    public static final SimpleCommandExceptionType ERROR_NOT_COMPLETE =
            new SimpleCommandExceptionType(Component.translatable("argument.pos3d.incomplete"));
    public static final SimpleCommandExceptionType ERROR_MIXED_TYPE =
            new SimpleCommandExceptionType(Component.translatable("argument.pos.mixed"));

    private final boolean centerCorrect;

    public VectorArgument(boolean centerCorrect) {
        this.centerCorrect = centerCorrect;
    }

    public static VectorArgument vector() {
        return new VectorArgument(true);
    }

    public static VectorArgument vector(boolean centerCorrect) {
        return new VectorArgument(centerCorrect);
    }

    public static Vec3 getVector(CommandContext<?> context, String name) {
        // getPosition() nhận CommandSourceStack; ở đây generic <?> để không
        // ép kiểu context (giống cách vanilla làm trong Vec3Argument thật,
        // context source thực tế luôn là CommandSourceStack khi lệnh chạy
        // server-side).
        return ((Coordinates) context.getArgument(name, Coordinates.class))
                .getPosition((net.minecraft.commands.CommandSourceStack) context.getSource());
    }

    public static Coordinates getCoordinates(CommandContext<?> context, String name) {
        return context.getArgument(name, Coordinates.class);
    }

    @Override
    public Coordinates parse(StringReader reader) throws CommandSyntaxException {
        if (!reader.canRead()) {
            throw ERROR_NOT_COMPLETE.createWithContext(reader);
        }

        if (reader.peek() == '^') {
            // Giữ nguyên local coordinates (^ ^ ^) y hệt vanilla — không
            // liên quan tới yêu cầu vector-mode, chỉ có ~ mới đổi hành vi.
            // Vanilla tự throw ERROR_MIXED_TYPE bên trong LocalCoordinates.parse
            // nếu người dùng lỡ trộn ^ với ~/số tuyệt đối ở trục khác trong
            // cùng bộ 3 toạ độ, nên không cần check thêm ở đây.
            return LocalCoordinates.parse(reader);
        }

        return VectorCoordinates.parseDouble(reader, this.centerCorrect);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (builder.getRemaining().isEmpty()) {
            return SharedSuggestionProvider.suggest(
                    List.of("~ ~ ~"),
                    builder
            );
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
