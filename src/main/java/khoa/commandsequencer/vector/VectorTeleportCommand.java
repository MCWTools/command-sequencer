package khoa.commandsequencer.vector;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import khoa.commandsequencer.CommandSequencer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.LookAt;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

/**
 * /vectortp — cùng cấu trúc argument với /teleport vanilla (đã verify từ
 * bytecode TeleportCommand.register trong 26_1_2_Fabric.jar), chỉ thay
 * {@code Vec3Argument.vec3()} bằng {@link VectorArgument#vector()} cho phần
 * toạ độ, nên {@code ~x ~y ~z} được hiểu theo hướng nhìn (yaw) thay vì trục
 * world.
 *
 * Phần thực thi (teleportToPos) gọi LẠI đúng method private static của
 * {@code TeleportCommand} gốc bằng reflection, thay vì viết lại bằng tay —
 * để đảm bảo behavior (feedback message, xử lý passenger, portal cooldown...)
 * y hệt /tp vanilla 100%, không có nguy cơ lệch do đoán sai logic.
 */
public final class VectorTeleportCommand {

    private static final SimpleCommandExceptionType ERROR_REFLECTION =
            new SimpleCommandExceptionType(Component.literal(
                    "vectortp: nội bộ lỗi phản chiếu tới TeleportCommand (mapping có thể đã đổi giữa các version)."));

    private static final Method TELEPORT_TO_POS;
    private static final Method TELEPORT_TO_ENTITY;

    static {
        try {
            TELEPORT_TO_POS = TeleportCommand.class.getDeclaredMethod(
                    "teleportToPos",
                    CommandSourceStack.class,
                    Collection.class,
                    ServerLevel.class,
                    Coordinates.class,
                    Coordinates.class,
                    LookAt.class
            );
            TELEPORT_TO_POS.setAccessible(true);

            TELEPORT_TO_ENTITY = TeleportCommand.class.getDeclaredMethod(
                    "teleportToEntity",
                    CommandSourceStack.class,
                    Collection.class,
                    Entity.class
            );
            TELEPORT_TO_ENTITY.setAccessible(true);
        } catch (NoSuchMethodException e) {
            // Nếu Mojang đổi chữ ký ở version khác, mod sẽ fail loud ngay ở
            // classload thay vì lỗi mập mờ lúc chạy lệnh.
            throw new ExceptionInInitializerError(
                    "khoa.commandsequencer.vector: không tìm thấy TeleportCommand#teleportToPos/teleportToEntity"
                            + " với chữ ký đã verify cho 26.1.2 — kiểm tra lại mapping trước khi build: " + e);
        }
    }

