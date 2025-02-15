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
import sharttils.sharttilsmod.features.impl.handlers.AuctionData
import sharttils.sharttilsmod.features.impl.protectitems.strategy.ItemProtectStrategy

object ItemWorthStrategy : ItemProtectStrategy() {
    override fun worthProtecting(item: ItemStack, extraAttr: NBTTagCompound?, type: ProtectType): Boolean {
        val id = AuctionData.getIdentifier(item) ?: return false
        if (AuctionData.lowestBINs.size == 0) return true
        val value = AuctionData.lowestBINs.getOrDefault(id, 0.0)
        val threshold = Sharttils.config.protectItemBINThreshold.toInt()
        return when (type) {
            ProtectType.CLICKOUTOFWINDOW, ProtectType.DROPKEYININVENTORY, ProtectType.SALVAGE, ProtectType.SELLTONPC, ProtectType.USERCLOSEWINDOW -> {
                value >= threshold
            }
            ProtectType.HOTBARDROPKEY -> {
                !DungeonFeatures.hasClearedText && value >= threshold
            }
        }
    }

    override val isToggled: Boolean
        get() = try {
            Sharttils.config.protectItemBINThreshold.toInt() > 0
        } catch (e: NumberFormatException) {
            false
        }
}