package com.blazkness.entityrush.client.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 禁止单人游戏对局域网开放。
 * 拦截 IntegratedServer.publishServer 的两个重载（暂停菜单按钮和 /publish 命令最终都走这里），
 * 给玩家发一条明确的提示后返回 false（发布失败），从而阻止局域网开放。
 */
@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin {
	@Inject(
		method = "publishServer(Lnet/minecraft/server/MinecraftServer$MultiplayerScope;ZI)Z",
		at = @At("HEAD"),
		cancellable = true
	)
	private void entityrush$cancelPublishFull(CallbackInfoReturnable<Boolean> cir) {
		entityrush$notifyBlocked();
		cir.setReturnValue(false);
	}

	@Inject(
		method = "publishServer(Lnet/minecraft/server/MinecraftServer$MultiplayerScope;I)Z",
		at = @At("HEAD"),
		cancellable = true
	)
	private void entityrush$cancelPublishShort(CallbackInfoReturnable<Boolean> cir) {
		entityrush$notifyBlocked();
		cir.setReturnValue(false);
	}

	/** 给所有在线玩家发一条明确的提示，说明局域网被模组禁用 */
	private void entityrush$notifyBlocked() {
		MinecraftServer server = (MinecraftServer) (Object) this;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			player.sendSystemMessage(
				Component.translatable("message.entity-rush.lan_blocked").withStyle(ChatFormatting.RED)
			);
		}
	}
}
