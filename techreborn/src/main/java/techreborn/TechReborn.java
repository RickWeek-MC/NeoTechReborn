package techreborn;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reborncore.RebornRegistry;
import reborncore.common.powerSystem.RebornCapabilities;
import reborncore.common.config.Configuration;
import reborncore.common.util.Torus;
import techreborn.utils.PoweredCraftingHandler;
import techreborn.component.TRDataComponentTypes;
import techreborn.config.TechRebornConfig;
import techreborn.events.ApplyArmorToDamageHandler;
import techreborn.events.TRCapabilities;
import techreborn.init.ModLoot;
import techreborn.init.TRCauldronBehavior;
import techreborn.init.TRDispenserBehavior;
import techreborn.init.TRVillager;
import techreborn.init.ModSounds;
import techreborn.init.TRArmorMaterials;
import techreborn.init.TRContent;
import techreborn.blockentity.GuiType;
import techreborn.init.ModRecipes;
import techreborn.init.TRBlockEntities;
import techreborn.init.TRItemGroup;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import techreborn.events.OreDepthSyncHandler;
import techreborn.init.TREntities;
import techreborn.world.WorldGenerator;
import techreborn.packets.Packets;

/**
 * Entry point of Neo Tech Reborn (NeoForge port of Tech Reborn).
 * Porting is done piece by piece from the upstream sources in _upstream/techreborn-src.
 */
@Mod(TechReborn.MOD_ID)
public class TechReborn {
	public static final String MOD_ID = "techreborn";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public TechReborn(IEventBus modEventBus) {
		LOGGER.info("Neo Tech Reborn loading");

		// Config first: TRContent and OreDistribution read these values while they are being built.
		new Configuration(TechRebornConfig.class, "techreborn");

		// These have to be hooked up before anything is built from them, and they deliberately
		// register ahead of the content: armor materials and data components are needed by the
		// items that TRContent::load creates.
		TRArmorMaterials.register(modEventBus);
		TRDataComponentTypes.register(modEventBus);
		ModSounds.register(modEventBus);

		// Blocks and items cannot be built here -- the registries are frozen during mod
		// construction and every Block and Item constructor takes a holder from its registry.
		// RebornCore runs this callback inside the registration window instead.
		RebornRegistry.addContentLoader(TRContent::load);

		// What this mod's items expose to other mods and to the machines: right now the cell's
		// fluid handler. Read the warning in TRCapabilities before adding to it.
		TRCapabilities.init(modEventBus);

		// Network payloads and the creative tab.
		Packets.init(modEventBus);
		TRItemGroup.register(modEventBus);

		// I tipi di ricetta delle macchine. Qui si creano soltanto, RebornCore li registra
		// dentro il suo evento: vanno creati durante la costruzione della mod, prima che
		// RecipeManager chiuda la coda.
		ModRecipes.init();

		// Machine run sounds, as upstream: off when the volume is 0.
		if (TechRebornConfig.machineSoundVolume > 0) {
			if (TechRebornConfig.machineSoundVolume > 1) {
				TechRebornConfig.machineSoundVolume = 1F;
			}
			reborncore.common.recipes.RecipeCrafter.soundHandler = new ModSounds.SoundHandler();
		}

		// Block entity type e MenuType delle macchine. Non si toccano le classi adesso: i loro
		// campi statici leggono i blocchi di TRContent, che esistono solo dentro la finestra di
		// registrazione. Il primo contatto avviene quando arriva l'evento del registro giusto.
		modEventBus.addListener((RegisterEvent event) -> {
			if (event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) {
				TRBlockEntities.registerAll(event);
			} else if (event.getRegistryKey().equals(Registries.MENU)) {
				GuiType.registerAll(event);
			}
		});

		// Quello che le macchine mostrano al mondo: energia, fluidi e item su ogni lato.
		// Senza questo i cavi delle altre mod non si attaccano nemmeno. RebornCore ha il codice,
		// ma ogni mod deve chiamarlo per i PROPRI block entity type: non c'e' un registro globale.
		modEventBus.addListener((RegisterCapabilitiesEvent event) -> {
			// ATTENZIONE ALL'ORDINE: NeoForge tiene una lista di fornitori per capability e usa IL PRIMO
			// che risponde. La capability generica di RebornCore qui sotto risponde per QUALUNQUE macchina,
			// quindi l'unita' di stoccaggio va registrata PRIMA, altrimenti i tubi vedrebbero solo i suoi
			// due slot e non la riserva interna (blocco M6).
			event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, TRBlockEntities.STORAGE_UNIT, (unit, side) -> unit.getExposedStorage(side));
			RebornCapabilities.registerMachines(event, TRBlockEntities.TYPES.toArray(new BlockEntityType<?>[0]));
			// Il cavo non e' un PowerAcceptor: la sua energia la espone lui (RebornCore per lui da' null).
			event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, TRBlockEntities.CABLE, (cable, side) -> cable.getSideEnergyStorage(side));
		});

		// La tabella delle dimensioni dell'anello del reattore a fusione: si calcola una volta sola,
		// all'avvio, e la usano sia il block entity sia la sua schermata.
		Torus.genSizeMap(TechRebornConfig.fusionControlComputerMaxCoilSize);

		// Nano and Quantum armour soaking up damage. This listens on the game event bus, so it is
		// hooked up here and not from the content loader.
		ApplyArmorToDamageHandler.init();

		// --- blocco M7 ---
		// Il tipo di entita' della bomba atomica.
		TREntities.register(modEventBus);
		// Mondo: il tipo di decoratore dell'albero della gomma e il nostro biome modifier, che
		// sostituisce la Biome Modification API di Fabric (vedi la nota in WorldGenerator).
		WorldGenerator.register(modEventBus);
		// L'altezza dei minerali va al client in fase di configurazione, per il tooltip.
		OreDepthSyncHandler.init(modEventBus);
		PoweredCraftingHandler.setup();
		ModLoot.init();
		TRVillager.register(modEventBus);
		// Infiammabilita' del legno della gomma: su Fabric la metteva FlammableBlockRegistry dal
		// costruttore del blocco, qui la tiene FireBlock e si imposta a setup, con gli stessi numeri
		// che vanilla usa per la quercia.
		modEventBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(TechReborn::commonSetup));
	}

	private static void commonSetup() {
		// blocco M8: i comportamenti di vanilla che si riempiono a mano (servono gli oggetti registrati)
		TRDispenserBehavior.init();
		TRCauldronBehavior.init();
		TRVillager.setup();

		FireBlock fire = (FireBlock) Blocks.FIRE;
		fire.setFlammable(TRContent.RUBBER_PLANKS, 5, 20);
		fire.setFlammable(TRContent.RUBBER_LEAVES, 30, 60);
		fire.setFlammable(TRContent.RUBBER_LOG, 5, 5);

		// Il germoglio della gomma nel vaso: il collegamento sta nel vaso VUOTO di vanilla, e si
		// aggiunge qui perche' ora il registro dei blocchi e' completo (vedi ModRegistry).
		((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(
			BuiltInRegistries.BLOCK.getKey(TRContent.RUBBER_SAPLING), () -> TRContent.POTTED_RUBBER_SAPLING);
	}
}
