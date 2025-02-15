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
package sharttils.sharttilsmod.mixins.hooks.patcher

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import sharttils.sharttilsmod.Sharttils
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

val sbaOverridePatcher: MethodHandle? by lazy {
    try {
        val sbaClass = Class.forName("codes.biscuit.skyblockaddons.asm.hooks.FontRendererHook")
        val mt = MethodType.methodType(Boolean::class.javaPrimitiveType, String::class.java)
        return@lazy MethodHandles.publicLookup().findStatic(sbaClass, "shouldOverridePatcher", mt)
    } catch (e: Throwable) {
        println("SBA override method not found.")
        e.printStackTrace()
    }
    return@lazy null
}

fun overridePatcherFontRendererHook(text: String, shadow: Boolean, cir: CallbackInfoReturnable<Boolean>) {
    try {
        if (sbaOverridePatcher != null && Sharttils.config.fixSbaChroma) {
            if (sbaOverridePatcher!!.invokeExact(text) as Boolean) cir.returnValue = false
        }
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}