package cn.org.agatha.agSupply;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.Gson;

public class AgSupply extends JavaPlugin {

    private static final String API_URL = "";
    private static final String API_LIST_URL = "";

    @Override
    public void onEnable() {
        getLogger().info("AgSupply 插件已启用！");
    }

    @Override
    public void onDisable() {
        getLogger().info("AgSupply 插件已禁用。");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c该命令只能由玩家执行。");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("supply")) {
            if (args.length != 1) {
                player.sendMessage("§c用法: /supply <ID>");
                return true;
            }

            String id = args[0];
//            {"status":"1","maintainer":"HELP_FRESH","world":"world","x":"-2048","y":"68","z":"-2208","efficiency":"150\/h","content":"\u9e21\u8089\u751f\u4ea7\u673a\u5668","type":"producer","confirmation":"2024\u5e747\u670826\u65e5","message":"\u65e0"}
            if(id.equalsIgnoreCase("list")){
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    try {
                        String jsonResponse = sendListRequest();
                        Gson gson = new Gson();
                        ResponseData responseData = gson.fromJson(jsonResponse, ResponseData.class);
                        ListData[] dataList = responseData.data;

                        Bukkit.getScheduler().runTask(this, () -> {
                            for(ListData data : dataList){
                                if (data.status.equalsIgnoreCase("0")){
                                    data.id = "§c×";
                                }
                                else data.id += ".";
                                if(data.type.equalsIgnoreCase("producer")){
                                    data.type = "生产";
                                }
                                else{
                                    data.type = "储存";
                                }
                                player.sendMessage("§f" + data.id + "  §b" + data.type + "§r " +data.content);
                            }

                        });

                    } catch (Exception e) {
                        player.sendMessage("§c请求API时发生错误，请稍后再试。");
                        e.printStackTrace();
                    }
                });
            }
            else{
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    try {
                        String jsonResponse = sendGetRequest(id);
                        Gson gson = new Gson();
                        SupplyData data = gson.fromJson(jsonResponse, SupplyData.class);

                        if (!"1".equals(data.status) && !"0".equals(data.status)) {
                            player.sendMessage("§c获取数据失败，请检查 ID 是否有效。");
                            return;
                        }
                        if("0".equals(data.status)){
                            player.sendMessage("§c该设施处于不可用状态。");
                        }

                        World world = Bukkit.getWorld(data.world);
                        if (world == null) {
                            player.sendMessage("§c世界不存在: " + data.world);
                            return;
                        }

                        double x = Double.parseDouble(data.x);
                        double y = Double.parseDouble(data.y);
                        double z = Double.parseDouble(data.z);
                        Location location = new Location(world, x, y, z);

                        Bukkit.getScheduler().runTask(this, () -> {
                            player.teleport(location);
                            player.sendMessage("§a传送成功！设施信息：");
                            player.sendMessage("§f维护者: §b" + data.maintainer);
                            player.sendMessage("§f世界: §b" + translateWorld(data.world));
                            player.sendMessage("§f坐标: §bX:" + x + " Y:" + y + " Z:" + z);
                            player.sendMessage("§f效率: §b" + data.efficiency);
                            player.sendMessage("§f名称: §b" + data.content);
                            player.sendMessage("§f类型: §b" + translateType(data.type));
                            player.sendMessage("§f备注: §b" + data.message);
                            player.sendMessage("§f上次确认: §b" + data.confirmation);
                        });

                    } catch (Exception e) {
                        player.sendMessage("§c请求API时发生错误，请稍后再试。");
                        e.printStackTrace();
                    }
                });
            }

            return true;
        }

        return false;
    }

    private String sendGetRequest(String id) throws Exception {
        URL url = new URL(API_URL + id);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        return response.toString();
    }
    private String sendListRequest() throws Exception {
        URL url = new URL(API_LIST_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        return response.toString();
    }

    /**
     * 翻译“世界”字段为中文表达
     */
    private String translateWorld(String world) {
        if (world == null || world.isEmpty()) {
            return "未知世界";
        }

        switch (world.toLowerCase()) {
            case "world":
                return "主世界";
            case "world_nether":
                return "下界";
            case "world_the_end":
                return "末地";
            default:
                return world; // 原样返回未匹配内容
        }
    }

    /**
     * 翻译“类型”字段为中文表达
     */
    private String translateType(String type) {
        if (type == null || type.isEmpty()) {
            return "未知类型";
        }

        switch (type.toLowerCase()) {
            case "producer":
                return "生产";
            case "storage":
                return "储存";
            default:
                return type; // 原样返回未匹配内容
        }
    }

    // 内部类用于解析 JSON 数据
    private static class SupplyData {
        String status;
        String maintainer;
        String world;
        String x;
        String y;
        String z;
        String efficiency;
        String content;
        String type;
        String confirmation;
        String message;
    }
    private static class ResponseData {
        ListData[] data;
    }
    private static class ListData {
        String id;
        String content;
        String type;
        String status;

    }
}