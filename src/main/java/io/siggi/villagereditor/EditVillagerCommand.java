package io.siggi.villagereditor;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.siggi.villagereditor.Util.add;
import static io.siggi.villagereditor.Util.nullIfEmpty;

public class EditVillagerCommand implements CommandExecutor, TabCompleter {
	EditVillagerCommand(VillagerEditor plugin) {
		this.plugin = plugin;
	}

	private final VillagerEditor plugin;

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] split) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("This command can only be used by in-game players.");
			return true;
		}
		Player player = (Player) sender;
		VillagerEditorSession session = plugin.getSession(player);
		if (session.villager == null) {
			player.sendMessage("Select a villager by right clicking with a golden shovel while in creative mode!");
			return true;
		}
		if (!session.villager.isValid()) {
			session.villager = null;
			player.sendMessage("The selected villager is no longer available.");
			return true;
		}
		switch (split[0]) {
			case "info": {
				player.sendMessage(ChatColor.AQUA + "Type: " + session.villager.getVillagerType().name().toLowerCase());
				player.sendMessage(ChatColor.AQUA + "Profession: " + session.villager.getProfession().name().toLowerCase());
				player.sendMessage(ChatColor.AQUA + "XP: " + session.villager.getVillagerExperience());
				player.sendMessage(ChatColor.AQUA + "Level: " + session.villager.getVillagerLevel());
			}
			break;
			case "recipes": {
				plugin.openRecipeEditor(player, session.villager);
			}
			break;
			case "restock": {
				List<MerchantRecipe> recipes = new ArrayList<>();
				recipes.addAll(session.villager.getRecipes());
				for (MerchantRecipe recipe : recipes) {
					recipe.setUses(0);
				}
				session.villager.setRecipes(recipes);
			}
			break;
			case "inventory": {
				Inventory villagerInventory = session.villager.getInventory();
				int displayInventorySize = ((villagerInventory.getSize() + 8) / 9) * 9;
				Inventory displayInventory = Bukkit.createInventory(player, displayInventorySize, "Edit Villager Inventory");
				for (ItemStack stack : villagerInventory.getStorageContents()) {
					stack = nullIfEmpty(stack);
					if (stack == null) continue;
					displayInventory.addItem(stack);
				}
				InventoryView view = player.openInventory(displayInventory);
				session.inventory = view.getTopInventory();
				session.editorType = VillagerEditorSession.EditorType.INVENTORY;
			}
			break;
			case "settype": {
				try {
					session.villager.setVillagerType(Villager.Type.valueOf(split[1].toUpperCase()));
				} catch (Exception e) {
				}
			}
			break;
			case "setprofession": {
				try {
					session.villager.setProfession(Villager.Profession.valueOf(split[1].toUpperCase()));
				} catch (Exception e) {
				}
			}
			break;
			case "setxp": {
				session.villager.setVillagerExperience(Integer.parseInt(split[1]));
			}
			break;
			case "setlevel": {
				session.villager.setVillagerLevel(Integer.parseInt(split[1]));
			}
			break;
			case "load": {
				String name = split[1];
				if (name.contains("/") || name.contains("\\")) {
					break;
				}
				try (BufferedReader reader = new BufferedReader(new FileReader(new File(plugin.getSavedVillagerDirectory(), name + ".json")))) {
					VillagerInfo info = Util.getGson(false).fromJson(reader, VillagerInfo.class);
					info.apply(session.villager);
				} catch (Exception e) {
				}
			}
			break;
			case "save": {
				String name = split[1];
				if (name.contains("/") || name.contains("\\")) {
					break;
				}
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(plugin.getSavedVillagerDirectory(), name + ".json")))) {
					writer.write(Util.getGson(true).toJson(new VillagerInfo(session.villager)));
				} catch (Exception e) {
				}
			}
			break;
			case "addgossip": {
				String targetPlayerName = split[1];
				String type = split[2];
				int amount = Integer.parseInt(split[3]);
				UUID targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName).getUniqueId();
				Enum gossipType = session.gossipEditor.getGossip(type);
				session.gossipEditor.addGossip(targetPlayer, gossipType, amount);
			}
			break;
			case "getgossip": {
				String targetPlayerName = split.length > 1 ? split[1] : null;
				String type = split.length > 2 ? split[2] : null;
				UUID targetPlayer = targetPlayerName == null ? player.getUniqueId() : Bukkit.getOfflinePlayer(targetPlayerName).getUniqueId();
				if (type == null) {
					for (Enum gossipType : session.gossipEditor.getGossipTypes()) {
						player.sendMessage(ChatColor.AQUA + gossipType.name().toLowerCase() + ": " + ChatColor.WHITE + session.gossipEditor.getGossip(targetPlayer, gossipType));
					}
					player.sendMessage(ChatColor.AQUA + "Overall: " + ChatColor.WHITE + session.gossipEditor.getGossip(targetPlayer, (gossipType) -> true));
				} else {
					Enum gossipType = session.gossipEditor.getGossip(type);
					player.sendMessage(ChatColor.AQUA + type.toLowerCase() + ": " + ChatColor.WHITE + session.gossipEditor.getGossip(targetPlayer, gossipType));
				}
			}
			break;
			default: {
				player.sendMessage("Unknown command: " + split[0]);
			}
		}
		return true;
	}


	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] split) {
		List<String> list = new ArrayList<>();
		if (!(sender instanceof Player)) return list;
		Player player = (Player) sender;
		VillagerEditorSession session = plugin.getSession(player);
		if (split.length == 1) {
			if (session.villager != null) {
				add(list, split, "info");
				add(list, split, "recipes");
				add(list, split, "inventory");
				add(list, split, "settype");
				add(list, split, "setprofession");
				add(list, split, "setlevel");
				add(list, split, "setxp");
				add(list, split, "load");
				add(list, split, "save");
				add(list, split, "restock");
			}
			if (session.gossipEditor != null) {
				add(list, split, "addgossip");
				add(list, split, "getgossip");
			}
		} else if (split.length == 2) {
			switch (split[0]) {
				case "settype": {
					for (Villager.Type type : Villager.Type.values()) {
						add(list, split, type.name().toLowerCase());
					}
				}
				break;
				case "setprofession": {
					for (Villager.Profession profession : Villager.Profession.values()) {
						add(list, split, profession.name().toLowerCase());
					}
				}
				break;
				case "setlevel": {
					add(list, split, "1");
					add(list, split, "2");
					add(list, split, "3");
					add(list, split, "4");
					add(list, split, "5");
				}
				break;
				case "load": {
					for (File file : plugin.getSavedVillagerDirectory().listFiles()) {
						String name = file.getName();
						if (!name.startsWith(".") && name.endsWith(".json")) {
							name = name.substring(0, name.length() - 5);
							add(list, split, name);
						}
					}
				}
				break;
				case "addgossip":
				case "getgossip": {
					if (session.gossipEditor != null) {
						for (Player p : Bukkit.getOnlinePlayers()) {
							add(list, split, p.getName());
						}
					}
				}
				break;
			}
		} else if (split.length == 3) {
			switch (split[0]) {
				case "addgossip":
				case "getgossip": {
					if (session.gossipEditor != null) {
						for (Enum gossipType : session.gossipEditor.getGossipTypes()) {
							add(list, split, gossipType.name().toLowerCase());
						}
					}
				}
				break;
			}
		}
		return list;
	}
}
