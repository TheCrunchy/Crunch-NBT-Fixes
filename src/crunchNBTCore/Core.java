package crunchNBTCore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import de.tr7zw.nbtapi.NBTItem;

public class Core extends JavaPlugin {
	public static Plugin plugin;

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(new MountEvent(), this);
		plugin = this;
	}

	public class MountEvent implements Listener {

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

				Core.plugin.getServer().getScheduler().runTaskLater(Core.plugin, () -> {
					inventory.setItem(localSlot, replaced);
		
				}, 1L);
			}
		}

		@EventHandler
		public void onInventoryClose(InventoryCloseEvent event) {
			playerItemCache.remove(event.getPlayer().getUniqueId());
		}
	}
}
