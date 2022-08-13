package io.siggi.villagereditor;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.siggi.villagereditor.Util.nullIfEmpty;

public class VillagerEditor extends JavaPlugin implements Listener {

	private File savedVillagerDirectory;

	public File getSavedVillagerDirectory() {
		return savedVillagerDirectory;
	}

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);

		PluginCommand pluginCommandEditVillager = getCommand("editvillager");
		EditVillagerCommand editVillagerCommand = new EditVillagerCommand(this);
		pluginCommandEditVillager.setExecutor(editVillagerCommand);
		pluginCommandEditVillager.setTabCompleter(editVillagerCommand);

		PluginCommand pluginCommandEditRecipe = getCommand("editrecipe");
		EditRecipeCommand editRecipeCommand = new EditRecipeCommand(this);
		pluginCommandEditRecipe.setExecutor(editRecipeCommand);
		pluginCommandEditRecipe.setTabCompleter(editRecipeCommand);

		savedVillagerDirectory = new File(getDataFolder(), "savedvillagers");
		if (!savedVillagerDirectory.exists()) savedVillagerDirectory.mkdirs();
	}

	private final Map<Player, VillagerEditorSession> sessionMap = new HashMap<>();

	@EventHandler
	public void playerJoinEvent(PlayerJoinEvent event) {
		sessionMap.put(event.getPlayer(), new VillagerEditorSession(event.getPlayer()));
	}

	@EventHandler
	public void playerQuitEvent(PlayerQuitEvent event) {
		sessionMap.remove(event.getPlayer());
	}

	public VillagerEditorSession getSession(Player player) {
		return sessionMap.get(player);
	}

	@EventHandler
	public void clickOnVillager(PlayerInteractEntityEvent event) {
		Player player = event.getPlayer();
		if (player.getGameMode() != GameMode.CREATIVE) {
			return;
		}

		Entity entity = event.getRightClicked();
		if (!(entity instanceof Villager)) {
			return;
		}
		Villager villager = (Villager) entity;

		VillagerEditorSession session = getSession(player);

		PlayerInventory inventory = player.getInventory();
		ItemStack mainHand = nullIfEmpty(inventory.getItemInMainHand());
		if (mainHand != null) {
			MerchantRecipe recipe;
			if ((recipe = Util.unpackRecipe(mainHand)) != null) {
				List<MerchantRecipe> recipes = new ArrayList<>();
				recipes.addAll(villager.getRecipes());
				recipes.add(recipe);
				villager.setRecipes(recipes);
				event.setCancelled(true);
				return;
			}
			if (mainHand.getType() == Material.GOLDEN_SHOVEL) {
				session.villager = villager;
				try {
					session.gossipEditor = new GossipEditor(session.villager);
				} catch (Exception e) {
				}
				player.sendMessage("Selected villager");
				event.setCancelled(true);
				return;
			}
		}

		if (inventory.getHeldItemSlot() != 0) {
			return;
		}

		ItemStack stick = nullIfEmpty(inventory.getItem(0));
		if (stick == null || stick.getType() != Material.STICK) {
			return;
		}
		int count = stick.getAmount();

		ItemStack item1 = nullIfEmpty(inventory.getItem(1));
		ItemStack item2 = nullIfEmpty(inventory.getItem(2));
		ItemStack output = nullIfEmpty(inventory.getItem(3));

		if (output == null) return;

		List<ItemStack> ingredients = new ArrayList<>();
		if (item1 != null) ingredients.add(item1);
		if (item2 != null) ingredients.add(item2);
		if (ingredients.isEmpty()) return;

		MerchantRecipe recipe = new MerchantRecipe(output, count);
		recipe.setIngredients(ingredients);

		List<MerchantRecipe> recipes = new ArrayList<>();
		recipes.addAll(villager.getRecipes());
		recipes.add(recipe);
		villager.setRecipes(recipes);

		event.setCancelled(true);
	}

	@EventHandler
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		Player player = (Player) event.getPlayer();
		VillagerEditorSession session = getSession(player);
		if (session == null || session.inventory == null || session.villager == null) return;
		InventoryView view = event.getView();
		Inventory topInventory = view.getTopInventory();
		if (session.inventory != topInventory) return;
		switch (session.editorType) {
			case TRADES: {
				List<MerchantRecipe> recipes = new ArrayList<>();
				for (ItemStack item : session.inventory.getStorageContents()) {
					MerchantRecipe recipe = Util.unpackRecipe(item);
					if (recipe != null) {
						recipes.add(recipe);
					}
				}
				session.villager.setRecipes(recipes);
			}
			break;
			case INVENTORY: {
				Inventory villagerInventory = session.villager.getInventory();
				villagerInventory.clear();
				for (ItemStack stack : session.inventory.getStorageContents()) {
					stack = nullIfEmpty(stack);
					if (stack == null) continue;
					villagerInventory.addItem(stack);
				}
			}
			break;
		}
	}

	void openRecipeEditor(Player player, Villager villager) {
		VillagerEditorSession session = getSession(player);
		Inventory inventory = Bukkit.createInventory(player, 54, "Edit Villager Recipes");
		for (MerchantRecipe recipe : villager.getRecipes()) {
			inventory.addItem(Util.packRecipe(recipe));
		}
		session.villager = villager;
		InventoryView view = player.openInventory(inventory);
		Inventory topInventory = view.getTopInventory();
		session.inventory = topInventory;
		session.editorType = VillagerEditorSession.EditorType.TRADES;
	}
}
