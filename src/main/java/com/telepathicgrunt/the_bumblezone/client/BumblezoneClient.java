package com.telepathicgrunt.the_bumblezone.client;

import com.telepathicgrunt.the_bumblezone.Bumblezone;
import com.telepathicgrunt.the_bumblezone.blocks.BzBlocks;
import com.telepathicgrunt.the_bumblezone.client.rendering.FluidRender;
import com.telepathicgrunt.the_bumblezone.client.rendering.HoneySlimeRendering;
import com.telepathicgrunt.the_bumblezone.dimension.BzSkyProperty;
import com.telepathicgrunt.the_bumblezone.entities.BzEntities;
import com.telepathicgrunt.the_bumblezone.items.BzItems;
import com.telepathicgrunt.the_bumblezone.mixin.SkyPropertiesAccessor;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@SuppressWarnings("deprecation")
public class BumblezoneClient
{
    public static final ResourceLocation FLUID_STILL = new ResourceLocation(Bumblezone.MODID + ":block/sugar_water_still");
    public static final ResourceLocation FLUID_FLOWING = new ResourceLocation(Bumblezone.MODID + ":block/sugar_water_flow");

    public static void subscribeClientEvents(IEventBus modBus, IEventBus forgeBus)
    {
        modBus.addListener(BumblezoneClient::onClientSetup);
        modBus.addListener(BumblezoneClient::registerItemColors);
        forgeBus.addListener(FluidRender::sugarWaterOverlay);
    }

    public static void registerItemColors(final ColorHandlerEvent.Item event) {
        event.getItemColors().register((stack, tintIndex) ->
                ((SpawnEggItem) BzItems.HONEY_SLIME_SPAWN_EGG).getColor(tintIndex), BzItems.HONEY_SLIME_SPAWN_EGG);
    }

    //Deferred because I have been told RenderTypeLookup is not thread safe
    @OnlyIn(Dist.CLIENT)
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        RenderingRegistry.registerEntityRenderingHandler(BzEntities.HONEY_SLIME, HoneySlimeRendering::new);
        SkyPropertiesAccessor.getBY_ResourceLocation().put(new ResourceLocation(Bumblezone.MODID, "sky_property"), new BzSkyProperty());

        FluidRender.setupFluidRendering(BzBlocks.SUGAR_WATER_FLUID, BzBlocks.SUGAR_WATER_FLUID_FLOWING, FLUID_STILL, FLUID_FLOWING);
        DeferredWorkQueue.runLater(() -> {
            RenderTypeLookup.setRenderLayer(BzBlocks.STICKY_HONEY_REDSTONE, RenderType.getCutout());
            RenderTypeLookup.setRenderLayer(BzBlocks.STICKY_HONEY_RESIDUE, RenderType.getCutout());
            RenderTypeLookup.setRenderLayer(BzBlocks.HONEY_CRYSTAL, RenderType.getTranslucent());
            RenderTypeLookup.setRenderLayer(BzBlocks.SUGAR_WATER_BLOCK, RenderType.getTranslucent());
            RenderTypeLookup.setRenderLayer(BzBlocks.SUGAR_WATER_FLUID, RenderType.getTranslucent());
            RenderTypeLookup.setRenderLayer(BzBlocks.SUGAR_WATER_FLUID_FLOWING, RenderType.getTranslucent());
        });


        // Allows shield to use the blocking json file for offset
        ItemModelsProperties.register(
                BzItems.HONEY_CRYSTAL_SHIELD,
                new ResourceLocation("blocking"),
                (itemStack, world, livingEntity) ->
                        livingEntity != null &&
                                livingEntity.isHandActive() &&
                                livingEntity.getActiveItemStack() == itemStack ? 1.0F : 0.0F
        );
    }
}