package ru.maxthetomas.craftminedailies.auth.meta;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record InventoryMeta(List<SlotItem> itemSlots) {
    public static MapCodec<InventoryMeta> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SlotItem.CODEC.codec().listOf().fieldOf("inventory").forGetter(InventoryMeta::itemSlots)
    ).apply(instance, InventoryMeta::new));

    public record SlotItem(ItemStack stack, int slot) {
        public static MapCodec<SlotItem> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ItemStack.SIMPLE_ITEM_CODEC.fieldOf("item").forGetter(SlotItem::stack),
                Codec.INT.fieldOf("slot").forGetter(SlotItem::slot)
        ).apply(instance, SlotItem::new));
    }

    public static InventoryMeta createForPlayer(ServerPlayer player) {
        var inventoryList = player.getInventory().save(new ListTag());

        List<SlotItem> slotItems = new ArrayList<SlotItem>();
        for (int i = 0; i < inventoryList.size(); ++i) {
            CompoundTag compoundTag = inventoryList.getCompoundOrEmpty(i);
            int slot = compoundTag.getByteOr("Slot", (byte) 0) & 255;
            ItemStack itemStack = ItemStack.parse(player.registryAccess(),
                    compoundTag).orElse(ItemStack.EMPTY);

            slotItems.add(new SlotItem(itemStack, slot));
        }


        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
            ItemStack itemStack = player.getItemBySlot(equipmentSlot);
            if (itemStack.isEmpty()) {
                continue;
            }
            if (equipmentSlot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                var slotId = equipmentSlot.getIndex(36);
                slotItems.add(new SlotItem(itemStack, slotId));
            }
        }

        slotItems.add(new SlotItem(player.getOffhandItem(), Inventory.SLOT_OFFHAND));
        slotItems = slotItems.stream().filter(v -> !v.stack.isEmpty()).toList();

        return new InventoryMeta(slotItems);
    }

    public JsonElement toJson(HolderLookup.Provider lookup) {
        var json = CODEC.encode(this, lookup.createSerializationContext(JsonOps.INSTANCE),
                JsonOps.INSTANCE.mapBuilder()).build(new JsonObject());

        return json.getOrThrow().getAsJsonObject().get("inventory");
    }
}