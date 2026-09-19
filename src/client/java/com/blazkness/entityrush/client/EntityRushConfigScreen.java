package com.blazkness.entityrush.client;

import com.blazkness.entityrush.CountdownSounds;
import com.blazkness.entityrush.EntityClassification;
import com.blazkness.entityrush.EntityRush;
import com.blazkness.entityrush.EntityRushConfig;
import com.blazkness.entityrush.EntityRushWorldConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SelectorBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 用 Cloth Config 生成的配置界面，由 Mod Menu 的配置按钮打开。
 * 一级分类：「通用」「传送实体」「音效」（全局设置）、「当前世界」（特定世界设置，覆盖全局）。
 * 「当前世界」下再用子分类分成「通用」「传送实体」「音效」三组。
 */
public class EntityRushConfigScreen {
	/** 名单单元格校验（全局与世界共用） */
	private static Optional<Component> cellError(String value) {
		if (value.isEmpty()) {
			return Optional.of(Component.literal("名单不能为空"));
		}
		if (!EntityClassification.getAllEntityIds().contains(value)) {
			return Optional.of(Component.literal("未知实体：" + value));
		}
		return Optional.empty();
	}

	/** 整份名单校验（全局与世界共用） */
	private static Optional<Component> listError(List<String> list) {
		return list.stream().anyMatch(String::isEmpty)
			? Optional.of(Component.literal("存在空的名单，请填写或删除"))
			: Optional.empty();
	}

	/** 本世界启用了独立设置时，在该栏目顶部提醒，避免误以为在这里改了就生效 */
	private static void addOverrideNotice(ConfigEntryBuilder entry, ConfigCategory category, EntityRushWorldConfig world) {
		if (world == null || !world.enabled) {
			return;
		}
		category.addEntry(entry.startTextDescription(
			Component.literal("⚠ 本世界已启用独立的特定世界设置").withStyle(ChatFormatting.GOLD)).build());
		category.addEntry(entry.startTextDescription(
			Component.literal("此处修改不影响本世界，详见「当前世界」栏目").withStyle(ChatFormatting.GRAY)).build());
	}

	/** 一个音色选择器（全局与世界共用），prefix 为空时是全局项 */
	private static SelectorBuilder<String> soundSelector(ConfigEntryBuilder entry, String label, String prefix,
														 String current, String defaultValue, String tooltip,
														 Consumer<String> saveConsumer) {
		return entry.startSelector(Component.literal(label), CountdownSounds.ids().toArray(new String[0]), current)
			.setDefaultValue(defaultValue)
			.setTooltip(Component.literal(prefix + tooltip))
			.setSaveConsumer(saveConsumer);
	}

	public static Screen create(Screen parent) {
		Minecraft mc = Minecraft.getInstance();
		EntityRushWorldConfig world = EntityRushWorldConfig.get();

		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(parent)
			.setTitle(Component.literal("Entity Rush 设置"))
			.setSavingRunnable(() -> {
				EntityRushConfig.save();
				// 单人世界下把世界设置写进 <世界文件夹>/data/entity-rush.json
				if (mc.hasSingleplayerServer()) {
					EntityRushWorldConfig.save(mc.getSingleplayerServer());
				}
			});

		ConfigEntryBuilder entry = builder.entryBuilder();
		EntityRushConfig config = EntityRushConfig.get();

		EntityRush.LOGGER.info("打开配置界面：world={} enabled={}", world != null, world != null && world.enabled);

		// ===== 全局：「通用」 =====
		ConfigCategory general = builder.getOrCreateCategory(Component.literal("通用"));
		addOverrideNotice(entry, general, world);

		general.addEntry(entry.startBooleanToggle(Component.literal("模组开关"), config.moduleEnabled)
			.setDefaultValue(true)
			.setTooltip(Component.literal("关闭后倒计时冻结，不传送、不播音效或提示；在世界内改开后立即生效"))
			.setSaveConsumer(value -> config.moduleEnabled = value)
			.build());

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
			.setTooltip(Component.literal("传送执行时是否在聊天栏提示，并播放传送提示音"))
			.setSaveConsumer(value -> config.showTeleportMessage = value)
			.build());

		general.addEntry(entry.startBooleanToggle(Component.literal("进入时显示剩余时间"), config.showRemainingOnJoin)
			.setDefaultValue(true)
			.setTooltip(Component.literal("进入世界时在聊天栏显示距离下次传送的剩余时间（不播放提示音）"))
			.setSaveConsumer(value -> config.showRemainingOnJoin = value)
			.build());

