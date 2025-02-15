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

package sharttils.sharttilsmod.mixins.transformers.sba;

import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import sharttils.sharttilsmod.core.Config;
import sharttils.sharttilsmod.utils.RenderUtil;

@Pseudo
@Mixin(targets = "codes.biscuit.skyblockaddons.features.backpacks.ContainerPreviewManager", remap = false)
public class MixinContainerPreviewManager {
    @Dynamic
    @Redirect(method = "drawContainerPreviews", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RenderItem;func_180450_b(Lnet/minecraft/item/ItemStack;II)V"))
    private static void drawRarityBackground(RenderItem instance, ItemStack itemStack, int x, int y) {
        if (Config.INSTANCE.getShowItemRarity()) {
            RenderUtil.renderRarity(itemStack, x, y);
        }
        instance.renderItemAndEffectIntoGUI(itemStack, x, y);
    }
}
