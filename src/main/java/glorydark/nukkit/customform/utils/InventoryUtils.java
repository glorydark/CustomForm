package glorydark.nukkit.customform.utils;

import cn.nukkit.item.Item;
import cn.nukkit.utils.ConfigSection;
import glorydark.nukkit.customform.CustomFormMain;

public class InventoryUtils {

    public static ConfigSection itemStringCaches = new ConfigSection();

    private static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("null")) {
            return new byte[0];
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length == 0) {
            return "null";
        }
        for (byte aSrc : src) {
            int v = aSrc & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public static String saveItemToString(Item item) {
        switch (NukkitTypeUtils.getNukkitType()) {
            case POWER_NUKKIT_X:
            case POWER_NUKKIT_X_2:
            case MOT:
                if (item.hasCompoundTag()) {
                    return item.getNamespaceId() + ":" + item.getDamage() + ":" + item.getCount() + ":" + bytesToHexString(item.getCompoundTag());
                } else {
                    return item.getNamespaceId() + ":" + item.getDamage() + ":" + item.getCount() + ":null";
                }
            default:
                if (item.hasCompoundTag()) {
                    return item.getId() + ":" + item.getDamage() + ":" + item.getCount() + ":" + bytesToHexString(item.getCompoundTag());
                } else {
                    return item.getId() + ":" + item.getDamage() + ":" + item.getCount() + ":null";
                }
        }
    }

    public static Item toItem(String itemString) {
        String[] strings = itemString.split(":");
        if (strings.length == 0) {
            CustomFormMain.plugin.getLogger().error("Error in parsing item string, caused by: cannot parse the item string { " + itemString + " } in the save_nbt_cache.yml");
            return Item.AIR_ITEM;
        }
        Item item = getItemFromConfig(itemString);
        if (item.getId() != 0) {
            return item;
        }
        boolean isNumericId = false;
        try {
            int test = Integer.parseInt(strings[0]);
            isNumericId = true;
        } catch (Exception ignored) {

        }
        if (isNumericId) {
            item = Item.get(Integer.parseInt(strings[0]), strings.length >= 2? Integer.parseInt(strings[1]) : 0, strings.length >= 3? Integer.parseInt(strings[2]):1);
            if (strings.length >= 4) {
                item.setCompoundTag(hexStringToBytes(strings[3]));
            }
        } else {
            item = Item.fromString(itemString);
            item.setCount(strings.length >= 3? Integer.parseInt(strings[2]): 1);
            if (strings.length >= 4) {
                item.setCompoundTag(hexStringToBytes(strings[3]));
            }
        }
        if (item.getId() == 0) {
            CustomFormMain.plugin.getLogger().error("Error in parsing item string, caused by: cannot parse the item string { " + item + " } in the save_nbt_cache.yml");
        } else {
            System.out.println(item.getId());
        }
        return item;
    }

    public static Item getItemFromConfig(String key) {
        String[] strings = key.split(":");
        if (itemStringCaches.exists(strings[0])) {
            Item item = toItem(itemStringCaches.getString(strings[0]));
            if (strings.length == 2) {
                item.setCount(Integer.parseInt(strings[1]));
            }
            return item;
        }
        return Item.get(0);
    }
}
