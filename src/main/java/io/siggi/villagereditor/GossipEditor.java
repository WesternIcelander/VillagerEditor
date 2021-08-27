package io.siggi.villagereditor;

import org.bukkit.entity.Villager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Predicate;

public class GossipEditor {
	private final Villager villager;
	private final Object nmsVillager;
	private final Object gossipContainer;
	private final Class gossipContainerClass;
	private final Class<? extends Enum> gossipTypeEnum;
	private final Enum[] gossipTypes;
	private final Method getGossipType;
	private final Method addGossip; // (UUID, GossipType, int)
	private final Method getGossip; // (UUID, Predicate<GossipType>)

	public GossipEditor(Villager villager) {
		try {
			this.villager = villager;
			this.nmsVillager = villager.getClass().getDeclaredMethod("getHandle").invoke(villager);
			Object gossipContainer = null;
			for (Method method : nmsVillager.getClass().getDeclaredMethods()) {
				if (method.getParameterCount() == 0 && method.getReturnType().getName().endsWith(".Reputation")) {
					gossipContainer = method.invoke(this.nmsVillager);
				}
			}
			this.gossipContainer = gossipContainer;
			this.gossipContainerClass = gossipContainer.getClass();
			Class<? extends Enum> gossipTypeEnum = null;
			Method addGossip = null;
			Method getGossip = null;
			for (Method method : gossipContainerClass.getDeclaredMethods()) {
				Class<?>[] params = method.getParameterTypes();
				if (params.length == 3) {
					if (params[0].equals(UUID.class) && params[2].equals(int.class)) {
						addGossip = method;
						gossipTypeEnum = (Class<? extends Enum>) params[1];
					}
				} else if (params.length == 2) {
					if (params[0].equals(UUID.class) && params[1].equals(Predicate.class)) {
						getGossip = method;
					}
				}
			}
			this.gossipTypeEnum = gossipTypeEnum;
			this.addGossip = addGossip;
			this.getGossip = getGossip;
			this.gossipTypes = (Enum[]) gossipTypeEnum.getMethod("values").invoke(null);
			this.getGossipType = gossipTypeEnum.getMethod("valueOf", String.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void addGossip(UUID player, Enum gossipType, int amount) {
		try {
			addGossip.invoke(gossipContainer, player, gossipType, amount);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public int getGossip(UUID player, Predicate<Enum> types) {
		try {
			return ((Integer) getGossip.invoke(gossipContainer, player, types)).intValue();
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public int getGossip(UUID player, Enum type) {
		try {
			Predicate<Enum> predicate = (t) -> t == type;
			return ((Integer) getGossip.invoke(gossipContainer, player, predicate)).intValue();
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public Enum[] getGossipTypes() {
		return Arrays.copyOf(gossipTypes, gossipTypes.length);
	}

	public Enum getGossip(String type) {
		try {
			return (Enum) getGossipType.invoke(null, type.toUpperCase());
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
