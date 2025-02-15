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

package sharttils.sharttilsmod.mixins.transformers.renderer;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sharttils.sharttilsmod.mixins.hooks.renderer.RenderHookKt;

@Mixin(Render.class)
public abstract class MixinRender<T extends Entity> {
    @Inject(method = "renderEntityOnFire", at = @At("HEAD"), cancellable = true)
    private void removeEntityOnFire(Entity entity, double x, double y, double z, float partialTicks, CallbackInfo ci) {
        RenderHookKt.removeEntityOnFire(entity, x, y, z, partialTicks, ci);
    }

    @Inject(method = "renderLivingLabel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V", shift = At.Shift.AFTER))
    private void renderLivingLabel(T entityIn, String str, double x, double y, double z, int maxDistance, CallbackInfo ci) {
        RenderHookKt.renderLivingLabel(entityIn, str, x, y, z, maxDistance, ci);
    }
}
