package glorydark.customform.utils;

import cn.nukkit.block.BlockUnknown;
import cn.nukkit.item.Item;
import glorydark.customform.CustomFormMain;

public class InventoryUtils {

    private static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("null")) {
            return null;
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
        if (src == null || src.length <= 0) {
            return null;
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
        if (item.hasCompoundTag()) {
            return item.getId() + ":" + item.getDamage() + ":" + item.getCount() + ":" + bytesToHexString(item.getCompoundTag());
        } else {
            return item.getId() + ":" + item.getDamage() + ":" + item.getCount() + ":null";
        }
    }

    public static Item toItem(String itemString) {
        String[] strings = itemString.split(":");
        boolean isNumericId = false;
        try {
            int test = Integer.parseInt(strings[0]);
            isNumericId = true;
        } catch (Exception ignored) {

        }
        if (isNumericId) {
            Item item = Item.get(Integer.parseInt(strings[0]), Integer.parseInt(strings[1]), Integer.parseInt(strings[2]));
            item.setCompoundTag(hexStringToBytes(strings[3]));
            return item;
        } else {
            if (strings.length == 5) {
                // minecraft:test:0:1:null
                int countIndex = strings.length - 2;
                StringBuilder identifierAndMeta = new StringBuilder();
                for (int i = 0; i < strings.length - 2; i++) {
                    identifierAndMeta.append(strings[i]);
                    if (i != strings.length - 3) {
                        identifierAndMeta.append(":");
                    }
                }
                Item item = Item.fromString(identifierAndMeta.toString());
                item.setCount(Integer.parseInt(strings[countIndex]));
                item.setCompoundTag(hexStringToBytes(strings[countIndex+1]));
                // CustomFormMain.plugin.getLogger().info(item.toString());
                return item;
            } else if (strings.length == 4) {
                // minecraft:test:1:null
                int countIndex = strings.length - 2;
                StringBuilder identifierAndMeta = new StringBuilder();
                for (int i = 0; i < strings.length - 2; i++) {
                    identifierAndMeta.append(strings[i]);
                    if (i != strings.length - 3) {
                        identifierAndMeta.append(":");
                    }
                }
                Item item = Item.fromString(identifierAndMeta.toString());
                item.setCount(Integer.parseInt(strings[countIndex]));
                item.setCompoundTag(hexStringToBytes(strings[countIndex+1]));
                // CustomFormMain.plugin.getLogger().info(item.toString());
                return item;
            }
        }
        return new BlockUnknown(999).toItem();
    }
}