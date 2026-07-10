package dev.leonetic.features.gui.items.buttons;

import dev.leonetic.features.settings.Bind;
import dev.leonetic.features.settings.Setting;

import java.awt.Color;

public abstract class SettingButton<T> extends Button {
    protected final Setting<T> setting;

    protected SettingButton(Setting<T> setting) {
        super(setting.getName());
        this.setting = setting;
    }

    @Override
    public void update() {
        this.setHidden(!this.setting.isVisible());
    }

    @Override
    public String getPage() {
        return this.setting.getPage();
    }

    public Setting<T> getSetting() {
        return this.setting;
    }

    @SuppressWarnings("unchecked")
    public static SettingButton<?> create(Setting<?> setting) {
        if (setting.isColorSetting())              return new ColorButton((Setting<Color>) setting);
        if (setting.isNumberSetting() && setting.hasRestriction()) return new Slider((Setting<Number>) setting);
        if (setting.isEnumSetting())               return new EnumButton((Setting<Enum<?>>) setting);
        Object v = setting.getValue();
        if (v instanceof Boolean) return new BooleanButton((Setting<Boolean>) setting);
        if (v instanceof Bind)    return new BindButton((Setting<Bind>) setting);
        if (v instanceof String)  return new StringButton((Setting<String>) setting);
        return null;
    }
}