		// ===== 全局：「传送实体」 =====
		ConfigCategory teleport = builder.getOrCreateCategory(Component.literal("传送实体"));
		addOverrideNotice(entry, teleport, world);

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
			.setCellErrorSupplier(EntityRushConfigScreen::cellError)
			.setErrorSupplier(EntityRushConfigScreen::listError)
			.setSaveConsumer(value -> config.tpEntityList = new ArrayList<>(value))
			.build());

		// ===== 全局：「音效」 =====
		ConfigCategory sound = builder.getOrCreateCategory(Component.literal("音效"));
		addOverrideNotice(entry, sound, world);

		sound.addEntry(entry.startBooleanToggle(Component.literal("倒计时提示音"), config.countdownSound)
			.setDefaultValue(true)
			.setTooltip(Component.literal("每次广播「还剩 X 秒」时是否播放提示音（进入世界的那句提示不播）"))
			.setSaveConsumer(value -> config.countdownSound = value)
			.build());

		sound.addEntry(soundSelector(entry, "大于 10 秒的音效", "", config.countdownSoundType,
				CountdownSounds.DEFAULT, "每次广播「还剩 X 秒」时播放（X 大于 10）",
				value -> config.countdownSoundType = value)
			.build());

		sound.addEntry(soundSelector(entry, "10 秒以内的音效", "", config.countdownFinalSoundType,
				CountdownSounds.DEFAULT_FINAL, "最后 10 秒每秒播放（X 小于等于 10）",
				value -> config.countdownFinalSoundType = value)
			.build());

		sound.addEntry(soundSelector(entry, "传送提示音效", "", config.teleportSoundType,
				CountdownSounds.DEFAULT_TELEPORT, "实体传送完成并提示时播放（需开启「传送提示」）",
				value -> config.teleportSoundType = value)
			.build());

		// ===== 当前世界（特定世界设置，覆盖全局） =====
		ConfigCategory worldCategory = builder.getOrCreateCategory(Component.literal("当前世界"));

		if (world == null) {
			String message = mc.level == null
				? "尚未进入世界，无法使用特定世界设置。"
				: "当前为多人游戏，特定世界设置仅在单人世界可用。";
			worldCategory.addEntry(entry.startTextDescription(Component.literal(message)).build());
		} else {
			// --- 子分类：「通用」 ---
			SubCategoryBuilder worldGeneral = entry.startSubCategory(Component.literal("通用"));
			worldGeneral.setExpanded(true);

			worldGeneral.add(entry.startBooleanToggle(Component.literal("启用特定世界设置"), world.enabled)
				.setDefaultValue(false)
				.setTooltip(Component.literal("开启后，本世界使用下面的设置覆盖全局设置；关闭则完全跟随全局"))
				.setSaveConsumer(value -> world.enabled = value)
				.build());

			worldGeneral.add(entry.startBooleanToggle(Component.literal("模组开关"), world.moduleEnabled)
				.setDefaultValue(true)
				.setTooltip(Component.literal("覆盖全局：关闭后本世界倒计时冻结，不传送、不播音效或提示"))
				.setSaveConsumer(value -> world.moduleEnabled = value)
				.build());

			worldGeneral.add(entry.startIntField(Component.literal("倒计时间隔（秒）"), world.intervalSeconds)
				.setDefaultValue(60)
				.setMin(10)
				.setMax(3600)
				.setTooltip(Component.literal("覆盖全局：每隔多少秒执行一次传送"))
				.setSaveConsumer(value -> world.intervalSeconds = value)
				.build());

			worldGeneral.add(entry.startBooleanToggle(Component.literal("倒计时提示"), world.showCountdown)
				.setDefaultValue(true)
				.setTooltip(Component.literal("覆盖全局：是否在聊天栏显示「还剩 X 秒」倒计时"))
				.setSaveConsumer(value -> world.showCountdown = value)
				.build());

			worldGeneral.add(entry.startBooleanToggle(Component.literal("传送提示"), world.showTeleportMessage)
				.setDefaultValue(true)
				.setTooltip(Component.literal("覆盖全局：传送执行时是否在聊天栏提示，并播放传送提示音"))
				.setSaveConsumer(value -> world.showTeleportMessage = value)
				.build());

			worldGeneral.add(entry.startBooleanToggle(Component.literal("进入时显示剩余时间"), world.showRemainingOnJoin)
				.setDefaultValue(true)
				.setTooltip(Component.literal("覆盖全局：进入世界时在聊天栏显示距离下次传送的剩余时间（不播放提示音）"))
				.setSaveConsumer(value -> world.showRemainingOnJoin = value)
				.build());

			worldCategory.addEntry(worldGeneral.build());

			// --- 子分类：「传送实体」 ---
			SubCategoryBuilder worldTeleport = entry.startSubCategory(Component.literal("传送实体"));
			worldTeleport.setExpanded(true);

			worldTeleport.add(entry.startBooleanToggle(Component.literal("名单模式"), "list".equals(world.teleportMode))
				.setDefaultValue(false)
				.setTooltip(Component.literal("覆盖全局：关=模板多选；开=名单模式"))
				.setSaveConsumer(value -> world.teleportMode = value ? "list" : "template")
				.build());

			worldTeleport.add(entry.startBooleanToggle(Component.literal("传送友好生物"), world.tpFriendly)
				.setDefaultValue(true)
				.setTooltip(Component.literal("覆盖全局：模板模式是否传送友好生物"))
				.setSaveConsumer(value -> world.tpFriendly = value)
				.build());

			worldTeleport.add(entry.startBooleanToggle(Component.literal("传送中立生物"), world.tpNeutral)
				.setDefaultValue(true)
				.setTooltip(Component.literal("覆盖全局：模板模式是否传送中立生物"))
				.setSaveConsumer(value -> world.tpNeutral = value)
				.build());

			worldTeleport.add(entry.startBooleanToggle(Component.literal("传送敌对生物"), world.tpHostile)
				.setDefaultValue(true)
				.setTooltip(Component.literal("覆盖全局：模板模式是否传送敌对生物"))
				.setSaveConsumer(value -> world.tpHostile = value)
				.build());

			worldTeleport.add(entry.startBooleanToggle(Component.literal("传送非生物实体"), world.tpNonEntity)
				.setDefaultValue(true)
				.setTooltip(Component.literal("覆盖全局：模板模式是否传送非生物实体"))
				.setSaveConsumer(value -> world.tpNonEntity = value)
				.build());

			worldTeleport.add(entry.startBooleanToggle(Component.literal("白名单模式"), world.tpUseWhitelist)
				.setDefaultValue(false)
				.setTooltip(Component.literal("覆盖全局：开=白名单（只传名单内），关=黑名单（排除名单内）"))
				.setSaveConsumer(value -> world.tpUseWhitelist = value)
				.build());

			worldTeleport.add(entry.startStrList(Component.literal("实体名单"), world.tpEntityList)
				.setTooltip(Component.literal("覆盖全局：名单模式的实体 id，每行一个，输入时自动补全"))
				.setCreateNewInstance(list -> new EntityListCell("", list))
				.setDefaultValue(() -> new ArrayList<>())
				.setCellErrorSupplier(EntityRushConfigScreen::cellError)
				.setErrorSupplier(EntityRushConfigScreen::listError)
				.setSaveConsumer(value -> world.tpEntityList = new ArrayList<>(value))
				.build());

			worldCategory.addEntry(worldTeleport.build());

			// --- 子分类：「音效」 ---
			SubCategoryBuilder worldSound = entry.startSubCategory(Component.literal("音效"));
			worldSound.setExpanded(true);

			worldSound.add(entry.startBooleanToggle(Component.literal("倒计时提示音"), world.countdownSound)
				.setDefaultValue(true)
				.setTooltip(Component.literal("覆盖全局：每次广播「还剩 X 秒」时是否播放提示音"))
				.setSaveConsumer(value -> world.countdownSound = value)
				.build());

			worldSound.add(soundSelector(entry, "大于 10 秒的音效", "覆盖全局：", world.countdownSoundType,
					CountdownSounds.DEFAULT, "每次广播「还剩 X 秒」时播放（X 大于 10）",
					value -> world.countdownSoundType = value)
				.build());

			worldSound.add(soundSelector(entry, "10 秒以内的音效", "覆盖全局：", world.countdownFinalSoundType,
					CountdownSounds.DEFAULT_FINAL, "最后 10 秒每秒播放（X 小于等于 10）",
					value -> world.countdownFinalSoundType = value)
				.build());

			worldSound.add(soundSelector(entry, "传送提示音效", "覆盖全局：", world.teleportSoundType,
					CountdownSounds.DEFAULT_TELEPORT, "实体传送完成并提示时播放（需开启「传送提示」）",
					value -> world.teleportSoundType = value)
				.build());

			worldCategory.addEntry(worldSound.build());
		}

		return builder.build();
	}
}
