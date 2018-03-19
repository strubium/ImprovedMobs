package com.flemmli97.improvedmobs.handler;

import java.util.Iterator;

import org.lwjgl.opengl.GL11;

import com.flemmli97.improvedmobs.entity.ai.EntityAIBlockBreaking;
import com.flemmli97.improvedmobs.entity.ai.EntityAIRideBoat;
import com.flemmli97.improvedmobs.entity.ai.EntityAIUseItem;
import com.flemmli97.improvedmobs.entity.ai.NewPathNavigateGround;
import com.flemmli97.improvedmobs.handler.helper.GeneralHelperMethods;
import com.flemmli97.improvedmobs.handler.packet.PacketHandler;
import com.flemmli97.improvedmobs.handler.packet.PathDebugging;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathNavigateSwimmer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.SpecialSpawn;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EventHandlerAI {
	
	public static DifficultyData data;
	
	@SubscribeEvent
	public void entityProps(EntityConstructing e) {
		if (e.getEntity() instanceof EntityMob && e.getEntity().world!=null && !e.getEntity().world.isRemote)
		{
			if(ConfigHandler.breakerChance!=0 &&e.getEntity().world.rand.nextFloat()<ConfigHandler.breakerChance)
			{
				e.getEntity().addTag("Breaker");
			}
		}
	}
	
	@SubscribeEvent
    public void initTracker(WorldEvent.Load e)
    {
    		if(e.getWorld()!=null && !e.getWorld().isRemote)
    			EventHandlerAI.data = DifficultyData.get(e.getWorld());
    }
	
	@SubscribeEvent
    public void increaseDifficulty(WorldTickEvent e)
    {
    		if(e.phase==Phase.END && e.world!=null && !e.world.isRemote)
    		{
    			if(ConfigHandler.shouldPunishTimeSkip)
			{
	    			long timeDiff = (int) Math.abs(e.world.getWorldTime() - data.getPrevTime());
	    			if(timeDiff>2400)
	    			{
	    				long i = timeDiff/2400;
	    				if(timeDiff-i*2400<(i+1)*2400-timeDiff)
	    					i *= 2400;
	    				else
	    					i*=2400+2400;
	    				EventHandlerAI.data.increaseDifficultyBy(i/24000F, e.world.getWorldTime());
    				}
    			}
    			else
    			{
    				if(e.world.getWorldTime() - data.getPrevTime()>2400)
	    				EventHandlerAI.data.increaseDifficultyBy(0.1F, e.world.getWorldTime());
    			}
    		}
    }
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
    public void showDifficulty(RenderGameOverlayEvent.Post e)
    {
		if (e.isCancelable() || e.getType() != ElementType.EXPERIENCE)
			return;
		if(data!=null)
		{
			FontRenderer font = Minecraft.getMinecraft().fontRenderer;
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			GL11.glDisable(GL11.GL_LIGHTING);
			int x = ConfigHandler.guiX==0?2:ConfigHandler.guiX==1?e.getResolution().getScaledWidth()/2:e.getResolution().getScaledWidth()-2;
			int y = ConfigHandler.guiY==0?2:ConfigHandler.guiY==1?e.getResolution().getScaledHeight()/2:e.getResolution().getScaledHeight()-2;
			if(ConfigHandler.guiX==2)
			{
				String t = "Difficulty "+String.format(java.util.Locale.US,"%.1f", data.getDifficulty());
				font.drawString(t, x-font.getStringWidth(t), y, 0x6d0c9e);
			}
			else if(ConfigHandler.guiX==1)
			{
				String t = "Difficulty "+String.format(java.util.Locale.US,"%.1f", data.getDifficulty());
				font.drawString(t, x-font.getStringWidth(t)/2, y, 0x6d0c9e);
			}
			else
				font.drawString("Difficulty "+String.format(java.util.Locale.US,"%.1f", data.getDifficulty()), x, y, 0x6d0c9e);
		}
    }

	@SubscribeEvent
	public void equip(SpecialSpawn e)
	{
		if (e.getEntityLiving() instanceof EntityMob && e.getEntityLiving().world!=null && !e.getEntityLiving().world.isRemote && !(e.getEntityLiving() instanceof IEntityOwnable))
		{			
			EntityMob mob = (EntityMob) e.getEntityLiving();
			if(!GeneralHelperMethods.isMobInList((EntityLiving) mob, ConfigHandler.armorMobBlacklist))
			{
				//List<IRecipe> r= CraftingManager.getInstance().getRecipeList(); for further things maybe	
				if(ConfigHandler.baseEquipChance!=0 )
					GeneralHelperMethods.tryEquipArmor(mob);
				if(ConfigHandler.baseEnchantChance!=0)
					GeneralHelperMethods.enchantGear(mob);
				if(ConfigHandler.baseItemChance!=0)
					GeneralHelperMethods.equipItem(mob);		
				if(ConfigHandler.healthIncrease!=0)
				{
					GeneralHelperMethods.modifyAttr(mob, SharedMonsterAttributes.MAX_HEALTH, ConfigHandler.healthIncrease*0.02, ConfigHandler.healthMax,  true);
					mob.setHealth(mob.getMaxHealth());
				}
				if(ConfigHandler.damageIncrease!=0)
					GeneralHelperMethods.modifyAttr(mob, SharedMonsterAttributes.ATTACK_DAMAGE, ConfigHandler.damageIncrease*0.01, ConfigHandler.damageMax,  true);
				if(ConfigHandler.speedIncrease!=0)
					GeneralHelperMethods.modifyAttr(mob, SharedMonsterAttributes.MOVEMENT_SPEED, ConfigHandler.speedIncrease*0.001, ConfigHandler.speedMax,  false);
				if(ConfigHandler.knockbackIncrease!=0)
					GeneralHelperMethods.modifyAttr(mob, SharedMonsterAttributes.KNOCKBACK_RESISTANCE, ConfigHandler.knockbackIncrease*0.002, ConfigHandler.knockbackMax,  false);
			}
		}
	}
	
	@SubscribeEvent
	public void entityProps(CheckSpawn e) {
		if(e.getEntityLiving() instanceof EntityLiving && !e.getWorld().isRemote)
		{
			if(GeneralHelperMethods.isMobInList((EntityLiving) e.getEntityLiving(), ConfigHandler.mobListLight))
			{
				int light = e.getWorld().getLightFor(EnumSkyBlock.BLOCK, e.getEntity().getPosition());
				if(light>=ConfigHandler.light)
				{
					e.setResult(Result.DENY);
					return;
				}
				else
				{
					e.setResult(Result.ALLOW);
					return;
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onEntityLoad(EntityJoinWorldEvent e) {
	    if (e.getEntity() instanceof EntityMob && !e.getWorld().isRemote) 
	    {    	
	    		EntityMob living= (EntityMob) e.getEntity();

		    PathNavigate oldNav = living.getNavigator();
			
			if(oldNav != null && oldNav.getClass() == PathNavigateGround.class)
			{
				NewPathNavigateGround newNav = new NewPathNavigateGround(living, e.getWorld());
				
				ObfuscationReflectionHelper.setPrivateValue(EntityLiving.class, living, newNav, "field_70699_by", "navigator");
			}
		    	if(living.getTags().contains("Breaker"))
	        {
		    		living.tasks.addTask(1, new EntityAIBlockBreaking(living));
		    		living.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, new ItemStack(Items.DIAMOND_PICKAXE));
		    		living.setDropChance(EntityEquipmentSlot.OFFHAND, 0);
	        }
		    	if((ConfigHandler.mobListAsWhitelist && GeneralHelperMethods.isMobInList((EntityLiving) e.getEntity(), ConfigHandler.mobListAIBlacklist)) ||!GeneralHelperMethods.isMobInList((EntityLiving) e.getEntity(), ConfigHandler.mobListAIBlacklist))
		    	{
		    		living.tasks.addTask(3, new EntityAIUseItem(living, 15));
		    		if(!(living.canBreatheUnderwater() && living.getNavigator() instanceof PathNavigateSwimmer))
		    			living.tasks.addTask(6, new EntityAIRideBoat(living));
		    	}
	    		if(ConfigHandler.targetVillager && !(living instanceof EntityZombie))
				living.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityVillager>(living, EntityVillager.class, living.getTags().contains("Breaker")? false:living.world.rand.nextFloat()<=0.5));
		    	Iterator<EntityAITaskEntry> targetTask = living.targetTasks.taskEntries.iterator();
	    		while (targetTask.hasNext())
	        {
        			EntityAITaskEntry entry = targetTask.next();
        			EntityAIBase ai = entry.action;
        			
        			if(ai instanceof EntityAINearestAttackableTarget)
        			{		  
        				EntityAINearestAttackableTarget<?> aiNearestTarget = (EntityAINearestAttackableTarget<?>) ai;
        				if(living.getTags().contains("Breaker"))
        				{
        					Class<?> targetCls = ObfuscationReflectionHelper.getPrivateValue(EntityAINearestAttackableTarget.class, aiNearestTarget, "field_75307_b","targetClass");
        					if(targetCls == EntityPlayer.class)
        					{
        						if(!(living instanceof EntityEnderman && living instanceof EntityPigZombie))
        							ObfuscationReflectionHelper.setPrivateValue(EntityAITarget.class, (EntityAITarget)aiNearestTarget, false, "field_75297_f","shouldCheckSight");
        						else if(ConfigHandler.neutralAggressiv!=0 && living.world.rand.nextFloat() <= ConfigHandler.neutralAggressiv)
        							ObfuscationReflectionHelper.setPrivateValue(EntityAITarget.class, (EntityAITarget)aiNearestTarget, false, "field_75297_f","shouldCheckSight");
        					}
        				}
        			}
        		}
	    }
	}
	
	@SubscribeEvent
	public void pathDebug(LivingEvent e)
	{
		if(ConfigHandler.debuggingPath && e.getEntityLiving() instanceof EntityLiving && !e.getEntityLiving().world.isRemote)
		{
			Path path= ((EntityLiving)e.getEntityLiving()).getNavigator().getPath();
			if(path!=null)
			{
				for(int i = 0; i < path.getCurrentPathLength(); i++)
					PacketHandler.sendToAllAround(new PathDebugging(EnumParticleTypes.NOTE.getParticleID(), path.getPathPointFromIndex(i).x,path.getPathPointFromIndex(i).y,path.getPathPointFromIndex(i).z), 0, e.getEntityLiving().posX, e.getEntityLiving().posY, e.getEntityLiving().posZ, 64);
				PacketHandler.sendToAllAround(new PathDebugging(EnumParticleTypes.HEART.getParticleID(), path.getFinalPathPoint().x,path.getFinalPathPoint().y,path.getFinalPathPoint().z), 0, e.getEntityLiving().posX, e.getEntityLiving().posY, e.getEntityLiving().posZ, 64);
			}
		}
	}
	
	@SubscribeEvent
	public void friendlyFire(LivingAttackEvent e)
	{
		if(!ConfigHandler.friendlyFire &&e.getEntityLiving() instanceof IEntityOwnable && !e.getEntityLiving().world.isRemote)
		{
			IEntityOwnable pet = (IEntityOwnable) e.getEntityLiving();
			if(e.getSource().getTrueSource()!=null && e.getSource().getTrueSource() == pet.getOwner() && !e.getSource().getTrueSource().isSneaking())
			{
				e.setCanceled(true);
			}
		}
	}
	
	@SubscribeEvent
    public void equipPet(EntityInteract e)
    {
    		if(e.getTarget() instanceof EntityLiving && e.getTarget() instanceof IEntityOwnable && !e.getTarget().world.isRemote && e.getEntityPlayer().isSneaking() && !GeneralHelperMethods.isMobInList((EntityLiving) e.getTarget(), ConfigHandler.petArmorBlackList))
    		{
    			IEntityOwnable pet = (IEntityOwnable) e.getTarget();
    			if(e.getEntityPlayer() == pet.getOwner())
	    		{
	    			EntityLiving living = (EntityLiving) e.getTarget();
	    			
	    			ItemStack heldItem = e.getEntityPlayer().getHeldItemMainhand();
	    			if(heldItem != null && heldItem.getItem() instanceof ItemArmor)
	    			{
	    				ItemArmor armor = (ItemArmor) heldItem.getItem();
	    				EntityEquipmentSlot type = armor.armorType;
	    				switch(type)
	    				{
	    					case HEAD:
	    		    				ItemStack helmet = living.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
	    		    				if(helmet != null && !e.getEntityPlayer().capabilities.isCreativeMode)
	    		    				{
	    		    					EntityItem entityitem = new EntityItem(living.world, living.posX, living.posY, living.posZ, helmet);
	    		    		            entityitem.setNoPickupDelay();
	    		    		            living.world.spawnEntity(entityitem);
	    		    				}
		    					living.setItemStackToSlot(EntityEquipmentSlot.HEAD, heldItem.copy());
	
		    					if(!e.getEntityPlayer().capabilities.isCreativeMode)
		    					{
		    						heldItem.setCount(heldItem.getCount()-1);
			    					if(heldItem.getCount()==0 && !e.getEntityPlayer().capabilities.isCreativeMode)
			    						e.getEntityPlayer().setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
		    					}
	    					break;
	    					case CHEST:
			    				ItemStack chest = living.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
			    				
			    				if(chest != null&& !e.getEntityPlayer().capabilities.isCreativeMode)
			    				{
			    					EntityItem entityitem = new EntityItem(living.world, living.posX, living.posY, living.posZ, chest);
			    		            entityitem.setNoPickupDelay();
			    		            living.world.spawnEntity(entityitem);
			    				}
		    					living.setItemStackToSlot(EntityEquipmentSlot.CHEST, heldItem.copy());
		
		    					if(!e.getEntityPlayer().capabilities.isCreativeMode)
		    					{
		    						heldItem.setCount(heldItem.getCount()-1);
			    					if(heldItem.getCount()==0 && !e.getEntityPlayer().capabilities.isCreativeMode)
			    						e.getEntityPlayer().setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
		    					}
						break;
	    					case LEGS:
			    				ItemStack leggs = living.getItemStackFromSlot(EntityEquipmentSlot.LEGS);
			    				if(leggs != null&& !e.getEntityPlayer().capabilities.isCreativeMode)
			    				{
			    					EntityItem entityitem = new EntityItem(living.world, living.posX, living.posY, living.posZ, leggs);
			    		            entityitem.setNoPickupDelay();
			    		            living.world.spawnEntity(entityitem);
			    				}
	    					living.setItemStackToSlot(EntityEquipmentSlot.LEGS, heldItem.copy());
	
	    					if(!e.getEntityPlayer().capabilities.isCreativeMode)
	    					{
	    						heldItem.setCount(heldItem.getCount()-1);
		    					if(heldItem.getCount()==0 && !e.getEntityPlayer().capabilities.isCreativeMode)
		    						e.getEntityPlayer().setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
	    					}
						break;
	    					case FEET:
			    				ItemStack boots = living.getItemStackFromSlot(EntityEquipmentSlot.FEET);
			    				if(boots != null&& !e.getEntityPlayer().capabilities.isCreativeMode)
			    				{
			    					EntityItem entityitem = new EntityItem(living.world, living.posX, living.posY, living.posZ, boots);
			    		            entityitem.setNoPickupDelay();
			    		            living.world.spawnEntity(entityitem);
			    				}
	    					living.setItemStackToSlot(EntityEquipmentSlot.FEET, heldItem.copy());
	
	    					if(!e.getEntityPlayer().capabilities.isCreativeMode)
	    					{
	    						heldItem.setCount(heldItem.getCount()-1);
		    					if(heldItem.getCount()==0 && !e.getEntityPlayer().capabilities.isCreativeMode)
		    						e.getEntityPlayer().setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
	    					}
						break;
						default:
							break;
	    				}
	        			e.setCanceled(true);
	    			}
    			}
    		}
    }
}
