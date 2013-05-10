/**
 * Jobs Plugin for Bukkit
 * Copyright (C) 2011 Zak Ford <zak.j.ford@gmail.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.zford.jobs.bukkit.config;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import me.zford.jobs.Jobs;
import me.zford.jobs.Location;
import me.zford.jobs.bukkit.JobsPlugin;
import me.zford.jobs.config.JobsConfiguration;
import me.zford.jobs.container.RestrictedArea;
import me.zford.jobs.container.Title;
import me.zford.jobs.dao.JobsDAOH2;
import me.zford.jobs.dao.JobsDAOMySQL;
import me.zford.jobs.dao.JobsDAOSQLite;
import me.zford.jobs.util.ChatColor;
import me.zford.jobs.util.FileDownloader;

public class BukkitJobsConfiguration extends JobsConfiguration {
    private JobsPlugin plugin;
    public BukkitJobsConfiguration(JobsPlugin plugin) {
        super();
        this.plugin = plugin;
    }
    
    @Override    
    public synchronized void reload() {
        // general settings
        loadGeneralSettings();
        // title settings
        loadTitleSettings();
        // restricted areas
        loadRestrictedAreaSettings();
    }

    /**
     * Method to load the general configuration
     * 
     * loads from Jobs/generalConfig.yml
     */
    private synchronized void loadGeneralSettings(){
        File f = new File(plugin.getDataFolder(), "generalConfig.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(f);
        
        CommentedYamlConfiguration writer = new CommentedYamlConfiguration();
        StringBuilder header = new StringBuilder();
        header.append("常规配置.");
        header.append(System.getProperty("line.separator"));
        header.append("  职业插件的常规配置主要包括插件保存用户数据的间隔");
        header.append(System.getProperty("line.separator"));
        header.append("数据库的储存方法, 是否在玩家升级时广播消息给其他玩家");
        header.append(System.getProperty("line.separator"));
        header.append("  它还允许管理员设置玩家最多能够担任的职业数量");
        header.append(System.getProperty("line.separator"));
        
        config.options().copyDefaults(true);
        
        writer.options().header(header.toString());
        
        writer.addComment("locale-language", "默认语言. 使用标准ISO639-1语言代码,和标准ISO-3166-1国家代码.",
                "如: en, en_US");
        config.addDefault("locale-language", Locale.getDefault().getLanguage());

        writer.addComment("storage-method", "储存方式, 可以是 MySQL, sqlite, h2");
        config.addDefault("storage-method", "sqlite");
        
        writer.addComment("mysql-username", "需要安装Mysql.");
        config.addDefault("mysql-username", "root");        
        config.addDefault("mysql-password", "");
        config.addDefault("mysql-url", "jdbc:mysql://localhost:3306/minecraft");
        config.addDefault("mysql-table-prefix", "");
        
        writer.addComment("save-period",  "每次保存的时间间隔, 必须是一个大于0的数");
        config.addDefault("save-period", 10);
        
        writer.addComment("save-on-disconnect",
                "在玩家退出游戏时保存数据",
                "玩家的数据是周期性的保存的.",
                "仅在你运行多个服务器或者有一个清楚的注意来做这件事.",
                "开启这一功能会降低数据库性能.");
        config.addDefault("save-on-disconnect", false);
        
        writer.addComment("broadcast-on-skill-up", "在玩家技能升级时其他玩家是否能收到提示");
        config.addDefault("broadcast-on-skill-up", false);
        
        writer.addComment("broadcast-on-level-up", "在玩家职业升级时其他玩家是否能收到提示");
        config.addDefault("broadcast-on-level-up", false);
        
        writer.addComment("max-jobs",
                "玩家最多可以加入的职业数量.",
                " 0 表示没有限制"
        );
        config.addDefault("max-jobs", 3);
        
        writer.addComment("hide-jobs-without-permission", "对玩家隐藏没有权限担任的职业");
        config.addDefault("hide-jobs-without-permission", false);
        
        writer.addComment("enable-pay-near-spawner", "允许玩家在刷怪笼周围杀死怪物时获得经验");
        config.addDefault("enable-pay-near-spawner", false);
        
        writer.addComment("enable-pay-creative", "允许玩家在创造模式时获得收益");
        config.addDefault("enable-pay-creative", false);
        
        writer.addComment("add-xp-player", "将玩家的Minecraft经验条作为职业插件的经验条");
        config.addDefault("add-xp-player", false);
        
        writer.addComment("modify-chat", "修改聊天标题.  如果你使用其他聊天插件,添加 {jobs} 到你的插件的聊天格式中.");
        config.addDefault("modify-chat", true);
        
        writer.addComment("economy-batch-delay", "付给玩家收益的间隔时间.  默认是 5 秒.",
                "太低的值会影响性能.  请根据实际情况来设置.");
        config.addDefault("economy-batch-delay", 5);
        
        String storageMethod = config.getString("storage-method");
        if(storageMethod.equalsIgnoreCase("mysql")) {
            String username = config.getString("mysql-username");
            if(username == null) {
                Jobs.getPluginLogger().severe("mysql-username 属性无效");
            }
            String password = config.getString("mysql-password");
            String url = config.getString("mysql-url");
            String prefix = config.getString("mysql-table-prefix");
            if (plugin.isEnabled())
                Jobs.setDAO(new JobsDAOMySQL(url, username, password, prefix));
        } else if(storageMethod.equalsIgnoreCase("h2")) {
            File h2jar = new File(plugin.getDataFolder(), "h2.jar");
            if (!h2jar.exists()) {
                Jobs.getPluginLogger().info("[职业插件] H2 数据库组件下载中...");
                try {
                    FileDownloader.downloadFile(new URL("http://dev.bukkit.org/media/files/692/88/h2-1.3.171.jar"), h2jar);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    Jobs.getPluginLogger().severe("无法下载 H2 数据库组件!");
                }
            }
            if (plugin.isEnabled()) {
                try {
                    Jobs.getJobsClassloader().addFile(h2jar);
                } catch (IOException e) {
                    Jobs.getPluginLogger().severe("无法加载 H2 数据库组件!");
                }
                if (plugin.isEnabled())
                    Jobs.setDAO(new JobsDAOH2());
            }
        } else if(storageMethod.equalsIgnoreCase("sqlite")) {
            Jobs.setDAO(new JobsDAOSQLite());
        } else {
            Jobs.getPluginLogger().warning("无效的储存方式!  重置到 sqlite!");
            Jobs.setDAO(new JobsDAOSQLite());
        }
        
        if (config.getInt("save-period") <= 0) {
            Jobs.getPluginLogger().severe("保存间隔 0!  重置到 10 分钟!");
            config.set("save-period", 10);
        }
        
        String localeString = config.getString("locale-language");
        try {
            int i = localeString.indexOf('_');
            if (i == -1) {
                locale = new Locale(localeString);
            } else {
                locale = new Locale(localeString.substring(0, i), localeString.substring(i+1));
            }
        } catch (IllegalArgumentException e) {
            locale = Locale.getDefault();
            Jobs.getPluginLogger().warning("无效的语言 \""+localeString+"\" 重置到 "+locale.getLanguage());
        }
        
        savePeriod = config.getInt("save-period");
        isBroadcastingSkillups = config.getBoolean("broadcast-on-skill-up");
        isBroadcastingLevelups = config.getBoolean("broadcast-on-level-up");
        payInCreative = config.getBoolean("enable-pay-creative");
        addXpPlayer = config.getBoolean("add-xp-player");
        hideJobsWithoutPermission = config.getBoolean("hide-jobs-without-permission");
        maxJobs = config.getInt("max-jobs");
        payNearSpawner = config.getBoolean("enable-pay-near-spawner");
        modifyChat = config.getBoolean("modify-chat");
        economyBatchDelay = config.getInt("economy-batch-delay");
        saveOnDisconnect = config.getBoolean("save-on-disconnect");
        
        // Make sure we're only copying settings we care about
        copySetting(config, writer, "locale-language");
        copySetting(config, writer, "storage-method");
        copySetting(config, writer, "mysql-username");
        copySetting(config, writer, "mysql-password");
        copySetting(config, writer, "mysql-url");
        copySetting(config, writer, "mysql-table-prefix");
        copySetting(config, writer, "save-period");
        copySetting(config, writer, "save-on-disconnect");
        copySetting(config, writer, "broadcast-on-skill-up");
        copySetting(config, writer, "broadcast-on-level-up");
        copySetting(config, writer, "max-jobs");
        copySetting(config, writer, "hide-jobs-without-permission");
        copySetting(config, writer, "enable-pay-near-spawner");
        copySetting(config, writer, "enable-pay-creative");
        copySetting(config, writer, "add-xp-player");
        copySetting(config, writer, "modify-chat");
        copySetting(config, writer, "economy-batch-delay");
        
        // Write back config
        try {
            writer.save(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private synchronized void copySetting(Configuration reader, Configuration writer, String path) {
        writer.set(path, reader.get(path));
    }
    
    /**
     * Method to load the title configuration
     * 
     * loads from Jobs/titleConfig.yml
     */
    private synchronized void loadTitleSettings(){
        this.titles.clear();
        File f = new File(plugin.getDataFolder(), "titleConfig.yml");
        YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);
        StringBuilder header = new StringBuilder()
            .append("职称配置")
            .append(System.getProperty("line.separator"))
            .append(System.getProperty("line.separator"))
            .append("储存在到达一定等级时所显示的职业称号.")
            .append(System.getProperty("line.separator"))
            .append("每个职称都有一个长名称的和短名称")
            .append(System.getProperty("line.separator"))
            .append("1 job) the colour of the title and the level requrirement to attain the title.")
            .append(System.getProperty("line.separator"))
            .append(System.getProperty("line.separator"))
            .append("虽然不是必需的,但是建议给每个职业在等级0时设置一个职称.")
            .append(System.getProperty("line.separator"))
            .append(System.getProperty("line.separator"))
            .append("Titles are completely optional.")
            .append(System.getProperty("line.separator"))
            .append(System.getProperty("line.separator"))
            .append("Titles:").append(System.getProperty("line.separator"))
            .append("  Apprentice:").append(System.getProperty("line.separator"))
            .append("    Name: Apprentice").append(System.getProperty("line.separator"))
            .append("    ShortName: A").append(System.getProperty("line.separator"))
            .append("    ChatColour: WHITE").append(System.getProperty("line.separator"))
            .append("    levelReq: 0").append(System.getProperty("line.separator"))
            .append("  Novice:").append(System.getProperty("line.separator"))
            .append("    Name: Novice").append(System.getProperty("line.separator"))
            .append("    ShortName: N").append(System.getProperty("line.separator"))
            .append("    ChatColour: GRAY").append(System.getProperty("line.separator"))
            .append("    levelReq: 30").append(System.getProperty("line.separator"))
            .append("  Journeyman:").append(System.getProperty("line.separator"))
            .append("    Name: Journeyman").append(System.getProperty("line.separator"))
            .append("    ShortName: J").append(System.getProperty("line.separator"))
            .append("    ChatColour: GOLD").append(System.getProperty("line.separator"))
            .append("    levelReq: 60").append(System.getProperty("line.separator"))
            .append("  Master:").append(System.getProperty("line.separator"))
            .append("    Name: Master").append(System.getProperty("line.separator"))
            .append("    ShortName: M").append(System.getProperty("line.separator"))
            .append("    ChatColour: BLACK").append(System.getProperty("line.separator"))
            .append("    levelReq: 90").append(System.getProperty("line.separator"))
            .append(System.getProperty("line.separator"));
        conf.options().header(header.toString());
        conf.options().copyDefaults(true);
        conf.options().indent(2);
        
        ConfigurationSection titleSection = conf.getConfigurationSection("Titles");
        if (titleSection == null) {
            titleSection = conf.createSection("Titles");
        }
        for (String titleKey : titleSection.getKeys(false)) {
            String titleName = conf.getString("Titles."+titleKey+".Name");
            String titleShortName = conf.getString("Titles."+titleKey+".ShortName");
            ChatColor titleColor = ChatColor.matchColor(conf.getString("Titles."+titleKey+".ChatColour", ""));
            int levelReq = conf.getInt("Titles."+titleKey+".levelReq", -1);
            
            if (titleName == null) {
                Jobs.getPluginLogger().severe("职称 " + titleKey + " Name 属性无效. 跳过!");
                continue;
            }
            if (titleShortName == null) {
                Jobs.getPluginLogger().severe("职称 " + titleKey + " ShortName 属性无效. 跳过!");
                continue;
            }
            if (titleColor == null) {
                Jobs.getPluginLogger().severe("职称 " + titleKey + " ChatColour 属性无效. 跳过!");
                continue;
            }
            if (levelReq <= -1) {
                Jobs.getPluginLogger().severe("职称 " + titleKey + " levelReq 属性无效. 跳过!");
                continue;
            }
            
            this.titles.add(new Title(titleName, titleShortName, titleColor, levelReq));
        }
        
        try {
            conf.save(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    /**
     * Method to load the restricted areas configuration
     * 
     * loads from Jobs/restrictedAreas.yml
     */
    private synchronized void loadRestrictedAreaSettings(){
        this.restrictedAreas.clear();
        File f = new File(plugin.getDataFolder(), "restrictedAreas.yml");
        YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);
        conf.options().indent(2);
        conf.options().copyDefaults(true);
        StringBuilder header = new StringBuilder();
        
        header.append("禁用区域")
            .append(System.getProperty("line.separator"))
            .append(System.getProperty("line.separator"))
            .append("设置一些在工作时不能得到经验或者钱的区域")
            .append(System.getProperty("line.separator"))
            .append(System.getProperty("line.separator"))
            .append("参数multiplier是职业收益的乘数.")
            .append(System.getProperty("line.separator"))
            .append("0.0的值意味着不能得到任何收益, 0.5意味着得到平时一半的收益")
            .append(System.getProperty("line.separator"))
            .append(System.getProperty("line.separator"))
            .append("restrictedareas:")
            .append(System.getProperty("line.separator"))
            .append("  area1:")
            .append(System.getProperty("line.separator"))
            .append("    world: 'world'")
            .append(System.getProperty("line.separator"))
            .append("    multiplier: 0.0")
            .append(System.getProperty("line.separator"))
            .append("    point1:")
            .append(System.getProperty("line.separator"))
            .append("      x: 125")
            .append(System.getProperty("line.separator"))
            .append("      y: 0")
            .append(System.getProperty("line.separator"))
            .append("      z: 125")
            .append(System.getProperty("line.separator"))
            .append("    point2:")
            .append(System.getProperty("line.separator"))
            .append("      x: 150")
            .append(System.getProperty("line.separator"))
            .append("      y: 100")
            .append(System.getProperty("line.separator"))
            .append("      z: 150")
            .append(System.getProperty("line.separator"))
            .append("  area2:")
            .append(System.getProperty("line.separator"))
            .append("    world: 'world_nether'")
            .append(System.getProperty("line.separator"))
            .append("    multiplier: 0.0")
            .append(System.getProperty("line.separator"))
            .append("    point1:")
            .append(System.getProperty("line.separator"))
            .append("      x: -100")
            .append(System.getProperty("line.separator"))
            .append("      y: 0")
            .append(System.getProperty("line.separator"))
            .append("      z: -100")
            .append(System.getProperty("line.separator"))
            .append("    point2:")
            .append(System.getProperty("line.separator"))
            .append("      x: -150")
            .append(System.getProperty("line.separator"))
            .append("      y: 100")
            .append(System.getProperty("line.separator"))
            .append("      z: -150");
        conf.options().header(header.toString());
        ConfigurationSection areaSection = conf.getConfigurationSection("restrictedareas");
        if (areaSection != null) {
            for (String areaKey : areaSection.getKeys(false)) {
                String worldName = conf.getString("restrictedareas."+areaKey+".world");
                double multiplier = conf.getDouble("restrictedareas."+areaKey+".multiplier", 0.0);
                Location point1 = new Location(worldName,
                        conf.getDouble("restrictedareas."+areaKey+".point1.x", 0.0),
                        conf.getDouble("restrictedareas."+areaKey+".point1.y", 0.0),
                        conf.getDouble("restrictedareas."+areaKey+".point1.z", 0.0));
    
                Location point2 = new Location(worldName,
                        conf.getDouble("restrictedareas."+areaKey+".point2.x", 0.0),
                        conf.getDouble("restrictedareas."+areaKey+".point2.y", 0.0),
                        conf.getDouble("restrictedareas."+areaKey+".point2.z", 0.0));
                this.restrictedAreas.add(new RestrictedArea(point1, point2, multiplier));
            }
        }
        try {
            conf.save(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