    private VectorTeleportCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("vectortp")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("location", VectorArgument.vector())
                                .executes(ctx -> teleportSelfToPos(ctx)))
                        .then(Commands.argument("destination", EntityArgument.entity())
                                .executes(ctx -> teleportSelfToEntity(ctx)))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("location", VectorArgument.vector())
                                        .executes(ctx -> teleportToPos(ctx, null, null))
                                        .then(Commands.argument("rotation", RotationArgument.rotation())
                                                .executes(ctx -> teleportToPos(ctx, "rotation", null)))
                                        .then(Commands.literal("facing")
                                                .then(Commands.literal("entity")
                                                        .then(Commands.argument("facingEntity", EntityArgument.entity())
                                                                .executes(ctx -> teleportToPosFacingEntity(ctx, EntityAnchorArgument.Anchor.FEET))
                                                                .then(Commands.argument("facingAnchor", EntityAnchorArgument.anchor())
                                                                        .executes(ctx -> teleportToPosFacingEntity(
                                                                                ctx, EntityAnchorArgument.getAnchor(ctx, "facingAnchor"))))))
                                                .then(Commands.argument("facingLocation", VectorArgument.vector())
                                                        .executes(VectorTeleportCommand::teleportToPosFacingLocation))))
                                .then(Commands.argument("destination", EntityArgument.entity())
                                        .executes(ctx -> teleportMultipleToEntity(ctx)))));
    }

    private static int teleportSelfToPos(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Entity self = ctx.getSource().getEntityOrException();
        return invokeTeleportToPos(
                ctx.getSource(),
                Collections.singleton(self),
                ctx.getSource().getLevel(),
                VectorArgument.getCoordinates(ctx, "location"),
                null,
                null
        );
    }

    private static int teleportSelfToEntity(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Entity self = ctx.getSource().getEntityOrException();
        Entity destination = EntityArgument.getEntity(ctx, "destination");
        return invokeTeleportToEntity(ctx.getSource(), Collections.singleton(self), destination);
    }

    private static int teleportMultipleToEntity(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
        Entity destination = EntityArgument.getEntity(ctx, "destination");
        return invokeTeleportToEntity(ctx.getSource(), targets, destination);
    }

    private static int teleportToPos(CommandContext<CommandSourceStack> ctx, String rotationArgName, LookAt lookAt) throws CommandSyntaxException {
        Coordinates rotation = rotationArgName == null ? null : RotationArgument.getRotation(ctx, rotationArgName);
        return invokeTeleportToPos(
                ctx.getSource(),
                EntityArgument.getEntities(ctx, "targets"),
                ctx.getSource().getLevel(),
                VectorArgument.getCoordinates(ctx, "location"),
                rotation,
                lookAt
        );
    }

    private static int teleportToPosFacingEntity(CommandContext<CommandSourceStack> ctx, EntityAnchorArgument.Anchor anchor) throws CommandSyntaxException {
        Entity facingEntity = EntityArgument.getEntity(ctx, "facingEntity");
        LookAt lookAt = new LookAt.LookAtEntity(facingEntity, anchor);
        return invokeTeleportToPos(
                ctx.getSource(),
                EntityArgument.getEntities(ctx, "targets"),
                ctx.getSource().getLevel(),
                VectorArgument.getCoordinates(ctx, "location"),
                null,
                lookAt
        );
    }

    private static int teleportToPosFacingLocation(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        LookAt lookAt = new LookAt.LookAtPosition(VectorArgument.getVector(ctx, "facingLocation"));
        return invokeTeleportToPos(
                ctx.getSource(),
                EntityArgument.getEntities(ctx, "targets"),
                ctx.getSource().getLevel(),
                VectorArgument.getCoordinates(ctx, "location"),
                null,
                lookAt
        );
    }

    private static int invokeTeleportToPos(
            CommandSourceStack source,
            Collection<? extends Entity> targets,
            ServerLevel level,
            Coordinates location,
            Coordinates rotation,
            LookAt lookAt
    ) throws CommandSyntaxException {
        try {
            return (int) TELEPORT_TO_POS.invoke(null, source, targets, level, location, rotation, lookAt);
        } catch (ReflectiveOperationException e) {
            CommandSequencer.LOGGER.error("vectortp: lỗi khi gọi TeleportCommand#teleportToPos qua reflection", e);
            Throwable cause = e.getCause();
            if (cause instanceof CommandSyntaxException cse) {
                throw cse;
            }
            throw ERROR_REFLECTION.create();
        }
    }

    private static int invokeTeleportToEntity(
            CommandSourceStack source,
            Collection<? extends Entity> targets,
            Entity destination
    ) throws CommandSyntaxException {
        try {
            return (int) TELEPORT_TO_ENTITY.invoke(null, source, targets, destination);
        } catch (ReflectiveOperationException e) {
            CommandSequencer.LOGGER.error("vectortp: lỗi khi gọi TeleportCommand#teleportToEntity qua reflection", e);
            Throwable cause = e.getCause();
            if (cause instanceof CommandSyntaxException cse) {
                throw cse;
            }
            throw ERROR_REFLECTION.create();
        }
    }
}
