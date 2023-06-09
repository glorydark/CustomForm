package glorydark.customform.scriptForms.data.requirement.item;

import cn.nukkit.item.Item;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NeedItem {

    private Item item;

    private List<Item> alternatives = new ArrayList<>();

    private Item finalComparedItem;

    private Item hasItem;

    public NeedItem(String item, List<String> alternatives){
        this.item = toItem(item);
        for (String s: alternatives){
            this.alternatives.add(toItem(s));
        }
    }

    public Item toItem(String string){
        String[] s1 = string.split(":");
        Item output = new Item(Integer.parseInt(s1[0]), Integer.parseInt(s1[1]), Integer.parseInt(s1[2]));
        if(!s1[3].equals("null")){
            byte[] tag = hexStringToBytes(s1[3]);
            output.setCompoundTag(tag);
        }
        return output;
    }

    public byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
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

    public byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

}
