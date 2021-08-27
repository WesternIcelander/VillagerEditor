package io.siggi.villagereditor;

import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;

public class VillagerEditorSession {

	public enum EditorType {
		TRADES, INVENTORY;
	}
	private final Player player;
	public Villager villager = null;
	public GossipEditor gossipEditor;
	public Inventory inventory = null;
	public EditorType editorType = null;
	public VillagerEditorSession(Player player){
		if (player == null) throw new NullPointerException();
		this.player = player;
	}
}
