package com.blazkness.entityrush;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 特定世界设置：每个世界独立一份，存在 &lt;世界文件夹&gt;/data/entity-rush.json。
 * 启用后完全覆盖全局设置（{@link EntityRushConfig}）；关闭时世界跟随全局设置。
 */
public class EntityRushWorldConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** 是否启用特定世界设置（关闭时完全跟随全局设置） */
	public boolean enabled = false;

	/** 模组总开关（覆盖全局） */
	public boolean moduleEnabled = true;

	// ===== 以下字段与全局设置一一对应，启用后覆盖全局 =====
	public int intervalSeconds = 60;
	public boolean showCountdown = true;
	public boolean showTeleportMessage = true;
	public boolean showRemainingOnJoin = true;
	public boolean countdownSound = true;
	public String countdownSoundType = CountdownSounds.DEFAULT;
	public String countdownFinalSoundType = CountdownSounds.DEFAULT_FINAL;
	public String teleportSoundType = CountdownSounds.DEFAULT_TELEPORT;
	public String teleportMode = "template";
	public boolean tpFriendly = true;
	public boolean tpNeutral = true;
	public boolean tpHostile = true;
	public boolean tpNonEntity = true;
	public boolean tpUseWhitelist = false;
	public List<String> tpEntityList = new ArrayList<>();

	/** 当前已加载的世界设置；未进入世界时为 null */
	private static volatile EntityRushWorldConfig instance;

	public static EntityRushWorldConfig get() {
		return instance;
	}

	/** 当前是否应使用世界设置（已加载且已启用） */
	public static boolean isActive() {
		return instance != null && instance.enabled;
	}

	private static Path file(MinecraftServer server) {
		return server.getWorldPath(LevelResource.DATA).resolve("entity-rush.json");
	}

	public static void load(MinecraftServer server) {
		Path f = file(server);
		if (!Files.exists(f)) {
			instance = new EntityRushWorldConfig();
			return;
		}
		try (Reader reader = Files.newBufferedReader(f)) {
			instance = GSON.fromJson(reader, EntityRushWorldConfig.class);
		} catch (Exception e) {
			EntityRush.LOGGER.error("读取世界设置失败，回退到默认值", e);
			instance = new EntityRushWorldConfig();
		}
		if (instance == null) {
			instance = new EntityRushWorldConfig();
		}
		EntityRush.LOGGER.info("已加载世界设置：enabled={}", instance.enabled);
	}

	public static void save(MinecraftServer server) {
		if (instance == null) {
			return;
		}
		try (Writer writer = Files.newBufferedWriter(file(server))) {
			GSON.toJson(instance, writer);
		} catch (Exception e) {
			EntityRush.LOGGER.error("保存世界设置失败", e);
		}
	}

	/** 世界卸载时清空 */
	public static void unload() {
		instance = null;
	}
}
