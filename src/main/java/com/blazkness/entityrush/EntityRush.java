package com.blazkness.entityrush;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityRush implements ModInitializer {
	public static final String MOD_ID = "entity-rush";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** tp 间隔，单位 tick（60 秒 = 1200 tick） */
	private static final int INTERVAL_TICKS = 60 * 20;

	/** 当前世界的计时状态（随世界持久化） */
	private static EntityRushState state;

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(EntityRush::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> state = null);
		ServerTickEvents.END_SERVER_TICK.register(EntityRush::onEndTick);
	}

	/** 进入世界（服务器启动）时读取之前保存的计时 */
	private static void onServerStarted(MinecraftServer server) {
		state = EntityRushState.get(server.overworld());
		LOGGER.info("Entity Rush 已加载，距离下次传送剩余 {} tick", state.getTicksRemaining());
	}

	/** 每个服务端 tick 递减计时 */
	private static void onEndTick(MinecraftServer server) {
		if (state == null) {
			return;
		}

		int ticks = state.getTicksRemaining() - 1;

		if (ticks <= 0) {
			// 倒计时结束：执行 tp，并重新开始 60 秒循环
			teleportAllToPlayer(server);
			state.setTicksRemaining(INTERVAL_TICKS);
			broadcast(server, "所有实体已传送到你身边！");
			return;
		}

		state.setTicksRemaining(ticks);

		// 在整 10 秒处（50/40/30/20/10 秒）广播倒计时
		if (ticks % 20 == 0) {
			int seconds = ticks / 20;
			if (seconds % 10 == 0) {
				broadcast(server, "还剩" + seconds + "秒！");
			}
		}
	}

	/** 执行 /tp @e <playername>，把所有实体传送到玩家 */
	private static void teleportAllToPlayer(MinecraftServer server) {
		ServerPlayer target = null;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			target = player;
			break;
		}
		if (target == null) {
			LOGGER.warn("没有在线玩家，跳过传送");
			return;
		}

		String name = target.getGameProfile().name();
		String command = "tp @e " + name;
		try {
			server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
		} catch (Exception e) {
			LOGGER.error("执行传送命令失败: {}", command, e);
		}
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
