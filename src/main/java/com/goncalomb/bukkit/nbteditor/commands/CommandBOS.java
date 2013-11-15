package com.goncalomb.bukkit.nbteditor.commands;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.goncalomb.bukkit.bkglib.Lang;
import com.goncalomb.bukkit.bkglib.bkgcommand.BKgCommandException;
import com.goncalomb.bukkit.bkglib.bkgcommand.BKgCommandListener;
import com.goncalomb.bukkit.bkglib.namemaps.EntityTypeMap;
import com.goncalomb.bukkit.bkglib.utils.UtilsMc;
import com.goncalomb.bukkit.nbteditor.NBTEditor;
import com.goncalomb.bukkit.nbteditor.bos.BookOfSouls;
import com.goncalomb.bukkit.nbteditor.nbt.EntityNBT;
import com.goncalomb.bukkit.nbteditor.nbt.FallingBlockNBT;
import com.goncalomb.bukkit.nbteditor.nbt.MobNBT;
import com.goncalomb.bukkit.nbteditor.nbt.attributes.Attribute;
import com.goncalomb.bukkit.nbteditor.nbt.attributes.AttributeContainer;
import com.goncalomb.bukkit.nbteditor.nbt.attributes.AttributeType;
import com.goncalomb.bukkit.nbteditor.nbt.variable.NBTVariable;

public class CommandBOS implements BKgCommandListener {
	
	static BookOfSouls getBos(Player player) throws BKgCommandException {
		return getBos(player, false);
	}
	
	static BookOfSouls getBos(Player player, boolean nullIfMissing) throws BKgCommandException {
		ItemStack item = player.getItemInHand();
		if (BookOfSouls.isValidBook(item)) {
			BookOfSouls bos = BookOfSouls.getFromBook(item);
			if (bos != null) {
				return bos;
			}
			throw new BKgCommandException(Lang._(NBTEditor.class, "bos.corrupted"));
		} else if (!nullIfMissing) {
			throw new BKgCommandException(Lang._(NBTEditor.class, "commands.bookofsouls.holding"));
		}
		return null;
	}
	
	@Command(args = "bookofsouls get", type = CommandType.PLAYER_ONLY, maxargs = 1, usage = "<entity>")
	public boolean getCommand(CommandSender sender, String[] args) throws BKgCommandException {
		if (args.length == 1) {
			EntityType entityType = EntityTypeMap.getByName(args[0]);
			if (entityType != null && EntityNBT.isValidType(entityType)) {
				PlayerInventory inv = CommandUtils.checkFullInventory((Player) sender);
				BookOfSouls bos = new BookOfSouls(EntityNBT.fromEntityType(entityType));
				inv.addItem(bos.getBook());
				sender.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.give"));
				if (entityType == EntityType.ENDERMAN) {
					sender.sendMessage(ChatColor.YELLOW + "(Enderman's carring block id is limited to 127 due to a minecraft bug)");
				}
				return true;
			}
			sender.sendMessage(Lang._(NBTEditor.class, "invalid-entity"));
		}
		sender.sendMessage(Lang._(NBTEditor.class, "entities-prefix") + StringUtils.join(EntityTypeMap.getNames(EntityNBT.getValidEntityTypes()), ", "));
		return false;
	}
	
	@Command(args = "bookofsouls getempty", type = CommandType.PLAYER_ONLY)
	public boolean getemptyCommand(CommandSender sender, String[] args) throws BKgCommandException {
		CommandUtils.checkFullInventory((Player) sender).addItem(BookOfSouls.getEmpty());
		sender.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.give"));
		return true;
	}
	
	@Command(args = "bookofsouls var", type = CommandType.PLAYER_ONLY, minargs = 1, maxargs = Integer.MAX_VALUE, usage = "<variable> [value]")
	public boolean varCommand(CommandSender sender, String[] args) throws BKgCommandException {
		BookOfSouls bos = getBos((Player) sender);
		NBTVariable variable = bos.getEntityNBT().getVariable(args[0]);
		if (variable != null) {
			if(args.length >= 2) {
				if (bos.getEntityNBT() instanceof FallingBlockNBT && ((FallingBlockNBT) bos.getEntityNBT()).hasTileEntityData() && variable.getName().equals("block")) {
					sender.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.falling-block-nop"));
					return true;
				}
				String value = UtilsMc.parseColors(StringUtils.join(args, " ", 1, args.length));
				if (variable.setValue(value)) {
					bos.saveBook();
					sender.sendMessage(Lang._(NBTEditor.class, "variable.updated"));
					return true;
				} else {
					sender.sendMessage(Lang._(NBTEditor.class, "variable.invalid-format", args[0]));
				}
			}
			sender.sendMessage(ChatColor.YELLOW + variable.getFormat());
		} else {
			sender.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.no-variable", args[0]));
		}
		return true;
	}
	
	@Command(args = "bookofsouls clearvar", type = CommandType.PLAYER_ONLY, minargs = 1, usage = "<variable>")
	public boolean clearvarCommand(CommandSender sender, String[] args) throws BKgCommandException {
		BookOfSouls bos = getBos((Player) sender);
		NBTVariable variable = bos.getEntityNBT().getVariable(args[0]);
		if (variable != null) {
			variable.clear();
			bos.saveBook();
			sender.sendMessage(Lang._(NBTEditor.class, "variable.cleared"));
		} else {
			sender.sendMessage(Lang._(NBTEditor.class, "variable.invalid-format", args[0]));
		}
		return true;
	}
	
