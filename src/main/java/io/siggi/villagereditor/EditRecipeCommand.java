package io.siggi.villagereditor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static io.siggi.villagereditor.Util.add;

public class EditRecipeCommand implements CommandExecutor, TabCompleter {
	EditRecipeCommand(VillagerEditor plugin) {
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
		ItemStack item = player.getInventory().getItemInMainHand();
		MerchantRecipe recipe = Util.unpackRecipe(item);
		if (recipe == null) {
			sender.sendMessage("You're not holding a recipe item!");
		}
		switch (split[0]) {
			case "setmaxuses": {
				recipe.setMaxUses(Integer.parseInt(split[1]));
			}
			break;
			case "setuses": {
				recipe.setUses(Integer.parseInt(split[1]));
			}
			break;
			case "setpricemultiplier": {
				recipe.setPriceMultiplier(Float.parseFloat(split[1]));
			}
			break;
			case "setxp": {
				recipe.setVillagerExperience(Integer.parseInt(split[1]));
			}
			break;
			case "setxpreward": {
				recipe.setExperienceReward(Boolean.parseBoolean(split[1]));
			}
			break;
		}
		player.getInventory().setItemInMainHand(Util.packRecipe(recipe));
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] split) {
		List<String> list = new ArrayList<>();
		if (split.length == 1) {
			add(list, split, "setmaxuses");
			add(list, split, "setuses");
			add(list, split, "setpricemultiplier");
			add(list, split, "setxp");
			add(list, split, "setxpreward");
		} else if (split.length == 2) {
			switch (split[0]) {
				case "setxpreward":{
					add(list, split, "true");
					add(list, split, "false");
				}
					break;
			}
		}
		return list;
	}
}
