package com.petitioner0.divinecore.items.AbstractOrbitingItem.logics;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.*;

import com.petitioner0.divinecore.items.ModItems;
import com.petitioner0.divinecore.items.AbstractOrbitingItem.OrbitingItemManager;

@EventBusSubscriber(modid = "divinecore")
public class OrbitingEffectHandler {

    private static final Map<Item, IOrbitingLogic> LOGICS = new HashMap<>();

    public static void registerLogic(Item item, IOrbitingLogic logic) {
        LOGICS.put(item, logic);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide()) return;

        UUID playerId = player.getUUID();
        Set<Item> items = OrbitingItemManager.getItemsFor(playerId);
        if (items == null || items.isEmpty()) return;

        for (Item item : items) {
            IOrbitingLogic logic = LOGICS.get(item);
            if (logic != null) {
                logic.tick(player);
            }
        }
    }
    public static void registerOrbitingLogics() {
            OrbitingEffectHandler.registerLogic(ModItems.AETHERIAL_REVERIE.get(), new AetheriaReverieLogic());
            /*OrbitingEffectHandler.registerLogic(ModItems.FRAGMENT_OF_NIHIL.get(), new FragmentOfNihilLogic());
            OrbitingEffectHandler.registerLogic(ModItems.PRIMORDIAL_SHATTERER.get(), new PrimordialShattererLogic());
            OrbitingEffectHandler.registerLogic(ModItems.SHARD_OF_THE_SOUL.get(), new ShardOfTheSoulLogic());
            OrbitingEffectHandler.registerLogic(ModItems.ULTIMATE_FULFILLMENT.get(), new UltimateFulfillmentLogic());
            OrbitingEffectHandler.registerLogic(ModItems.INDOMITABLE_WILL.get(), new IndomitableWillLogic());
            OrbitingEffectHandler.registerLogic(ModItems.MYSTIC_ESSENCE.get(), new MysticEssenceLogic());
            OrbitingEffectHandler.registerLogic(ModItems.WAILING_WRAITH.get(), new WailingWraithLogic());
            OrbitingEffectHandler.registerLogic(ModItems.CINDER_OF_MIGHT.get(), new CinderOfMightLogic());
            OrbitingEffectHandler.registerLogic(ModItems.SUNDERED_GLOOM.get(), new SunderedGloomLogic());
            OrbitingEffectHandler.registerLogic(ModItems.EMBER_OF_THE_SUNFORGED.get(), new EmberOfTheSunforgedLogic());
            OrbitingEffectHandler.registerLogic(ModItems.NETHERWYRM_COVENANT_CORE.get(), new NetherwyrmCovenantCoreLogic());
            OrbitingEffectHandler.registerLogic(ModItems.SHATTERED_RIFTSTONE.get(), new ShatteredRiftstoneLogic());
            OrbitingEffectHandler.registerLogic(ModItems.SACRIFICIAL_VERDANT.get(), new SacrificialVerdantLogic());
            OrbitingEffectHandler.registerLogic(ModItems.AURUM_OF_THE_FIRMAMENT.get(), new AurumOfTheFirmamentLogic());
            OrbitingEffectHandler.registerLogic(ModItems.MIMIC_MALEDICTION.get(), new MimicMaledictionLogic());
            OrbitingEffectHandler.registerLogic(ModItems.CRIMSON_BLOODCRYSTAL.get(), new CrimsonBloodcrystalLogic());
            OrbitingEffectHandler.registerLogic(ModItems.HEART_OF_THE_ABYSSAL_MAW.get(), new HeartOfTheAbyssalMawLogic());
            OrbitingEffectHandler.registerLogic(ModItems.WHISPER_OF_THE_VOID.get(), new WhisperOfTheVoidLogic());
            OrbitingEffectHandler.registerLogic(ModItems.ASHLUNG_RELIC.get(), new AshlungRelicLogic());
            OrbitingEffectHandler.registerLogic(ModItems.EVERBLOOM.get(), new EverbloomLogic());
            OrbitingEffectHandler.registerLogic(ModItems.BLOOD_OF_THE_FROST_CORROSION.get(), new BloodOfTheFrostCorrosionLogic());
            OrbitingEffectHandler.registerLogic(ModItems.BLOOD_OF_THE_INFERNAL_SCORCH.get(), new BloodOfTheInfernalScorchLogic());
            OrbitingEffectHandler.registerLogic(ModItems.ORIGIN_OF_FORM_AND_ESSENCE.get(), new OriginOfFormAndEssenceLogic());
            OrbitingEffectHandler.registerLogic(ModItems.ROOT_OF_THOUGHT_AND_SOUL.get(), new RootOfThoughtAndSoulLogic());
            OrbitingEffectHandler.registerLogic(ModItems.GENESIS_OF_LIFE_AND_DEATH.get(), new GenesisOfLifeAndDeathLogic());*/
    }
}