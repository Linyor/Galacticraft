package micdoodle8.mods.galacticraft.core.client.model;

import micdoodle8.mods.galacticraft.api.vector.Vector3;
import micdoodle8.mods.galacticraft.api.world.IGalacticraftWorldProvider;
import micdoodle8.mods.galacticraft.core.entities.EntityFlag;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

import org.lwjgl.opengl.GL11;

public class ModelFlag extends ModelBase
{
	ModelRenderer base;
	ModelRenderer pole;
	ModelRenderer flag;
	ModelRenderer[] flagMain;

	public ModelFlag()
	{
		this.textureWidth = 128;
		this.textureHeight = 64;
		this.base = new ModelRenderer(this, 4, 0);
		this.base.addBox(-1.5F, 0F, -1.5F, 3, 1, 3);
		this.base.setRotationPoint(0F, 23F, 0F);
		this.base.setTextureSize(128, 64);
		this.base.mirror = true;
		this.setRotation(this.base, 0F, 0F, 0F);
		this.pole = new ModelRenderer(this, 0, 0);
		this.pole.addBox(-0.5F, -40F, -0.5F, 1, 40, 1);
		this.pole.setRotationPoint(0F, 23F, 0F);
		this.pole.setTextureSize(128, 64);
		this.pole.mirror = true;
		this.setRotation(this.pole, 0F, 0F, 0F);
		this.flag = new ModelRenderer(this, 86, 0);
		this.flag.addBox(0F, 0F, 0F, 20, 12, 1);
		this.flag.setRotationPoint(0.5F, -16F, -0.5F);
		this.flag.setTextureSize(128, 64);
		this.flag.mirror = true;
		this.setRotation(this.flag, 0F, 0F, 0F);
	}

	@Override
	public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5)
	{
		super.render(entity, f, f1, f2, f3, f4, f5);

		if (entity instanceof EntityFlag)
		{
			EntityFlag flag = (EntityFlag) entity;
			this.renderPole(flag, f5);
			this.renderFlag(flag, f5, flag.ticksExisted);
		}
	}

	public void renderPole(Entity entity, float f5)
	{
		this.base.render(f5);
		this.pole.render(f5);
	}

	public void renderFlag(EntityFlag entity, float f5, float ticks)
	{
		if (entity.flagData != null && (this.flagMain == null || this.flagMain.length != entity.flagData.getWidth() * entity.flagData.getHeight()))
		{
			this.flagMain = new ModelRenderer[entity.flagData.getWidth() * entity.flagData.getHeight()];

			for (int i = 0; i < entity.flagData.getWidth(); i++)
			{
				for (int j = 0; j < entity.flagData.getHeight(); j++)
				{
					this.flagMain[j * entity.flagData.getWidth() + i] = new ModelRenderer(this, 86, 0);
					this.flagMain[j * entity.flagData.getWidth() + i].addBox(i, j, 0.0F, 1, 1, 1);
					this.flagMain[j * entity.flagData.getWidth() + i].setRotationPoint(0.5F, -16F, -0.5F);
					this.flagMain[j * entity.flagData.getWidth() + i].setTextureSize(128, 64);
					this.flagMain[j * entity.flagData.getWidth() + i].mirror = true;
					this.setRotation(this.flagMain[j * entity.flagData.getWidth() + i], 0F, 0F, 0F);
				}
			}
		}

		if (this.flagMain != null && entity.flagData != null)
		{
			GL11.glPushMatrix();

			GL11.glScalef(0.5F, 0.5F, 0.5F);
			GL11.glTranslatef(0.0F, -1.1F, 0.0F);

			for (int i = 0; i < this.flagMain.length; i++)
			{
				int xPos = i % entity.flagData.getWidth();
				GL11.glPushMatrix();
				float offset = (float) (Math.sin(ticks / 2.0F + xPos * 50 + 3) / 25.0F) * xPos / (entity.worldObj.provider instanceof IGalacticraftWorldProvider ? 100.0F : 30.0F);
				GL11.glTranslatef(0, offset, offset);
				GL11.glDisable(GL11.GL_TEXTURE_2D);
				Vector3 col = entity.flagData.getColorAt(i % entity.flagData.getWidth(), i / entity.flagData.getWidth());
				GL11.glColor3f(col.floatX(), col.floatY(), col.floatZ());
				this.flagMain[i].render(f5);
				GL11.glEnable(GL11.GL_TEXTURE_2D);
				GL11.glColor3f(1, 1, 1);
				GL11.glPopMatrix();
			}

			GL11.glPopMatrix();
		}
	}

	private void setRotation(ModelRenderer model, float x, float y, float z)
	{
		model.rotateAngleX = x;
		model.rotateAngleY = y;
		model.rotateAngleZ = z;
	}
}
