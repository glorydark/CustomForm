package glorydark.nukkit.customform.scriptForms.data.requirement.custom;

import cn.nukkit.Player;

/**
 * @author glorydark
 */
public abstract class ReducibleRequirementData extends RequirementData {

    public abstract boolean isReduce();

    public abstract void reduceCost(Player player, int multiply);
}
