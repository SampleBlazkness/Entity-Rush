package com.blazkness.entityrush.client;

import com.blazkness.entityrush.EntityClassification;
import me.shedaniel.clothconfig2.gui.entries.StringListListEntry;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 自定义的实体名单单元格：
 * 输入时按实体 id 前缀做单条自动补全（灰色提示剩余部分，Tab 接受），
 * 并为输入框设置提示文字。
 */
public class EntityListCell extends StringListListEntry.StringListCell {
	private static final List<String> ENTITY_IDS = EntityClassification.getAllEntityIds();

	public EntityListCell(String value, StringListListEntry list) {
		super(value, list);
		EditBox box = this.widget;
		if (box != null) {
			box.setHint(Component.translatable("text.entity-rush.cell_hint"));
			box.setResponder(text -> box.setSuggestion(findSuggestion(text)));
		}
	}

	/** 找第一个以输入为前缀的实体 id，返回剩余部分（供 setSuggestion 追加显示） */
	private static String findSuggestion(String input) {
		if (input == null || input.isEmpty()) {
			return "";
		}
		for (String id : ENTITY_IDS) {
			if (id.startsWith(input) && id.length() > input.length()) {
				return id.substring(input.length());
			}
		}
		return "";
	}
}
