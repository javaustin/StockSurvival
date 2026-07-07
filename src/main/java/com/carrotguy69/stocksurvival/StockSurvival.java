package com.carrotguy69.stocksurvival;

import com.carrotguy69.cxyz.events.custom.PublicChatEvent;
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
import com.carrotguy69.stocksurvival.cmd.protection._ProtectionSupercommand;
import com.carrotguy69.stocksurvival.event.ChatHandler;
import com.carrotguy69.stocksurvival.messages.MessageGrabber;
import com.carrotguy69.stocksurvival.messages.SurvivalMessageKey;
import com.carrotguy69.stocksurvival.tabCompleters.Protection;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
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
import java.util.HashMap;
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

    public static Map<UUID, Long> combatLogMap = new HashMap<>(); // represents (UUID of player, In expire timestamp)

    public static boolean endDisabled;

    public static PublicChannel publicChat;

    public static String publicWebhookUrl;

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

        saveConfig();

        Objects.requireNonNull(plugin.getCommand("protection")).setExecutor(_ProtectionSupercommand.executor);
        Objects.requireNonNull(plugin.getCommand("protection")).setTabCompleter(Protection.tabCompleter);

        plugin.getServer().getPluginManager().registerEvents(plugin, plugin);

        xrayBlocks = configYML.getStringList("xray.blocks");
        xrayChannel = configYML.getString("xray.notification.channel");
        xrayChatFormat = configYML.getString("xray.notification.message");

        newbieProtectionSeconds = configYML.getInt("newbie-protection", 3600);
        spawnProtectionSeconds = configYML.getInt("spawn-protection", 10);
        combatLoggerSeconds = configYML.getInt("combat-logger-seconds", 10);

        endDisabled = configYML.getBoolean("disable-end", false);

        publicWebhookUrl = configYML.getString("public-webhook-url");


        publicChat = (PublicChannel) ChannelRegistry.getChannelByFunction(ChannelFunction.PUBLIC);

        EventService.addEventHandler(PublicChatEvent.class, new ChatHandler(), Priority.NORMAL);

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

        // I suppose we'll cover both arrow cases
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

        p.setNoDamageTicks(spawnProtectionSeconds * 20);

        SurvivalMessageKey key = p.hasPlayedBefore() ? ON_JOIN : ON_JOIN_FIRST;

        Map<String, Object> commonMap = MapFormatters.playerFormatter(NetworkPlayer.resolvePlayer(p.getUniqueId()));


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
        NetworkPlayer np = NetworkPlayer.resolvePlayer(p.getUniqueId());

        return getProtectionTimeLeft(p) >= 0 && !hasForfeitProtection(p);
    }

    public static boolean hasForfeitProtection(Player p) {
        NetworkPlayer np = NetworkPlayer.resolvePlayer(p.getUniqueId());

        GameStat stat = GameStat.getStat(np.getUUID(), "survival-forfeit-protection");

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

            if (combatLogMap.get(p.getUniqueId()) == null || originalExpireTimestamp != combatLogMap.get(p.getUniqueId())) {
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




}
