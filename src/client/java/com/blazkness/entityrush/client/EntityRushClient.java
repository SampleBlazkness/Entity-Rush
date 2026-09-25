package com.blazkness.entityrush.client;

import com.blazkness.entityrush.EntityRush;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;
import java.util.Locale;

/**
 * 客户端初始化：注册打开配置界面的快捷键。
 *
 * <p>不装 Mod Menu 的玩家也能进设置，因此这里自己提供一个可改键的入口。
 * 若检测到没有 Mod Menu，会在进入世界后用红色提示快捷键（按键名取自按键绑定，改键后提示内容会跟着变），
 * 并播放系统错误音。
 */
public class EntityRushClient implements ClientModInitializer {

	/** 按键分类（显示名取自 lang 里的 key.category.entity-rush.main） */
	public static final KeyMapping.Category CATEGORY =
		KeyMapping.Category.register(Identifier.fromNamespaceAndPath("entity-rush", "main"));

	/** 打开配置界面，默认 F12，可在「选项 → 按键绑定」里修改 */
	public static final KeyMapping OPEN_CONFIG = KeyMappingHelper.registerKeyMapping(new KeyMapping(
		"key.entity-rush.open_config",
		InputConstants.Type.KEYBOARD,
		InputConstants.KEY_F12,
		CATEGORY
	));

	/** Windows 标准错误音（「关键停止」），比走 SystemSounds.Hand 更可控 */
	private static final String ERROR_SOUND_PATH = "C:\\Windows\\Media\\Windows Critical Stop.wav";

	/** 预加载好的错误音，常驻内存；播放时只需 start()，约 1 毫秒返回 */
	private static Clip errorSound;

	/** 没有 Mod Menu 的提示只发一次 */
	private static boolean missingModMenuNotified = false;

	@Override
	public void onInitializeClient() {
		// 提前加载音频（约 180ms），这样真正播放时没有延迟
		preloadErrorSound();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (OPEN_CONFIG.consumeClick()) {
				client.setScreenAndShow(EntityRushConfigScreen.create(client.gui.screen()));
			}
			notifyIfModMenuMissing(client);
		});
	}

	/** 进入世界后检查一次 Mod Menu；缺失时用红色提示，并播放系统错误音 */
	private static void notifyIfModMenuMissing(Minecraft client) {
		if (missingModMenuNotified || client.level == null || client.player == null) {
			return;
		}
		missingModMenuNotified = true;
		if (FabricLoader.getInstance().isModLoaded("modmenu")) {
			return;
		}
		// 按键名用 getTranslatedKeyMessage() 取，玩家改键后这里会跟着变
		client.player.sendSystemMessage(
			Component.translatable("text.entity-rush.no_modmenu", OPEN_CONFIG.getTranslatedKeyMessage())
				.withStyle(ChatFormatting.RED)
		);
		playErrorSound();
	}

	/**
	 * 预加载错误音。一次性开销约 180ms，放在客户端初始化阶段，玩家感知不到。
	 *
	 * <p>之所以不用 PowerShell 调 SystemSounds.Hand：那样每次播放都要新建进程（Windows 上
	 * CreateProcess 会被杀毒软件扫描），延迟约 1 秒；而且它播的是注册表里 SystemHand 当前绑定的
	 * 文件，用户的设置未必是标准错误音。直接读 Windows 自带的 wav 更准也更快。
	 */
	private static void preloadErrorSound() {
		if (!isWindows()) {
			return;
		}
		File wav = new File(ERROR_SOUND_PATH);
		if (!wav.isFile()) {
			EntityRush.LOGGER.warn("未找到系统错误音，将跳过播放：{}", ERROR_SOUND_PATH);
			return;
		}
		try (AudioInputStream in = AudioSystem.getAudioInputStream(wav)) {
			Clip clip = AudioSystem.getClip();
			clip.open(in);
			errorSound = clip;
		} catch (Exception e) {
			EntityRush.LOGGER.warn("预加载系统错误音失败，将跳过播放", e);
		}
	}

	/** 播放错误音：回到开头再 start，异步，约 1 毫秒返回，不阻塞主线程 */
	private static void playErrorSound() {
		Clip clip = errorSound;
		if (clip == null) {
			return;
		}
		clip.setFramePosition(0);
		clip.start();
	}

	private static boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
	}
}
