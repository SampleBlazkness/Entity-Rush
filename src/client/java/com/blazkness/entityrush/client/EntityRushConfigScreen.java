package com.blazkness.entityrush.client;

import com.blazkness.entityrush.CountdownSounds;
import com.blazkness.entityrush.EntityClassification;
import com.blazkness.entityrush.EntityRush;
import com.blazkness.entityrush.EntityRushConfig;
import com.blazkness.entityrush.EntityRushWorldConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
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
 * 用 Cloth Config 生成的配置界面，由 Mod Menu 的配置按钮或快捷键打开。
 *
 * <p>一级分类只有两个：「全局设置」与「当前世界」（特定世界设置，启用时覆盖全局）。
 * 两者结构完全对称，各自都由「通用」「传送实体」「音效」三个子分类组成，
 * 且由同一套 helper 生成，因此外观与交互保证一致。
 *
 * <p>界面文案一律走翻译键（见 assets/entity-rush/lang/），不在代码里写死文本。
 */
public class EntityRushConfigScreen {

	// ==================================================================
	// 翻译键
	// ==================================================================
	private static final String KEY_TITLE = "text.entity-rush.config.title";
	private static final String KEY_CAT_GLOBAL = "category.entity-rush.global";
	private static final String KEY_CAT_WORLD = "category.entity-rush.world";
	private static final String KEY_CAT_GENERAL = "category.entity-rush.general";
	private static final String KEY_CAT_TELEPORT = "category.entity-rush.teleport";
	private static final String KEY_CAT_SOUND = "category.entity-rush.sound";
	private static final String KEY_OVERRIDE_PREFIX = "text.entity-rush.override_prefix";
	private static final String KEY_OVERRIDE_WARNING = "text.entity-rush.world_override_warning";
	private static final String KEY_OVERRIDE_HINT = "text.entity-rush.world_override_hint";
	private static final String KEY_NO_WORLD = "text.entity-rush.no_world";
	private static final String KEY_MULTIPLAYER = "text.entity-rush.multiplayer";
	private static final String KEY_ENABLE_OVERRIDE = "option.entity-rush.enable_world_override";

	/** 「覆盖全局：」前缀拼在提示前；前缀本身也来自语言文件 */
	private static Component tooltip(boolean override, String key) {
		return override
			? Component.translatable(KEY_OVERRIDE_PREFIX).append(Component.translatable(key))
			: Component.translatable(key);
	}

	/** 名单单元格校验（全局与世界共用） */
	private static Optional<Component> cellError(String value) {
		if (value.isEmpty()) {
			return Optional.of(Component.translatable("text.entity-rush.error.empty_cell"));
		}
		if (!EntityClassification.getAllEntityIds().contains(value)) {
			return Optional.of(Component.translatable("text.entity-rush.error.unknown_entity", value));
		}
		return Optional.empty();
	}

	/** 整份名单校验（全局与世界共用） */
	private static Optional<Component> listError(List<String> list) {
		return list.stream().anyMatch(String::isEmpty)
			? Optional.of(Component.translatable("text.entity-rush.error.empty_list"))
			: Optional.empty();
	}

	/** 本世界启用了独立设置时，在「全局设置」栏目顶部提醒，避免误以为在这里改了就生效 */
	private static void addOverrideNotice(ConfigEntryBuilder entry, ConfigCategory category, EntityRushWorldConfig world) {
		if (world == null || !world.enabled) {
			return;
		}
		category.addEntry(entry.startTextDescription(
			Component.translatable(KEY_OVERRIDE_WARNING).withStyle(ChatFormatting.GOLD)).build());
		category.addEntry(entry.startTextDescription(
			Component.translatable(KEY_OVERRIDE_HINT).withStyle(ChatFormatting.GRAY)).build());
	}

	// ==================================================================
	// 条目构造 helper：全局与世界共用，保证两边外观与交互完全一致
	// ==================================================================

