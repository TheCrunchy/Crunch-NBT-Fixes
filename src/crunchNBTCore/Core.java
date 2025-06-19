package crunchNBTCore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.LogManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import de.tr7zw.nbtapi.NBTItem;

public class Core extends JavaPlugin {
	public static Plugin plugin;
	private final List<Location> simulatedPlayerLocations = new ArrayList<>();

    private final Random random = new Random();
    private volatile long lastTick = System.nanoTime();
    private final long timeout = 180_000; // 60 seconds

	@Override
	public void onEnable() {
		this.getCommand("freeze").setExecutor(new FreezeCommand(this));
		getServer().getPluginManager().registerEvents(new MountEvent(), this);
		plugin = this;
		
	    for (World world : Bukkit.getWorlds()) {
	        for (Entity entity : world.getEntities()) {
	            if (entity.getType().toString().equals("ALEXSMOBS_LEAFCUTTER_ANT")) {
	                entity.remove();
	                getLogger().info("Removed a Leafcutter Ant in world: " + world.getName());
	            }
	        }
	    }
	    
	    // Heartbeat tick
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            lastTick = System.nanoTime();
        }, 0L, 1L); // every tick

        // Watchdog monitor thread
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10_000); // check every 10s
                } catch (InterruptedException ignored) {}

                long elapsed = (System.nanoTime() - lastTick) / 1_000_000;
                if (elapsed > timeout) {
                    getLogger().severe("Main thread unresponsive for " + elapsed + " ms. Attempting shutdown.");
   
                  Runtime.getRuntime().halt(1);
                }
            }
        }, "WatchdogPlugin-Monitor").start();
  
