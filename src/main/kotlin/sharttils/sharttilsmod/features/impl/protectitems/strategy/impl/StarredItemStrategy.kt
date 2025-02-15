/*
 * Sharttils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2022 Sharttils
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package sharttils.sharttilsmod.features.impl.protectitems.strategy.impl

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import sharttils.sharttilsmod.Sharttils
import sharttils.sharttilsmod.features.impl.dungeons.DungeonFeatures
import sharttils.sharttilsmod.features.impl.protectitems.strategy.ItemProtectStrategy

object StarredItemStrategy : ItemProtectStrategy() {
    override fun worthProtecting(item: ItemStack, extraAttr: NBTTagCompound?, type: ProtectType): Boolean {
        if (extraAttr == null) return false
        when (type) {
            ProtectType.CLICKOUTOFWINDOW, ProtectType.DROPKEYININVENTORY, ProtectType.SALVAGE, ProtectType.SELLTONPC, ProtectType.USERCLOSEWINDOW -> {
                if (extraAttr.hasKey("dungeon_item_level")) {
                    return true
                }
            }
            ProtectType.HOTBARDROPKEY -> {
                if (!DungeonFeatures.hasClearedText && extraAttr.hasKey("dungeon_item_level")) {
                    return true
                }
            }
        }
        return false
    }

    override val isToggled: Boolean
        get() = Sharttils.config.protectStarredItems
}