package com.blazkness.entityrush;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * 随世界存档持久化的计时状态。
 * ticksRemaining：距离下次传送还剩多少 tick。
 * intervalTicks：这一轮所用的间隔（用于检测配置是否被修改）。
 */
public class EntityRushState extends SavedData {
	/** 默认间隔：60 秒 = 1200 tick */
	private static final int DEFAULT_TICKS = 60 * 20;

	private int ticksRemaining;
	private int intervalTicks;

	public EntityRushState() {
		this(DEFAULT_TICKS, DEFAULT_TICKS);
	}

	public EntityRushState(int ticksRemaining, int intervalTicks) {
		this.ticksRemaining = ticksRemaining;
		this.intervalTicks = intervalTicks;
	}

	private static final Codec<EntityRushState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.fieldOf("ticksRemaining").forGetter(s -> s.ticksRemaining),
		Codec.INT.optionalFieldOf("intervalTicks", DEFAULT_TICKS).forGetter(s -> s.intervalTicks)
	).apply(instance, EntityRushState::new));

	private static final SavedDataType<EntityRushState> TYPE = new SavedDataType<>(
		Identifier.fromNamespaceAndPath(EntityRush.MOD_ID, "entity_rush_state"),
		EntityRushState::new,
		CODEC,
		DataFixTypes.LEVEL
	);

	/** 获取（不存在则创建）该世界对应的计时状态 */
	public static EntityRushState get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	public int getTicksRemaining() {
		return ticksRemaining;
	}

	public void setTicksRemaining(int ticksRemaining) {
		this.ticksRemaining = ticksRemaining;
		setDirty();
	}

	public int getIntervalTicks() {
		return intervalTicks;
	}

	public void setIntervalTicks(int intervalTicks) {
		this.intervalTicks = intervalTicks;
		setDirty();
	}
}
