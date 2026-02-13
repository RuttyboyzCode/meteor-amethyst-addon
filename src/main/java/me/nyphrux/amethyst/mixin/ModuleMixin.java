/*
    | Credits to AutismINC for this code.
    | This file is a part of AutismINC's ModuleCredits system.
 */

package me.nyphrux.amethyst.mixin;

import me.nyphrux.amethyst.util.ModuleCreditsIntergration;
import meteordevelopment.meteorclient.systems.modules.Module;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(
    value = {Module.class},
    remap = false
)

public abstract class ModuleMixin implements ModuleCreditsIntergration {
    @Unique
    private String credits = null;

    public String getCredits() {
        return this.credits;
    }

    public void setCredits(String credits) {
        this.credits = credits;
    }
}
