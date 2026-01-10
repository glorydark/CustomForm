package glorydark.nukkit.customform.scriptForms.data.execute_data.config;

import cn.nukkit.Player;
import cn.nukkit.utils.Config;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.utils.ConfigUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author glorydark
 * @date {2023/11/28} {22:32}
 */
@Getter
@AllArgsConstructor
public class ConfigModification {

    ConfigModificationType type;

    Object value;

    Object defaultValue;

    int configType;

    String configName;

    String keyName;

    public ConfigModification(int configType, String extraData, Object value, ConfigModificationType modificationType) {
        if (configType == 0) {
            this.keyName = extraData;
            this.value = value;
            this.type = modificationType;
        } else if (configType == 1) {
            this.configName = extraData;
            this.value = value;
            this.type = modificationType;
        }
        this.configType = configType;
    }

    // This operation is needed when using the operation ADD, DEDUCT
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void execute(Player player) {
        Config config;
        if (configType == 0) {
            config = new Config(ConfigUtils.getPlayerConfigCacheFile(player), Config.YAML);
            final String keyString = keyName;
            switch (type) {
                case SET:
                    config.set(keyString, value);
                    break;
                case ADD:
                    Object current = config.get(keyString, defaultValue);
                    if (value instanceof Double && current instanceof Double) {
                        config.set(keyString, (Double) current + (Double) value);
                    } else if (value instanceof Integer && current instanceof Integer) {
                        config.set(keyString, (Integer) current + (Integer) value);
                    } else if (value instanceof Float && current instanceof Float) {
                        config.set(keyString, (Float) current + (Float) value);
                    } else {
                        CustomFormMain.plugin.getLogger().error("Cannot execute config modification " +
                                "because the number is not a executable figure." +
                                "\nConfigType: " + configType + "\nConfigName: caches/players/" + player.getName() + ".yml\nKeyName: " + keyString);
                        return;
                    }
                    break;
                case DEDUCT:
                    current = config.get(keyString, defaultValue);
                    if (value instanceof Double && current instanceof Double) {
                        config.set(keyString, (Double) current - (Double) value);
                    } else if (value instanceof Integer && current instanceof Integer) {
                        config.set(keyString, (Integer) current - (Integer) value);
                    } else if (value instanceof Float && current instanceof Float) {
                        config.set(keyString, (Float) current - (Float) value);
                    } else {
                        CustomFormMain.plugin.getLogger().error("Cannot execute config modification " +
                                "because the number is not a executable figure." +
                                "\nConfigType: " + configType + "\nConfigName: caches/players/" + player.getName() + ".yml\nKeyName: " + keyString);
                        return;
                    }
                    break;
                case REMOVE:
                    config.remove(keyString);
                    break;
            }
            CustomFormMain.playerConfCaches.put(player.getName(), config.getAll());
            config.save();
        } else {
            config = new Config(ConfigUtils.getSpecificConfigCacheFile(configName), Config.YAML);
            final String keyString = player.getName();
            switch (type) {
                case SET:
                    config.set(keyString, value);
                    break;
                case ADD:
                    Object current = config.get(keyString, defaultValue);
                    if (value instanceof Double && current instanceof Double) {
                        config.set(keyString, (Double) current + (Double) value);
                    } else if (value instanceof Integer && current instanceof Integer) {
                        config.set(keyString, (Integer) current + (Integer) value);
                    } else if (value instanceof Float && current instanceof Float) {
                        config.set(keyString, (Float) current + (Float) value);
                    } else {
                        CustomFormMain.plugin.getLogger().error("Cannot execute config modification " +
                                "because the number is not a executable figure." +
                                "\nConfigType: " + configType + "\nConfigName: caches/specific/" + configName + ".yml\nKeyName: " + keyString);
                        return;
                    }
                    break;
                case DEDUCT:
                    current = config.get(keyString, defaultValue);
                    if (value instanceof Double && current instanceof Double) {
                        config.set(keyString, (Double) current - (Double) value);
                    } else if (value instanceof Integer && current instanceof Integer) {
                        config.set(keyString, (Integer) current - (Integer) value);
                    } else if (value instanceof Float && current instanceof Float) {
                        config.set(keyString, (Float) current - (Float) value);
                    } else {
                        CustomFormMain.plugin.getLogger().error("Cannot execute config modification " +
                                "because the number is not a executable figure." +
                                "\nConfigType: " + configType + "\nConfigName: caches/specific/" + configName + ".yml\nKeyName: " + keyString);
                        return;
                    }
                    break;
                case REMOVE:
                    config.remove(keyString);
                    break;
            }
            CustomFormMain.specificConfCaches.put(configName, config.getAll());
            config.save();
        }
    }
}
