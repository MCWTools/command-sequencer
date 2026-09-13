package khoa.commandsequencer.vector;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import khoa.commandsequencer.CommandSequencer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

/**
 * /vectorsummon — cùng cấu trúc argument với /summon vanilla (verify từ
 * bytecode SummonCommand.register), chỉ thay {@code Vec3Argument.vec3()}
 * bằng {@link VectorArgument#vector()} ở tham số {@code pos}, nên
 * {@code ~x ~y ~z} được hiểu theo hướng nhìn của người gọi lệnh.
 *
 * spawnEntity thật sự gọi qua reflection vào {@code SummonCommand} gốc để
 * giữ đúng behavior vanilla (feedback message, NBT merge, entity type
 * validation...).
 *
 * Lưu ý: tham số boolean cuối của {@code spawnEntity} (randomizeProperties)
 * KHÔNG verify được trực tiếp từ bytecode (opcode ICONST không nằm trong
 * phạm vi scan hiện có) — dùng {@code false} dựa trên hành vi đã biết của
 * /summon vanilla (không random hoá properties khi summon qua lệnh, trừ khi
 * NBT chỉ định). Nếu entity summon ra có property bị random không mong
 * muốn, đây là chỗ đầu tiên cần kiểm tra lại.
 */
public final class VectorSummonCommand {

    private static final SimpleCommandExceptionType ERROR_REFLECTION =
            new SimpleCommandExceptionType(Component.literal(
                    "vectorsummon: nội bộ lỗi phản chiếu tới SummonCommand (mapping có thể đã đổi giữa các version)."));

    private static final Method SPAWN_ENTITY;

    static {
        try {
            SPAWN_ENTITY = SummonCommand.class.getDeclaredMethod(
                    "spawnEntity",
                    CommandSourceStack.class,
                    Holder.Reference.class,
                    Vec3.class,
                    CompoundTag.class,
                    boolean.class
            );
            SPAWN_ENTITY.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(
                    "khoa.commandsequencer.vector: không tìm thấy SummonCommand#spawnEntity"
                            + " với chữ ký đã verify cho 26.1.2 — kiểm tra lại mapping trước khi build: " + e);
        }
    }

    private VectorSummonCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(
                Commands.literal("vectorsummon")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("entity", ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE))
                                .suggests(SuggestionProviders.cast(SuggestionProviders.SUMMONABLE_ENTITIES))
                                .executes(VectorSummonCommand::summonAtSelf)
                                .then(Commands.argument("pos", VectorArgument.vector())
                                        .executes(VectorSummonCommand::summonAtPos)
                                        .then(Commands.argument("nbt", CompoundTagArgument.compoundTag())
                                                .executes(VectorSummonCommand::summonAtPosWithNbt))))
        );
    }

    private static int summonAtSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Vec3 pos = ctx.getSource().getPosition();
        return invokeSpawnEntity(ctx.getSource(), ResourceArgument.getSummonableEntityType(ctx, "entity"), pos, new CompoundTag());
    }

    private static int summonAtPos(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Vec3 pos = VectorArgument.getVector(ctx, "pos");
        return invokeSpawnEntity(ctx.getSource(), ResourceArgument.getSummonableEntityType(ctx, "entity"), pos, new CompoundTag());
    }

    private static int summonAtPosWithNbt(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Vec3 pos = VectorArgument.getVector(ctx, "pos");
        CompoundTag nbt = CompoundTagArgument.getCompoundTag(ctx, "nbt");
        return invokeSpawnEntity(ctx.getSource(), ResourceArgument.getSummonableEntityType(ctx, "entity"), pos, nbt);
    }

    private static int invokeSpawnEntity(
            CommandSourceStack source,
            Holder.Reference<EntityType<?>> entityType,
            Vec3 pos,
            CompoundTag nbt
    ) throws CommandSyntaxException {
        try {
            return (int) SPAWN_ENTITY.invoke(null, source, entityType, pos, nbt, false);
        } catch (ReflectiveOperationException e) {
            CommandSequencer.LOGGER.error("vectorsummon: lỗi khi gọi SummonCommand#spawnEntity qua reflection", e);
            Throwable cause = e.getCause();
            if (cause instanceof CommandSyntaxException cse) {
                throw cse;
            }
            throw ERROR_REFLECTION.create();
        }
    }
}
