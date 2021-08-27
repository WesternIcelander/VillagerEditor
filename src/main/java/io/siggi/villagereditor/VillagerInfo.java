package io.siggi.villagereditor;

import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;

public class VillagerInfo {

	public VillagerInfo() {
	}
	public VillagerInfo(Villager villager) {
		profession = villager.getProfession();
		type = villager.getVillagerType();
		experience = villager.getVillagerExperience();
		level = villager.getVillagerLevel();
		recipes.addAll(villager.getRecipes());
	}
	public Villager.Profession profession = Villager.Profession.NONE;
	public Villager.Type type = Villager.Type.PLAINS;
	public int experience = 0;
	public int level = 1;
	public final List<MerchantRecipe> recipes = new ArrayList<>();

	public void apply(Villager villager) {
		villager.setProfession(profession);
		villager.setVillagerType(type);
		villager.setVillagerExperience(experience);
		villager.setVillagerLevel(level);
		villager.setRecipes(recipes);
	}
}
