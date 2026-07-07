package com.trapka.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrapkaPlugin extends JavaPlugin {

    // Настройки (можно менять)
    public static final int TRAP_DURATION_SECONDS = 20; // сколько живёт ловушка
    public static final int COOLDOWN_SECONDS = 45;       // кулдаун предмета
    public static final int TRAP_SIZE = 4;                // размер куба 4x4x4

    private NamespacedKey trapkaItemKey;

    // Кулдауны игроков: UUID -> время (мс) когда снова можно использовать
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    // Все активные ловушки, для отмены/защиты блоков
    private final Map<UUID, ActiveTrap> activeTraps = new HashMap<>();

    private TrapkaListener listener;

    @Override
    public void onEnable() {
        this.trapkaItemKey = new NamespacedKey(this, "trapka_item");
        this.listener = new TrapkaListener(this);
        Bukkit.getPluginManager().registerEvents(listener, this);
        getLogger().info("TrapkaPlugin включен. Ловушка 4x4x4, длительность " + TRAP_DURATION_SECONDS
                + " сек, кулдаун " + COOLDOWN_SECONDS + " сек.");
    }

    @Override
    public void onDisable() {
        // Восстанавливаем все активные ловушки при выключении/перезагрузке плагина,
        // чтобы блоки/жидкости не остались испорченными
        for (ActiveTrap trap : new HashMap<>(activeTraps).values()) {
            trap.restore();
        }
        activeTraps.clear();
    }

    public NamespacedKey getTrapkaItemKey() {
        return trapkaItemKey;
    }

    public Map<UUID, Long> getCooldowns() {
        return cooldowns;
    }

    public Map<UUID, ActiveTrap> getActiveTraps() {
        return activeTraps;
    }

    public long getRemainingCooldownMillis(Player player) {
        Long until = cooldowns.get(player.getUniqueId());
        if (until == null) return 0L;
        long left = until - System.currentTimeMillis();
        return Math.max(left, 0L);
    }

    public void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + COOLDOWN_SECONDS * 1000L);
    }

    public ItemStack createTrapkaItem() {
        return TrapkaItem.create(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("trapka")) return false;

        if (args.length >= 1 && args[0].equalsIgnoreCase("give")) {
            Player target;
            if (args.length >= 2) {
                target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Игрок не найден: " + args[1]);
                    return true;
                }
            } else if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage(ChatColor.RED + "Укажи игрока: /trapka give <игрок>");
                return true;
            }

            target.getInventory().addItem(createTrapkaItem());
            sender.sendMessage(ChatColor.GREEN + "Трапка выдана игроку " + target.getName());
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Использование: /trapka give [игрок]");
        return true;
    }
}
