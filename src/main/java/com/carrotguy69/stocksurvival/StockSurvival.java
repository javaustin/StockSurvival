package com.carrotguy69.stocksurvival;

import com.carrotguy69.cxyz.events.custom.PublicChatEvent;
import com.carrotguy69.cxyz.events.custom.VanishToggleEvent;
import com.carrotguy69.cxyz.events.custom.base.Priority;
import com.carrotguy69.cxyz.events.custom.service.EventService;
import com.carrotguy69.cxyz.messages.MessageUtils;
import com.carrotguy69.cxyz.messages.utils.MapFormatters;
import com.carrotguy69.cxyz.models.config.channel.channelTypes.BaseChannel;
import com.carrotguy69.cxyz.models.config.channel.coreChannels.PublicChannel;
import com.carrotguy69.cxyz.models.config.channel.registry.ChannelFunction;
import com.carrotguy69.cxyz.models.config.channel.registry.ChannelRegistry;
import com.carrotguy69.cxyz.models.db.GameStat;
import com.carrotguy69.cxyz.models.db.NetworkPlayer;
import com.carrotguy69.cxyz.utils.TimeUtils;
import com.carrotguy69.cxyz.webhook.DiscordEmbed;
import com.carrotguy69.cxyz.webhook.DiscordWebhook;
import com.carrotguy69.stocksurvival.cmd.lifesteal._LifestealSupercommand;
import com.carrotguy69.stocksurvival.cmd.protection._ProtectionSupercommand;
import com.carrotguy69.stocksurvival.event.ChatHandler;
import com.carrotguy69.stocksurvival.event.VanishToggleHandler;
import com.carrotguy69.stocksurvival.messages.MessageGrabber;
import com.carrotguy69.stocksurvival.messages.SurvivalMessageKey;
import com.carrotguy69.stocksurvival.tabCompleters.Protection;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.carrotguy69.cxyz.CXYZ.f;
import static com.carrotguy69.cxyz.messages.MessageUtils.formatPlaceholders;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.ATTACKER_PROTECTED;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.COMBAT_LOGGED;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.DEATH_LOCATION;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.END_DISABLED;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.IN_COMBAT;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.ON_JOIN;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.ON_JOIN_FIRST;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.ON_LEAVE;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.OUT_COMBAT;
import static com.carrotguy69.stocksurvival.messages.SurvivalMessageKey.TARGET_PROTECTED;

public final class StockSurvival extends JavaPlugin implements Listener {

    public static StockSurvival plugin;

    public static FileConfiguration msgYML;
    public static FileConfiguration configYML;

    public static List<String> xrayBlocks = new ArrayList<>();
    public static String xrayChannel = "";
    public static String xrayChatFormat = "";

    public static int newbieProtectionSeconds = 0;
    public static int spawnProtectionSeconds = 0;
    public static int combatLoggerSeconds = 0;

    public static Map<UUID, Long> combatLogMap = new Hashtable<>(); // represents (UUID of player, In expire timestamp)

    public static Map<UUID, DamageSource> damageCauseMap = new Hashtable<>(); // To ensure lifesteal is working we need a map to track who damaged who


    public static boolean lifestealEnabled;
    public static int lifestealHPGain;
    public static int lifestealHPMax;
    public static List<String> lifestealActionsOnKill;
    public static List<String> lifestealActionsOnFinalKill;

    public static boolean endDisabled;

    public static PublicChannel publicChat;

    public static String publicWebhookUrl;

    public static String lifestealMaxHPKey = "survival-lifesteal-hp";

    public static String newbieProtForfeitKey = "survival-forfeit-protection";


    /*

    todo:
        - Anvil too expensive bypass
        - action bar notice for newbie protection
        - tpa system
    */

