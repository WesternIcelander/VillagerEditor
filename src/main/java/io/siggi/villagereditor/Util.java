package io.siggi.villagereditor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import io.siggi.nbt.NBTCompound;
import io.siggi.nbt.NBTTool;
import io.siggi.nbt.NBTToolBukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Util {
	private static final Gson gson;
	private static final Gson gsonPretty;

	public static Gson getGson(boolean pretty) {
		return pretty ? gsonPretty : gson;
	}

	static {
		GsonBuilder gsonBuilder = new GsonBuilder();
		NBTTool.registerTo(gsonBuilder);
		gsonBuilder.registerTypeAdapter(MerchantRecipe.class, new TypeAdapter<MerchantRecipe>() {
			@Override
			public MerchantRecipe read(JsonReader reader) throws IOException {
				List<ItemStack> ingredients = new ArrayList<>();
				ItemStack result = null;
				int uses = 0;
				int maxUses = 1;
				float priceMultiplier = 0.0f;
				int villagerExperience = 0;
				boolean experienceReward = false;
				reader.beginObject();
				JsonToken peek;
				while ((peek = reader.peek()) != JsonToken.END_OBJECT) {
					if (peek != JsonToken.NAME) {
						reader.skipValue();
					}
					switch (reader.nextName()) {
						case "ingredients": {
							reader.beginArray();
							while ((peek = reader.peek()) != JsonToken.END_ARRAY) {
								ingredients.add(gson.fromJson(reader, ItemStack.class));
							}
							reader.endArray();
						}
						break;
						case "result": {
							result = gson.fromJson(reader, ItemStack.class);
						}
						break;
						case "uses": {
							uses = reader.nextInt();
						}
						break;
						case "maxUses": {
							maxUses = reader.nextInt();
						}
						break;
						case "priceMultiplier": {
							priceMultiplier = (float) reader.nextDouble();
						}
						break;
						case "villagerExperience": {
							villagerExperience = reader.nextInt();
						}
						break;
						case "experienceReward": {
							experienceReward = reader.nextBoolean();
						}
						break;
					}
				}
				reader.endObject();
				MerchantRecipe recipe = new MerchantRecipe(result, uses, maxUses, experienceReward, villagerExperience, priceMultiplier);
				recipe.setIngredients(ingredients);
				return recipe;
			}

			@Override
			public void write(JsonWriter writer, MerchantRecipe recipe) throws IOException {
				writer.beginObject();
				writer.name("ingredients");
				writer.beginArray();
				for (ItemStack ingredient : recipe.getIngredients()) {
					if (ingredient.getType() == Material.AIR) continue;
					gson.toJson(ingredient, ItemStack.class, writer);
				}
				writer.endArray();
				writer.name("result");
				gson.toJson(recipe.getResult(), ItemStack.class, writer);
				writer.name("uses").value(recipe.getUses());
				writer.name("maxUses").value(recipe.getMaxUses());
				writer.name("priceMultiplier").value(recipe.getPriceMultiplier());
				writer.name("villagerExperience").value(recipe.getVillagerExperience());
				writer.name("experienceReward").value(recipe.hasExperienceReward());
				writer.endObject();
			}
		});
		gsonBuilder.registerTypeAdapter(VillagerInfo.class, new TypeAdapter<VillagerInfo>() {
			@Override
			public VillagerInfo read(JsonReader reader) throws IOException {
				VillagerInfo info = new VillagerInfo();
				reader.beginObject();
				JsonToken peek;
				while ((peek = reader.peek()) != JsonToken.END_OBJECT) {
					if (peek != JsonToken.NAME) {
						reader.skipValue();
					}
					switch (reader.nextName()) {
						case "profession": {
							info.profession = Villager.Profession.valueOf(reader.nextString());
						}
						break;
						case "type": {
							info.type = Villager.Type.valueOf(reader.nextString());
						}
						break;
						case "experience": {
							info.experience = reader.nextInt();
						}
						break;
						case "level": {
							info.level = reader.nextInt();
						}
						break;
						case "recipes": {
							reader.beginArray();
							while ((peek = reader.peek()) != JsonToken.END_ARRAY) {
								info.recipes.add(gson.fromJson(reader, MerchantRecipe.class));
							}
							reader.endArray();
						}
						break;
					}
				}
				reader.endObject();
				return info;
			}

			@Override
			public void write(JsonWriter writer, VillagerInfo info) throws IOException {
				writer.beginObject();
				writer.name("profession").value(info.profession.name());
				writer.name("type").value(info.type.name());
				writer.name("experience").value(info.experience);
				writer.name("level").value(info.level);
				writer.name("recipes");
				writer.beginArray();
				for (MerchantRecipe recipe : info.recipes) {
					gson.toJson(recipe, MerchantRecipe.class, writer);
				}
				writer.endArray();
				writer.endObject();
			}
		});
		gson = gsonBuilder.create();
		gsonBuilder.setPrettyPrinting();
		gsonPretty = gsonBuilder.create();
	}

	public static ItemStack packRecipe(MerchantRecipe recipe) {
		List<ItemStack> ingredients = recipe.getIngredients();
		ItemStack output = recipe.getResult();
		int outputCount = output.getAmount();
		float priceMultiplier = recipe.getPriceMultiplier();
		int uses = recipe.getUses();
		int maxUses = recipe.getMaxUses();
		int villagerExperience = recipe.getVillagerExperience();
		boolean experienceReward = recipe.hasExperienceReward();

		ItemStack stack = new ItemStack(output.getType(), 1);
		ItemMeta meta = stack.getItemMeta();
		List<String> lore = new ArrayList<>();
		lore.add("" + ChatColor.RESET + ChatColor.AQUA + "Inputs:");
		for (ItemStack ingredient : ingredients) {
			if (ingredient.getType() == Material.AIR) continue;
			int amount = ingredient.getAmount();
			lore.add("" + ChatColor.RESET + ChatColor.WHITE + (amount > 1 ? (amount + "x ") : "") + NBTToolBukkit.getItemName(ingredient));
		}
		lore.add("" + ChatColor.RESET + ChatColor.AQUA + "Output:");
		lore.add("" + ChatColor.RESET + ChatColor.WHITE + (outputCount > 1 ? (outputCount + "x ") : "") + NBTToolBukkit.getItemName(output));
		lore.add("" + ChatColor.RESET + ChatColor.AQUA + "Price multiplier: " + ChatColor.WHITE + priceMultiplier);
		lore.add("" + ChatColor.RESET + ChatColor.AQUA + "Uses: " + ChatColor.WHITE + uses);
		lore.add("" + ChatColor.RESET + ChatColor.AQUA + "Max Uses: " + ChatColor.WHITE + maxUses);
		lore.add("" + ChatColor.RESET + ChatColor.AQUA + "Villager XP: " + ChatColor.WHITE + villagerExperience);
		lore.add("" + ChatColor.RESET + ChatColor.AQUA + "XP to player: " + ChatColor.WHITE + experienceReward);

		meta.setLore(lore);
		stack.setItemMeta(meta);

		NBTCompound tag = NBTToolBukkit.getTag(stack);
		tag.setString("villagerEditorRecipe", serializeRecipe(recipe));
		stack = NBTToolBukkit.setTag(stack, tag);
		return stack;
	}

	public static MerchantRecipe unpackRecipe(ItemStack stack) {
		try {
			NBTCompound tag = NBTToolBukkit.getTag(stack);
			return deserializeRecipe(tag.getString("villagerEditorRecipe"));
		} catch (Exception e) {
			return null;
		}
	}

	public static String serializeRecipe(MerchantRecipe recipe) {
		return gson.toJson(recipe, MerchantRecipe.class);
	}

	public static MerchantRecipe deserializeRecipe(String json) {
		return gson.fromJson(json, MerchantRecipe.class);
	}

	public static ItemStack nullIfEmpty(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) return null;
		return item;
	}

	public static void add(List<String> list, String[] split, String item) {
		if (item.startsWith(split[split.length - 1])) {
			list.add(item);
		}
	}
}