	/** 布尔开关 */
	private static void addBool(SubCategoryBuilder cat, ConfigEntryBuilder entry, String labelKey,
			String tooltipKey, boolean override, boolean value, boolean defaultValue, Consumer<Boolean> save) {
		cat.add(entry.startBooleanToggle(Component.translatable(labelKey), value)
			.setDefaultValue(defaultValue)
			.setTooltip(tooltip(override, tooltipKey))
			.setSaveConsumer(save)
			.build());
	}

	/** 整数输入 */
	private static void addInt(SubCategoryBuilder cat, ConfigEntryBuilder entry, String labelKey,
			String tooltipKey, boolean override, int value, int defaultValue, int min, int max, Consumer<Integer> save) {
		cat.add(entry.startIntField(Component.translatable(labelKey), value)
			.setDefaultValue(defaultValue)
			.setMin(min)
			.setMax(max)
			.setTooltip(tooltip(override, tooltipKey))
			.setSaveConsumer(save)
			.build());
	}

	/** 实体名单（带单元格补全与校验） */
	private static void addEntityList(SubCategoryBuilder cat, ConfigEntryBuilder entry, String labelKey,
			String tooltipKey, boolean override, List<String> value, Consumer<List<String>> save) {
		cat.add(entry.startStrList(Component.translatable(labelKey), value)
			.setTooltip(tooltip(override, tooltipKey))
			.setCreateNewInstance(list -> new EntityListCell("", list))
			.setDefaultValue(ArrayList::new)
			.setCellErrorSupplier(EntityRushConfigScreen::cellError)
			.setErrorSupplier(EntityRushConfigScreen::listError)
			.setSaveConsumer(save)
			.build());
	}

	/** 音色选择器（左侧试听按钮 + 音色切换 + 重置） */
	private static void addSoundSelector(SubCategoryBuilder cat, ConfigEntryBuilder entry, String labelKey,
			String tooltipKey, boolean override, String current, String defaultValue, Consumer<String> save) {
		cat.add(new SoundSelectorEntry(Component.translatable(labelKey), current, defaultValue, save,
			tooltip(override, tooltipKey)));
	}

	/**
	 * 三个子分类的内容。所有读写都通过传入的回调完成，因此全局与世界走的是同一份构建代码，
	 * {@code override} 只决定提示是否加「覆盖全局：」前缀。
	 */
	private static void buildGeneral(SubCategoryBuilder cat, ConfigEntryBuilder entry, boolean override,
			boolean moduleEnabled, Consumer<Boolean> setModuleEnabled,
			int intervalSeconds, Consumer<Integer> setIntervalSeconds,
			boolean showCountdown, Consumer<Boolean> setShowCountdown,
			boolean showTeleportMessage, Consumer<Boolean> setShowTeleportMessage,
			boolean showRemainingOnJoin, Consumer<Boolean> setShowRemainingOnJoin) {
		addBool(cat, entry, "option.entity-rush.module_enabled", "option.entity-rush.module_enabled.tooltip",
			override, moduleEnabled, true, setModuleEnabled);
		addInt(cat, entry, "option.entity-rush.interval", "option.entity-rush.interval.tooltip",
			override, intervalSeconds, 60, 10, 3600, setIntervalSeconds);
		addBool(cat, entry, "option.entity-rush.show_countdown", "option.entity-rush.show_countdown.tooltip",
			override, showCountdown, true, setShowCountdown);
		addBool(cat, entry, "option.entity-rush.show_teleport_message", "option.entity-rush.show_teleport_message.tooltip",
			override, showTeleportMessage, true, setShowTeleportMessage);
		addBool(cat, entry, "option.entity-rush.show_remaining_on_join", "option.entity-rush.show_remaining_on_join.tooltip",
			override, showRemainingOnJoin, true, setShowRemainingOnJoin);
	}

