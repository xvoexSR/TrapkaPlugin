package com.trapka.plugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Представляет одну активную ловушку "Трапка": куб 4x4x4,
 * запоминает исходные блоки (в т.ч. жидкости) и восстанавливает их
 * через заданное время.
 */
public class ActiveTrap {

    private final UUID id = UUID.randomUUID();
    private final TrapkaPlugin plugin;
    private final World world;
    private final int baseX, baseY, baseZ; // угол куба (минимальные координаты)
    private final int size;

    // Сохранённые оригинальные данные блоков: Location -> BlockData
    private final Map<Location, BlockData> savedBlocks = new LinkedHashMap<>();

    private BukkitTask removalTask;
    private boolean restored = false;

    // Материалы стен ловушки
    private static final Material[] WALL_MATERIALS = {
            Material.OBSIDIAN,
            Material.BLACK_CONCRETE,
            Material.BLACK_STAINED_GLASS
    };

    public ActiveTrap(TrapkaPlugin plugin, Location cornerLocation, int size) {
        this.plugin = plugin;
        this.world = cornerLocation.getWorld();
        this.baseX = cornerLocation.getBlockX();
        this.baseY = cornerLocation.getBlockY();
        this.baseZ = cornerLocation.getBlockZ();
        this.size = size;
    }

    public UUID getId() {
        return id;
    }

    public boolean containsLocation(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().equals(world)) return false;
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        return x >= baseX && x < baseX + size
                && y >= baseY && y < baseY + size
                && z >= baseZ && z < baseZ + size;
    }

    /**
     * Строит куб: сохраняет все исходные блоки (пол/стены/потолок и внутренность),
     * снаружи ставит материалы ловушки, внутренность освобождает (воздух),
     * включая жидкости — они будут восстановлены при исчезновении ловушки.
     */
    public void build() {
        int matIndex = 0;
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                for (int dz = 0; dz < size; dz++) {
                    int x = baseX + dx;
                    int y = baseY + dy;
                    int z = baseZ + dz;
                    Block block = world.getBlockAt(x, y, z);
                    Location loc = block.getLocation();

                    // Сохраняем исходное состояние блока (включая воду/лаву/и т.д.)
                    savedBlocks.put(loc, block.getBlockData().clone());

                    boolean isShell = (dx == 0 || dx == size - 1
                            || dy == 0 || dy == size - 1
                            || dz == 0 || dz == size - 1);

                    if (isShell) {
                        Material mat = WALL_MATERIALS[(dx + dy + dz) % WALL_MATERIALS.length];
                        block.setType(mat, false);
                    } else {
                        // Внутренность куба очищается — любые блоки/жидкости внутри пропадают
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }

        // Планируем автоматическое восстановление через TRAP_DURATION_SECONDS
        removalTask = new BukkitRunnable() {
            @Override
            public void run() {
                restore();
            }
        }.runTaskLater(plugin, TrapkaPlugin.TRAP_DURATION_SECONDS * 20L);
    }

    /**
     * Возвращает все блоки (и жидкости) на исходные места и убирает ловушку из реестра.
     */
    public synchronized void restore() {
        if (restored) return;
        restored = true;

        if (removalTask != null) {
            removalTask.cancel();
        }

        for (Map.Entry<Location, BlockData> entry : savedBlocks.entrySet()) {
            Block block = entry.getKey().getBlock();
            block.setBlockData(entry.getValue(), false);
        }

        plugin.getActiveTraps().remove(id);
    }
}
