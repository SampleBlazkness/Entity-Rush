package com.blazkness.entityrush.client;

import com.blazkness.entityrush.CountdownSounds;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 「试听按钮 + 音色选择」合并成一行：左边是试听，右边是音色切换与重置。
 *
 * <p>Cloth Config 自带的 {@code SelectorBuilder} 没有留出在旁边挂按钮的扩展点，
 * 因此这里基于 {@link TooltipListEntry} 自己实现一个条目。布局刻意与
 * Cloth 自带的 {@code SelectionListEntry} 对齐（右侧 150px 区域放音色与重置），
 * 这样在列表里看起来和别的条目一致。
 *
 * <p>按钮文案走翻译键，音色名取自 {@link CountdownSounds#labelKey(String)}。
 */
public class SoundSelectorEntry extends TooltipListEntry<String> {

	/** 与 Cloth 自带选择器一致：右侧控件区总宽 */
	private static final int RIGHT_AREA = 150;
	/** 试听按钮宽度 */
	private static final int PREVIEW_WIDTH = 44;
	/**
	 * 重置按钮的文字。
	 *
	 * <p>直接复用 Cloth Config 自己的翻译键（简中「重置」/ 英文「Reset」，且它自带繁中），
	 * 不要再字面写死——否则切换语言时这里不会跟着变。
	 */
	private static final Component RESET_TEXT = Component.translatable("text.cloth-config.reset_value");

	private final List<String> ids = CountdownSounds.ids();
	private final Supplier<String> defaultValue;
	private final Consumer<String> saveConsumer;
	private final Button previewButton;
	private final Button valueButton;
	private final Button resetButton;
	private String value;

	// Cloth Config 自带的条目（BooleanListEntry / TextFieldListEntry 等）同样继承 TooltipListEntry
	// 并调用这个构造，当前版本没有非弃用的替代，因此这里显式抑制。
	@SuppressWarnings("deprecation")
	public SoundSelectorEntry(Component fieldName, String current, String defaultValue,
			Consumer<String> saveConsumer, Component tooltip) {
		super(fieldName, () -> Optional.of(new Component[]{tooltip}), true);
		this.value = current;
		this.defaultValue = () -> defaultValue;
		this.saveConsumer = saveConsumer;

		this.previewButton = Button.builder(Component.translatable("text.entity-rush.preview"), button -> preview())
			.bounds(0, 0, PREVIEW_WIDTH, 20)
			.build();

		// 宽度按文字实测计算，与 Cloth 自带的重置按钮同一做法
		this.resetButton = Button.builder(RESET_TEXT, button -> reset())
			.bounds(0, 0, Minecraft.getInstance().font.width(RESET_TEXT) + 6, 20)
			.build();

		this.valueButton = Button.builder(label(value), button -> cycle())
			.bounds(0, 0, RIGHT_AREA - resetButton.getWidth() - 2, 20)
			.build();
	}

	/** 音色按钮显示的文本（本地化名） */
	private static Component label(String id) {
		return Component.translatable(CountdownSounds.labelKey(id));
	}

	/** 在当前客户端试听选中的音色 */
	private void preview() {
		Minecraft.getInstance().getSoundManager()
			.play(SimpleSoundInstance.forUI(CountdownSounds.get(value), 1.0F, 1.0F));
	}

	/** 切换到下一个音色 */
	private void cycle() {
		int index = ids.indexOf(value);
		value = ids.get((Math.max(index, 0) + 1) % ids.size());
		valueButton.setMessage(label(value));
		saveConsumer.accept(value);
	}

	/** 回到默认音色 */
	private void reset() {
		value = defaultValue.get();
		valueButton.setMessage(label(value));
		saveConsumer.accept(value);
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public Optional<String> getDefaultValue() {
		return Optional.of(defaultValue.get());
	}

	@Override
	public boolean isEdited() {
		return !value.equals(defaultValue.get());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int index, int entryY, int entryX,
			int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
		// 注意：Cloth 这个方法的前两个坐标参数是 (y, x)，与直觉相反。
		// 依据：其自带的 SelectionListEntry 用第 2 个参数调 setY()，用第 3 个参数算 setX(第3参 + 第4参 - 宽度)。
		super.extractRenderState(extractor, index, entryY, entryX, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);

		boolean editable = isEditable();
		int rightEdge = entryX + entryWidth;
		int resetWidth = resetButton.getWidth();

		previewButton.active = editable;
		previewButton.setX(rightEdge - RIGHT_AREA - PREVIEW_WIDTH - 6);
		previewButton.setY(entryY);

		// 右侧 150px 区域：音色按钮 + 2px 间隙 + 重置按钮（布局与 Cloth 自带条目一致）
		valueButton.active = editable;
		valueButton.setX(rightEdge - RIGHT_AREA);
		valueButton.setWidth(RIGHT_AREA - resetWidth - 2);
		valueButton.setY(entryY);

		resetButton.active = editable && isEdited();
		resetButton.setX(rightEdge - resetWidth);
		resetButton.setY(entryY);

		// 标签：位置与 Cloth 自带条目一致（左对齐，垂直居中偏下 6px）
		Font font = Minecraft.getInstance().font;
		extractor.text(font, getDisplayedFieldName().getVisualOrderText(),
			entryX, entryY + 6, getPreferredTextColor());

		// 子控件由条目自己渲染（Cloth 的列表不会替我们画 children）
		previewButton.extractRenderState(extractor, mouseX, mouseY, delta);
		valueButton.extractRenderState(extractor, mouseX, mouseY, delta);
		resetButton.extractRenderState(extractor, mouseX, mouseY, delta);
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return List.of(previewButton, valueButton, resetButton);
	}

	@Override
	public List<? extends NarratableEntry> narratables() {
		return List.of(previewButton, valueButton, resetButton);
	}
}
