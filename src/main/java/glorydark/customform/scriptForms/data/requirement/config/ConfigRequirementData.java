package glorydark.customform.scriptForms.data.requirement.config;

import cn.nukkit.Player;
import cn.nukkit.utils.Config;
import glorydark.customform.CustomFormMain;
import glorydark.customform.utils.ConfigUtils;

import java.util.List;

public class ConfigRequirementData {

    ConfigRequirementType requirementType;

    int configType;

    String configName;

    String keyString;

    Object comparedValue;

    List<String> failed_messages;


    public ConfigRequirementData(int configType, ConfigRequirementType requirementType, String inputExtra, Object comparedValue, List<String> failed_messages) {
        if (configType == 0) {
            this.keyString = inputExtra;
            this.requirementType = requirementType;
            this.comparedValue = comparedValue;
            this.failed_messages = failed_messages;
        } else if (configType == 1) {
            this.requirementType = requirementType;
            this.configName = inputExtra;
            this.comparedValue = comparedValue;
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
                return new Config(ConfigUtils.getConfig(player), Config.YAML).exists(keyString);
            } else if (configType == 1) {
                return new Config(ConfigUtils.getConfig(configName), Config.YAML).exists(player.getName());
            }
            return false;
        }
        if (comparedValue instanceof Double) {
            final double defaultValue = Double.NaN;
            double convertedCompared = Double.parseDouble(comparedValue.toString());
            double configValue = defaultValue;
            if (configType == 0) {
                configValue = new Config(ConfigUtils.getConfig(player), Config.YAML).getDouble(keyString, defaultValue);
            } else if (configType == 1) {
                configValue = new Config(ConfigUtils.getConfig(configName), Config.YAML).getDouble(player.getName(), defaultValue);
            }
            if (!Double.isNaN(configValue)) {
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
            }
            return false;
        } else if (comparedValue instanceof String) {
            if (requirementType.equals(ConfigRequirementType.EQUAL)) {
                final String defaultValue = "undefined_value";
                String compared = comparedValue.toString();
                String configValue = defaultValue;
                if (configType == 0) {
                    configValue = new Config(ConfigUtils.getConfig(player), Config.YAML).getString(keyString, defaultValue);
                } else if (configType == 1) {
                    configValue = new Config(ConfigUtils.getConfig(configName), Config.YAML).getString(player.getName(), defaultValue);
                }
                if (!configValue.equalsIgnoreCase(defaultValue)) {
                    return compared.equals(configValue);
                }
                return false;
            }
        } else if (comparedValue instanceof Integer) {
            final int defaultValue = Integer.MIN_VALUE;
            int configValue = defaultValue;
            int convertedCompared = Integer.parseInt(comparedValue.toString());
            if (configType == 0) {
                configValue = new Config(ConfigUtils.getConfig(player), Config.YAML).getInt(keyString, defaultValue);
            } else if (configType == 1) {
                configValue = new Config(ConfigUtils.getConfig(configName), Config.YAML).getInt(player.getName(), defaultValue);
            }
            if (configValue != Integer.MIN_VALUE) {
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
            }
            return false;
        }
        return false;
    }

    public void sendFailedMsg(Player player) {
        if (failed_messages.size() == 0) {
            return;
        }
        for (String msg : failed_messages) {
            player.sendMessage(msg.replace("%player%", player.getName()));
        }
    }

}
