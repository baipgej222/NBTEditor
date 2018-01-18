/*
 * Copyright (C) 2013-2018 Gonçalo Baltazar <me@goncalomb.com>
 *
 * This file is part of NBTEditor.
 *
 * NBTEditor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NBTEditor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NBTEditor.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.goncalomb.bukkit.nbteditor.nbt;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;

import com.goncalomb.bukkit.mylib.namemaps.EntityTypeMap;
import com.goncalomb.bukkit.mylib.reflect.NBTTagCompound;
import com.goncalomb.bukkit.mylib.reflect.NBTTagList;
import com.goncalomb.bukkit.mylib.reflect.NBTUtils;

public final class SpawnerNBTWrapper {

	private Block _spawnerBlock;
	private NBTTagCompound _data;
	private List<SpawnerEntityNBT> _entities;

	public SpawnerNBTWrapper(Block block) {
		_spawnerBlock = block;
		_data = NBTUtils.getTileEntityNBTData(block);

		if (!_data.hasKey("SpawnData")) {
			// on 1.11 we cannot fetch all the data from a spawner without SpawnData
			// this creates a simple pig (by calling save and loading again)
			// XXX: remove this workaround if not needed
			_entities = new ArrayList<SpawnerEntityNBT>();
			save();
			_data = NBTUtils.getTileEntityNBTData(block);
		}

		if (_data.hasKey("SpawnPotentials")) {
			NBTTagList spawnPotentials = _data.getList("SpawnPotentials");
			int l = spawnPotentials.size();

			_entities = new ArrayList<SpawnerEntityNBT>(l);
			for (int i = 0; i < l; ++i) {
				NBTTagCompound potential = (NBTTagCompound) spawnPotentials.get(i);
				EntityNBT entityNbt = EntityNBT.fromEntityData(potential.getCompound("Entity"));
				if (entityNbt != null) {
					_entities.add(new SpawnerEntityNBT(entityNbt, potential.getInt("Weight")));
				}
			}
			_data.remove("SpawnPotentials");
		} else {
			_entities = new ArrayList<SpawnerEntityNBT>();
		}
	}

	public void addEntity(SpawnerEntityNBT spawnerEntityNbt) {
		_data.setCompound("SpawnData", spawnerEntityNbt.getEntityNBT()._data.clone());
		_entities.add(spawnerEntityNbt);
	}

	public void clearEntities() {
		_entities.clear();
	}

	public void cloneFrom(SpawnerNBTWrapper other) {
		NBTTagCompound clone = other._data.clone();
		clone.remove("id");
		clone.remove("x");
		clone.remove("y");
		clone.remove("z");
		_data.merge(clone);
		this._entities = new ArrayList<SpawnerEntityNBT>(other._entities.size());
		for (SpawnerEntityNBT spawnerEntityNBT : other._entities) {
			this._entities.add(spawnerEntityNBT.clone());
		}
	}

	public List<SpawnerEntityNBT> getEntities() {
		return _entities;
	}

	public void removeEntity(int index) {
		_entities.remove(index);
	}

	public EntityType getCurrentEntity() {
		NBTTagCompound spawnData = _data.getCompound("SpawnData");
		if (spawnData != null) {
			return EntityTypeMap.getByName(spawnData.getString("id"));
		}
		return null;
	}

	public Location getLocation() {
		return _spawnerBlock.getLocation();
	}

	public void save() {
		if (_entities.size() > 0) {
			NBTTagList spawnPotentials = new NBTTagList();
			for (SpawnerEntityNBT spawnerEntityNbt : _entities) {
				spawnPotentials.add(spawnerEntityNbt.buildTagCompound());
			}
			_data.setList("SpawnPotentials", spawnPotentials);
		} else {
			NBTTagCompound simplePig = new NBTTagCompound();
			simplePig.setString("id", "minecraft:pig");
			_data.setCompound("SpawnData", simplePig);
			_data.remove("SpawnPotentials");
		}
		NBTUtils.setTileEntityNBTData(_spawnerBlock, _data);
	}

}
