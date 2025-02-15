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
package sharttils.sharttilsmod.features.impl.events

import gg.essential.universal.UChat
import gg.essential.universal.UResolution
import gg.essential.universal.wrappers.message.UTextComponent
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.event.ClickEvent
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumParticleTypes
import net.minecraft.util.Vec3i
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import org.apache.commons.lang3.time.StopWatch
import sharttils.hylin.request.HypixelAPIException
import sharttils.sharttilsmod.Sharttils
import sharttils.sharttilsmod.Sharttils.Companion.mc
import sharttils.sharttilsmod.core.structure.FloatPair
import sharttils.sharttilsmod.core.structure.GuiElement
import sharttils.sharttilsmod.events.impl.MainReceivePacketEvent
import sharttils.sharttilsmod.events.impl.PacketEvent
import sharttils.sharttilsmod.events.impl.SendChatMessageEvent
import sharttils.sharttilsmod.features.impl.handlers.MayorInfo
import sharttils.sharttilsmod.utils.*
import sharttils.sharttilsmod.utils.graphics.ScreenRenderer
import sharttils.sharttilsmod.utils.graphics.SmartFontRenderer
import sharttils.sharttilsmod.utils.graphics.SmartFontRenderer.TextAlignment
import sharttils.sharttilsmod.utils.graphics.colors.CommonColors
import java.awt.Color
import java.util.concurrent.Future
import kotlin.math.roundToInt

object GriffinBurrows {
    init {
        GriffinGuiElement()
    }

    var lastManualRefresh = 0L

    val burrows = hashMapOf<BlockPos, Burrow>()
    val dugBurrows = hashSetOf<BlockPos>()
    var lastDugBurrow: BlockPos? = null
    val particleBurrows = hashMapOf<BlockPos, ParticleBurrow>()
    var lastDugParticleBurrow: BlockPos? = null
    val burrowRefreshTimer = StopWatch()
    var shouldRefreshBurrows = false

    var hasSpadeInHotbar = false
    var overridePerkCheck = false

    fun refreshBurrows(): Future<*> {
        return Sharttils.threadPool.submit {
            try {
                if (!overridePerkCheck && MayorInfo.jerryMayor?.name != "Diana" && !MayorInfo.mayorPerks.contains("Mythological Ritual")) {
                    UChat.chat(
                        UTextComponent("§c§lHELLOOOOOO??? DIANA ISN'T MAYOR ARE YOU OK??? §6Am I wrong? Click me to disable this check.").setClick(
                            ClickEvent.Action.RUN_COMMAND,
                            "/sharttilsoverridedianacheck"
                        )
                    )
                    return@submit
                }
                val uuid = mc.thePlayer.gameProfile.id
                val apiKey = Sharttils.config.apiKey
                if (apiKey.isBlank()) {
                    UChat.chat("§c§lYour API key is required in order to use the burrow feature. §cPlease set it with /api new or /st setkey <key>")
                    Sharttils.config.showGriffinBurrows = false
                    return@submit
                }
                val profileData =
                    Sharttils.hylinAPI.getLatestSkyblockProfileForMemberSync(uuid)
                if (profileData == null) {
                    UChat.chat("§c§lUnable to find your Skyblock Profile!")
                    return@submit
                }
                val receivedBurrows = profileData.griffin.burrows.associateTo(hashMapOf()) {
                    val b = Burrow(it.x, it.y, it.z, it.type, it.tier, it.chain)
                    b.blockPos to b
                }
                Utils.checkThreadAndQueue {
                    dugBurrows.removeAll {
                        !receivedBurrows.containsKey(it)
                    }
                    particleBurrows.keys.removeAll {
                        receivedBurrows.containsKey(it)
                    }
                    val dupes = receivedBurrows.filterTo(hashMapOf()) { (bpos, _) ->
                        dugBurrows.contains(bpos) || particleBurrows[bpos]?.dug == true
                    }.also { receivedBurrows.entries.removeAll(it.entries) }
                    burrows.clear()
                    burrows.putAll(receivedBurrows)
                    particleBurrows.clear()
                    if (receivedBurrows.size == 0) {
                        if (dupes.isEmpty()) UChat.chat("§cSharttils failed to load griffin burrows. Try manually digging a burrow and switching hubs.") else UChat.chat(
                            "§cSharttils was unable to load fresh burrows. Please wait for the API refresh or switch hubs."
                        )
                    } else UChat.chat("§aSharttils loaded §2${receivedBurrows.size}§a burrows!")
                }
            } catch (apiException: HypixelAPIException) {
                UChat.chat("§cFailed to get burrows with reason: ${apiException.message?.replace(Sharttils.config.apiKey, "*".repeat(Sharttils.config.apiKey.length))}")
            } catch (e: Exception) {
                UChat.chat("§cSharttils ran into a fatal error whilst fetching burrows, please report this on our Discord. ${e::class.simpleName}: ${e.message?.replace(Sharttils.config.apiKey, "*".repeat(Sharttils.config.apiKey.length))}")
                e.printStackTrace()
            }
        }
    }

