package com.petitioner0.divinecore.items.AbstractFinalItem;

import com.petitioner0.divinecore.items.AbstractOrbitingItem.AbstractOrbitingItem;
import com.petitioner0.divinecore.items.AbstractOrbitingItem.OrbitPreset;
import com.petitioner0.divinecore.items.ItemHelper;
import com.petitioner0.divinecore.net.S2CStartStormFunnel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.petitioner0.divinecore.items.ModItems;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class AbstractFinalItem extends AbstractOrbitingItem {

    private static final String KEY_TRIGGERED = "divinecore_floating_triggered";
    private static final String KEY_START_Y   = "divinecore_floating_start_y";
    private static final String KEY_TICKS     = "divinecore_floating_ticks";

    // 上升高度 & 悬浮时长
    private static final double RISE_HEIGHT = 2.0;
    private static final int FLOAT_TICKS = 5 * 20; // 5 秒

    private static final Set<Supplier<Item>> FINAL_ITEMS = Set.of(
            ModItems.GENESIS_OF_LIFE_AND_DEATH,
            ModItems.ORIGIN_OF_FORM_AND_ESSENCE,
            ModItems.ROOT_OF_THOUGHT_AND_SOUL,
            ModItems.SOURCE_OF_VOID_AND_REALITY
    );

    private static final String KEY_CENTER_X = "divinecore_center_x";
    private static final String KEY_CENTER_Y = "divinecore_center_y";
    private static final String KEY_CENTER_Z = "divinecore_center_z";


    public AbstractFinalItem(Properties props, OrbitPreset preset) {
        super(props, preset);
    }
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity.level().isClientSide) return false;

        CompoundTag data = entity.getPersistentData();

        if (!data.getBoolean(KEY_TRIGGERED)) {
            if (shouldStartFloating(entity.level(), entity, stack)) {
                startFloating(data, entity);
            } else {
                return false;
            }
        }

        updateFloating(stack, entity, data);
        return false;
    }

    /** 判断当前是否该进入“无法拾取 + 上升”状态 */
    protected boolean shouldStartFloating(Level level, ItemEntity selfEntity, ItemStack selfStack) {
        BlockPos pos = selfEntity.blockPosition();
        AABB box = new AABB(pos);

        // 当前格子内所有物品实体（包括自己）
        List<ItemEntity> items = level.getEntitiesOfClass(
                ItemEntity.class,
                box,
                e -> true
        );

        // 记录每种最终物品是否存在
        boolean hasAll = true;

        for (Supplier<Item> sup : FINAL_ITEMS) {
            Item need = sup.get();

            boolean found = items.stream().anyMatch(e -> e.getItem().is(need));

            if (!found) {
                hasAll = false;
                break;
            }
        }

        return hasAll;
    }

    protected boolean isPartnerItem(ItemStack otherStack) {
        return FINAL_ITEMS.stream().anyMatch(s -> s.get() == otherStack.getItem());
    }
    /** 进入浮空状态：写 NBT + 设置实体状态 */
    private void startFloating(CompoundTag data, ItemEntity entity) {
        entity.noPhysics = true;
        data.putBoolean(KEY_TRIGGERED, true);

        BlockPos pos = entity.blockPosition();
        Vec3 center = Vec3.atCenterOf(pos);

        data.putDouble(KEY_CENTER_X, center.x);
        data.putDouble(KEY_CENTER_Y, center.y);
        data.putDouble(KEY_CENTER_Z, center.z);

        data.putDouble(KEY_START_Y, entity.getY());
        data.putInt(KEY_TICKS, 0);

        entity.setPickUpDelay(Integer.MAX_VALUE);
        entity.setNoGravity(true);

        ServerLevel level = (ServerLevel) entity.level();

        PacketDistributor.sendToPlayersInDimension(level, new S2CStartStormFunnel(center.x, center.y, center.z));
    }

    /** 已触发浮空后的每 tick 行为 */
    private boolean updateFloating(ItemStack stack, ItemEntity entity, CompoundTag data) {

        double centerX = data.getDouble(KEY_CENTER_X);
        double centerY = data.getDouble(KEY_CENTER_Y);
        double centerZ = data.getDouble(KEY_CENTER_Z);
        int ticks = data.getInt(KEY_TICKS);

        Vec3 pos = entity.position();
        double targetY = centerY + RISE_HEIGHT;

        double dx = centerX - pos.x;
        double dz = centerZ - pos.z;

        double vx = Mth.clamp(dx * 0.2, -0.15, 0.15);
        double vz = Mth.clamp(dz * 0.2, -0.15, 0.15);

        double vy = pos.y >= targetY - 0.02 ? 0 : 0.05;

        entity.setDeltaMovement(vx, vy, vz);
        entity.hasImpulse = true; // ✅ 保证立刻同步到客户端

        if (pos.y >= targetY - 0.02) {
            ticks++;
            data.putInt(KEY_TICKS, ticks);
            if (ticks >= FLOAT_TICKS) {
                onFloatingFinished(entity.level(), entity, stack);
                entity.discard();
            }
        }

        return false;
    }

    protected void onFloatingFinished(Level level, ItemEntity entity, ItemStack stack) {

    }
}