	private static void buildTeleport(SubCategoryBuilder cat, ConfigEntryBuilder entry, boolean override,
			String teleportMode, Consumer<String> setTeleportMode,
			boolean tpFriendly, Consumer<Boolean> setTpFriendly,
			boolean tpNeutral, Consumer<Boolean> setTpNeutral,
			boolean tpHostile, Consumer<Boolean> setTpHostile,
			boolean tpNonEntity, Consumer<Boolean> setTpNonEntity,
			boolean tpUseWhitelist, Consumer<Boolean> setTpUseWhitelist,
			List<String> tpEntityList, Consumer<List<String>> setTpEntityList) {
		addBool(cat, entry, "option.entity-rush.list_mode", "option.entity-rush.list_mode.tooltip",
			override, "list".equals(teleportMode), false,
			value -> setTeleportMode.accept(value ? "list" : "template"));
		addBool(cat, entry, "option.entity-rush.tp_friendly", "option.entity-rush.tp_friendly.tooltip",
			override, tpFriendly, true, setTpFriendly);
		addBool(cat, entry, "option.entity-rush.tp_neutral", "option.entity-rush.tp_neutral.tooltip",
			override, tpNeutral, true, setTpNeutral);
		addBool(cat, entry, "option.entity-rush.tp_hostile", "option.entity-rush.tp_hostile.tooltip",
			override, tpHostile, true, setTpHostile);
		addBool(cat, entry, "option.entity-rush.tp_non_entity", "option.entity-rush.tp_non_entity.tooltip",
			override, tpNonEntity, true, setTpNonEntity);
		addBool(cat, entry, "option.entity-rush.tp_use_whitelist", "option.entity-rush.tp_use_whitelist.tooltip",
			override, tpUseWhitelist, false, setTpUseWhitelist);
		addEntityList(cat, entry, "option.entity-rush.tp_entity_list", "option.entity-rush.tp_entity_list.tooltip",
			override, tpEntityList, setTpEntityList);
	}

	private static void buildSound(SubCategoryBuilder cat, ConfigEntryBuilder entry, boolean override,
			boolean countdownSound, Consumer<Boolean> setCountdownSound,
			String countdownSoundType, Consumer<String> setCountdownSoundType,
			String countdownFinalSoundType, Consumer<String> setCountdownFinalSoundType,
			String teleportSoundType, Consumer<String> setTeleportSoundType) {
		addBool(cat, entry, "option.entity-rush.countdown_sound", "option.entity-rush.countdown_sound.tooltip",
			override, countdownSound, true, setCountdownSound);
		addSoundSelector(cat, entry, "option.entity-rush.sound_over_10", "option.entity-rush.sound_over_10.tooltip",
			override, countdownSoundType, CountdownSounds.DEFAULT, setCountdownSoundType);
		addSoundSelector(cat, entry, "option.entity-rush.sound_within_10", "option.entity-rush.sound_within_10.tooltip",
			override, countdownFinalSoundType, CountdownSounds.DEFAULT_FINAL, setCountdownFinalSoundType);
		addSoundSelector(cat, entry, "option.entity-rush.sound_teleport", "option.entity-rush.sound_teleport.tooltip",
			override, teleportSoundType, CountdownSounds.DEFAULT_TELEPORT, setTeleportSoundType);
	}

	private static SubCategoryBuilder sub(ConfigEntryBuilder entry, String nameKey) {
		SubCategoryBuilder builder = entry.startSubCategory(Component.translatable(nameKey));
		builder.setExpanded(true);
		return builder;
	}