    @Override
    public void onEnable() {
        plugin = JavaPlugin.getPlugin(StockSurvival.class);

        configYML = plugin.getConfig();

        // This is how you actually load a yml file
        File msgFile = new File(getDataFolder(), "messages.yml");
        if (!msgFile.exists()) {
            saveResource("messages.yml", false);
        }
        msgYML = YamlConfiguration.loadConfiguration(msgFile);

        getConfig().options().copyDefaults(true); // Copies default values (values that are not set by user.) Keep this as it will be useful for version changes.
        msgYML.options().copyDefaults(true);

        saveConfig();

        Objects.requireNonNull(plugin.getCommand("protection")).setExecutor(_ProtectionSupercommand.executor);
        Objects.requireNonNull(plugin.getCommand("protection")).setTabCompleter(Protection.tabCompleter);
        Objects.requireNonNull(plugin.getCommand("lifesteal")).setExecutor(_LifestealSupercommand.executor);
        Objects.requireNonNull(plugin.getCommand("lifesteal")).setTabCompleter(_LifestealSupercommand.tabCompleter);

        plugin.getServer().getPluginManager().registerEvents(plugin, plugin);

        xrayBlocks = configYML.getStringList("xray.blocks");
        xrayChannel = configYML.getString("xray.notification.channel");
        xrayChatFormat = configYML.getString("xray.notification.message");

        newbieProtectionSeconds = configYML.getInt("newbie-protection", 3600);
        spawnProtectionSeconds = configYML.getInt("spawn-protection", 10);
        combatLoggerSeconds = configYML.getInt("combat-logger-seconds", 10);

        endDisabled = configYML.getBoolean("disable-end", false);

        publicWebhookUrl = configYML.getString("public-webhook-url");

        lifestealEnabled = configYML.getBoolean("lifesteal.enabled", false);
        lifestealHPGain = configYML.getInt("lifesteal.hp.gain", 1);
        lifestealHPMax = configYML.getInt("lifesteal.hp.max", 40);

        lifestealActionsOnKill = configYML.getStringList("lifesteal.kill-actions");
        lifestealActionsOnFinalKill = configYML.getStringList("lifesteal.final-kill-actions");

        publicChat = (PublicChannel) ChannelRegistry.getChannelByFunction(ChannelFunction.PUBLIC);

        EventService.registerHandler(PublicChatEvent.class, new ChatHandler(), Priority.NORMAL);
        EventService.registerHandler(VanishToggleEvent.class, new VanishToggleHandler(), Priority.NORMAL);

        getLogger().info(f("&dStockSurvival fully loaded"));

        DiscordEmbed embed = new DiscordEmbed();
        embed.setTitle("");
        embed.setDescription("**Server started!**");
        embed.setColor(0x5ac155);

        new DiscordWebhook().setURL(publicWebhookUrl).addEmbed(embed).send();
    }

    @Override
    public void onDisable() {
        // discord message
        DiscordEmbed embed = new DiscordEmbed();
        embed.setTitle("");
        embed.setDescription("**Server stopped!**");
        embed.setColor(0xc16155);

        new DiscordWebhook().setURL(publicWebhookUrl).addEmbed(embed).send();
        // Plugin shutdown logic
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        // Simple Xray Notifications

        Player p = e.getPlayer();
        Block block = e.getBlock();


        boolean isXrayBlock = false;
        for (String xrayBlock : xrayBlocks) {
            if (xrayBlock.equalsIgnoreCase(block.getType().name())) {
                isXrayBlock = true;
            }
        }

        if (!isXrayBlock)
            return;

        NetworkPlayer np = NetworkPlayer.resolvePlayer(p.getUniqueId());
        Map<String, Object> commonMap = MapFormatters.locationFormatter(block.getLocation());
        commonMap.putAll(MapFormatters.playerFormatter(np));
        commonMap.put("block", e.getBlock().getType().name());

        BaseChannel notifyChannel = ChannelRegistry.getChannelByName(xrayChannel);
        if (notifyChannel == null)
            return;

        notifyChannel.sendChannelMessage(xrayChatFormat, commonMap);
    }

