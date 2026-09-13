package com.blazkness.entityrush;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.portal.TeleportTransition;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityRush implements ModInitializer {
	public static final String MOD_ID = "entity-rush";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** 当前世界的计时状态（随世界持久化） */
	private static EntityRushState state;

	@Override
	public void onInitialize() {
		EntityRushConfig.load();

		ServerLifecycleEvents.SERVER_STARTED.register(EntityRush::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> state = null);
		ServerTickEvents.END_SERVER_TICK.register(EntityRush::onEndTick);
		ServerPlayConnectionEvents.JOIN.register(EntityRush::onPlayerJoin);
	}

	/** 进入世界（服务器启动）时读取之前保存的计时 */
	private static void onServerStarted(MinecraftServer server) {
		state = EntityRushState.get(server.overworld());
		LOGGER.info("Entity Rush 已加载，距离下次传送剩余 {} tick", state.getTicksRemaining());
	}

	/** 玩家进入世界时显示剩余时间 */
	private static void onPlayerJoin(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) {
		if (state == null || !EntityRushConfig.get().showRemainingOnJoin) {
			return;
		}
		syncInterval(); // 先同步间隔，确保显示正确的剩余时间
		int seconds = (state.getTicksRemaining() + 19) / 20;
		handler.getPlayer().sendSystemMessage(
			Component.literal("距离下次传送还剩 " + seconds + " 秒！").withStyle(ChatFormatting.GOLD)
		);
	}

	/** 每个服务端 tick 递减计时 */
	private static void onEndTick(MinecraftServer server) {
		if (state == null) {
			return;
		}

		if (syncInterval()) {
			return;
		}

		// 玩家处于旁观者模式时冻结倒计时
		ServerPlayer target = firstPlayer(server);
		if (target != null && target.isSpectator()) {
			return;
		}

		int ticks = state.getTicksRemaining() - 1;

		if (ticks <= 0) {
			// 倒计时结束：执行 tp，并重新开始下一轮循环
			int teleported = teleportAllToPlayer(server);
			state.setTicksRemaining(intervalTicks());
			if (EntityRushConfig.get().showTeleportMessage) {
				broadcast(server, "已将 " + teleported + " 个实体传送到你身边！");
			}
			return;
		}

		state.setTicksRemaining(ticks);

		// 整 10 秒处（50/40/30/20）广播，最后 10 秒每秒倒计时
		if (EntityRushConfig.get().showCountdown && ticks % 20 == 0) {
			int seconds = ticks / 20;
			if (seconds <= 10 || seconds % 10 == 0) {
				broadcast(server, "还剩 " + seconds + " 秒！");
			}
		}
	}

	/** 从配置读取当前的传送间隔（tick），至少 1 秒 */
	private static int intervalTicks() {
		return Math.max(1, EntityRushConfig.get().intervalSeconds) * 20;
	}

	/** 如果配置间隔变了，重置当前这一轮；返回是否发生了重置 */
	private static boolean syncInterval() {
		int configured = intervalTicks();
		if (state.getIntervalTicks() != configured) {
			state.setIntervalTicks(configured);
			state.setTicksRemaining(configured);
			return true;
		}
		return false;
	}

	/** 返回第一个在线玩家作为传送目标，无在线玩家返回 null */
	private static ServerPlayer firstPlayer(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			return player;
		}
		return null;
	}

	/** 根据配置，把所有应传送的实体传送到第一个在线玩家，返回传送数量 */
	private static int teleportAllToPlayer(MinecraftServer server) {
		ServerPlayer target = firstPlayer(server);
		if (target == null) {
			LOGGER.warn("没有在线玩家，跳过传送");
			return 0;
		}

		TeleportTransition transition = new TeleportTransition(
			target.level(),
			target.position(),
			target.getDeltaMovement(),
			target.getYRot(),
			target.getXRot(),
			TeleportTransition.DO_NOTHING
		);

		int teleported = 0;
		for (ServerLevel level : server.getAllLevels()) {
			// 先收集快照，避免遍历时 tp 移除实体导致迭代器返回 null
			List<Entity> snapshot = new ArrayList<>();
			for (Entity entity : level.getAllEntities()) {
				snapshot.add(entity);
			}
			for (Entity entity : snapshot) {
				if (entity == null || entity instanceof Player) {
					continue; // 永远不传送玩家
				}
				if (shouldTeleport(entity)) {
					entity.teleport(transition);
					teleported++;
				}
			}
		}
		return teleported;
	}

	/** 判断某实体是否应该被传送 */
	private static boolean shouldTeleport(Entity entity) {
		EntityRushConfig config = EntityRushConfig.get();
		String id = EntityType.getKey(entity.getType()).getPath();

		// 名单模式
		if ("list".equals(config.teleportMode)) {
			boolean inList = config.tpEntityList.contains(id);
			return config.tpUseWhitelist ? inList : !inList;
		}

		// 模板模式（默认）
		String category = EntityClassification.categoryOf(id);
		if (category == null) {
			return true; // 未知实体默认传送
		}
		if ("friendly".equals(category)) return config.tpFriendly;
		if ("neutral".equals(category)) return config.tpNeutral;
		if ("hostile".equals(category)) return config.tpHostile;
		if ("non_entity".equals(category)) return config.tpNonEntity;
		return true;
	}

	/** 在聊天栏广播消息 */
	private static void broadcast(MinecraftServer server, String message) {
		server.getPlayerList().broadcastSystemMessage(
			Component.literal(message).withStyle(ChatFormatting.GOLD),
			false
		);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
