package com.petitioner0.divinecore.items;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import com.petitioner0.divinecore.items.AllItems.*;
import com.petitioner0.divinecore.edicts.sixteenth_edicts.PrimordialShatterer;

public final class ModItems {
    public static final String MODID = "divinecore";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<Item> AETHERIAL_REVERIE =
            ITEMS.register("aetherial_reverie", id -> new AetherialReverie(defaultProps()));

    public static final DeferredItem<Item> FRAGMENT_OF_NIHIL =
            ITEMS.register("fragment_of_nihil", id -> new FragmentOfNihil(defaultProps()));

    public static final DeferredItem<Item> PRIMORDIAL_SHATTERER =
            ITEMS.register("primordial_shatterer", id -> new PrimordialShatterer(new Item.Properties()
                    .durability(3)
                    .setNoRepair()
                    .stacksTo(1)
                    .fireResistant()
                    .rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> SHARD_OF_THE_SOUL =
            ITEMS.register("shard_of_the_soul", id -> new ShardOfTheSoul(defaultProps()));
    public static final DeferredItem<Item> ULTIMATE_FULFILLMENT =
            ITEMS.register("ultimate_fulfillment", id -> new UltimateFulfillment(defaultProps()));
    public static final DeferredItem<Item> INDOMITABLE_WILL =
            ITEMS.register("indomitable_will", id -> new IndomitableWill(defaultProps()));
    public static final DeferredItem<Item> MYSTIC_ESSENCE =
            ITEMS.register("mystic_essence", id -> new MysticEssence(defaultProps()));
    public static final DeferredItem<Item> WAILING_WRAITH =
            ITEMS.register("wailing_wraith", id -> new WailingWraith(defaultProps()));
    public static final DeferredItem<Item> CINDER_OF_MIGHT =
            ITEMS.register("cinder_of_might", id -> new CinderOfMight(defaultProps()));
    public static final DeferredItem<Item> SUNDERED_GLOOM =
            ITEMS.register("sundered_gloom", id -> new SunderedGloom(defaultProps()));
    public static final DeferredItem<Item> EMBER_OF_THE_SUNFORGED =
            ITEMS.register("ember_of_the_sunforged", id -> new EmberOfTheSunforged(defaultProps()));
    public static final DeferredItem<Item> NETHERWYRM_COVENANT_CORE =
            ITEMS.register("netherwyrm_covenant_core", id -> new NetherwyrmCovenantCore(defaultProps()));
    public static final DeferredItem<Item> SHATTERED_RIFTSTONE =
            ITEMS.register("shattered_riftstone", id -> new ShatteredRiftstone(defaultProps()));
    public static final DeferredItem<Item> SACRIFICIAL_VERDANT =
            ITEMS.register("sacrificial_verdant", id -> new SacrificialVerdant(defaultProps()));
    public static final DeferredItem<Item> AURUM_OF_THE_FIRMAMENT =
            ITEMS.register("aurum_of_the_firmament", id -> new AurumOfTheFirmament(defaultProps()));
    public static final DeferredItem<Item> MIMIC_MALEDICTION =
            ITEMS.register("mimic_malediction", id -> new MimicMalediction(defaultProps()));
    public static final DeferredItem<Item> CRIMSON_BLOODCRYSTAL =
            ITEMS.register("crimson_bloodcrystal", id -> new CrimsonBloodcrystal(defaultProps()));
    public static final DeferredItem<Item> HEART_OF_THE_ABYSSAL_MAW =
            ITEMS.register("heart_of_the_abyssal_maw", id -> new HeartOfTheAbyssalMaw(defaultProps()));
    public static final DeferredItem<Item> WHISPER_OF_THE_VOID =
            ITEMS.register("whisper_of_the_void", id -> new WhisperOfTheVoid(defaultProps()));
    public static final DeferredItem<Item> ASHLUNG_RELIC =
            ITEMS.register("ashlung_relic", id -> new AshlungRelic(defaultProps()));
    public static final DeferredItem<Item> EVERBLOOM =
            ITEMS.registerSimpleItem("everbloom", defaultProps());
    public static final DeferredItem<Item> BLOOD_OF_THE_FROST_CORROSION =
            ITEMS.register("blood_of_the_frost_corrosion", id -> new BloodOfTheFrostCorrosion(defaultProps()));
    public static final DeferredItem<Item> BLOOD_OF_THE_INFERNAL_SCORCH =
            ITEMS.register("blood_of_the_infernal_scorch", id -> new BloodOfTheInfernalScorch(defaultProps()));
    public static final DeferredItem<Item> ORIGIN_OF_FORM_AND_ESSENCE =
            ITEMS.register("origin_of_form_and_essence", id -> new OriginOfFormAndEssence(defaultProps()));
    public static final DeferredItem<Item> ROOT_OF_THOUGHT_AND_SOUL =
            ITEMS.register("root_of_thought_and_soul", id -> new RootOfThoughtAndSoul(defaultProps()));
    public static final DeferredItem<Item> GENESIS_OF_LIFE_AND_DEATH =
            ITEMS.register("genesis_of_life_and_death", id -> new GenesisOfLifeAndDeath(defaultProps()));
    public static final DeferredItem<Item> SOURCE_OF_VOID_AND_REALITY =
            ITEMS.register("source_of_void_and_reality", id -> new SourceOfVoidAndReality(defaultProps()));

    static {
        ItemHelper.register("aetherial_reverie", AETHERIAL_REVERIE);
        ItemHelper.register("fragment_of_nihil", FRAGMENT_OF_NIHIL);
        ItemHelper.register("primordial_shatterer", PRIMORDIAL_SHATTERER);
        ItemHelper.register("shard_of_the_soul", SHARD_OF_THE_SOUL);
        ItemHelper.register("ultimate_fulfillment", ULTIMATE_FULFILLMENT);
        ItemHelper.register("indomitable_will", INDOMITABLE_WILL);
        ItemHelper.register("mystic_essence", MYSTIC_ESSENCE);
        ItemHelper.register("wailing_wraith", WAILING_WRAITH);
        ItemHelper.register("cinder_of_might", CINDER_OF_MIGHT);
        ItemHelper.register("sundered_gloom", SUNDERED_GLOOM);
        ItemHelper.register("ember_of_the_sunforged", EMBER_OF_THE_SUNFORGED);
        ItemHelper.register("netherwyrm_covenant_core", NETHERWYRM_COVENANT_CORE);
        ItemHelper.register("shattered_riftstone", SHATTERED_RIFTSTONE);
        ItemHelper.register("sacrificial_verdant", SACRIFICIAL_VERDANT);
        ItemHelper.register("aurum_of_the_firmament", AURUM_OF_THE_FIRMAMENT);
        ItemHelper.register("mimic_malediction", MIMIC_MALEDICTION);
        ItemHelper.register("crimson_bloodcrystal", CRIMSON_BLOODCRYSTAL);
        ItemHelper.register("heart_of_the_abyssal_maw", HEART_OF_THE_ABYSSAL_MAW);
        ItemHelper.register("whisper_of_the_void", WHISPER_OF_THE_VOID);
        ItemHelper.register("ashlung_relic", ASHLUNG_RELIC);
        ItemHelper.register("everbloom", EVERBLOOM);
        ItemHelper.register("blood_of_the_frost_corrosion", BLOOD_OF_THE_FROST_CORROSION);
        ItemHelper.register("blood_of_the_infernal_scorch", BLOOD_OF_THE_INFERNAL_SCORCH);
        ItemHelper.register("origin_of_form_and_essence", ORIGIN_OF_FORM_AND_ESSENCE);
        ItemHelper.register("root_of_thought_and_soul", ROOT_OF_THOUGHT_AND_SOUL);
        ItemHelper.register("genesis_of_life_and_death", GENESIS_OF_LIFE_AND_DEATH);
        ItemHelper.register("source_of_void_and_reality", SOURCE_OF_VOID_AND_REALITY);
    }

    private static Item.Properties defaultProps() {
        return new Item.Properties()
                .stacksTo(1)
                .fireResistant()
                .rarity(Rarity.EPIC);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