//        World world = Bukkit.getWorlds().get(0); // use first world
//     		    
//     	    if (simulatedPlayerLocations.isEmpty()) {
//     	        int simulationRadiusChunks = 500; // simulate players in 500 chunk radius area
//     	        for (int i = 0; i < 40; i++) {
//     	            int chunkX = random.nextInt(simulationRadiusChunks * 2) - simulationRadiusChunks;
//     	            int chunkZ = random.nextInt(simulationRadiusChunks * 2) - simulationRadiusChunks;
//     	            Location loc = new Location(world, chunkX << 4, 16, chunkZ << 4);
//     	            simulatedPlayerLocations.add(loc);
//     	        }
//     	    }
//     		 new BukkitRunnable() {
//     		        @Override
//     		        public void run() {
//     		            int maxChunksPerRun = 200; // max chunks to load per run
//     		            List<Location> chunksToLoad = new ArrayList<>();
//
//     		            for (Location playerLoc : simulatedPlayerLocations) {
//     		                World w = playerLoc.getWorld();
//     		                int chunkX = playerLoc.getChunk().getX();
//     		                int chunkZ = playerLoc.getChunk().getZ();
//
//     		                // 3x3 chunk area around simulated player location
//     		                for (int dx = -1; dx <= 1; dx++) {
//     		                    for (int dz = -1; dz <= 1; dz++) {
//     		                        int loadX = chunkX + dx;
//     		                        int loadZ = chunkZ + dz;
//     		                        Location chunkLoc = new Location(w, loadX << 4, 16, loadZ << 4);
//     		                        chunksToLoad.add(chunkLoc);
//     		                    }
//     		                }
//     		            }
//
//     		            // Deduplicate and limit chunk loads
//     		            List<Location> uniqueChunks = chunksToLoad.stream()
//     		            	    .distinct()
//     		            	    .limit(maxChunksPerRun)
//     		            	    .collect(Collectors.toList());
//
//     		            for (Location loc : uniqueChunks) {
//     		                Chunk chunk = loc.getWorld().getChunkAt(loc);
//     		                if (!chunk.isLoaded()) {
//     		                    chunk.load(true);
//     		                }
//     		            }
//     		        }
//     		    }.runTaskTimer(this, 20L, 100L); // run every second
     		}
	
	public class FreezeCommand implements CommandExecutor {

	    private final JavaPlugin plugin;

	    public FreezeCommand(JavaPlugin plugin) {
	        this.plugin = plugin;
	    }

	    @Override
	    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
	        if (!sender.hasPermission("watchdog.freeze")) {
	            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
	            return true;
	        }

	        sender.sendMessage(ChatColor.RED + "Freezing the main thread");
	        plugin.getLogger().warning("Intentionally freezing the main thread.");

	        try {
	            // This freezes the main thread (bad practice, but intentional for the watchdog test)
	            Thread.sleep(500_000);
	        } catch (InterruptedException ignored) {
	        }

	        sender.sendMessage(ChatColor.GREEN + "Main thread unfrozen (if still running).");
	        return true;
	    }
	}
	
	public class MountEvent implements Listener {
		
		@EventHandler(priority = EventPriority.LOWEST)
		public void onEntitySpawn(CreatureSpawnEvent event) {
		    Entity entity = event.getEntity();
		    
		    if (!entity.getType().toString().equals("ALEXSMOBS_LEAFCUTTER_ANT")) {
		        return;
		    }

		    // Always cancel the ant spawn
		    event.setCancelled(true);

		    Location loc = entity.getLocation();
		    World world = loc.getWorld();
		    int radius = 10;

		    // Scan nearby blocks in a 21x21x21 cube
		    for (int x = -radius; x <= radius; x++) {
		        for (int y = -radius; y <= radius; y++) {
		            for (int z = -radius; z <= radius; z++) {
		                Location checkLoc = loc.clone().add(x, y, z);
		                Block block = world.getBlockAt(checkLoc);
		                String blockName = block.getType().name().toLowerCase();
		                if (blockName.contains("leafcutter")) {
		                  
		                    block.setType(Material.AIR); 
		                }
		            }
		        }
		    }
		}

		
		private final Map<UUID, ItemStack> playerItemCache = new HashMap<>();

		@EventHandler
		public void onInventoryClick(InventoryClickEvent event) {
			if (!(event.getWhoClicked() instanceof Player))
				return;
			// getLogger().info("Clicked");
			Player player = (Player) event.getWhoClicked();
			ItemStack clicked = event.getCurrentItem();

			if (clicked == null || clicked.getType() == Material.AIR)
				return;

			// Check if the item has NBT data using NBT API
			NBTItem nbtClicked = new NBTItem(clicked);
			if (nbtClicked.getKeys().isEmpty()) {
				// No NBT data, don't cache
				playerItemCache.remove(player.getUniqueId());
				return;
			}

			// Cache the full ItemStack (clone to avoid modifying original)
			playerItemCache.put(player.getUniqueId(), clicked.clone());
		}

		@EventHandler
		public void onInventoryDrag(InventoryDragEvent event) {
			if (!(event.getWhoClicked() instanceof Player))
				return;
			Player player = (Player) event.getWhoClicked();

			ItemStack cachedItem = playerItemCache.get(player.getUniqueId());
			if (cachedItem == null)
				return;

			NBTItem cachedNBT = new NBTItem(cachedItem);
			if (cachedNBT.getKeys().isEmpty())
				return;

			InventoryView view = player.getOpenInventory();

			for (Map.Entry<Integer, ItemStack> entry : event.getNewItems().entrySet()) {
				int rawSlot = entry.getKey();
				ItemStack draggedItem = entry.getValue();

				if (draggedItem == null || draggedItem.getType() == Material.AIR)
					continue;
				if (draggedItem.getType() != cachedItem.getType())
					continue;

				ItemStack replaced = cachedItem.clone();
				replaced.setAmount(draggedItem.getAmount());

				Inventory inventory = view.getInventory(rawSlot);
				int localSlot = view.convertSlot(rawSlot);

				if (localSlot < 0 || localSlot >= inventory.getSize())
					continue;

				Core.plugin.getServer().getScheduler().runTask(Core.plugin, () -> {
					inventory.setItem(localSlot, replaced);
		
				});
			}
		}

		@EventHandler
		public void onInventoryClose(InventoryCloseEvent event) {
			playerItemCache.remove(event.getPlayer().getUniqueId());
		}
	}
}
