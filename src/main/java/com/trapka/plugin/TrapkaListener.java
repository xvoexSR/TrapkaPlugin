package com.trapka.plugin;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Iterator;

public class TrapkaListener implements Listener {

    private final TrapkaPlugin plugin;

    public TrapkaListener(TrapkaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!TrapkaItem.isTrapkaItem(plugin, item)) return;

        event.setCancelled(true);

        if (!player.hasPermission("trapka.use")) {
            player.sendMessage(ChatColor.RED + "У тебя нет прав использовать Трапку.");
            return;
        }

        long remaining = plugin.getRemainingCooldownMillis(player);
        if (remaining > 0) {
            player.sendMessage(ChatColor.RED + "Трапка перезаряжается: "
                    + String.format("%.1f", remaining / 1000.0) + " сек.");
            return;
        }

        activateTrap(player, item);
    }

    private void activateTrap(Player player, ItemStack item) {
        int size = TrapkaPlugin.TRAP_SIZE;

        // Базовая точка (пол куба) — блок под ногами игрока
        Location feet = player.getLocation();
        int floorY = feet.getBlockY();

        // Центрируем куб по игроку: 4 блока по X и Z вокруг игрока (игрок примерно в центре)
        int cornerX = feet.getBlockX() - (size / 2 - 1); // при size=4 -> смещение -1
        int cornerZ = feet.getBlockZ() - (size / 2 - 1);

        Location corner = new Location(feet.getWorld(), cornerX, floorY, cornerZ);

        // Списываем предмет
        ItemStack hand = player.getInventory().getItemInMainHand();
        hand.setAmount(hand.getAmount() - 1);
        player.getInventory().setItemInMainHand(hand.getAmount() <= 0 ? null : hand);

        // Ставим кулдаун сразу при использовании
        plugin.setCooldown(player);

        // Строим ловушку
        ActiveTrap trap = new ActiveTrap(plugin, corner, size);
        plugin.getActiveTraps().put(trap.getId(), trap);
        trap.build();

        // Небольшой "выталкивающий вверх" эффект не нужен — просто звук/сообщение
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_PLACE, 1.5f, 0.6f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1.0f, 0.8f);
        player.sendMessage(ChatColor.RED + "Трапка активирована! Ловушка продержится "
                + TrapkaPlugin.TRAP_DURATION_SECONDS + " сек.");
    }

    // Защита блоков ловушки от разрушения игроками, пока она активна
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isProtected(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    // Защита от разрушения взрывами (TNT, крипер и т.д.)
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<org.bukkit.block.Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            org.bukkit.block.Block block = iterator.next();
            if (isProtected(block.getLocation())) {
                iterator.remove();
            }
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        Iterator<org.bukkit.block.Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            org.bukkit.block.Block block = iterator.next();
            if (isProtected(block.getLocation())) {
                iterator.remove();
            }
        }
    }

    private boolean isProtected(Location loc) {
        for (ActiveTrap trap : plugin.getActiveTraps().values()) {
            if (trap.containsLocation(loc)) {
                return true;
            }
        }
        return false;
    }
}
