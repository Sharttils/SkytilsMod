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

package sharttils.sharttilsmod.asm.transformers

import dev.falsehonesty.asmhelper.dsl.instructions.InsnListBuilder
import dev.falsehonesty.asmhelper.dsl.modify
import net.minecraft.util.ResourceLocation
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode
import sharttils.sharttilsmod.utils.SuperSecretSettings
import sharttils.sharttilsmod.utils.Utils
import java.util.*
import kotlin.random.Random

fun injectSplashProgressTransformer() = modify("net.minecraftforge.fml.client.SplashProgress") {
    findMethod("start", "()V").apply {
        val v = localVariables.find {
            it.name == "forgeLoc" && (it.desc == "Ljy;" || it.desc == "Lnet/minecraft/util/ResourceLocation;")
        } ?: return@modify println("unable to find localvar")
        var index = -1
        for (insn in instructions) {
            if (insn is MethodInsnNode) {
                if (insn.owner == "net/minecraftforge/fml/client/SplashProgress" && insn.name == "getMaxTextureSize") {
                    val list = InsnListBuilder(this).apply {
                        aload(v.index)
                        invokeStatic(
                            "sharttils/sharttilsmod/asm/transformers/SplashProgressTransformer",
                            "setForgeGif",
                            "(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/util/ResourceLocation;"
                        )
                        astore().also {
                            index = it.index
                        }
                    }
                    instructions.insertBefore(insn, list.build())
                }
                if (insn.owner == "net/minecraftforge/fml/client/SplashProgress$3" && insn.name == "<init>") {
                    if (index == -1) {
                        println("Failed to inject local variable, breaking")
                        break
                    }
                    instructions.remove(insn.previous)
                    instructions.insertBefore(insn, VarInsnNode(Opcodes.ALOAD, index))
                }
            }
        }
    }
}

object SplashProgressTransformer {
    val gifs = mapOf(
        0.0 to ResourceLocation("sharttils", "sychicpet.gif"),
        88.5 to ResourceLocation("sharttils", "sychiccat.png"),
        94.5 to ResourceLocation("sharttils", "breefingdog.png"),
        96.0 to ResourceLocation("sharttils", "azoopet.gif"),
        99.0 to ResourceLocation("sharttils", "abdpfp.gif"),
        99.7 to ResourceLocation("sharttils", "bigrat.png"),
        // this is around the chance of winning the jackpot on the lottery
        100 - 100 * 1 / 302_575_350.0 to ResourceLocation("sharttils", "jamcat.gif")
    )

    @JvmStatic
    fun setForgeGif(resourceLocation: ResourceLocation): ResourceLocation {
        val cal = GregorianCalendar.getInstance()
        val month = cal.get(GregorianCalendar.MONTH) + 1
        val date = cal.get(GregorianCalendar.DATE)
        if (month == 2 && date == 6) return ResourceLocation(
            "sharttils",
            "partysychic.gif"
        )
        if (SuperSecretSettings.noSychic) return resourceLocation
        if (Utils.isBSMod) return ResourceLocation("sharttils", "bigrat.png")
        if (month == 12 || (month == 1 && date == 1)) return ResourceLocation(
            "sharttils",
            "christmassychicpet.gif"
        )
        return if (SuperSecretSettings.breefingDog) ResourceLocation("sharttils", "breefingdog.png")
        else {
            val weight = Random.nextDouble() * 100
            (gifs.entries.reversed().find { weight >= it.key }?.value ?: ResourceLocation(
                "sharttils",
                "sychicpet.gif"
            )).also {
                println("Rolled a $weight, displaying ${it.resourcePath}")
            }
        }
    }
}