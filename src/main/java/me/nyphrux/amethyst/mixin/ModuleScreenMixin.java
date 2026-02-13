/*
    | Credits to AutismINC for this code.
    | This file is a part of AutismINC's ModuleCredits system.
 */

package me.nyphrux.amethyst.mixin;

import me.nyphrux.amethyst.util.ModuleCreditsIntergration;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.screens.ModuleScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.systems.modules.Module;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    value = {ModuleScreen.class},
    remap = false
)
public abstract class ModuleScreenMixin extends WidgetScreen {
    @Shadow
    @Final
    private Module module;

    public ModuleScreenMixin(GuiTheme theme, String title) {
        super(theme, title);
    }

    @Inject(
        method = {"initWidgets"},
        at = {@At(
            value = "INVOKE",
            target = "Ljava/util/List;isEmpty()Z",
            ordinal = 0
        )}
    )
    private void onInitWidgets(CallbackInfo ci) {
        Module var3 = this.module;
        if (var3 instanceof ModuleCreditsIntergration creditsModule) {
            String credits = creditsModule.getCredits();
            if (credits != null && !credits.isEmpty() && this.module.addon != null && this.module.addon != MeteorClient.ADDON) {
                WHorizontalList creditsRow = (WHorizontalList)this.add(this.theme.horizontalList()).expandX().widget();
                creditsRow.add(this.theme.label("Credits: ").color(this.theme.textSecondaryColor())).widget();
                creditsRow.add(this.theme.label(credits)).widget();
            }
        }

    }
}
