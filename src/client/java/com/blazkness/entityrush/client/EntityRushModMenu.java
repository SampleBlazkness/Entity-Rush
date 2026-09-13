package com.blazkness.entityrush.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu 集成：把 EntityRushConfigScreen 挂到 Mod Menu 的配置按钮上。
 */
public class EntityRushModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return EntityRushConfigScreen::create;
	}
}
