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
package sharttils.sharttilsmod.features.impl.dungeons

import gg.essential.api.EssentialAPI
import gg.essential.universal.UResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemStack
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import sharttils.sharttilsmod.Sharttils
import sharttils.sharttilsmod.core.structure.FloatPair
import sharttils.sharttilsmod.core.structure.GuiElement
import sharttils.sharttilsmod.events.impl.GuiContainerEvent
import sharttils.sharttilsmod.features.impl.handlers.AuctionData
import sharttils.sharttilsmod.utils.*
import sharttils.sharttilsmod.utils.graphics.ScreenRenderer
import sharttils.sharttilsmod.utils.graphics.SmartFontRenderer
import sharttils.sharttilsmod.utils.graphics.SmartFontRenderer.TextAlignment
import sharttils.sharttilsmod.utils.graphics.colors.CommonColors
import sharttils.sharttilsmod.utils.graphics.colors.CustomColor


/**
 * Based off of chest profit from code by Quantizr
 * Licensed under GNU GPL v3, with permission given from author
 * @author Quantizr
 */
class ChestProfit {
    @SubscribeEvent
    fun onGUIDrawnEvent(event: GuiContainerEvent.ForegroundDrawnEvent) {
        if (!Utils.inDungeons || DungeonTimer.scoreShownAt == -1L) return
        if (!Sharttils.config.dungeonChestProfit) return
        if (event.container is ContainerChest) {
            val inv = event.container.lowerChestInventory
            if (event.chestName.endsWith(" Chest")) {
                val chestType = DungeonChest.getFromName(event.chestName) ?: return
                val openChest = inv.getStackInSlot(31) ?: return
                if (openChest.displayName == "§aOpen Reward Chest") {
                    for (unclean in ItemUtil.getItemLore(openChest)) {
                        val line = unclean.stripControlCodes()
                        if (line.contains("FREE")) {
                            chestType.price = 0.0
                            break
                        } else if (line.contains(" Coins")) {
                            chestType.price =
                                line.substring(0, line.indexOf(" ")).replace(",".toRegex(), "").toDouble()
                            break
                        }
                    }
                    chestType.value = 0.0
                    chestType.items.clear()
                    for (i in 9..17) {
                        val lootSlot = inv.getStackInSlot(i)
                        val identifier = AuctionData.getIdentifier(lootSlot)
                        if (identifier != null) {
                            var value = AuctionData.lowestBINs[identifier]
                            if (value == null) value = 0.0
                            chestType.value += value
                            chestType.items.add(DungeonChestLootItem(lootSlot, value))
                        }
                    }
                }
                if (chestType.items.size > 0) {
                    val sr = UResolution
                    val leftAlign = element.actualX < sr.scaledWidth / 2f
                    val alignment = if (leftAlign) TextAlignment.LEFT_RIGHT else TextAlignment.RIGHT_LEFT
                    GlStateManager.color(1f, 1f, 1f, 1f)
                    GlStateManager.disableLighting()
                    var drawnLines = 1
                    val profit = chestType.profit
                    ScreenRenderer.fontRenderer.drawString(
                        chestType.displayText + "§f: §" + (if (profit > 0) "a" else "c") + NumberUtil.nf.format(
                            profit
                        ),
                        if (leftAlign) element.actualX else element.actualX + element.width,
                        element.actualY,
                        chestType.displayColor,
                        alignment,
                        SmartFontRenderer.TextShadow.NORMAL
                    )
                    for (item in chestType.items) {
                        val line = item.item.displayName + "§f: §a" + NumberUtil.nf.format(item.value)
                        ScreenRenderer.fontRenderer.drawString(
                            line,
                            if (leftAlign) element.actualX else element.actualX + element.width,
                            element.actualY + drawnLines * ScreenRenderer.fontRenderer.FONT_HEIGHT,
                            CommonColors.WHITE,
                            alignment,
                            SmartFontRenderer.TextShadow.NORMAL
                        )
                        drawnLines++
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        for (chest in DungeonChest.values()) {
            chest.reset()
        }
        rerollBypass = false
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (Sharttils.config.kismetRerollThreshold == 0 || !Utils.inDungeons) return
        if (!rerollBypass && event.slotId == 50 && event.chestName.endsWith(" Chest")) {
            val chestType = DungeonChest.getFromName(event.chestName) ?: return
            if (chestType.value >= Sharttils.config.kismetRerollThreshold * 1_000_000) {
                event.isCanceled = true
                EssentialAPI.getNotifications()
                    .push(
                        "Blocked Chest Reroll",
                        "The ${chestType.displayText} you are rerolling has ${chestType.profit}!\nClick me to disable this warning.",
                        4f,
                        action = {
                            rerollBypass = true
                        })
            }
        }
    }

    private enum class DungeonChest(var displayText: String, var displayColor: CustomColor) {
        WOOD("Wood Chest", CommonColors.BROWN), GOLD("Gold Chest", CommonColors.YELLOW), DIAMOND(
            "Diamond Chest",
            CommonColors.LIGHT_BLUE
        ),
        EMERALD("Emerald Chest", CommonColors.LIGHT_GREEN), OBSIDIAN(
            "Obsidian Chest",
            CommonColors.BLACK
        ),
        BEDROCK("Bedrock Chest", CommonColors.LIGHT_GRAY);

        var price = 0.0
        var value = 0.0
        var items = ArrayList<DungeonChestLootItem>(3)
        val profit
            get() = value - price

        fun reset() {
            price = 0.0
            value = 0.0
            items.clear()
        }

        companion object {
            fun getFromName(name: String?): DungeonChest? {
                if (name.isNullOrBlank()) return null
                return values().find {
                    it.displayText == name
                }
            }
        }
    }

    private class DungeonChestLootItem(var item: ItemStack, var value: Double)
    class DungeonChestProfitElement : GuiElement("Dungeon Chest Profit", FloatPair(200, 120)) {
        override fun render() {
            if (toggled && Utils.inDungeons) {
                val leftAlign = actualX < sr.scaledWidth / 2f
                GlStateManager.color(1f, 1f, 1f, 1f)
                GlStateManager.disableLighting()
                DungeonChest.values().filter { it.items.isNotEmpty() }.forEachIndexed { i, chest ->
                    val profit = chest.value - chest.price
                    ScreenRenderer.fontRenderer.drawString(
                        "${chest.displayText}§f: §${(if (profit > 0) "a" else "c")}${NumberUtil.format(profit.toLong())}",
                        if (leftAlign) 0f else width.toFloat(),
                        (i * ScreenRenderer.fontRenderer.FONT_HEIGHT).toFloat(),
                        chest.displayColor,
                        if (leftAlign) TextAlignment.LEFT_RIGHT else TextAlignment.RIGHT_LEFT,
                        SmartFontRenderer.TextShadow.NORMAL
                    )
                }
            }
        }

        override fun demoRender() {
            RenderUtil.drawAllInList(this, DungeonChest.values().map { "${it.displayText}: §a+300M" })
        }

        override val height: Int
            get() = ScreenRenderer.fontRenderer.FONT_HEIGHT * DungeonChest.values().size
        override val width: Int
            get() = ScreenRenderer.fontRenderer.getStringWidth("Obsidian Chest: 300M")

        override val toggled: Boolean
            get() = Sharttils.config.dungeonChestProfit

        init {
            Sharttils.guiManager.registerElement(this)
        }
    }

    companion object {
        private val element = DungeonChestProfitElement()
        private var rerollBypass = false
    }
}