	public static Screen create(Screen parent) {
		Minecraft mc = Minecraft.getInstance();
		EntityRushWorldConfig world = EntityRushWorldConfig.get();

		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(parent)
			.setTitle(Component.translatable(KEY_TITLE))
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

		// ===== 栏目一：全局设置 =====
		ConfigCategory globalCategory = builder.getOrCreateCategory(Component.translatable(KEY_CAT_GLOBAL));
		addOverrideNotice(entry, globalCategory, world);

		SubCategoryBuilder globalGeneral = sub(entry, KEY_CAT_GENERAL);
		buildGeneral(globalGeneral, entry, false,
			config.moduleEnabled, value -> config.moduleEnabled = value,
			config.intervalSeconds, value -> config.intervalSeconds = value,
			config.showCountdown, value -> config.showCountdown = value,
			config.showTeleportMessage, value -> config.showTeleportMessage = value,
			config.showRemainingOnJoin, value -> config.showRemainingOnJoin = value);
		globalCategory.addEntry(globalGeneral.build());

		SubCategoryBuilder globalTeleport = sub(entry, KEY_CAT_TELEPORT);
		buildTeleport(globalTeleport, entry, false,
			config.teleportMode, value -> config.teleportMode = value,
			config.tpFriendly, value -> config.tpFriendly = value,
			config.tpNeutral, value -> config.tpNeutral = value,
			config.tpHostile, value -> config.tpHostile = value,
			config.tpNonEntity, value -> config.tpNonEntity = value,
			config.tpUseWhitelist, value -> config.tpUseWhitelist = value,
			config.tpEntityList, value -> config.tpEntityList = new ArrayList<>(value));
		globalCategory.addEntry(globalTeleport.build());

		SubCategoryBuilder globalSound = sub(entry, KEY_CAT_SOUND);
		buildSound(globalSound, entry, false,
			config.countdownSound, value -> config.countdownSound = value,
			config.countdownSoundType, value -> config.countdownSoundType = value,
			config.countdownFinalSoundType, value -> config.countdownFinalSoundType = value,
			config.teleportSoundType, value -> config.teleportSoundType = value);
		globalCategory.addEntry(globalSound.build());

		// ===== 栏目二：当前世界（特定世界设置，覆盖全局）=====
		ConfigCategory worldCategory = builder.getOrCreateCategory(Component.translatable(KEY_CAT_WORLD));

		if (world == null) {
			worldCategory.addEntry(entry.startTextDescription(
				Component.translatable(mc.level == null ? KEY_NO_WORLD : KEY_MULTIPLAYER)).build());
		} else {
			SubCategoryBuilder worldGeneral = sub(entry, KEY_CAT_GENERAL);
			worldGeneral.add(entry.startBooleanToggle(Component.translatable(KEY_ENABLE_OVERRIDE), world.enabled)
				.setDefaultValue(false)
				.setTooltip(Component.translatable(KEY_ENABLE_OVERRIDE + ".tooltip"))
				.setSaveConsumer(value -> world.enabled = value)
				.build());
			buildGeneral(worldGeneral, entry, true,
				world.moduleEnabled, value -> world.moduleEnabled = value,
				world.intervalSeconds, value -> world.intervalSeconds = value,
				world.showCountdown, value -> world.showCountdown = value,
				world.showTeleportMessage, value -> world.showTeleportMessage = value,
				world.showRemainingOnJoin, value -> world.showRemainingOnJoin = value);
			worldCategory.addEntry(worldGeneral.build());

			SubCategoryBuilder worldTeleport = sub(entry, KEY_CAT_TELEPORT);
			buildTeleport(worldTeleport, entry, true,
				world.teleportMode, value -> world.teleportMode = value,
				world.tpFriendly, value -> world.tpFriendly = value,
				world.tpNeutral, value -> world.tpNeutral = value,
				world.tpHostile, value -> world.tpHostile = value,
				world.tpNonEntity, value -> world.tpNonEntity = value,
				world.tpUseWhitelist, value -> world.tpUseWhitelist = value,
				world.tpEntityList, value -> world.tpEntityList = new ArrayList<>(value));
			worldCategory.addEntry(worldTeleport.build());

			SubCategoryBuilder worldSound = sub(entry, KEY_CAT_SOUND);
			buildSound(worldSound, entry, true,
				world.countdownSound, value -> world.countdownSound = value,
				world.countdownSoundType, value -> world.countdownSoundType = value,
				world.countdownFinalSoundType, value -> world.countdownFinalSoundType = value,
				world.teleportSoundType, value -> world.teleportSoundType = value);
			worldCategory.addEntry(worldSound.build());
		}

		return builder.build();
	}
}
