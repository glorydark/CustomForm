package glorydark.nukkit.customform.scriptForms.data.requirement.custom;

import cn.nukkit.Player;

import java.util.List;
import java.util.Map;

/**
 * @author glorydark
 */
public abstract class RequirementData {

    public abstract String identifier();

    public abstract List<String> getFailedMessages();

    public abstract boolean isQualified(Player player, int multiply);

    public abstract void sendFailedMessage(Player player);
}
