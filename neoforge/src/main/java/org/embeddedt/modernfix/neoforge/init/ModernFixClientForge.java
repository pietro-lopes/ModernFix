package org.embeddedt.modernfix.neoforge.init;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.ModLoadingStage;
import net.neoforged.fml.ModLoadingWarning;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.client.ConfigScreenHandler;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.embeddedt.modernfix.ModernFixClient;
import org.embeddedt.modernfix.neoforge.config.NightConfigFixer;
import org.embeddedt.modernfix.screen.ModernFixConfigScreen;

import java.util.ArrayList;
import java.util.List;

public class ModernFixClientForge {
    private static ModernFixClient commonMod;

    public ModernFixClientForge() {
        commonMod = new ModernFixClient();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::keyBindRegister);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () ->  new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new ModernFixConfigScreen(screen))
        );
    }

    private KeyMapping configKey;

    private void keyBindRegister(RegisterKeyMappingsEvent event) {
        configKey = new KeyMapping("key.modernfix.config", KeyConflictContext.UNIVERSAL, InputConstants.UNKNOWN, "key.modernfix");
        event.register(configKey);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        if(false) {
            event.enqueueWork(() -> {
                ModLoader.get().addWarning(new ModLoadingWarning(ModLoadingContext.get().getActiveContainer().getModInfo(), ModLoadingStage.SIDED_SETUP, "modernfix.connectedness_dynresoruces"));
            });
        }
    }

    @SubscribeEvent
    public void onConfigKey(TickEvent.ClientTickEvent event) {
        if(event.phase == TickEvent.Phase.START && configKey != null && configKey.consumeClick()) {
            Minecraft.getInstance().setScreen(new ModernFixConfigScreen(Minecraft.getInstance().screen));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onClientChat(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("mfrc")
                .executes(context -> {
                    NightConfigFixer.runReloads();
                    return 1;
                }));
    }

    private static final List<String> brandingList = new ArrayList<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderOverlay(CustomizeGuiOverlayEvent.DebugText event) {
        if(commonMod.brandingString != null && Minecraft.getInstance().getDebugOverlay().showDebugScreen()) {
            if(brandingList.size() == 0) {
                brandingList.add("");
                brandingList.add(commonMod.brandingString);
            }
            int targetIdx = 0, numSeenBlanks = 0;
            List<String> right = event.getRight();
            while(targetIdx < right.size()) {
                String s = right.get(targetIdx);
                if(s == null || s.length() == 0) {
                    numSeenBlanks++;
                }
                if(numSeenBlanks == 3)
                    break;
                targetIdx++;
            }
            right.addAll(targetIdx, brandingList);
        }
    }

    @SubscribeEvent
    public void onDisconnect(LevelEvent.Unload event) {
        if(event.getLevel().isClientSide()) {
            DebugScreenOverlay overlay = Minecraft.getInstance().getDebugOverlay();
            Minecraft.getInstance().tell(overlay::clearChunkCache);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartedEvent event) {
        commonMod.onServerStarted(event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderTickEnd(TickEvent.RenderTickEvent event) {
        if(event.phase == TickEvent.Phase.END)
            commonMod.onRenderTickEnd();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRecipes(RecipesUpdatedEvent e) {
        commonMod.onRecipesUpdated();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTags(TagsUpdatedEvent e) {
        commonMod.onTagsUpdated();
    }
}