package glorydark.nukkit.customform;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author glorydark
 */
public class Test {

    private static final Pattern REG_PLUGIN_INFO =
            Pattern.compile(
                    "^\\s*regPluginInfo\\s*\\(\\s*\"([^\"]*)\"(?:\\s*,\\s*\"([^\"]*)\")?(?:\\s*,\\s*\"([^\"]*)\")?(?:\\s*,\\s*\"([^\"]*)\")?.*\\)",
                    Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static void main(String[] args) {
        derivePluginData("""
                // Nukkit JS Red Packet Plugin with YAML cache
                importClass(Packages.cn.nukkit.scheduler.Task);
                importClass(Packages.cn.nukkit.event.Listener);
                importClass(Packages.cn.nukkit.Player);
                importClass(Packages.cn.nukkit.utils.Config);
                importClass(Packages.cn.nukkit.utils.ConfigSection);
                importClass(Packages.java.io.File);
                importClass(Packages.cn.nukkit.event.EventPriority);
                importClass(Packages.gameapi.commands.base.EasyCommand);
                importClass(Packages.java.lang.reflect.Array);
                importClass(Packages.me.onebone.economyapi.EconomyAPI);
                
                regPluginInfo("DRedPocket", "1.1.0", "GloryDark", "一个酷炫的红包插件");
                
                var basePath = server.getPluginPath() + "/red_pockets";
                var cacheFile = basePath + "/caches.yml";
                
                var cache;
                var expireInterval = 60000; // 60000ms = 60s（要设置为50的倍数哦，因为用的task是1tick为单位的，即50ms）
                
                function getConfig() {
                	return new Config(cacheFile, Config.YAML);
                }
                
                function initStorage(){
                	var args = api.newStringArray(2);
                	args[0] = "发红包";
                	args[1] = "抢红包";
                	server.getCommandMap().unregister(args);
                	server.getCommandMap().register("发红包", RedCommand);
                	server.getCommandMap().register("抢红包", GetCommand);
                    var dir = new File(basePath);
                    if(!dir.exists()) dir.mkdirs();
                	cache = getConfig().getRootSection();
                	if(!cache.exists("packets")) cache.set("packets", {});
                    savePackets();
                	checkExpiredPackets();
                	plugin.getLogger().info(cacheFile.toString());
                }
                
                function packets(){
                    var sec = cache.getSection("packets");
                    if (sec == null) {
                        sec = new ConfigSection();
                		cache.set("packets", sec);
                    }
                    return sec;
                }
                
                function savePackets(){
                	var config = getConfig();
                    config.setAll(cache);
                	config.save();
                }
                
                function generateId(){ return Math.random().toString(36).substr(2,8); }
                
                function calculate(total,count){
                    var arr=[]; var remain=total; var rc=count;
                    for(var i=0;i<count-1;i++){
                        var max = remain/rc*2;
                        var v = Math.floor(Math.random()*max)+1;
                        arr.push(v); remain-=v; rc--;
                    }
                    arr.push(remain); return arr;
                }
                
                function createRed(sender,total,count){
                	if(sender.isPlayer()) {
                		if(total<=0||count<=0){ sender.sendMessage("§c参数错误"); return; }
                		if(total<count){ sender.sendMessage("§c金额必须>=数量"); return; }
                		var player = sender.asPlayer();
                		var leftMoney = EconomyAPI.getInstance().myMoney(player);
                		if(leftMoney < total){ sender.sendMessage("§c余额不足"); return; }
                		EconomyAPI.getInstance().reduceMoney(player, total);
                	}
                    var id = generateId();
                    var p = packets();
                	p.set(id, {
                		sender: sender.getName(),
                		total: total,
                		count: count,
                		left: calculate(total,count),
                		claimed: {},
                		sendTime: Date.now()  // 添加发送时间
                	});
                	savePackets();
                
                
                    server.broadcastMessage("§6🧧 "+sender.getName()+" 发了红包，输入以下指令抢红包: /抢红包 "+id);
                    expireLater(id);
                }
                
                function claim(player,id){
                	var p = packets();
                    if(!p.containsKey(id)){ player.sendMessage("§c红包不存在"); return; }
                	var data = p.getSection(id);
                
                	// 检查是否过期
                    var sendTime = data.get("sendTime");
                    var now = Date.now();
                    if (sendTime && (now - sendTime > expireInterval)) {
                        player.sendMessage("§c红包已过期");
                        // 自动清理
                        p.remove(id);
                        savePackets();
                        return;
                    }
                    if (data.sender==player.getName()){ player.sendMessage("§c不能抢自己"); return; }
                	// 2. 检查是否已抢过
                	var claimed = data.get("claimed");
                	if (claimed && claimed.containsKey(player.getName())) {
                		player.sendMessage("§c你已抢过");
                		return;
                	}
                	// 3. 检查是否抢完
                	var left = data.get("left");
                	if (!left || left.size() === 0) {
                		player.sendMessage("§c已抢完");
                		return;
                	}
                    var idx = Math.floor(Math.random()*left.size());
                	var amount = left.get(idx);  // 获取元素
                	left.remove(idx);            // 移除元素
                	data.set("left", left);
                    claimed.set(player.getName(), amount);
                    EconomyAPI.getInstance().addMoney(player,amount);
                	p.set(id, data);
                	savePackets();
                
                    server.broadcastMessage("§e"+player.getName()+" 抢到 "+amount+" 金币");
                    if(left.length===0){ p.remove(id); savePackets(); server.broadcastMessage("§6红包抢完"); }
                }
                
                function expireLater(id){
                    var T = new Task {\s
                	  onRun:function(currentTick){
                        var p=packets();
                		if(!p.containsKey(id)) return;
                        var data=p.getSection(id);
                		var sum=0;
                		var left = data.get("left");
                		if (!left || left.size() === 0) {
                			player.sendMessage("§c已抢完");
                		} else {
                			for(var i in left) {
                				sum+=left.get(i);
                			}
                			if(sum>0) {
                				var sender = data.getString("sender");
                                EconomyAPI.getInstance().addMoney(sender, sum);
                				plugin.getLogger().info("红包已过期，剩余 " + sum + " 金币，已返还给玩家: " + sender);
                			}
                		}
                		p.remove(id);
                		savePackets();
                        server.broadcastMessage("§c红包 "+id+" 已过期");
                    }};
                    server.getScheduler().scheduleDelayedTask(plugin, T, expireInterval / 50);
                }
                
                var RedCommand = new JavaAdapter(EasyCommand, {
                    execute: function(sender, label, args) {
                        if (args.length < 2) {
                            sender.sendMessage("§e用法: /red <金额> <数量>");
                            return true;
                        }
                        createRed(sender, parseInt(args[0]), parseInt(args[1]));
                        return true;
                    }
                }, "发红包");   // ← 这里就是 public EasyCommand(String name)
                
                var GetCommand = new JavaAdapter(EasyCommand, {
                    execute: function(sender, label, args) {
                        if (!(sender instanceof Player)) return true;
                        claim(sender.asPlayer(), args[0]);
                        return true;
                    }
                }, "抢红包");
                
                function checkExpiredPackets() {
                    var p = packets();
                    var now = Date.now();
                    var expiredPackets = [];
                   \s
                    // 遍历所有红包
                    for (var id in p.getKeys()) {
                        var data = p.get(id);
                        if (data && data.get("sendTime")) {
                            var sendTime = data.get("sendTime");
                            if (now - sendTime > expireInterval) {
                                expiredPackets.push(id);
                            }
                        }
                    }
                   \s
                    // 处理过期红包
                    if (expiredPackets.length > 0) {
                        plugin.getLogger().info("发现 " + expiredPackets.length + " 个过期红包");
                        for (var i in expiredPackets) {
                            var id = expiredPackets[i];
                            var data = p.get(id);
                            if (data) {
                                // 退还剩余金额
                                var left = data.get("left");
                                if (left && left.size() > 0) {
                                    var sum = 0;
                                    for (var j in left) {
                                        sum += left.get(j);
                                    }
                                    if (sum > 0) {
                						var sender = data.getString("sender");
                                        EconomyAPI.getInstance().addMoney(sender, sum);
                						plugin.getLogger().info("红包已过期，剩余 " + sum + " 金币，已返还给玩家: " + sender);
                                    }
                                }
                                p.remove(id);
                            }
                        }
                        savePackets();
                    }
                }
                
                initStorage();
                plugin.getLogger().info("RedPockets loaded with YAML cache");
                """);
    }

    public static void derivePluginData(String scriptCode) {
        Matcher m = REG_PLUGIN_INFO.matcher(scriptCode);
        if (m.find()) {
            System.out.println(m.group(0));
            System.out.println(m.group(1));
            System.out.println(m.group(2));
            System.out.println(m.group(3));
        }
    }
}
