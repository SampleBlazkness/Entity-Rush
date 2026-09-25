package com.blazkness.entityrush;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/**
 * 倒计时提示音可选的音符盒音色。
 * 直接引用原版注册的音效（不打包任何音频文件）。
 * 显示名走翻译键（sound.entity-rush.&lt;id&gt;），见 assets/entity-rush/lang/。
 */
public final class CountdownSounds {
	/** 一种可选音色：id（也是 sounds/ 里的文件名）与对应原版音效 */
	public record Type(String id, Holder.Reference<SoundEvent> sound) {
	}

	private static final List<Type> TYPES = List.of(
		new Type("harp", SoundEvents.NOTE_BLOCK_HARP),
		new Type("bell", SoundEvents.NOTE_BLOCK_BELL),
		new Type("pling", SoundEvents.NOTE_BLOCK_PLING),
		new Type("chime", SoundEvents.NOTE_BLOCK_CHIME),
		new Type("bit", SoundEvents.NOTE_BLOCK_BIT),
		new Type("banjo", SoundEvents.NOTE_BLOCK_BANJO),
		new Type("bass", SoundEvents.NOTE_BLOCK_BASS),
		new Type("basedrum", SoundEvents.NOTE_BLOCK_BASEDRUM),
		new Type("cow_bell", SoundEvents.NOTE_BLOCK_COW_BELL),
		new Type("didgeridoo", SoundEvents.NOTE_BLOCK_DIDGERIDOO),
		new Type("flute", SoundEvents.NOTE_BLOCK_FLUTE),
		new Type("guitar", SoundEvents.NOTE_BLOCK_GUITAR),
		new Type("hat", SoundEvents.NOTE_BLOCK_HAT),
		new Type("iron_xylophone", SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE),
		new Type("snare", SoundEvents.NOTE_BLOCK_SNARE),
		new Type("xylophone", SoundEvents.NOTE_BLOCK_XYLOPHONE),
		new Type("trumpet", SoundEvents.NOTE_BLOCK_TRUMPET),
		new Type("trumpet_exposed", SoundEvents.NOTE_BLOCK_TRUMPET_EXPOSED),
		new Type("trumpet_weathered", SoundEvents.NOTE_BLOCK_TRUMPET_WEATHERED),
		new Type("trumpet_oxidized", SoundEvents.NOTE_BLOCK_TRUMPET_OXIDIZED)
	);

	/** 默认音色 id：大于 10 秒时 */
	public static final String DEFAULT = "pling";
	/** 默认音色 id：10 秒以内（含 10 秒） */
	public static final String DEFAULT_FINAL = "hat";
	/** 默认音色 id：传送提示 */
	public static final String DEFAULT_TELEPORT = "chime";

	/** 全部可选音色 id（按界面显示顺序） */
	public static List<String> ids() {
		return TYPES.stream().map(Type::id).toList();
	}

	/** 音色显示名对应的翻译键 */
	public static String labelKey(String id) {
		return "sound.entity-rush." + id;
	}

	/** 按 id 取音效；未知 id 回退到默认音色 */
	public static SoundEvent get(String id) {
		for (Type type : TYPES) {
			if (type.id().equals(id)) {
				return type.sound().value();
			}
		}
		return SoundEvents.NOTE_BLOCK_PLING.value();
	}

	private CountdownSounds() {
	}
}
