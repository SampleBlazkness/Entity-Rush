package com.blazkness.entityrush;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * 随世界存档持久化的计时状态：记录距离下次传送还剩多少 tick。
 */
public class EntityRushState extends SavedData {
	/** 默认间隔：60 秒 = 1200 tick */
	private static final int DEFAULT_TICKS = 60 * 20;

	private int ticksRemaining;

	public EntityRushState() {
		this.ticksRemaining = DEFAULT_TICKS;
	}

	public EntityRushState(int ticksRemaining) {
		this.ticksRemaining = ticksRemaining;
	}

	private static final Codec<EntityRushState> CODEC = Codec.INT
		.xmap(EntityRushState::new, state -> state.ticksRemaining)
		.fieldOf("ticksRemaining")
		.codec();

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
}
