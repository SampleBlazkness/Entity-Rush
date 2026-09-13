package com.blazkness.entityrush.client;

import com.blazkness.entityrush.EntityClassification;
import com.blazkness.entityrush.EntityRushConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Optional;

/**
 * 用 Cloth Config 生成的配置界面，由 Mod Menu 的配置按钮打开。
 */
public class EntityRushConfigScreen {
	public static Screen create(Screen parent) {
		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(parent)
			.setTitle(Component.literal("Entity Rush 设置"))
			.setSavingRunnable(EntityRushConfig::save);

		ConfigEntryBuilder entry = builder.entryBuilder();
		EntityRushConfig config = EntityRushConfig.get();

		// ===== 通用 =====
		ConfigCategory general = builder.getOrCreateCategory(Component.literal("通用"));

		general.addEntry(entry.startIntField(Component.literal("倒计时间隔（秒）"), config.intervalSeconds)
			.setDefaultValue(60)
			.setMin(10)
			.setMax(3600)
			.setTooltip(Component.literal("每隔多少秒执行一次传送"))
			.setSaveConsumer(value -> config.intervalSeconds = value)
			.build());

		general.addEntry(entry.startBooleanToggle(Component.literal("倒计时提示"), config.showCountdown)
			.setDefaultValue(true)
			.setTooltip(Component.literal("是否在聊天栏显示「还剩 X 秒」倒计时"))
			.setSaveConsumer(value -> config.showCountdown = value)
			.build());

		general.addEntry(entry.startBooleanToggle(Component.literal("传送提示"), config.showTeleportMessage)
			.setDefaultValue(true)
			.setTooltip(Component.literal("传送执行时是否在聊天栏提示"))
			.setSaveConsumer(value -> config.showTeleportMessage = value)
			.build());

		general.addEntry(entry.startBooleanToggle(Component.literal("进入时显示剩余时间"), config.showRemainingOnJoin)
			.setDefaultValue(true)
			.setTooltip(Component.literal("进入世界时在聊天栏显示距离下次传送的剩余时间"))
			.setSaveConsumer(value -> config.showRemainingOnJoin = value)
			.build());

		// ===== 传送实体 =====
		ConfigCategory teleport = builder.getOrCreateCategory(Component.literal("传送实体"));

		teleport.addEntry(entry.startBooleanToggle(Component.literal("名单模式"), "list".equals(config.teleportMode))
			.setDefaultValue(false)
			.setTooltip(Component.literal("关=模板多选（勾选友好/中立/敌对/非生物）；开=名单模式（白名单/黑名单）"))
			.setSaveConsumer(value -> config.teleportMode = value ? "list" : "template")
			.build());

		teleport.addEntry(entry.startBooleanToggle(Component.literal("传送友好生物"), config.tpFriendly)
			.setDefaultValue(true)
			.setTooltip(Component.literal("模板模式：是否传送友好生物（猪、牛、羊等）"))
			.setSaveConsumer(value -> config.tpFriendly = value)
			.build());

		teleport.addEntry(entry.startBooleanToggle(Component.literal("传送中立生物"), config.tpNeutral)
			.setDefaultValue(true)
			.setTooltip(Component.literal("模板模式：是否传送中立生物（末影人、蜘蛛、狼等）"))
			.setSaveConsumer(value -> config.tpNeutral = value)
			.build());

		teleport.addEntry(entry.startBooleanToggle(Component.literal("传送敌对生物"), config.tpHostile)
			.setDefaultValue(true)
			.setTooltip(Component.literal("模板模式：是否传送敌对生物（僵尸、骷髅、苦力怕等）"))
			.setSaveConsumer(value -> config.tpHostile = value)
			.build());

		teleport.addEntry(entry.startBooleanToggle(Component.literal("传送非生物实体"), config.tpNonEntity)
			.setDefaultValue(true)
			.setTooltip(Component.literal("模板模式：是否传送非生物实体（矿车、掉落物、投射物等）"))
			.setSaveConsumer(value -> config.tpNonEntity = value)
			.build());

		teleport.addEntry(entry.startBooleanToggle(Component.literal("白名单模式"), config.tpUseWhitelist)
			.setDefaultValue(false)
			.setTooltip(Component.literal("名单模式：开=白名单（只传名单内实体），关=黑名单（排除名单内实体）"))
			.setSaveConsumer(value -> config.tpUseWhitelist = value)
			.build());

		teleport.addEntry(entry.startStrList(Component.literal("实体名单"), config.tpEntityList)
			.setTooltip(Component.literal("名单模式的实体 id，每行一个（如 skeleton、pig），输入时自动补全"))
			.setCreateNewInstance(list -> new EntityListCell("", list))
			.setDefaultValue(() -> new ArrayList<>())
			.setCellErrorSupplier(value -> {
				if (value.isEmpty()) {
					return Optional.of(Component.literal("名单不能为空"));
				}
				if (!EntityClassification.getAllEntityIds().contains(value)) {
					return Optional.of(Component.literal("未知实体：" + value));
				}
				return Optional.empty();
			})
			.setErrorSupplier(list -> list.stream().anyMatch(String::isEmpty)
				? Optional.of(Component.literal("存在空的名单，请填写或删除"))
				: Optional.empty())
			.setSaveConsumer(value -> config.tpEntityList = new ArrayList<>(value))
			.build());

		return builder.build();
	}
}