    @SubscribeEvent
    fun onSendMessage(event: SendChatMessageEvent) {
        if (!event.addToChat && event.message == "/sharttilsoverridedianacheck") {
            event.isCanceled = true
        }
    }


    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        val player = mc.thePlayer
        if (event.phase != TickEvent.Phase.START) return
        hasSpadeInHotbar = player != null && Utils.inSkyblock && (0..7).any {
            player.inventory.getStackInSlot(it).isSpade
        }
        if (!Utils.inSkyblock || player == null || !Sharttils.config.showGriffinBurrows || SBInfo.mode != SkyblockIsland.Hub.mode) return
        if (!burrowRefreshTimer.isStarted) burrowRefreshTimer.start()
        if ((burrowRefreshTimer.time >= 60_000L || shouldRefreshBurrows)) {
            burrowRefreshTimer.reset()
            shouldRefreshBurrows = false
            if (hasSpadeInHotbar) {
                UChat.chat("§aSharttils is looking for burrows...")
                refreshBurrows()
            }
        }
    }

    @SubscribeEvent(receiveCanceled = true, priority = EventPriority.HIGHEST)
    fun onChat(event: ClientChatReceivedEvent) {
        val unformatted = event.message.unformattedText.stripControlCodes()
        if (Sharttils.config.showGriffinBurrows &&
            (unformatted.startsWith("You died") ||
                    unformatted.startsWith("You dug out a Griffin Burrow! (") ||
                    unformatted == "You finished the Griffin burrow chain! (4/4)")
        ) {
            if (lastDugBurrow != null) {
                dugBurrows.add(lastDugBurrow!!)
                burrows.remove(lastDugBurrow!!)
                lastDugBurrow = null
            }
            if (lastDugParticleBurrow != null) {
                val particleBurrow =
                    particleBurrows[lastDugParticleBurrow] ?: return
                particleBurrow.dug = true
                dugBurrows.add(particleBurrow.blockPos)
                particleBurrows.remove(particleBurrow.blockPos)
                lastDugParticleBurrow = null
            }
        }
    }

    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.SendEvent) {
        if (!Utils.inSkyblock || !Sharttils.config.showGriffinBurrows || mc.theWorld == null || mc.thePlayer == null) return
        val pos =
            when {
                event.packet is C07PacketPlayerDigging && event.packet.status == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK -> {
                    event.packet.position
                }
                event.packet is C08PacketPlayerBlockPlacement && event.packet.stack != null -> event.packet.position
                else -> return
            }
        if (mc.thePlayer.heldItem?.isSpade != true || mc.theWorld.getBlockState(pos).block !== Blocks.grass) return
        lastDugBurrow = burrows[pos]?.blockPos ?: lastDugBurrow
        lastDugParticleBurrow = particleBurrows[pos]?.blockPos ?: lastDugParticleBurrow
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorldLastEvent) {
        if (Sharttils.config.showGriffinBurrows) {
            for (burrow in burrows.values) {
                burrow.drawWaypoint(event.partialTicks)
            }
            if (Sharttils.config.particleBurrows) {
                for (pb in particleBurrows.values) {
                    if (pb.hasEnchant && pb.hasFootstep && pb.type != -1) {
                        pb.drawWaypoint(event.partialTicks)
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        burrows.clear()
        particleBurrows.clear()
        shouldRefreshBurrows = true
    }

    class GriffinGuiElement : GuiElement("Griffin Timer", FloatPair(100, 10)) {
        override fun render() {
            if (SBInfo.mode != SkyblockIsland.Hub.mode) return
            val player = mc.thePlayer
            if (toggled && Utils.inSkyblock && player != null && hasSpadeInHotbar) {
                val diff = ((60_000L - burrowRefreshTimer.time) / 1000L).toFloat().roundToInt().toLong()
                val sr = UResolution
                val leftAlign = actualX < sr.scaledWidth / 2f
                val alignment = if (leftAlign) TextAlignment.LEFT_RIGHT else TextAlignment.RIGHT_LEFT
                ScreenRenderer.fontRenderer.drawString(
                    "Time until refresh: " + diff + "s",
                    if (leftAlign) 0f else actualWidth,
                    0f,
                    CommonColors.WHITE,
                    alignment,
                    SmartFontRenderer.TextShadow.NORMAL
                )
            }
        }

        override fun demoRender() {
            ScreenRenderer.fontRenderer.drawString(
                "Time until refresh: 10s",
                0f,
                0f,
                CommonColors.WHITE,
                TextAlignment.LEFT_RIGHT,
                SmartFontRenderer.TextShadow.NORMAL
            )
        }

        override val height: Int
            get() = ScreenRenderer.fontRenderer.FONT_HEIGHT
        override val width: Int
            get() = ScreenRenderer.fontRenderer.getStringWidth("Time until refresh: 10s")
        override val toggled: Boolean
            get() = Sharttils.config.showGriffinBurrows && Sharttils.config.showGriffinCountdown

        init {
            Sharttils.guiManager.registerElement(this)
        }
    }

    @SubscribeEvent
    fun onReceivePacket(event: MainReceivePacketEvent<*, *>) {
        if (!Utils.inSkyblock) return
        if (Sharttils.config.showGriffinBurrows && Sharttils.config.particleBurrows && event.packet is S2APacketParticles) {
            if (SBInfo.mode != SkyblockIsland.Hub.mode) return
            event.packet.apply {
                val pos = BlockPos(x, y, z).down()
                val footstepFilter =
                    type == EnumParticleTypes.FOOTSTEP && count == 1 && speed == 0.0f && xOffset == 0.05f && yOffset == 0.0f && zOffset == 0.05f
                val enchantFilter =
                    type == EnumParticleTypes.ENCHANTMENT_TABLE && count == 5 && speed == 0.05f && xOffset == 0.5f && yOffset == 0.4f && zOffset == 0.5f
                val startFilter =
                    type == EnumParticleTypes.CRIT_MAGIC && count == 4 && speed == 0.01f && xOffset == 0.5f && yOffset == 0.1f && zOffset == 0.5f
                val mobFilter =
                    type == EnumParticleTypes.CRIT && count == 3 && speed == 0.01f && xOffset == 0.5f && yOffset == 0.1f && zOffset == 0.5f
                val treasureFilter =
                    type == EnumParticleTypes.DRIP_LAVA && count == 2 && speed == 0.01f && xOffset == 0.35f && yOffset == 0.1f && zOffset == 0.35f
                if (isLongDistance && (footstepFilter || enchantFilter || startFilter || mobFilter || treasureFilter)) {
                    if (!burrows.containsKey(pos) && !dugBurrows.contains(pos)) {
                        if (burrows.keys.any { it.distanceSq(x, y, z) < 4 }) return
                        val burrow = particleBurrows.getOrPut(pos) {
                            ParticleBurrow(pos, hasFootstep = false, hasEnchant = false, type = -1)
                        }

                        if (!burrow.hasFootstep && footstepFilter) {
                            burrow.hasFootstep = true
                        } else if (!burrow.hasEnchant && enchantFilter) {
                            burrow.hasEnchant = true
                        } else if (burrow.type == -1 && type != EnumParticleTypes.FOOTSTEP && type != EnumParticleTypes.ENCHANTMENT_TABLE) {
                            when {
                                startFilter -> burrow.type = 0
                                mobFilter -> burrow.type = 1
                                treasureFilter -> burrow.type = 2
                            }
                        }
                    }
                }
            }
        }
    }

    abstract class Diggable {
        abstract val x: Int
        abstract val y: Int
        abstract val z: Int
        abstract var type: Int
        val blockPos: BlockPos by lazy {
            BlockPos(x, y, z)
        }

        protected abstract val waypointText: String
        protected abstract val color: Color
        fun drawWaypoint(partialTicks: Float) {
            val (viewerX, viewerY, viewerZ) = RenderUtil.getViewerPos(partialTicks)
            val pos = blockPos
            val x = pos.x - viewerX
            val y = pos.y - viewerY
            val z = pos.z - viewerZ
            val distSq = x * x + y * y + z * z
            GlStateManager.disableDepth()
            GlStateManager.disableCull()
            RenderUtil.drawFilledBoundingBox(
                AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1).expandBlock(),
                this.color,
                (0.1f + 0.005f * distSq.toFloat()).coerceAtLeast(0.2f)
            )
            GlStateManager.disableTexture2D()
            if (distSq > 5 * 5) RenderUtil.renderBeaconBeam(x, y + 1, z, this.color.rgb, 1.0f, partialTicks)
            RenderUtil.renderWaypointText(
                waypointText,
                blockPos.x + 0.5,
                blockPos.y + 5.0,
                blockPos.z + 0.5,
                partialTicks
            )
            GlStateManager.disableLighting()
            GlStateManager.enableTexture2D()
            GlStateManager.enableDepth()
            GlStateManager.enableCull()
        }
    }

    data class ParticleBurrow(
        override val x: Int,
        override val y: Int,
        override val z: Int,
        var hasFootstep: Boolean,
        var hasEnchant: Boolean,
        override var type: Int
    ) : Diggable() {
        var dug = false

        constructor(vec3: Vec3i, hasFootstep: Boolean, hasEnchant: Boolean, type: Int) : this(
            vec3.x,
            vec3.y,
            vec3.z,
            hasFootstep,
            hasEnchant,
            type
        )

        override val waypointText: String
            get() {
                var type = "Burrow"
                when (this.type) {
                    0 -> type = "§aStart"
                    1 -> type = "§cMob"
                    2 -> type = "§6Treasure"
                }
                return "$type §a(Particle)"
            }
        override val color: Color
            get() {
                return when (this.type) {
                    0 -> Sharttils.config.emptyBurrowColor
                    1 -> Sharttils.config.mobBurrowColor
                    2 -> Sharttils.config.treasureBurrowColor
                    else -> Color.WHITE
                }
            }
    }

    data class Burrow(
        override val x: Int, override val y: Int, override val z: Int,
        /**
         * This variable seems to hold whether or not the burrow is the start/empty, a mob, or treasure
         */
        override var type: Int,
        /**
         * This variable holds the Griffin used, -1 means no Griffin, 0 means Common, etc.
         */
        val tier: Int,
        /**
         * This variable appears to hold what order the burrow is in the chain
         */
        private val chain: Int
    ) : Diggable() {
        override val waypointText: String
            get() {
                var type = "Burrow"
                when (this.type) {
                    0 -> type =
                        if (chain == 0) "§aStart" else "§fEmpty"
                    1 -> type = "§cMob"
                    2, 3 -> type = "§6Treasure"
                }
                var closest: FastTravelLocations? = null
                var distance = mc.thePlayer.position.distanceSq(blockPos)
                for (warp in FastTravelLocations.values()) {
                    if (!warp.toggled) continue
                    val warpDistance = blockPos.distanceSq(warp.pos)
                    if (warpDistance < distance) {
                        distance = warpDistance
                        closest = warp
                    }
                }
                return "$type §bPosition: ${chain + 1}/4${
                    if (closest != null) " ${closest.nameWithColor}" else ""
                }"
            }

        override val color: Color
            get() {
                return when (this.type) {
                    0 -> Sharttils.config.emptyBurrowColor
                    1 -> Sharttils.config.mobBurrowColor
                    2, 3 -> Sharttils.config.treasureBurrowColor
                    else -> Color.WHITE
                }
            }
    }

    enum class FastTravelLocations(val nameWithColor: String, val pos: BlockPos) {
        CASTLE("§7CASTLE", BlockPos(-250, 130, 45)),
        CRYPTS("§2CRYPTS", BlockPos(-162, 60, -100)),
        DA("§5DA", BlockPos(91, 74, 173)),
        HUB("§fHUB", BlockPos(-3, 70, -70)),
        MUSEUM("§bMUSEUM", BlockPos(-76, 76, 80));

        val toggled: Boolean
            get() = when (this) {
                CASTLE -> Sharttils.config.burrowCastleFastTravel
                CRYPTS -> Sharttils.config.burrowCryptsFastTravel
                DA -> Sharttils.config.burrowDarkAuctionFastTravel
                HUB -> Sharttils.config.burrowHubFastTravel
                MUSEUM -> Sharttils.config.burrowMuseumFastTravel
            }
    }

    private val ItemStack?.isSpade
        get() = ItemUtil.getSkyBlockItemID(this) == "ANCESTRAL_SPADE"
}