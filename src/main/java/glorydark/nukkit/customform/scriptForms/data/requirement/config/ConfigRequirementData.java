package glorydark.nukkit.customform.scriptForms.data.requirement.config;

import cn.nukkit.Player;
import cn.nukkit.utils.Config;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.utils.ConfigUtils;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.List;

@ToString
public class ConfigRequirementData {

    ConfigRequirementType requirementType;

    int configType;

    String configName;

    String keyString;

    Object comparedValue;

    List<String> failed_messages;

    Object defaultComparedValue;

    public ConfigRequirementData(int configType, ConfigRequirementType requirementType, String inputExtra, Object comparedValue, Object defaultComparedValue, List<String> failed_messages) {
        if (configType == 0) {
            this.keyString = inputExtra;
            this.requirementType = requirementType;
            this.comparedValue = comparedValue;
            this.defaultComparedValue = defaultComparedValue;
            this.failed_messages = failed_messages;
        } else if (configType == 1) {
            this.requirementType = requirementType;
            this.configName = inputExtra;
            this.comparedValue = comparedValue;
            this.defaultComparedValue = defaultComparedValue;
            this.failed_messages = failed_messages;
        }
        this.configType = configType;
    }

    public int getConfigType() {
        return configType;
    }

    public ConfigRequirementType getRequirementType() {
        return requirementType;
    }

    public Object getComparedValue() {
        return comparedValue;
    }

    public String getConfigName() {
        return configName;
    }

    /*
        This method is to check whether player is qualified or not.
        Variable 'type' is used to select the comparing type.
        * This method is designed specially for Tips!
     */
    public boolean isQualified(Player player) {
        if (requirementType == ConfigRequirementType.EXIST) {
            if (configType == 0) {
                return new Config(ConfigUtils.getPlayerConfigCacheFile(player), Config.YAML).exists(keyString);
            } else if (configType == 1) {
                return new Config(ConfigUtils.getSpecificConfigCacheFile(configName), Config.YAML).exists(player.getName());
            }
            return false;
        }
        if (comparedValue instanceof Double) {
            final double defaultValue = defaultComparedValue == null ? 0d : Double.parseDouble(this.defaultComparedValue.toString());
            double convertedCompared = Double.parseDouble(comparedValue.toString());
            double configValue = defaultValue;
            if (configType == 0) {
                configValue = Double.parseDouble(String.valueOf(CustomFormMain.playerConfCaches.getOrDefault(player.getName(), new LinkedHashMap<>()).getOrDefault(keyString, 0)));
            } else if (configType == 1) {
                configValue = Double.parseDouble(String.valueOf(CustomFormMain.specificConfCaches.getOrDefault(configName, new LinkedHashMap<>()).getOrDefault(player.getName(), 0)));
            }
            switch (requirementType) {
                case EQUAL:
                    return configValue == convertedCompared;
                case BIGGER:
                    return configValue > convertedCompared;
                case BIGGER_OR_EQUAL:
                    return configValue >= convertedCompared;
                case SMALLER:
                    return configValue < convertedCompared;
                case SMALLER_OR_EQUAL:
                    return configValue <= convertedCompared;
            }
            return false;
        } else if (comparedValue instanceof String) {
            if (requirementType.equals(ConfigRequirementType.EQUAL)) {
                final String defaultValue = defaultComparedValue == null ? "undefined_value" : defaultComparedValue.toString();
                String compared = comparedValue.toString();
                String configValue = defaultValue;
                if (configType == 0) {
                    configValue = (String) CustomFormMain.playerConfCaches.getOrDefault(player.getName(), new LinkedHashMap<>()).getOrDefault(keyString, 0);
                } else if (configType == 1) {
                    configValue = (String) CustomFormMain.specificConfCaches.getOrDefault(configName, new LinkedHashMap<>()).getOrDefault(player.getName(), 0);
                }
                if (!configValue.equalsIgnoreCase(defaultValue)) {
                    return compared.equals(configValue);
                }
                return false;
            }
        } else if (comparedValue instanceof Integer) {
            final int defaultValue = defaultComparedValue == null ? 0 : Integer.parseInt(defaultComparedValue.toString());
            int configValue = defaultValue;
            int convertedCompared = Integer.parseInt(comparedValue.toString());
            if (configType == 0) {
                configValue = (int) CustomFormMain.playerConfCaches.getOrDefault(player.getName(), new LinkedHashMap<>()).getOrDefault(keyString, 0);
            } else if (configType == 1) {
                configValue = (int) CustomFormMain.specificConfCaches.getOrDefault(configName, new LinkedHashMap<>()).getOrDefault(player.getName(), 0);
            }
            switch (requirementType) {
                case EQUAL:
                    return configValue == convertedCompared;
                case BIGGER:
                    return configValue > convertedCompared;
                case BIGGER_OR_EQUAL:
                    return configValue >= convertedCompared;
                case SMALLER:
                    return configValue < convertedCompared;
                case SMALLER_OR_EQUAL:
                    return configValue <= convertedCompared;
            }
            return false;
        }
        return false;
    }

    public void sendFailedMsg(Player player) {
        if (failed_messages.isEmpty()) {
            return;
        }
        for (String msg : failed_messages) {
            player.sendMessage(msg.replace("%player%", player.getName()));
        }
    }

}
