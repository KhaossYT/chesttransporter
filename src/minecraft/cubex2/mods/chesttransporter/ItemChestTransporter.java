package cubex2.mods.chesttransporter;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.src.ModLoader;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.minecart.MinecartInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;

public class ItemChestTransporter extends Item {

	protected ItemChestTransporter(int id) {
		super(id);
		setItemName("chesttransporter");
		setTextureFile("/cubex2/mods/chesttransporter/client/textures/textures.png");
		setMaxStackSize(1);
		setIconIndex(0);
		setHasSubtypes(true);
		setMaxDamage(0);
		setCreativeTab(CreativeTabs.tabTools);
		MinecraftForge.EVENT_BUS.register(this);
	}

	@ForgeSubscribe
	public void OnPlayerInteract(PlayerInteractEvent event) {
		if (event.action == Action.RIGHT_CLICK_BLOCK) {
			ItemStack stack = event.entityPlayer.getCurrentEquippedItem();
			if (stack == null)
				return;
			if (stack.itemID != this.shiftedIndex)
				return;
			EntityPlayer player = event.entityPlayer;

			World world = event.entityPlayer.worldObj;
			int x = event.x;
			int y = event.y;
			int z = event.z;
			int face = event.face;
			if (stack.getItemDamage() == 0 && isChestAt(world, x, y, z)) {
				IInventory chest = (IInventory) world.getBlockTileEntity(x, y, z);
				if (chest != null) {
					Block chestBlock = Block.blocksList[world.getBlockId(x, y, z)];
					int metadata = world.getBlockMetadata(x, y, z);
					stack.setItemDamage(getNewDamageFromChest(chestBlock, metadata));
					moveItemsIntoStack(chest, stack);
					world.setBlockAndMetadataWithNotify(x, y, z, 0, 0);
					world.playSoundEffect((x + 0.5F), (y + 0.5F), (z + 0.5F), chestBlock.stepSound.getStepSound(), (chestBlock.stepSound.getVolume() + 1.0F) / 2.0F, chestBlock.stepSound.getPitch() * 0.5F);
				}
			}
			else if (stack.getItemDamage() != 0) {
				int[] chestCoords = placeChestBlock(getStackFromDamage(stack.getItemDamage()), player, world, x, y, z, face, 0.0f, 0.0f, 0.0f);
				if (chestCoords != null) {
					IInventory chest = (IInventory) world.getBlockTileEntity(chestCoords[0], chestCoords[1], chestCoords[2]);
					moveItemsIntoChest(stack, chest);
					player.renderBrokenItemStack(stack);
					stack.stackSize--;
				}
			}
		}
	}

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean par5) {
		if (stack.getItemDamage() != 0 && entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) entity;
			if (player.capabilities.isCreativeMode)
				return;
			if (player.getActivePotionEffect(Potion.moveSlowdown) == null || player.getActivePotionEffect(Potion.moveSlowdown).getDuration() < 20) {
				player.addPotionEffect(new PotionEffect(Potion.moveSlowdown.getId(), 20 * 3, 2));
			}
			if (player.getActivePotionEffect(Potion.digSlowdown) == null || player.getActivePotionEffect(Potion.digSlowdown).getDuration() < 20) {
				player.addPotionEffect(new PotionEffect(Potion.digSlowdown.getId(), 20 * 3, 3));
			}
			if (player.getActivePotionEffect(Potion.jump) == null || player.getActivePotionEffect(Potion.jump).getDuration() < 20) {
				player.addPotionEffect(new PotionEffect(Potion.jump.getId(), 20 * 3, -2));
			}
			if (player.getActivePotionEffect(Potion.hunger) == null || player.getActivePotionEffect(Potion.hunger).getDuration() < 20) {
				player.addPotionEffect(new PotionEffect(Potion.hunger.getId(), 20 * 3, 0));
			}
		}

	}

	private int[] placeChestBlock(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int facing, float hitX, float hitY, float hitZ) {
		int blockId = world.getBlockId(x, y, z);

		if (blockId == Block.snow.blockID) {
			facing = 1;
		}
		else if (blockId != Block.vine.blockID && blockId != Block.tallGrass.blockID && blockId != Block.deadBush.blockID && (Block.blocksList[blockId] == null || !Block.blocksList[blockId].isBlockReplaceable(world, x, y, z))) {
			if (facing == 0) {
				--y;
			}

			if (facing == 1) {
				++y;
			}

			if (facing == 2) {
				--z;
			}

			if (facing == 3) {
				++z;
			}

			if (facing == 4) {
				--x;
			}

			if (facing == 5) {
				++x;
			}
		}

		if (stack.stackSize == 0) {
			return null;
		}
		else if (!player.canPlayerEdit(x, y, z, facing, stack)) {
			return null;
		}
		else if (y == 255 && Block.blocksList[stack.itemID].blockMaterial.isSolid()) {
			return null;
		}
		else if (world.canPlaceEntityOnSide(stack.itemID, x, y, z, false, facing, player)) {
			Block var12 = Block.blocksList[stack.itemID];
			int var13 = stack.getItemDamage();
			int var14 = Block.blocksList[stack.itemID].func_85104_a(world, x, y, z, facing, hitX, hitY, hitZ, var13);

			if (placeBlockAt(stack, player, world, x, y, z, facing, hitX, hitY, hitZ, var14)) {
				world.playSoundEffect((x + 0.5F), (y + 0.5F), (z + 0.5F), var12.stepSound.getPlaceSound(), (var12.stepSound.getVolume() + 1.0F) / 2.0F, var12.stepSound.getPitch() * 0.8F);
				--stack.stackSize;
			}

			return new int[] { x, y, z };
		}
		else {
			return null;
		}
	}

	private boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata) {
		if (!world.setBlockAndMetadataWithNotify(x, y, z, stack.itemID, metadata)) {
			return false;
		}

		if (world.getBlockId(x, y, z) == stack.itemID) {
			Block.blocksList[stack.itemID].onBlockPlacedBy(world, x, y, z, player);
			Block.blocksList[stack.itemID].func_85105_g(world, x, y, z, metadata);
		}

		return true;
	}

	@ForgeSubscribe
	public void mincartInteract(MinecartInteractEvent event) {
		ItemStack stack = event.player.getCurrentEquippedItem();
		EntityMinecart minecart = event.minecart;
		if (stack == null || stack.getItem() != this || (stack.getItemDamage() != getNewDamageFromChest(Block.chest, 0) && stack.getItemDamage() != 0) || minecart == null)
			return;

		if (stack.getItemDamage() == 0 && minecart.isStorageCart()) {
			moveItemsIntoStack(minecart, stack);
			stack.setItemDamage(1);
			minecart.worldObj.playSoundEffect(((float) minecart.posX + 0.5F), ((float) minecart.posY + 0.5F), ((float) minecart.posZ + 0.5F), Block.chest.stepSound.getStepSound(), (Block.chest.stepSound.getVolume() + 1.0F) / 2.0F, Block.chest.stepSound.getPitch() * 0.5F);
			ModLoader.setPrivateValue(EntityMinecart.class, minecart, 3, 0);
		}
		else if (stack.getItemDamage() == 1 && minecart.getMinecartType() == 0 && minecart.riddenByEntity == null) {
			ModLoader.setPrivateValue(EntityMinecart.class, minecart, 3, 1);
			moveItemsIntoChest(stack, minecart);
			minecart.worldObj.playSoundEffect(((float) minecart.posX + 0.5F), ((float) minecart.posY + 0.5F), ((float) minecart.posZ + 0.5F), Block.chest.stepSound.getStepSound(), (Block.chest.stepSound.getVolume() + 1.0F) / 2.0F, Block.chest.stepSound.getPitch() * 0.8F);
			event.player.renderBrokenItemStack(stack);
			event.player.inventory.setInventorySlotContents(event.player.inventory.currentItem, null);
		}
		event.setCanceled(true);
	}

	@Override
	public boolean getShareTag() {
		return true;
	}

	@Override
	public int getIconFromDamage(int i) {
		return i;
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean flag) {
		if (stack.getItemDamage() != 0) {
			int numItems = 0;
			NBTTagList nbtList = stack.stackTagCompound.getTagList("Items");

			for (int i = 0; i < nbtList.tagCount(); ++i) {
				NBTTagCompound nbtTagCompound = (NBTTagCompound) nbtList.tagAt(i);
				int j = nbtTagCompound.getByte("Slot") & 255;

				if (j >= 0) {
					ItemStack itemstack = ItemStack.loadItemStackFromNBT(nbtTagCompound);
					if (itemstack != null) {
						numItems += itemstack.stackSize;
					}
				}
			}

			list.add("Contains " + numItems + " items");
		}
	}

	private boolean isChestAt(World world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		if (id == Block.chest.blockID)
			return true;
		if (ChestTransporter.ironChestBlock != null && id == ChestTransporter.ironChestBlock.blockID)
			return true;
		return false;
	}

	private int getNewDamageFromChest(Block block, int metadata) {
		if (block == Block.chest)
			return 1;
		if (ChestTransporter.ironChestBlock != null && block == ChestTransporter.ironChestBlock) {
			if (metadata < 6)
				return 2 + metadata;
		}
		return 0;
	}

	private ItemStack getStackFromDamage(int damage) {
		if (damage == 1)
			return new ItemStack(Block.chest);
		if (damage > 1 && damage <= 7)
			return new ItemStack(ChestTransporter.ironChestBlock, 1, damage - 2);
		return null;
	}

	private void moveItemsIntoStack(IInventory chest, ItemStack stack) {
		if (stack.stackTagCompound == null)
			stack.setTagCompound(new NBTTagCompound());
		NBTTagList nbtList = new NBTTagList();

		for (int i = 0; i < chest.getSizeInventory(); ++i) {
			if (chest.getStackInSlot(i) != null) {
				NBTTagCompound nbtTabCompound2 = new NBTTagCompound();
				nbtTabCompound2.setByte("Slot", (byte) i);
				chest.getStackInSlot(i).copy().writeToNBT(nbtTabCompound2);
				chest.setInventorySlotContents(i, null);
				nbtList.appendTag(nbtTabCompound2);
			}
		}
		stack.stackTagCompound.setTag("Items", nbtList);
	}

	private void moveItemsIntoChest(ItemStack stack, IInventory chest) {
		NBTTagList nbtList = stack.stackTagCompound.getTagList("Items");

		for (int i = 0; i < nbtList.tagCount(); ++i) {
			NBTTagCompound nbtTagCompound = (NBTTagCompound) nbtList.tagAt(i);
			int j = nbtTagCompound.getByte("Slot") & 255;

			if (j >= 0 && j < chest.getSizeInventory()) {
				chest.setInventorySlotContents(j, ItemStack.loadItemStackFromNBT(nbtTagCompound).copy());
			}
		}

		stack.stackTagCompound.getTags().remove(nbtList);
	}
}
