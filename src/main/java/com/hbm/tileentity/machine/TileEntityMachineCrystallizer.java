package com.hbm.tileentity.machine;

import com.hbm.forgefluid.FFUtils;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.interfaces.IConsumer;
import com.hbm.interfaces.ITankPacketAcceptor;
import com.hbm.inventory.CrystallizerRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.packet.FluidTankPacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.tileentity.TileEntityMachineBase;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemStackHandler;

public class TileEntityMachineCrystallizer extends TileEntityMachineBase implements ITickable, IConsumer, IFluidHandler, ITankPacketAcceptor {

	public long power;
	public static final long maxPower = 1000000;
	public static final int demand = 1000;
	public static final int acidRequired = 500;
	public short progress;
	public static final short duration = 600;

	public float angle;
	public float prevAngle;

	public FluidTank tank;

	public TileEntityMachineCrystallizer() {
		super(0);
		inventory = new ItemStackHandler(7){
			@Override
			protected void onContentsChanged(int slot) {
				super.onContentsChanged(slot);
				markDirty();
			}
			@Override
			public void setStackInSlot(int slot, ItemStack stack) {
				super.setStackInSlot(slot, stack);
				if(stack != null && slot >= 5 && slot <= 6 && stack.getItem() instanceof ItemMachineUpgrade)
					world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, HBMSoundHandler.upgradePlug, SoundCategory.BLOCKS, 1.0F, 1.0F);
			}
		};
		tank = new FluidTank(8000);
	}

	@Override
	public String getName() {
		return "container.crystallizer";
	}

	@Override
	public void update() {
		if(!world.isRemote) {
			power = Library.chargeTEFromItems(inventory, 1, power, maxPower);
			FFUtils.fillFromFluidContainer(inventory, tank, 3, 4);

			for(int i = 0; i < getCycleCount(); i++) {

				if(canProcess()) {

					progress++;
					power -= getPowerRequired();

					if(progress > getDuration()) {
						progress = 0;
						tank.drain(getRequiredAcid(), true);
						processItem();

						this.markDirty();
					}

				} else {
					progress = 0;
				}
			}

			PacketDispatcher.wrapper.sendToAllAround(new FluidTankPacket(pos, new FluidTank[]{tank}), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 10));

			NBTTagCompound data = new NBTTagCompound();
			data.setShort("progress", progress);
			data.setLong("power", power);
			this.networkPack(data, 25);
		} else {

			prevAngle = angle;

			if(progress > 0) {
				angle += 5F;

				if(angle >= 360) {
					angle -= 360;
					prevAngle -= 360;
				}
			}
		}
	}

	@Override
	public void networkUnpack(NBTTagCompound data) {
		this.power = data.getLong("power");
		this.progress = data.getShort("progress");
	}

	private void processItem() {

		ItemStack result = CrystallizerRecipes.getOutput(inventory.getStackInSlot(0));

		if(result == null) //never happens but you can't be sure enough
			return;

		if(inventory.getStackInSlot(2).isEmpty())
			inventory.setStackInSlot(2, result);
		else if(inventory.getStackInSlot(2).getCount() < inventory.getStackInSlot(2).getMaxStackSize())
			inventory.getStackInSlot(2).setCount(inventory.getStackInSlot(2).getCount()+result.getCount());

		//Drillgon200: I think this does the same thing as decrStackSize?
		float freeChance = this.getFreeChance();

		if(freeChance == 0 || freeChance < world.rand.nextFloat())
			inventory.getStackInSlot(0).shrink(1);
		//this.decrStackSize(0, 1);
	}

	private boolean canProcess() {

		//Is there no input?
		if(inventory.getStackInSlot(0).isEmpty())
			return false;

		if(power < getPowerRequired())
			return false;

		if(tank.getFluidAmount() < getRequiredAcid())
			return false;

		ItemStack result = CrystallizerRecipes.getOutput(inventory.getStackInSlot(0));

		//Or output?
		if(result == null)
			return false;
		//Does the output not match?
		if(!inventory.getStackInSlot(2).isEmpty() && (inventory.getStackInSlot(2).getItem() != result.getItem() || inventory.getStackInSlot(2).getItemDamage() != result.getItemDamage()))
			return false;
		//Or is the output slot already full?
		if(inventory.getStackInSlot(2).getCount() >= inventory.getStackInSlot(2).getMaxStackSize())
			return false;

		return true;
	}

	public int getRequiredAcid() {

		int extra = 0;

		for(int i = 5; i <= 6; i++) {

			if(inventory.getStackInSlot(i).getItem() == ModItems.upgrade_effect_1)
				extra += 1000;
			if(inventory.getStackInSlot(i).getItem() == ModItems.upgrade_effect_2)
				extra += 2000;
			if(inventory.getStackInSlot(i).getItem() == ModItems.upgrade_effect_3)
				extra += 3000;
		}

		return acidRequired + Math.min(extra, 3000);
	}

	public float getFreeChance() {

		float chance = 0.0F;

		for(int i = 5; i <= 6; i++) {

			if(inventory.getStackInSlot(i).getItem() == ModItems.upgrade_effect_1)
				chance += 0.05F;
			if(inventory.getStackInSlot(i).getItem() == ModItems.upgrade_effect_2)
				chance += 0.1F;
			if(inventory.getStackInSlot(i).getItem() == ModItems.upgrade_effect_3)
				chance += 0.15F;
		}

		return Math.min(chance, 0.15F);
	}

	public int getDuration() {

		float durationMod = 1;

		for(int i = 5; i <= 6; i++) {

			if(inventory.getStackInSlot(i).getItem() == ModItems.upgrade_speed_1)
				durationMod -= 0.1F;
			if(inventory.getStackInSlot(i).getItem() == ModItems.upgrade_speed_2)
				durationMod -= 0.2F;
			if(inventory.getStackInSlot(i).getItem() == ModItems.upgrade_speed_3)
				durationMod -= 0.3F;
		}

		return (int) (duration * Math.max(durationMod, 0.7F));
	}

	public int getPowerRequired() {

		int consumption = 0;

		for(int i = 5; i <= 6; i++) {

			if(inventory.getStackInSlot(i).getItem() == ModItems.upgrade_speed_1)
				consumption += 1000;
			if(inventory.getStackInSlot(i).getItem() == ModItems.upgrade_speed_2)
				consumption += 2000;
			if(inventory.getStackInSlot(i).getItem() == ModItems.upgrade_speed_3)
				consumption += 3000;
		}

		return (int) (demand + Math.min(consumption, 3000));
	}

	public float getCycleCount() {

		int cycles = 1;

		for(int i = 5; i <= 6; i++) {

			if(inventory.getStackInSlot(i).getItem() == ModItems.upgrade_overdrive_1)
				cycles += 1;
			if(inventory.getStackInSlot(i).getItem() == ModItems.upgrade_overdrive_2)
				cycles += 2;
			if(inventory.getStackInSlot(i).getItem() == ModItems.upgrade_overdrive_3)
				cycles += 3;
		}

		return Math.min(cycles, 4);
	}
	
	@Override
	public boolean canInsertItem(int slot, ItemStack itemStack, int amount) {
		return slot == 0;
	}
	
	@Override
	public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
		return slot == 2;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(EnumFacing face) {
		int side = face.getIndex();

		return side == 0 ? new int[] { 2 } : new int[] { 0, 2 };
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return TileEntity.INFINITE_EXTENT_AABB;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared() {
		return 65536.0D;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		power = compound.getLong("power");
		tank.readFromNBT(compound.getCompoundTag("tank"));
		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setLong("power", power);
		compound.setTag("tank", tank.writeToNBT(new NBTTagCompound()));
		return super.writeToNBT(compound);
	}

	public long getPowerScaled(int i) {
		return (power * i) / maxPower;
	}

	public int getProgressScaled(int i) {
		return (progress * i) / duration;
	}

	@Override
	public void setPower(long i) {
		this.power = i;
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public long getMaxPower() {
		return maxPower;
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return tank.getTankProperties();
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		if(resource.getFluid() == ModForgeFluids.acid) {
			return tank.fill(resource, doFill);
		}
		return 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		return null;
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		return null;
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
			return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this);
		}
		return super.getCapability(capability, facing);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}

	@Override
	public void recievePacket(NBTTagCompound[] tags) {
		if(tags.length == 1) {
			tank.readFromNBT(tags[0]);
		}
	}

}