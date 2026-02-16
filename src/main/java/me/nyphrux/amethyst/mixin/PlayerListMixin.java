package me.nyphrux.amethyst.mixin;

import me.nyphrux.amethyst.api.OnlineApi;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public class PlayerListMixin {

    @Inject(method = "renderLatencyIcon", at = @At("TAIL"))
    private void onRenderLatencyIcon(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        String uuid = entry.getProfile().getId().toString();

        if (OnlineApi.isPlayerOnline(uuid)) {
// fuck knows where the correct pos is so mess on with this :)
            context.fill(x + width + 2, y, x + width + 10, y + 8, 0xFF9966FF);
        }
    }
}
