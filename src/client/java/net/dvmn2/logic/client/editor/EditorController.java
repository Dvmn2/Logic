package net.dvmn2.logic.client.editor;

import net.dvmn2.logic.client.network.ClosePayload;
import net.dvmn2.logic.client.network.LogicProtocol;
import net.dvmn2.logic.client.network.OpenPayload;
import net.dvmn2.logic.client.network.SavePayload;
import net.dvmn2.logic.client.network.SignalsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Glue between the network receivers and the editor screen. Every method runs on the client thread. */
public final class EditorController {

    private static final Logger LOGGER = LoggerFactory.getLogger("logic");

    private static boolean sendFailureLogged;

    private EditorController() {
    }

    /** Opens the editor for a circuit sent by the server. */
    public static void onOpen(OpenPayload payload) {
        EditorModel model = EditorModel.from(payload);
        if (model == null) {
            return;
        }
        MinecraftClient.getInstance().setScreen(new EditorScreen(model));
    }

    /** Updates the signals shown by the open editor. */
    public static void onSignals(SignalsPayload payload) {
        if (payload.protocol() != LogicProtocol.PROTOCOL) {
            return;
        }
        if (MinecraftClient.getInstance().currentScreen instanceof EditorScreen screen
                && screen.circuit().equals(payload.circuit())) {
            screen.setSignals(payload.bits());
        }
    }

    /** Closes the editor when the server ended the session. */
    public static void onClose(ClosePayload payload) {
        if (payload.protocol() != LogicProtocol.PROTOCOL) {
            return;
        }
        if (MinecraftClient.getInstance().currentScreen instanceof EditorScreen screen
                && screen.circuit().equals(payload.circuit())) {
            screen.closeByServer();
        }
    }

    /** Forgets the editor when the connection ends. */
    public static void clear() {
        if (MinecraftClient.getInstance().currentScreen instanceof EditorScreen screen) {
            screen.closeByServer();
        }
    }

    /** Sends the result of an editing session; a failure is logged once. */
    static void send(SavePayload payload) {
        try {
            ClientPlayNetworking.send(payload);
        } catch (IllegalStateException exception) {
            if (!sendFailureLogged) {
                sendFailureLogged = true;
                LOGGER.warn("Cannot send the editor result: {}", exception.getMessage());
            }
        }
    }
}
