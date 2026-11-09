package com.vaelohq.valor.mixin;

import com.google.common.util.concurrent.UncheckedExecutionException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Fixes a vanilla freeze on "Save and quit". initiateShutdown queues a logout task on the server thread and waits for
 * it. If the server handles the owner's disconnect in the same tick, it shuts itself down and never runs the queue, so
 * vanilla waits forever. Here the wait gives up once the server has stopped running: a stopping server saves and
 * removes every player itself (stopServer), and the owner was already logged out by the disconnect.
 */
@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer {

    @Redirect(method = "initiateShutdown", at = @At(value = "INVOKE", remap = false,
            target = "Lcom/google/common/util/concurrent/Futures;getUnchecked(Ljava/util/concurrent/Future;)Ljava/lang/Object;"))
    private Object valor$waitWhileRunning(Future<?> task) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        while (true) {
            try {
                return task.get(50, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                if (!server.isServerRunning()) return null; // shutting down: the queued task will never run
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (ExecutionException e) {
                throw new UncheckedExecutionException(e.getCause());
            }
        }
    }
}
