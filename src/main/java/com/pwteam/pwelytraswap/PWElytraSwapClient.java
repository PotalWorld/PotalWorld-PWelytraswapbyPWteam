package com.pwteam.pwelytraswap;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PWElytraSwapClient implements ClientModInitializer {
    public static boolean enabled = true;
    private static boolean wasAirborne;
    private static boolean waitingForJumpRelease;
    private static boolean secondJumpReady;
    private static boolean pendingFlightStart;

    private static int findItem(Minecraft client, boolean elytra) {
        for (int slot : slotArray()) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            if (elytra ? stack.is(Items.ELYTRA) : isChestplate(stack)) return slot;
        }
        return -1;
    }

    private static boolean isChestplate(ItemStack stack) {
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        return !stack.isEmpty() && equippable != null && equippable.slot() == EquipmentSlot.CHEST && !stack.is(Items.ELYTRA);
    }

    private static void swap(int inventorySlot, Minecraft client) {
        if (client.gameMode == null || client.player == null) return;
        int menuSlot = inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
        client.gameMode.handleContainerInput(0, menuSlot, 0, ContainerInput.PICKUP, client.player);
        client.gameMode.handleContainerInput(0, 6, 0, ContainerInput.PICKUP, client.player);
        client.gameMode.handleContainerInput(0, menuSlot, 0, ContainerInput.PICKUP, client.player);
    }

    public static void tryWearElytra(Minecraft client) {
        if (client.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return;
        int slot = findItem(client, true);
        if (slot >= 0) {
            swap(slot, client);
            pendingFlightStart = true;
        }
    }

    public static boolean isSecondJumpReady() {
        return secondJumpReady;
    }

    private static void tryWearChestplate(Minecraft client) {
        if (!client.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return;
        int slot = findItem(client, false);
        if (slot >= 0) swap(slot, client);
    }

    private static int[] slotArray() {
        int[] slots = new int[36];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }

    @Override
    public void onInitializeClient() {
        KeyMapping toggle = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pwelytraswap.toggle", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("pwelytraswap", "general"))));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;
            if (toggle.consumeClick()) {
                enabled = !enabled;
                client.player.sendSystemMessage(Component.translatable("message.pwelytraswap." + (enabled ? "enabled" : "disabled")));
            }
            if (!enabled) return;
            if (pendingFlightStart && client.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
                pendingFlightStart = false;
                if (client.player.tryToStartFallFlying()) {
                    client.player.connection.send(new ServerboundPlayerCommandPacket(
                            client.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                }
            }
            boolean airborne = !client.player.onGround() && !client.player.isInWater();
            if (airborne && !wasAirborne) {
                waitingForJumpRelease = true;
                secondJumpReady = false;
            }
            if (waitingForJumpRelease && !client.options.keyJump.isDown()) {
                waitingForJumpRelease = false;
                secondJumpReady = true;
            }
            if (!airborne && wasAirborne) {
                tryWearChestplate(client);
                waitingForJumpRelease = false;
                secondJumpReady = false;
                pendingFlightStart = false;
            }
            wasAirborne = airborne;
        });
    }
}
