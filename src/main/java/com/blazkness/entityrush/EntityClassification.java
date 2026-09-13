package com.blazkness.entityrush;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 从 entity-classification.json 加载实体四分类（友好/中立/敌对/非生物），
 * 供模板模式判断某实体类型是否应该被传送。
 */
public class EntityClassification {
	private static final Map<String, Set<String>> CATEGORIES = load();

	private static Map<String, Set<String>> load() {
		try (Reader reader = new InputStreamReader(
				EntityClassification.class.getResourceAsStream("/entity-classification.json"))) {
			Type type = new TypeToken<Map<String, Set<String>>>() {}.getType();
			return new Gson().fromJson(reader, type);
		} catch (Exception e) {
			EntityRush.LOGGER.error("加载实体分类表失败", e);
			return Collections.emptyMap();
		}
	}

	/** 返回实体 id 所属分类（friendly/neutral/hostile/non_entity），未知返回 null */
	public static String categoryOf(String entityId) {
		for (Map.Entry<String, Set<String>> entry : CATEGORIES.entrySet()) {
			if (entry.getValue().contains(entityId)) {
				return entry.getKey();
			}
		}
		return null;
	}

	/** 返回所有已分类实体 id 的排序列表（用于自动补全候选） */
	public static List<String> getAllEntityIds() {
		Set<String> all = new HashSet<>();
		for (Set<String> ids : CATEGORIES.values()) {
			all.addAll(ids);
		}
		List<String> list = new ArrayList<>(all);
		Collections.sort(list);
		return list;
	}
}
