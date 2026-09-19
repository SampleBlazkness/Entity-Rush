package com.blazkness.entityrush;

import java.util.List;

/**
 * 设置解析层：决定当前生效的值来自世界设置（启用时）还是全局设置。
 * 所有读取设置的地方都应走这里，而不是直接读 {@link EntityRushConfig}。
 */
public class EntityRushSettings {
	private static boolean useWorld() {
		return EntityRushWorldConfig.isActive();
	}

	/** 模组总开关：关闭时倒计时冻结，不传送、不播音效或提示 */
	public static boolean moduleEnabled() {
		return useWorld() ? EntityRushWorldConfig.get().moduleEnabled : EntityRushConfig.get().moduleEnabled;
	}

	public static int intervalSeconds() {
		return useWorld() ? EntityRushWorldConfig.get().intervalSeconds : EntityRushConfig.get().intervalSeconds;
	}

	public static boolean showCountdown() {
		return useWorld() ? EntityRushWorldConfig.get().showCountdown : EntityRushConfig.get().showCountdown;
	}

	public static boolean showTeleportMessage() {
		return useWorld() ? EntityRushWorldConfig.get().showTeleportMessage : EntityRushConfig.get().showTeleportMessage;
	}

	public static boolean showRemainingOnJoin() {
		return useWorld() ? EntityRushWorldConfig.get().showRemainingOnJoin : EntityRushConfig.get().showRemainingOnJoin;
	}

	public static boolean countdownSound() {
		return useWorld() ? EntityRushWorldConfig.get().countdownSound : EntityRushConfig.get().countdownSound;
	}

	public static String countdownSoundType() {
		return useWorld() ? EntityRushWorldConfig.get().countdownSoundType : EntityRushConfig.get().countdownSoundType;
	}

	public static String countdownFinalSoundType() {
		return useWorld() ? EntityRushWorldConfig.get().countdownFinalSoundType : EntityRushConfig.get().countdownFinalSoundType;
	}

	public static String teleportSoundType() {
		return useWorld() ? EntityRushWorldConfig.get().teleportSoundType : EntityRushConfig.get().teleportSoundType;
	}

	public static String teleportMode() {
		return useWorld() ? EntityRushWorldConfig.get().teleportMode : EntityRushConfig.get().teleportMode;
	}

	public static boolean tpFriendly() {
		return useWorld() ? EntityRushWorldConfig.get().tpFriendly : EntityRushConfig.get().tpFriendly;
	}

	public static boolean tpNeutral() {
		return useWorld() ? EntityRushWorldConfig.get().tpNeutral : EntityRushConfig.get().tpNeutral;
	}

	public static boolean tpHostile() {
		return useWorld() ? EntityRushWorldConfig.get().tpHostile : EntityRushConfig.get().tpHostile;
	}

	public static boolean tpNonEntity() {
		return useWorld() ? EntityRushWorldConfig.get().tpNonEntity : EntityRushConfig.get().tpNonEntity;
	}

	public static boolean tpUseWhitelist() {
		return useWorld() ? EntityRushWorldConfig.get().tpUseWhitelist : EntityRushConfig.get().tpUseWhitelist;
	}

	public static List<String> tpEntityList() {
		return useWorld() ? EntityRushWorldConfig.get().tpEntityList : EntityRushConfig.get().tpEntityList;
	}
}
