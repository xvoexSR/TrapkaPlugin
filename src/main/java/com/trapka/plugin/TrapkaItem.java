package com.trapka.plugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class TrapkaItem {

    // Материал предмета в инвентаре — можно заменить на любой другой (или ресурспак-модель)
    public static final Material ITEM_MATERIAL = Material.TRIPWIRE_HOOK;

    public static ItemStack create(TrapkaPlugin plugin) {
        ItemStack item = new ItemStack(ITEM_MATERIAL, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Трапка");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "ПКМ — поставить ловушку 4x4x4",
                    ChatColor.GRAY + "вокруг себя",
                    "",
                    ChatColor.DARK_GRAY + "Длительность: " + TrapkaPlugin.TRAP_DURATION_SECONDS + " сек",
                    ChatColor.DARK_GRAY + "Перезарядка: " + TrapkaPlugin.COOLDOWN_SECONDS + " сек"
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(plugin.getTrapkaItemKey(), PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isTrapkaItem(TrapkaPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte value = meta.getPersistentDataContainer().get(plugin.getTrapkaItemKey(), PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }
}
