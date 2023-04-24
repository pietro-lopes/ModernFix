package org.embeddedt.modernfix.mixin.bugfix.preserve_early_window_pos;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.loading.progress.EarlyProgressVisualization;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

@Mixin(Window.class)
public class WindowMixin {
    @Shadow private boolean fullscreen;
    @Shadow private int width;
    @Shadow private int height;
    @Shadow private int windowedWidth;
    @Shadow private int windowedHeight;
    private static Class<?> VISUALIZER;

    static {
        try {
            VISUALIZER = Class.forName("net.minecraftforge.fml.loading.progress.ClientVisualization");
        } catch(ClassNotFoundException e) {
            VISUALIZER = null;
        }
    }

    private Object getEarlyProgressVisualizer() {
        if(VISUALIZER == null || this.fullscreen)
            return null;
        Object o = ObfuscationReflectionHelper.getPrivateValue(EarlyProgressVisualization.class, EarlyProgressVisualization.INSTANCE, "visualization");
        return VISUALIZER.isAssignableFrom(o.getClass()) ? o : null;
    }

    /**
     * Return a null monitor if not in fullscreen and the visualizer is present, so that the code grabs the
     * original X/Y position of the window.
     */
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/ScreenManager;getMonitor(J)Lcom/mojang/blaze3d/platform/Monitor;"))
    private Monitor getMonitor(ScreenManager manager, long id) {
        return getEarlyProgressVisualizer() != null ? null : manager.getMonitor(id);
    }

    /**
     * Grab the original width/height from the window and inject them into our state variables.
     */
    @SuppressWarnings("unchecked")
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/loading/progress/EarlyProgressVisualization;handOffWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J"))
    private long performHandoff(EarlyProgressVisualization instance, IntSupplier width, IntSupplier height, Supplier<String> title, LongSupplier monitor) {
        Object visualizer = getEarlyProgressVisualizer();
        if(visualizer != null) {
            long windowId = ObfuscationReflectionHelper.<Long, Object>getPrivateValue((Class<? super Object>)visualizer.getClass(), visualizer, "window");
            int[] w = new int[1];
            int[] h = new int[1];
            GLFW.glfwGetWindowSize(windowId, w, h);
            this.windowedWidth = this.width = w[0];
            this.windowedHeight = this.height = h[0];
        }
        return instance.handOffWindow(width, height, title, monitor);
    }
}