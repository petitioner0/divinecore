package com.petitioner0.divinecore.damage_type;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypes {
    // Fourth Edict - Run Exhaustion damage
    public static final ResourceKey<DamageType> RUN_EXHAUSTION = key("run_exhaustion");
    
    // Tenth Edict - Sun Gaze damage
    public static final ResourceKey<DamageType> SUN_GAZE = key("sun_gaze");
    
    // Twentieth Edict - Ender Eye Shatter damage
    public static final ResourceKey<DamageType> ENDER_EYE_SHATTER = key("ender_eye_shatter");
    
    // Thirteenth Edict - Crop Punishment damage
    public static final ResourceKey<DamageType> CROP_PUNISHMENT = key("crop_punishment");
    
    // Sixteenth Edict - Whisper of the Void damage
    public static final ResourceKey<DamageType> WHISPER_OF_THE_VOID = key("whisper_of_the_void");

    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath("divinecore", path));
    }

    private ModDamageTypes() {}
}