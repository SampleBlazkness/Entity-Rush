package com.blazkness.entityrush;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity Rush 的全局配置，持久化到 config/entity-rush.json。
 * 客户端（Cloth Config 界面）负责修改，服务端逻辑负责读取。
 */
public class EntityRushConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** 传送间隔，单位秒 */
	public int intervalSeconds = 60;
	/** 是否在聊天栏显示「还剩X秒」的倒计时提示 */
	public boolean showCountdown = true;
	/** 传送执行时是否在聊天栏提示 */
	public boolean showTeleportMessage = true;
	/** 进入世界时是否显示剩余时间 */
	public boolean showRemainingOnJoin = true;

	/** 传送模式："template"（模板多选）或 "list"（名单） */
	public String teleportMode = "template";
	/** 模板模式：是否传送友好生物 */
	public boolean tpFriendly = true;
	/** 模板模式：是否传送中立生物 */
	public boolean tpNeutral = true;
	/** 模板模式：是否传送敌对生物 */
	public boolean tpHostile = true;
	/** 模板模式：是否传送非生物实体 */
	public boolean tpNonEntity = true;
	/** 名单模式：true=白名单（只传名单内），false=黑名单（排除名单内） */
	public boolean tpUseWhitelist = false;
	/** 名单模式的实体 id 列表 */
	public List<String> tpEntityList = new ArrayList<>();

	private static EntityRushConfig instance = new EntityRushConfig();

	public static EntityRushConfig get() {
		return instance;
	}

	private static Path configFile() {
		return FabricLoader.getInstance().getConfigDir().resolve("entity-rush.json");
	}

	public static void load() {
		Path file = configFile();
		if (!Files.exists(file)) {
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(file)) {
			instance = GSON.fromJson(reader, EntityRushConfig.class);
		} catch (Exception e) {
			EntityRush.LOGGER.error("读取配置失败，回退到默认值", e);
			instance = new EntityRushConfig();
		}
	}

	public static void save() {
		try (Writer writer = Files.newBufferedWriter(configFile())) {
			GSON.toJson(instance, writer);
		} catch (Exception e) {
			EntityRush.LOGGER.error("保存配置失败", e);
		}
	}
}
