package org.embeddedt.modernfix.forge.mixin.perf.resourcepacks;

import net.minecraft.server.packs.PackType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.resource.PathResourcePack;
import org.embeddedt.modernfix.resources.PackResourcesCacheEngine;
import org.embeddedt.modernfix.util.PackTypeHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

@Mixin(PathResourcePack.class)
public abstract class ModFileResourcePackMixin {
    @Shadow public abstract Set<String> getNamespaces(PackType type);

    @Shadow protected abstract Path resolve(String... paths);

    private PackResourcesCacheEngine cacheEngine;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void cacheResources(String packName, Path source, CallbackInfo ci) {
        this.cacheEngine = null;
    }

    private void generateResourceCache() {
        synchronized (this) {
            if(this.cacheEngine != null)
                return;
            this.cacheEngine = new PackResourcesCacheEngine(this::getNamespaces, (type, namespace) -> this.resolve(type.getDirectory(), namespace));
        }
    }

    @Inject(method = "getNamespaces", at = @At("HEAD"), cancellable = true)
    private void useCacheForNamespaces(PackType type, CallbackInfoReturnable<Set<String>> cir) {
        if(cacheEngine != null) {
            Set<String> namespaces = cacheEngine.getNamespaces(type);
            if(namespaces != null)
                cir.setReturnValue(namespaces);
        }
    }

    @Inject(method = "hasResource(Ljava/lang/String;)Z", at = @At(value = "HEAD"), cancellable = true)
    private void useCacheForExistence(String path, CallbackInfoReturnable<Boolean> cir) {
        this.generateResourceCache();
        if(cacheEngine != null)
            cir.setReturnValue(this.cacheEngine.hasResource(path));
    }

    /**
     * @author embeddedt
     * @reason Use cached listing of mod resources
     */
    @Inject(method = "getResources", at = @At("HEAD"), cancellable = true)
    private void fastGetResources(PackType type, String resourceNamespace, String pathIn, int maxDepth, Predicate<String> filter, CallbackInfoReturnable<Collection<ResourceLocation>> cir)
    {
        if(!PackTypeHelper.isVanillaPackType(type) || this.cacheEngine == null)
            return;
        this.generateResourceCache();
        cir.setReturnValue(this.cacheEngine.getResources(type, resourceNamespace, pathIn, maxDepth, filter));
    }
}