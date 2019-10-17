package com.hbm.potion;

import com.hbm.explosion.ExplosionLarge;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.lib.ModDamageSource;
import com.hbm.lib.RefStrings;
import com.hbm.main.MainRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class HbmPotion extends Potion {
	
	public static HbmPotion taint;
	public static HbmPotion radiation;
	public static HbmPotion bang;
	public static HbmPotion mutation;
	public static HbmPotion radx;
	public static HbmPotion lead;
	
	public HbmPotion(boolean isBad, int color, String name, int x, int y){
		super(isBad, color);
		this.setPotionName(name);
		this.setRegistryName(RefStrings.MODID, name);
		this.setIconIndex(x, y);
	}

	public static void init() {
		taint = registerPotion(true, 8388736, "potion.hbm_taint", 0, 0);
		radiation = registerPotion(true, 8700200, "potion.hbm_radiation", 1, 0);
		bang = registerPotion(true, 1118481, "potion.hbm_bang", 3, 0);
		mutation = registerPotion(false, 8388736, "potion.hbm_mutation", 2, 0);
		radx = registerPotion(false, 0xBB4B00, "potion.hbm_radx", 5, 0);
		lead = registerPotion(false, 0x767682, "potion.hbm_lead", 6, 0);
	}

	public static HbmPotion registerPotion(boolean isBad, int color, String name, int x, int y) {

	/*	if (id >= Potion.potionTypes.length) {

			Potion[] newArray = new Potion[Math.max(256, id)];
			System.arraycopy(Potion.potionTypes, 0, newArray, 0, Potion.potionTypes.length);
			
			Field field = ReflectionHelper.findField(Potion.class, new String[] { "field_76425_a", "potionTypes" });
			field.setAccessible(true);
			
			try {
				
				Field modfield = Field.class.getDeclaredField("modifiers");
				modfield.setAccessible(true);
				modfield.setInt(field, field.getModifiers() & 0xFFFFFFEF);
				field.set(null, newArray);
				
			} catch (Exception e) {
				
			}
		}*/
		
		HbmPotion effect = new HbmPotion(isBad, color, name, x, y);
		ForgeRegistries.POTIONS.register(effect);
		
		return effect;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public int getStatusIconIndex() {
		ResourceLocation loc = new ResourceLocation("hbm","textures/gui/potions.png");
		Minecraft.getMinecraft().renderEngine.bindTexture(loc);
		return super.getStatusIconIndex();
	}

	public void performEffect(EntityLivingBase entity, int level) {

		if(this == taint) {
			if(!(entity instanceof EntityTaintedCreeper) && entity.world.rand.nextInt(80) == 0)
				entity.attackEntityFrom(ModDamageSource.taint, (level + 1));
			
			if(MainRegistry.enableHardcoreTaint && !entity.world.isRemote) {
				
				int x = (int)(entity.posX - 1);
				int y = (int)entity.posY;
				int z = (int)(entity.posZ);
				BlockPos pos = new BlockPos(x, y, z);
				
				if(entity.world.getBlockState(pos).getBlock()
						.isReplaceable(entity.world, pos) && 
						BlockTaint.hasPosNeightbour(entity.world, pos)) {
					
					entity.world.setBlock(x, y, z, ModBlocks.taint, 14, 2);
				}
			} 
		}
		if(this == radiation) {
			
			/*if (entity.getHealth() > entity.getMaxHealth() - (level + 1)) {
				entity.attackEntityFrom(ModDamageSource.radiation, 1);
			}*/
			
			//RadEntitySavedData data = RadEntitySavedData.getData(entity.worldObj);
			//data.increaseRad(entity, (float)(level + 1F) * 0.05F);
			
			Library.applyRadData(entity, (float)(level + 1F) * 0.05F);
		}
		if(this == bang) {
			
			entity.attackEntityFrom(ModDamageSource.bang, 1000);
			entity.setHealth(0.0F);

			if (!(entity instanceof EntityPlayer))
				entity.setDead();

			entity.world.playSound(null, new BlockPos(entity), HBMSoundHandler.laserBang, SoundCategory.AMBIENT, 100.0F, 1.0F);
			ExplosionLarge.spawnParticles(entity.world, entity.posX, entity.posY, entity.posZ, 10);
		}
		if(this == lead) {
			
			entity.attackEntityFrom(ModDamageSource.lead, (level + 1));
		}
	}

	public boolean isReady(int par1, int par2) {

		if(this == taint) {

	        return par1 % 2 == 0;
		}
		if(this == radiation) {
			
			return true;
		}
		if(this == bang) {

			return par1 <= 10;
		}
		if(this == lead) {

			int k = 60;
	        return k > 0 ? par1 % k == 0 : true;
		}
		
		return false;
	}
	
}