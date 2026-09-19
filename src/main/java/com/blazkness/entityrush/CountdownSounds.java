package com.blazkness.entityrush;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/**
 * 倒计时提示音可选的音符盒音色。
 * 直接引用原版注册的音效（不打包任何音频文件）。
 */
public final class CountdownSounds {
	/** 一种可选音色：id（也是 sounds/ 里的文件名）、中文名、对应原版音效 */
	public record Type(String id, String label, Holder.Reference<SoundEvent> sound) {
	}

	private static final List<Type> TYPES = List.of(
		new Type("harp", "竖琴", SoundEvents.NOTE_BLOCK_HARP),
		new Type("bell", "钟", SoundEvents.NOTE_BLOCK_BELL),
		new Type("pling", "清脆", SoundEvents.NOTE_BLOCK_PLING),
		new Type("chime", "风铃", SoundEvents.NOTE_BLOCK_CHIME),
		new Type("bit", "8-bit 方波", SoundEvents.NOTE_BLOCK_BIT),
		new Type("banjo", "班卓琴", SoundEvents.NOTE_BLOCK_BANJO),
		new Type("bass", "贝斯", SoundEvents.NOTE_BLOCK_BASS),
		new Type("basedrum", "底鼓", SoundEvents.NOTE_BLOCK_BASEDRUM),
		new Type("cow_bell", "牛铃", SoundEvents.NOTE_BLOCK_COW_BELL),
		new Type("didgeridoo", "迪吉里杜管", SoundEvents.NOTE_BLOCK_DIDGERIDOO),
		new Type("flute", "长笛", SoundEvents.NOTE_BLOCK_FLUTE),
		new Type("guitar", "吉他", SoundEvents.NOTE_BLOCK_GUITAR),
		new Type("hat", "踩镲", SoundEvents.NOTE_BLOCK_HAT),
		new Type("iron_xylophone", "铁木琴", SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE),
		new Type("snare", "军鼓", SoundEvents.NOTE_BLOCK_SNARE),
		new Type("xylophone", "木琴", SoundEvents.NOTE_BLOCK_XYLOPHONE),
		new Type("trumpet", "铜号", SoundEvents.NOTE_BLOCK_TRUMPET),
		new Type("trumpet_exposed", "铜号（斑驳）", SoundEvents.NOTE_BLOCK_TRUMPET_EXPOSED),
		new Type("trumpet_weathered", "铜号（风化）", SoundEvents.NOTE_BLOCK_TRUMPET_WEATHERED),
		new Type("trumpet_oxidized", "铜号（氧化）", SoundEvents.NOTE_BLOCK_TRUMPET_OXIDIZED)
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

	/** 下拉框里显示的文本，形如「竖琴 (harp)」 */
	public static String labelOf(String id) {
		for (Type type : TYPES) {
			if (type.id().equals(id)) {
				return type.label() + " (" + type.id() + ")";
			}
		}
		return id;
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
