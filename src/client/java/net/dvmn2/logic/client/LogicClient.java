package net.dvmn2.logic.client;

import net.dvmn2.logic.client.editor.EditorController;
import net.dvmn2.logic.client.network.ClosePayload;
import net.dvmn2.logic.client.network.OpenPayload;
import net.dvmn2.logic.client.network.SavePayload;
import net.dvmn2.logic.client.network.SignalsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/** Entry point: registers the payloads, the receivers and the disconnect hook. */
public final class LogicClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.playS2C().register(OpenPayload.ID, OpenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SignalsPayload.ID, SignalsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ClosePayload.ID, ClosePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SavePayload.ID, SavePayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(OpenPayload.ID, (payload, context) ->
                context.client().execute(() -> EditorController.onOpen(payload)));
        ClientPlayNetworking.registerGlobalReceiver(SignalsPayload.ID, (payload, context) ->
                context.client().execute(() -> EditorController.onSignals(payload)));
        ClientPlayNetworking.registerGlobalReceiver(ClosePayload.ID, (payload, context) ->
                context.client().execute(() -> EditorController.onClose(payload)));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                client.execute(EditorController::clear));
    }
}