    @EventHandler
    public void onDamageByPlayer(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player p)) {
            return;
        }

        Entity attackerEntity = e.getDamager();
        Player attacker = null;

        if (attackerEntity.getType() == EntityType.PLAYER) {
            assert attackerEntity instanceof Player;
            attacker = (Player) attackerEntity;
        }

        else if (attackerEntity instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }

        if (attacker == null) {
            // The attacker is not a player nor a projectile (arrow) launched by a player
            return;
        }


        if (isProtected(p)) {
            e.setCancelled(true);

            String remainingTime = TimeUtils.countdownShort(getProtectionTimeLeft(p));
            MessageUtils.sendParsedMessage(attacker, MessageGrabber.grab(TARGET_PROTECTED), Map.of("remaining-time", remainingTime));
            return;
        }

        if (isProtected(attacker)) {
            e.setCancelled(true);

            String remainingTime = TimeUtils.countdownShort(getProtectionTimeLeft(p));
            MessageUtils.sendParsedMessage(attacker, MessageGrabber.grab(ATTACKER_PROTECTED), Map.of("remaining-time", remainingTime));

            return;
        }

        putInCombat(p, 10);
        putInCombat(attacker, 10);

        if (!lifestealEnabled)
            return;

        if (p.getHealth() - e.getFinalDamage() >= 0) {
            damageCauseMap.put(p.getUniqueId(), e.getDamageSource());

            // Log the cause of the damage (if we assume it is by a player).
            new BukkitRunnable() {
                public void run() {
                    // If the entry is not exactly how we put it in the map, do nothing. Another task will handle it.
                    if (damageCauseMap.containsKey(p.getUniqueId()) && damageCauseMap.containsValue(e.getDamageSource()))
                        damageCauseMap.remove(p.getUniqueId());
                    else
                        this.cancel();
                }
            }.runTaskLater(plugin, 200L);
        }


        else {
            // PLAYER IS DEAD
            // Since we already have the cause of death (player and attacker), we can remove the entry from the damageCauseMap.

            // By removing the damageCauseMap entry, the onDeath handler will be tricked into thinking that this is a natural death (not caused by an attacking player).
            // Natural deaths do not trigger the lifesteal so the onDeath handler will ignore - so now all of our logic is concentrated to here.
            damageCauseMap.remove(p.getUniqueId());

            lifeStealHandleKill(p, attacker);
        }


    }

    @EventHandler
    public static void onTeleport(PlayerTeleportEvent e) {
        if (e.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL && endDisabled) {
            MessageUtils.sendParsedMessage(e.getPlayer(), MessageGrabber.grab(END_DISABLED), Map.of());
            e.setCancelled(true);
        }
    }

    @EventHandler
    public static void onJoin(PlayerJoinEvent e) {
        e.setJoinMessage("");

        Player p = e.getPlayer();

        doOnJoin(p);
    }

    public static void doOnJoin(Player p) {
        p.setNoDamageTicks(spawnProtectionSeconds * 20);

        SurvivalMessageKey key = p.hasPlayedBefore() ? ON_JOIN : ON_JOIN_FIRST;

        Map<String, Object> commonMap = MapFormatters.playerFormatter(NetworkPlayer.resolvePlayer(p.getUniqueId()));

        setPlayerMaxHP(p, getPlayerMaxHP(p));

        publicChat.sendChannelMessage(MessageGrabber.grab(key), commonMap);

        // discord message
        DiscordEmbed embed = new DiscordEmbed();
        embed.setTitle("");
        embed.setDescription("** " + ChatColor.stripColor(f(formatPlaceholders(MessageGrabber.grab(key), commonMap))) + "**");
        embed.setColor(0x79ff70);

        new DiscordWebhook().setURL(publicWebhookUrl).addEmbed(embed).send();
    }

    @EventHandler
    public static void onLeave(PlayerQuitEvent e) {
        e.setQuitMessage("");

        Player p = e.getPlayer();
        Map<String, Object> commonMap = MapFormatters.playerFormatter(NetworkPlayer.resolvePlayer(p.getUniqueId()));

        if (isInCombat(p)) {
            p.kill(p.getLastDamageCause() != null ? p.getLastDamageCause().getDamageSource() : DamageSource.builder(DamageType.GENERIC_KILL).build());

            BaseChannel publicChannel = ChannelRegistry.getChannelByFunction(ChannelFunction.PUBLIC);
            if (publicChannel == null) {
                return;
            }

            publicChannel.sendChannelMessage(MessageGrabber.grab(COMBAT_LOGGED), commonMap);
        }


        publicChat.sendChannelMessage(MessageGrabber.grab(ON_LEAVE), commonMap);


        // discord message
        DiscordEmbed embed = new DiscordEmbed();
        embed.setTitle("");
        embed.setDescription("**" + ChatColor.stripColor(f(formatPlaceholders(MessageGrabber.grab(ON_LEAVE), commonMap))) + "**");
        embed.setColor(0xff7070);

        new DiscordWebhook().setURL(publicWebhookUrl).addEmbed(embed).send();

    }

    @EventHandler
    public static void onDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        NetworkPlayer np = NetworkPlayer.resolvePlayer(p.getUniqueId());

        MessageUtils.sendParsedMessage(p, MessageGrabber.grab(DEATH_LOCATION), MapFormatters.playerFormatter(np));

        // discord message
        DiscordEmbed embed = new DiscordEmbed();
        embed.setTitle("");
        embed.setDescription(ChatColor.stripColor(f(Objects.requireNonNull(e.getDeathMessage()).replace(p.getName(), "**" + np.getDisplayName() + " **"))));
        embed.setColor(0xd60202);

        new DiscordWebhook().setURL(publicWebhookUrl).addEmbed(embed).send();

        // Lifesteal handle death

        if (lifestealEnabled && damageCauseMap.containsKey(p.getUniqueId())) {
            Player attacker;
            Entity damager = damageCauseMap.get(p.getUniqueId()).getCausingEntity();

            if (damager instanceof Player) {
                attacker = (Player) damager;
            }

            else if (damager instanceof Projectile) {
                // We already guaranteed that anything in the map will either be a Player or a Projectile with a causing player.
                attacker = (Player) ((Projectile) damager).getShooter();
            }

            else {
                // Should be impossible to get to this branch as long as the DamageSource is already filtered.
                plugin.getLogger().severe("\"Impossible\" condition reached in StockSurvival PlayerDeathEvent handler!");
                return;
            }

            lifeStealHandleKill(p, attacker);
        }
    }

    @EventHandler
    public static void onAdvancement(PlayerAdvancementDoneEvent e) {
        if (e.getAdvancement().getDisplay() == null || !e.getAdvancement().getDisplay().doesAnnounceToChat()) {
            return;
        }

        String advancementDisplayName  = PlainTextComponentSerializer.plainText().serialize(e.getAdvancement().getDisplay().title());

        DiscordEmbed embed = new DiscordEmbed();
        embed.setTitle("");
        embed.setDescription(NetworkPlayer.resolvePlayer(e.getPlayer().getUniqueId()).getDisplayName() + " has made the advancement **" + advancementDisplayName + "**");
        embed.setColor(0x8814ba);

        new DiscordWebhook().setURL(publicWebhookUrl).addEmbed(embed).send();
    }


    public static boolean isProtected(Player p) {
        return getProtectionTimeLeft(p) >= 0 && !hasForfeitProtection(p);
    }

    public static boolean hasForfeitProtection(Player p) {
        NetworkPlayer np = NetworkPlayer.resolvePlayer(p.getUniqueId());

        GameStat stat = GameStat.getStat(np.getUUID(), newbieProtForfeitKey);

        if (stat == null) {
            return false;
        }

        return stat.getValue().equalsIgnoreCase("true");
    }

    public static long getProtectionTimeLeft(Player p) {
        NetworkPlayer np = NetworkPlayer.resolvePlayer(p.getUniqueId());

        return newbieProtectionSeconds - np.getLivePlaytime();
    }

    public static void putInCombat(Player p, long seconds) {
        long originalExpireTimestamp = TimeUtils.unixTimeNow() + seconds;

        combatLogMap.put(p.getUniqueId(), originalExpireTimestamp);

         final long[] secs = new long[]{seconds};

        new BukkitRunnable() {public void run() {
            // To prevent multiple tasks from spinning on the same player, we can compare the original timestamp to what is in the map now.

            if (combatLogMap.get(p.getUniqueId()) == null || originalExpireTimestamp != combatLogMap.get(p.getUniqueId()) || p.isDead()) {
                combatLogMap.remove(p.getUniqueId());
                this.cancel();
                return;
            }

            if (secs[0] > 0) {
                String unparsed = MessageGrabber.grab(IN_COMBAT).replace("{remaining-time}", TimeUtils.countdown(secs[0]));
                MessageUtils.sendActionBar(p, unparsed);
            }

            else {
                MessageUtils.sendActionBar(p, MessageGrabber.grab(OUT_COMBAT));
                this.cancel();
                return;
            }

            secs[0] -= 1;

        }}.runTaskTimer(plugin, 0, 20L);
    }

    private static boolean isInCombat(Player p) {
        UUID uuid = p.getUniqueId();

        long expireTimestamp = combatLogMap.get(uuid) != null ? combatLogMap.get(uuid) : 0;

        if (expireTimestamp > TimeUtils.unixTimeNow())
            return true;

        // If not in combat, remove from the map
        combatLogMap.remove(uuid);
        return false;
    }

    public static double getCombatTimeLeft(Player p) {
        if (!isInCombat(p))
            return 0.0;

        long expireTimestamp = combatLogMap.get(p.getUniqueId());

        return expireTimestamp - ((double) System.currentTimeMillis() / 1000);
    }

    public static void lifeStealHandleKill(Player p, Player attacker) {
        if (!lifestealEnabled)
            return;

        int attackerHP = getPlayerMaxHP(attacker);

        if (attackerHP >= lifestealHPMax) {
            // If the attacker has reached the maximum HP then do not run lifesteal.
            return;
        }

        int playerHP = getPlayerMaxHP(p);

        int newPlayerHP = playerHP - lifestealHPGain;
        int newAttackerHP = Math.min(attackerHP + lifestealHPGain, lifestealHPMax);

        Map<String, Object> commonMap = MapFormatters.playerFormatter(NetworkPlayer.resolvePlayer(p.getUniqueId()));
        Map<String, Object> attackerMap = MapFormatters.playerFormatter(NetworkPlayer.resolvePlayer(attacker.getUniqueId()));
        commonMap.putAll(MapFormatters.cloneFormaterToNewKey(attackerMap, "player", "attacker"));
        commonMap.putAll(MapFormatters.cloneFormaterToNewKey(attackerMap, "player", "killer"));

        commonMap.put("player-health", newPlayerHP);
        commonMap.put("player-hp", newPlayerHP);
        commonMap.put("player-hearts", String.format("%.1f", (double) newPlayerHP / 2));

        commonMap.put("attacker-health", newAttackerHP);
        commonMap.put("attacker-hp", newAttackerHP);
        commonMap.put("attacker-hearts", String.format("%.1f", (double) newAttackerHP / 2));

        commonMap.put("killer-health", newAttackerHP);
        commonMap.put("killer-hp", newAttackerHP);
        commonMap.put("killer-hearts", String.format("%.1f", (double) newAttackerHP / 2));

        setPlayerMaxHP(attacker, newAttackerHP);
        setPlayerMaxHP(p, newPlayerHP);

        if (newPlayerHP <= 0) {
            // The player reaches 0 or below HP

            for (String command : lifestealActionsOnFinalKill) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formatPlaceholders(command, commonMap));
            }

        }

        else {
            for (String command : lifestealActionsOnKill) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formatPlaceholders(command, commonMap));
            }
        }

    }

    public static int getPlayerMaxHP(Player p) {

        GameStat npStat = GameStat.getStat(p.getUniqueId(), lifestealMaxHPKey);

        if (npStat == null) {

            int val = 20;
            AttributeInstance maxHealthAttr = p.getAttribute(Attribute.MAX_HEALTH);

            if (maxHealthAttr != null) {
                val = Math.toIntExact(Math.round(maxHealthAttr.getValue()));
            }

            npStat = new GameStat(p.getUniqueId(), lifestealMaxHPKey, String.valueOf(val));
        }

        return Integer.parseInt(npStat.getValue());
    }

    public static void setPlayerMaxHP(Player p, int newValue) {
        AttributeInstance playerAttr = p.getAttribute(Attribute.MAX_HEALTH);

        if (playerAttr == null) {
            throw new RuntimeException(String.format("Player MAX_HEALTH attribute for %s is null.", p.getName()));
        }

        playerAttr.removeModifier(Attribute.MAX_HEALTH.getKey());
        AttributeModifier modifier = new AttributeModifier(Attribute.MAX_HEALTH.getKey(), newValue - playerAttr.getValue(), AttributeModifier.Operation.ADD_NUMBER);
        playerAttr.addModifier(modifier);

        GameStat stat = GameStat.setStat(p.getUniqueId(), lifestealMaxHPKey, String.valueOf(newValue));
        stat.sync();
    }

}
