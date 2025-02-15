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
package sharttils.sharttilsmod.features.impl.misc

import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Items
import net.minecraft.inventory.ContainerChest
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import sharttils.sharttilsmod.Sharttils
import sharttils.sharttilsmod.Sharttils.Companion.mc
import sharttils.sharttilsmod.events.impl.GuiContainerEvent.SlotClickEvent
import sharttils.sharttilsmod.events.impl.GuiRenderItemEvent
import sharttils.sharttilsmod.events.impl.PacketEvent.ReceiveEvent
import sharttils.sharttilsmod.utils.ItemUtil.getExtraAttributes
import sharttils.sharttilsmod.utils.Utils
import sharttils.sharttilsmod.utils.stripControlCodes

class MinionFeatures {
    @SubscribeEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        if (event.gui is GuiChest) {
            val chest = (event.gui as GuiChest).inventorySlots as ContainerChest
            val chestName = chest.lowerChestInventory.displayName.unformattedText.trim()
            if (chestName == "Minion Chest") return
        }
        blockUnenchanted = false
    }

    @SubscribeEvent
    fun onReceivePacket(event: ReceiveEvent) {
        if (!Utils.inSkyblock) return
        if (event.packet is S29PacketSoundEffect) {
            val packet = event.packet
            if (packet.soundName == "random.chestopen" && packet.pitch == 1f && packet.volume == 1f) {
                blockUnenchanted = false
            }
        }
    }

    @SubscribeEvent
    fun onSlotClick(event: SlotClickEvent) {
        if (!Utils.inSkyblock) return
        if (event.container is ContainerChest) {
            val chest = event.container
            val inventory = chest.lowerChestInventory
            val slot = event.slot ?: return
            val item = slot.stack
            val inventoryName = inventory.displayName.unformattedText
            if (Sharttils.config.onlyCollectEnchantedItems && inventoryName.contains("Minion") && item != null) {
                if (!item.isItemEnchanted && item.item != Items.skull) {
                    if (inventoryName == "Minion Chest") {
                        if (!blockUnenchanted) {
                            for (i in 0 until inventory.sizeInventory) {
                                val stack = inventory.getStackInSlot(i) ?: continue
                                if (stack.isItemEnchanted || stack.item == Items.skull) {
                                    blockUnenchanted = true
                                    break
                                }
                            }
                        }
                        if (blockUnenchanted && slot.inventory !== mc.thePlayer.inventory) event.isCanceled = true
                    } else {
                        val minionType = inventory.getStackInSlot(4)
                        if (minionType != null) {
                            if (minionType.displayName.stripControlCodes().contains("Minion")) {
                                if (!blockUnenchanted) {
                                    val firstUpgrade = inventory.getStackInSlot(37)
                                    val secondUpgrade = inventory.getStackInSlot(46)
                                    if (firstUpgrade != null) {
                                        if (firstUpgrade.displayName.stripControlCodes()
                                                .contains("Super Compactor")
                                        ) {
                                            blockUnenchanted = true
                                        }
                                    }
                                    if (secondUpgrade != null) {
                                        if (secondUpgrade.displayName.stripControlCodes()
                                                .contains("Super Compactor")
                                        ) {
                                            blockUnenchanted = true
                                        }
                                    }
                                }
                                val index = slot.slotIndex
                                if (blockUnenchanted && slot.inventory !== mc.thePlayer.inventory && index >= 21 && index <= 43 && index % 9 >= 3 && index % 9 <= 7) {
                                    event.isCanceled = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.Post) {
        val item = event.stack ?: return
        if (!Utils.inSkyblock || item.stackSize != 1 || item.tagCompound?.hasKey("SharttilsNoItemOverlay") == true) return
        val extraAttributes = getExtraAttributes(item)
        if (Sharttils.config.showMinionTier && extraAttributes != null && extraAttributes.hasKey("generator_tier")) {
            val s = extraAttributes.getInteger("generator_tier").toString()
            GlStateManager.disableLighting()
            GlStateManager.disableDepth()
            GlStateManager.disableBlend()
            event.fr.drawStringWithShadow(
                s,
                (event.x + 17 - event.fr.getStringWidth(s)).toFloat(),
                (event.y + 9).toFloat(),
                16777215
            )
            GlStateManager.enableLighting()
            GlStateManager.enableDepth()
        }
    }

    companion object {

        private var blockUnenchanted = false
    }
}