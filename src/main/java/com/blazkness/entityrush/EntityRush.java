package com.blazkness.entityrush;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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
	/** 进入世界的「剩余时间提示」是否还没发出去（模组关闭期间不消耗，打开后补发一次） */
	private static boolean joinNoticePending = false;

	@Override
	public void onInitialize() {
		EntityRushConfig.load();

		ServerLifecycleEvents.SERVER_STARTED.register(EntityRush::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			state = null;
			EntityRushWorldConfig.unload();
		});
		ServerTickEvents.END_SERVER_TICK.register(EntityRush::onEndTick);
		ServerPlayConnectionEvents.JOIN.register(EntityRush::onPlayerJoin);
	}

	/** 进入世界（服务器启动）时读取之前保存的计时 */
	private static void onServerStarted(MinecraftServer server) {
		EntityRushWorldConfig.load(server);
		state = EntityRushState.get(server.overworld());
		LOGGER.info("Entity Rush 已加载，距离下次传送剩余 {} tick", state.getTicksRemaining());
	}

	/** 玩家进入世界时把「剩余时间提示」标记为待发；实际在 onEndTick 里发（模组打开时才发，每次进入只发一次） */
	private static void onPlayerJoin(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) {
		if (state == null) {
			return;
		}
		joinNoticePending = true;
	}

	/** 每个服务端 tick 递减计时 */
	private static void onEndTick(MinecraftServer server) {
		if (state == null) {
			return;
		}

		// 模组关闭：倒计时冻结，不传送、不播音效或提示
		if (!EntityRushSettings.moduleEnabled()) {
			return;
		}

		// 进入世界的剩余时间提示：每次进入只发一次（模组关闭时不消耗，打开后才补发）。
		// 放在计时同步检查之前——那一步在传送间隔变化时会直接 return，会把提示一起跳过，
		// 导致提示晚一帧才出现（50ms）。
		if (joinNoticePending && EntityRushSettings.showRemainingOnJoin()) {
			int seconds = (state.getTicksRemaining() + 19) / 20;
			broadcast(server, "message.entity-rush.join_remaining", seconds);
			joinNoticePending = false;
		}

		if (syncInterval()) {
			return;
		}

		// 玩家处于旁观者模式或已死亡时冻结倒计时
		ServerPlayer target = firstPlayer(server);
		if (target != null && (target.isSpectator() || target.isDeadOrDying())) {
			return;
		}

		int ticks = state.getTicksRemaining() - 1;

		if (ticks <= 0) {
			// 倒计时结束：执行 tp，并重新开始下一轮循环
			int teleported = teleportAllToPlayer(server);
			state.setTicksRemaining(intervalTicks());
			if (EntityRushSettings.showTeleportMessage()) {
				broadcast(server, "message.entity-rush.teleported", teleported);
				playToAll(server, CountdownSounds.get(EntityRushSettings.teleportSoundType()));
			}
			return;
		}

		state.setTicksRemaining(ticks);

		// 整 10 秒处（50/40/30/20）广播，最后 10 秒每秒倒计时
		if (EntityRushSettings.showCountdown() && ticks % 20 == 0) {
			int seconds = ticks / 20;
			if (seconds <= 10 || seconds % 10 == 0) {
				broadcast(server, "message.entity-rush.countdown", seconds);
			}
		}

		// 提示音与文字广播解耦：大于 10 秒跟着 10 秒节点走；10 秒以内越接近归零越密，制造紧迫感
		if (EntityRushSettings.countdownSound()) {
			int seconds = ticks / 20;
			if (seconds > 10) {
				if (ticks % 20 == 0 && seconds % 10 == 0) {
					playToAll(server, CountdownSounds.get(EntityRushSettings.countdownSoundType()));
				}
			} else {
				// 10~6 秒：每 0.5 秒；5~3 秒：每 0.25 秒；最后 2 秒：每 0.1 秒
				int gap = seconds > 5 ? 10 : (seconds > 2 ? 5 : 2);
				if (ticks % gap == 0) {
					playToAll(server, CountdownSounds.get(EntityRushSettings.countdownFinalSoundType()));
				}
			}
		}
	}

	/** 从配置读取当前的传送间隔（tick），至少 1 秒 */
	private static int intervalTicks() {
		return Math.max(1, EntityRushSettings.intervalSeconds()) * 20;
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
		String id = EntityType.getKey(entity.getType()).getPath();

		// 名单模式
		if ("list".equals(EntityRushSettings.teleportMode())) {
			boolean inList = EntityRushSettings.tpEntityList().contains(id);
			return EntityRushSettings.tpUseWhitelist() ? inList : !inList;
		}

		// 模板模式（默认）
		String category = EntityClassification.categoryOf(id);
		if (category == null) {
			return true; // 未知实体默认传送
		}
		if ("friendly".equals(category)) return EntityRushSettings.tpFriendly();
		if ("neutral".equals(category)) return EntityRushSettings.tpNeutral();
		if ("hostile".equals(category)) return EntityRushSettings.tpHostile();
		if ("non_entity".equals(category)) return EntityRushSettings.tpNonEntity();
		return true;
	}

	/** 在聊天栏广播消息（文案取自语言文件） */
	private static void broadcast(MinecraftServer server, String key, Object... args) {
		server.getPlayerList().broadcastSystemMessage(
			Component.translatable(key, args).withStyle(ChatFormatting.GOLD),
			false
		);
	}

	/** 给每个玩家发一次音效包（用 Player.playSound 会把自己排除掉，所以直接发包） */
	private static void playToAll(MinecraftServer server, SoundEvent sound) {
		Holder<SoundEvent> holder = Holder.direct(sound);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			player.connection.send(new ClientboundSoundPacket(
				holder,
				SoundSource.MASTER,
				player.getX(), player.getY(), player.getZ(),
				1.0F, 1.0F,
				player.getRandom().nextLong()
			));
		}
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
