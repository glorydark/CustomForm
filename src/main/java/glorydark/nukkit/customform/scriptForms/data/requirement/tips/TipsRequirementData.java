package glorydark.nukkit.customform.scriptForms.data.requirement.tips;

import cn.nukkit.Player;
import glorydark.nukkit.customform.utils.MathCompareSign;
import lombok.ToString;
import tip.utils.Api;

import java.util.List;

@ToString
public class TipsRequirementData {

    MathCompareSign requirementType;

    String identifier;

    Object comparedValue;

    String displayName;

    List<String> failed_messages;

    public TipsRequirementData(MathCompareSign requirementType, String identifier, Object comparedValue, String displayName, List<String> failed_messages) {
        this.requirementType = requirementType;
        this.identifier = identifier;
        this.comparedValue = comparedValue;
        this.displayName = displayName;
        this.failed_messages = failed_messages;
    }

    public MathCompareSign getRequirementType() {
        return requirementType;
    }

    public Object getComparedValue() {
        return comparedValue;
    }

    public String getIdentifier() {
        return identifier;
    }

    /*
        This method is to check whether player is qualified or not.
        Variable 'type' is used to select the comparing type.
        * This method is designed specially for Tips!
     */
    public boolean isQualified(Player player) {
        if (comparedValue instanceof Double) {
            double convertedCompared = Double.parseDouble(comparedValue.toString());
            double tipsValue = Double.parseDouble(Api.strReplace(identifier, player));
            switch (requirementType) {
                case EQUAL:
                    return tipsValue == convertedCompared;
                case BIGGER:
                    return tipsValue > convertedCompared;
                case BIGGER_OR_EQUAL:
                    return tipsValue >= convertedCompared;
                case SMALLER:
                    return tipsValue < convertedCompared;
                case SMALLER_OR_EQUAL:
                    return tipsValue <= convertedCompared;
            }
        } else if (comparedValue instanceof String) {
            if (requirementType.equals(MathCompareSign.EQUAL)) {
                String compared = comparedValue.toString();
                String tipsValue = Api.strReplace(identifier, player);
                return compared.equals(tipsValue);
            }
        } else if (comparedValue instanceof Integer) {
            int convertedCompared = Integer.parseInt(comparedValue.toString());
            int tipsValue = Integer.parseInt(Api.strReplace(identifier, player).split("\\.")[0]);
            switch (requirementType) {
                case EQUAL:
                    return tipsValue == convertedCompared;
                case BIGGER:
                    return tipsValue > convertedCompared;
                case BIGGER_OR_EQUAL:
                    return tipsValue >= convertedCompared;
                case SMALLER:
                    return tipsValue < convertedCompared;
                case SMALLER_OR_EQUAL:
                    return tipsValue <= convertedCompared;
            }
        }
        return false;
    }

    public String getDisplayName() {
        return displayName;
    }

    /*
        |      Target      |                    Function
        ---------------------------------------------------------------------------------------------------------------------
        |      %get%       |     the replacement text fetched from the "Tips" Api.
        |  %display_name%  |     the nickname of the variable, used to make forms' layout more decently.
        | %compared_value% |     the given value which is used to compare.
        |      %minus%     |     refers to %compared_values% minus %get%, while two of these variables are double or integer.
        | %absolute_diff%  |     the absolute value based on the difference.
        ---------------------------------------------------------------------------------------------------------------------
    */
    public void sendFailedMsg(Player player, boolean isDoubleOrInteger, Object... params) {
        if (failed_messages.isEmpty()) {
            return;
        }
        for (String msg : failed_messages) {
            String get = Api.strReplace(identifier, player);
            String out = msg.replace("%get%", get).replace("%display_name%", displayName).replace("%compared_value%", comparedValue.toString());
            if (isDoubleOrInteger) {
                //To get the absolute value of the diff between %get% and %compared_value%
                double convertedGet = Double.parseDouble(get);
                double absoluteDiff = Math.abs(convertedGet - Double.parseDouble(comparedValue.toString()));
                out = out.replace("%absolute_diff%", Double.toString(absoluteDiff));
            }
            player.sendMessage("[条件" + params[0] + "]§c" + out);
        }
    }

}