	@Command(args = "bookofsouls riding", type = CommandType.PLAYER_ONLY)
	public boolean ridingCommand(CommandSender sender, String[] args) throws BKgCommandException {
		Player player = (Player) sender;
		BookOfSouls bos = getBos(player);
		bos.openRidingInventory(player);
		return true;
	}
	
	@Command(args = "bookofsouls items", type = CommandType.PLAYER_ONLY)
	public boolean itemsCommand(CommandSender sender, String[] args) throws BKgCommandException {
		Player player = (Player) sender;
		BookOfSouls bos = getBos(player);
		if (!bos.openInventory(player)) {
			player.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.no-inventory"));
		}
		return true;
	}
	
	@Command(args = "bookofsouls offers", type = CommandType.PLAYER_ONLY)
	public boolean offersCommand(CommandSender sender, String[] args) throws BKgCommandException {
		Player player = (Player) sender;
		if (!getBos(player).openOffersInventory(player)) {
			player.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.no-villager"));
		} else {
			player.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.villager-info"));
		}
		return true;
	}
	
	@Command(args = "bookofsouls dropchance", type = CommandType.PLAYER_ONLY, maxargs = 5, usage = "[<head> <chest> <legs> <feet> <hand>]")
	public boolean dropchanceCommand(CommandSender sender, String[] args) throws BKgCommandException {
		Player player = (Player) sender;
		if (args.length == 0) {
			BookOfSouls bos = getBos(player);
			if (!bos.clearMobDropChance()) {
				player.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.no-mob"));
			} else {
				bos.saveBook();
				player.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.drop-chance.cleared"));
			}
			return true;
		} else if (args.length == 5) {

			float head = 0f, chest = 0f, legs = 0f, feet = 0f, hand = 0f;
			boolean invalid = false;
			try {
				head = Float.parseFloat(args[0]);
				chest = Float.parseFloat(args[1]);
				legs = Float.parseFloat(args[2]);
				feet = Float.parseFloat(args[3]);
				hand = Float.parseFloat(args[4]);
			} catch (NumberFormatException e) {
				invalid = true;
			}
			if (invalid || head < 0 || head > 1 || chest < 0 || chest > 1|| legs < 0 || legs > 1 || feet < 0 || feet > 1 || hand < 0 || hand > 1) {
				player.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.drop-chance.invalid"));
				return true;
			}
			
			BookOfSouls bos = getBos(player);
			if (!bos.setMobDropChance(head, chest, legs, feet, hand)) {
				player.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.no-mob"));
			} else {
				bos.saveBook();
				player.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.drop-chance.set"));
			}
			return true;
		}
		return false;
	}
	
	@Command(args = "bookofsouls attr add", type = CommandType.PLAYER_ONLY, maxargs = 2, usage = "<attribute> <base>")
	public boolean attr_addCommand(CommandSender sender, String[] args) throws BKgCommandException {
		if (args.length == 2) {
			BookOfSouls bos = getBos((Player) sender);
			EntityNBT entityNbt = bos.getEntityNBT();
			if (!(entityNbt instanceof MobNBT)) {
				sender.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.no-mob"));
				return true;
			} else {
				AttributeType attributeType = AttributeType.getByName(args[0]);
				if (attributeType == null) {
					sender.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.attr-invalid"));
				} else {
					double base;
					try {
						base = Double.parseDouble(args[1]);
					} catch (NumberFormatException e) {
						sender.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.attr-invalid-base"));
						return true;
					}
					AttributeContainer attributes = ((MobNBT) entityNbt).getAttributes();
					attributes.setAttribute(new Attribute(attributeType, base));
					((MobNBT) entityNbt).setAttributes(attributes);
					bos.saveBook();
					sender.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.attr-add"));
					return true;
				}
			}
		}
		sender.sendMessage(Lang._(NBTEditor.class, "attributes-prefix") + StringUtils.join(AttributeType.values(), ", "));
		return false;
	}
	
	@Command(args = "bookofsouls attr del", type = CommandType.PLAYER_ONLY, maxargs = 1, usage = "<attribute>")
	public boolean attr_delCommand(CommandSender sender, String[] args) throws BKgCommandException {
		if (args.length == 1) {
			BookOfSouls bos = getBos((Player) sender);
			EntityNBT entityNbt = bos.getEntityNBT();
			if (!(entityNbt instanceof MobNBT)) {
				sender.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.no-mob"));
				return true;
			} else {
				AttributeType attributeType = AttributeType.getByName(args[0]);
				if (attributeType == null) {
					sender.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.attr-invalid"));
				} else {
					AttributeContainer attributes = ((MobNBT) entityNbt).getAttributes();
					if (attributes.removeAttribute(attributeType) != null) {
						((MobNBT) entityNbt).setAttributes(attributes);
						bos.saveBook();
						sender.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.attr-del"));
						return true;
					}
					sender.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.attr-nop", attributeType.toString()));
					return true;
				}
			}
		}
		sender.sendMessage(Lang._(NBTEditor.class, "attributes-prefix") + StringUtils.join(AttributeType.values(), ", "));
		return false;
	}
	
	@Command(args = "bookofsouls attr delall", type = CommandType.PLAYER_ONLY)
	public boolean attr_delallCommand(CommandSender sender, String[] args) throws BKgCommandException {
		BookOfSouls bos = getBos((Player) sender);
		EntityNBT entityNbt = bos.getEntityNBT();
		if (entityNbt instanceof MobNBT) {
			((MobNBT) entityNbt).setAttributes(null);
			bos.saveBook();
			sender.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.attr-cleared"));
			return true;
		}
		sender.sendMessage(Lang._(NBTEditor.class, "commands.bookofsouls.no-mob"));
		return true;
	}
	
}