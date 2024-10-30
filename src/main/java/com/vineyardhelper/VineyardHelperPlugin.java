package com.vineyardhelper;

import net.runelite.api.*;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameStateChanged;


import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static net.runelite.api.ItemID.*;


@PluginDescriptor(
        name = "Vineyard Helper",
        description = "Highlights pickable grapevines and indicates when Grape barrel is full",
        tags = {"highlight", "grape", "vineyard", "grapevine", "wine", "aldarin"}
)
public class VineyardHelperPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private VineyardHelperOverlay overlay;

    @Inject
    private OverlayManager overlayManager;

    private static final int TARGET_X = 1370;
    private static final int TARGET_Y = 2915;
    private static final int object_id = 3005;
    private static final int barrel_id = 30037;

    private final Set<GraphicsObject> highlightedObjects = new HashSet<>();

    public boolean full = false;
    public boolean barrel = false;

    @Override
    protected void startUp() {
        overlayManager.add(overlay);

    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        // Check for inventory items when the player logs in
        if (event.getGameState() == GameState.LOGGED_IN) {
            checkForItemInInventory();
        }
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        highlightedObjects.clear();
        full = false;
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getContainerId() == InventoryID.INVENTORY.getId()) {
            checkForItemInInventory();
        }
    }

    private void checkForItemInInventory() {
        barrel = false;
        Arrays.stream(client.getItemContainer(InventoryID.INVENTORY).getItems())
                .forEach(item -> {
                    if (item.getId() == barrel_id) {
                        if (!barrel) {
                            overlayManager.add(overlay);
                            barrel = true;
                        }
                    }
                });
        if (!barrel) {
            overlayManager.remove(overlay);
        }
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated event) {
        GraphicsObject graphicsObject = event.getGraphicsObject();
        int objectId = graphicsObject.getId();

        if (object_id == objectId) {
            highlightedObjects.add(graphicsObject);
        }
    }

    public Set<GraphicsObject> getHighlightedObjects() {
        return highlightedObjects;
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        String message = event.getMessage();

        // Check for if the barrel is full
        if (message.contains("Your grape barrel is now full")) {
            full = true;
            }

        if (message.contains("Your grape barrel is already full")) {
            full = true;
        }

        if (message.contains("The grape barrel is empty")) {
            full = false;
        }

        if (message.contains("The grape barrel is partially full")) {
            full = false;
        }
    }

    private boolean isPlayerOnTargetTiles() {
        // Check if player is adjacent to Vineyard foreman
        int X = client.getLocalPlayer().getWorldLocation().getX();
        int Y = client.getLocalPlayer().getWorldLocation().getY();
        return Math.abs(X - TARGET_X) == 1 || Math.abs(Y - TARGET_Y) == 1;
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        // Check for NPC dialogue widget using group and child IDs
        Widget npcDialogueWidget = client.getWidget(ComponentID.DIALOG_NPC_TEXT);
        if (npcDialogueWidget != null) {
            if (isPlayerOnTargetTiles()) {
                String currentDialogue = npcDialogueWidget.getText();
                processDialogue(currentDialogue);
            }
        }
    }

    private void processDialogue(String dialogue) {
        // Check for dialogue corresponding to handing in barrel
        if (dialogue.contains("Great work!")) {
            full = false;
        }
    }

}
