package shit.zen.utils.misc;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.Generated;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ClientboundGameProfilePacket;
import net.minecraft.network.protocol.status.ClientboundPongResponsePacket;
import shit.zen.ClientBase;
import shit.zen.ZenClient;
import shit.zen.event.impl.PacketSendEvent;
import shit.zen.utils.misc.ReflectionUtil;

public final class PacketUtil
extends ClientBase {
    public static final ArrayList<Packet<ServerGamePacketListener>> queuedPackets = new ArrayList<>();
    private static final Queue<Packet<?>> rateLimitedPackets = new ConcurrentLinkedQueue<>();
    private static volatile long lastRateLimitedSend;
    private static final long RATE_LIMIT_INTERVAL_MS = 45L;
    private static final int MAX_RATE_LIMIT_QUEUE = 64;

    public static boolean shouldBypass(Packet<ServerGamePacketListener> packet) {
        PacketSendEvent packetSendEvent = new PacketSendEvent(packet);
        ZenClient.getInstance().getEventBus().call(packetSendEvent);
        if (packetSendEvent.isCancelled()) {
            return true;
        }
        if (queuedPackets.contains(packet)) {
            queuedPackets.remove(packet);
            return true;
        }
        return false;
    }

    public static void sendPredictive(PredictiveAction predictiveAction) {
        if (mc.getConnection() == null || mc.level == null) {
            return;
        }
        try {
            String fieldName = ReflectionUtil.getMappedFieldName(ClientLevel.class, "blockStatePredictionHandler");
            if (fieldName == null) {
                return;
            }
            Field field = ClientLevel.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            BlockStatePredictionHandler predictionHandler = (BlockStatePredictionHandler)field.get(mc.level);
            try (BlockStatePredictionHandler predicting = predictionHandler.startPredicting()){
                int sequence = predicting.currentSequence();
                mc.getConnection().send(predictiveAction.predict(sequence));
            }
        } catch (Exception ex) {
            logger.error(ex);
            ex.printStackTrace();
        }
    }

    public static void sendPredictiveDirect(PredictiveAction predictiveAction) {
        if (mc.getConnection() == null || mc.level == null) {
            return;
        }
        try {
            String fieldName = ReflectionUtil.getMappedFieldName(ClientLevel.class, "blockStatePredictionHandler");
            if (fieldName == null) {
                return;
            }
            Field field = ClientLevel.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            BlockStatePredictionHandler predictionHandler = (BlockStatePredictionHandler)field.get(mc.level);
            try (BlockStatePredictionHandler predicting = predictionHandler.startPredicting()){
                int sequence = predicting.currentSequence();
                PacketUtil.sendQueued(predictiveAction.predict(sequence));
            }
        } catch (Exception ex) {
            logger.error(ex);
            ex.printStackTrace();
        }
    }

    public static boolean isIgnored(Packet<?> packet) {
        if (mc.player == null) {
            return true;
        }
        if (mc.screen instanceof ReceivingLevelScreen) {
            return true;
        }
        if (packet instanceof ClientboundCustomQueryPacket) {
            return true;
        }
        if (packet instanceof ClientboundTabListPacket) {
            return true;
        }
        if (packet instanceof ClientboundDisconnectPacket) {
            return true;
        }
        if (packet instanceof ClientboundLevelChunkPacketData) {
            return true;
        }
        if (packet instanceof ClientboundForgetLevelChunkPacket) {
            return true;
        }
        if (packet instanceof ClientboundPongResponsePacket) {
            return true;
        }
        if (packet instanceof ClientboundLoginPacket) {
            return true;
        }
        if (packet instanceof ClientboundGameProfilePacket) {
            return true;
        }
        if (packet instanceof ClientboundMapItemDataPacket) {
            return true;
        }
        if (packet instanceof ClientboundSetDefaultSpawnPositionPacket) {
            return true;
        }
        if (packet instanceof ClientboundSetEntityMotionPacket) {
            return true;
        }
        if (packet instanceof ClientboundCustomPayloadPacket) {
            return true;
        }
        if (packet instanceof ClientboundPlayerChatPacket) {
            return true;
        }
        if (packet instanceof ClientboundSetTitleTextPacket) {
            return true;
        }
        return mc.player.tickCount <= 60;
    }

    /**
     * Applies a small client-side pacing window to burst-prone interaction packets.
     * Movement and keep-alive packets are intentionally left untouched.
     */
    public static boolean shouldThrottle(Packet<?> packet) {
        if (!isRateLimitedPacket(packet) || mc.player == null || mc.getConnection() == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (rateLimitedPackets.isEmpty() && now - lastRateLimitedSend >= RATE_LIMIT_INTERVAL_MS) {
            lastRateLimitedSend = now;
            return false;
        }
        if (rateLimitedPackets.size() >= MAX_RATE_LIMIT_QUEUE) {
            return true;
        }
        rateLimitedPackets.offer(packet);
        return true;
    }

    public static void flushRateLimited() {
        if (mc.player == null || mc.getConnection() == null || rateLimitedPackets.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastRateLimitedSend < RATE_LIMIT_INTERVAL_MS) {
            return;
        }
        Packet<?> packet = rateLimitedPackets.poll();
        if (packet == null) {
            return;
        }
        lastRateLimitedSend = now;
        @SuppressWarnings("unchecked")
        Packet<ServerGamePacketListener> typed = (Packet<ServerGamePacketListener>) packet;
        queuedPackets.add(typed);
        mc.getConnection().send(packet);
    }

    public static void clearRateLimited() {
        rateLimitedPackets.clear();
    }

    private static boolean isRateLimitedPacket(Packet<?> packet) {
        return packet instanceof ServerboundContainerClickPacket
                || packet instanceof ServerboundInteractPacket
                || packet instanceof ServerboundUseItemOnPacket
                || packet instanceof ServerboundUseItemPacket
                || packet instanceof ServerboundPlayerActionPacket;
    }

    public static void sendQueued(Packet<ServerGamePacketListener> packet) {
        if (mc.player == null) {
            return;
        }
        queuedPackets.add(packet);
        mc.player.connection.send(packet);
    }

    public static void send(Packet<ServerGamePacketListener> packet) {
        if (mc.player == null) {
            return;
        }
        mc.player.connection.send(packet);
    }

    @Generated
    private PacketUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}