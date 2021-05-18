package com.hbm.items.gear;

import java.util.List;

import com.hbm.entity.particle.EntityGasFlameFX;
import com.hbm.items.ModItems;
import com.hbm.render.model.ModelJetPack;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class JetpackBooster extends ItemArmor {

	private ModelJetPack model;
	public static int maxFuel = 750;

	public JetpackBooster(ArmorMaterial p_i45325_1_, int p_i45325_2_, EntityEquipmentSlot p_i45325_3_, String s) {
		super(p_i45325_1_, p_i45325_2_, p_i45325_3_);
		this.setUnlocalizedName(s);
		this.setRegistryName(s);
		
		ModItems.ALL_ITEMS.add(this);
	}
	
	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
		tooltip.add("Kerosene: " + getFuel(stack) + "mB / " + maxFuel + "mB");
	}


	@Override
	public boolean isValidArmor(ItemStack stack, EntityEquipmentSlot armorType, Entity entity) {
		return armorType == EntityEquipmentSlot.CHEST;
	}

	
	@Override
	@SideOnly(Side.CLIENT)
	public ModelBiped getArmorModel(EntityLivingBase entityLiving, ItemStack itemStack, EntityEquipmentSlot armorSlot, ModelBiped _default) {
		if (armorSlot == EntityEquipmentSlot.CHEST) {
			if (model == null) {
				this.model = new ModelJetPack();
			}
			return this.model;
		}
		
		return null;
	}
	
	@Override
	public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
		return "hbm:textures/models/JetPack.png";
	}

	public void onArmorTick(World world, EntityPlayer player, ItemStack stack) {
    	
    	if(player.isSneaking() && this.getBoost(stack) == 0 && this.getCooldown(stack) == 0 && getFuel(stack) > 0) {
    		this.setBoost(stack, 15);
    		this.setCooldown(stack, 40);
    	}
    	
    	if(this.getBoost(stack) > 0) {
    		
    		Vec3d vec = new Vec3d(player.getLookVec().x, 0, player.getLookVec().z);
    		vec.normalize();
    		player.motionY += 0.15;
    		
    		this.setBoost(stack, this.getBoost(stack) - 1);
    		
	    	if(!world.isRemote) {
	    		EntityGasFlameFX fx = new EntityGasFlameFX(world);
	    		fx.posX = player.posX - vec.x;
	    		fx.posY = player.posY - 1;
	    		fx.posZ = player.posZ - vec.z;
	    		fx.motionY = -0.1;
	    		world.spawnEntity(fx);
    		}
    		
    		setFuel(stack, getFuel(stack) - 1);
    		
    		if(player.motionY > 0)
    			player.fallDistance = 0;
    	}
    	
    	if(this.getCooldown(stack) > 0)
    		this.setCooldown(stack, this.getCooldown(stack) - 1);
    	
    	if(getFuel(stack) == 0)
    		this.setBoost(stack, 0);
    }
    
    public void setBoost(ItemStack stack, int i) {
    	if(!stack.hasTagCompound())
    		stack.setTagCompound(new NBTTagCompound());
    	
    	stack.getTagCompound().setInteger("boost", i);
    }
    
    public int getBoost(ItemStack stack) {
    	if(!stack.hasTagCompound())
    		return 0;
    	
    	return stack.getTagCompound().getInteger("boost");
    }
    
    public void setCooldown(ItemStack stack, int i) {
    	if(!stack.hasTagCompound())
    		stack.setTagCompound(new NBTTagCompound());
    	
    	stack.getTagCompound().setInteger("cool", i);
    }
    
    public int getCooldown(ItemStack stack) {
    	if(!stack.hasTagCompound())
    		return 0;
    	
    	return stack.getTagCompound().getInteger("cool");
    }
	
    public static int getFuel(ItemStack stack) {
		if(stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
			return 0;
		}
		
		return stack.getTagCompound().getInteger("fuel");
		
	}
	
	public static void setFuel(ItemStack stack, int i) {
		if(stack.getTagCompound() == null) {
			stack.setTagCompound(new NBTTagCompound());
		}
		
		stack.getTagCompound().setInteger("fuel", i);
		
	}
}