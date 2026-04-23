package ru.kirikws.reputationfabric.common.locks.mixin;

import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirikws.reputationfabric.common.locks.LockData;
import ru.kirikws.reputationfabric.common.locks.LockableChest;

@Mixin(ChestBlockEntity.class)
public class ChestBlockEntityMixin implements LockableChest {
    
    @Unique
    @Nullable
    private LockData reputationFabric$lockData;

    @Inject(method = "writeNbt", at = @At("RETURN"))
    private void writeLockData(NbtCompound nbt, CallbackInfo ci) {
        if (reputationFabric$lockData != null) {
            nbt.put("reputation-fabric:lock_data", reputationFabric$lockData.toNbt(new NbtCompound()));
        }
    }

    @Inject(method = "readNbt", at = @At("RETURN"))
    private void readLockData(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("reputation-fabric:lock_data")) {
            reputationFabric$lockData = LockData.fromNbt(nbt.getCompound("reputation-fabric:lock_data"));
        } else {
            reputationFabric$lockData = null;
        }
    }

    @Override
    @Nullable
    public LockData getLockData() {
        return reputationFabric$lockData;
    }

    @Override
    public void setLockData(@Nullable LockData lockData) {
        this.reputationFabric$lockData = lockData;
    }
}
