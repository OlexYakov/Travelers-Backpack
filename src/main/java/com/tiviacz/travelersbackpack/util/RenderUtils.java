package com.tiviacz.travelersbackpack.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.tiviacz.travelersbackpack.inventory.ITravelersBackpack;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.MissingTextureSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.apache.commons.lang3.tuple.Triple;
import org.lwjgl.opengl.GL11;

public class RenderUtils
{
    public static void renderScreenTank(FluidTank tank, double x, double y, double height, double width)
    {
        renderScreenTank(tank.getFluid(), tank.getCapacity(), tank.getFluidAmount(), x, y, height, width);
    }

    public static void renderScreenTank(FluidStack fluid, int capacity, int amount, double x, double y, double height, double width)
    {
        if(fluid == null || fluid.getFluid() == null || amount <= 0)
        {
            return;
        }

        TextureAtlasSprite icon = Minecraft.getInstance().getAtlasSpriteGetter(PlayerContainer.LOCATION_BLOCKS_TEXTURE).apply(fluid.getFluid().getAttributes().getStillTexture());

        if(icon == null)
        {
            icon = Minecraft.getInstance().getAtlasSpriteGetter(PlayerContainer.LOCATION_BLOCKS_TEXTURE).apply(MissingTextureSprite.getLocation());
        }

        int renderAmount = (int) Math.max(Math.min(height, amount * height / capacity), 1);
        int posY = (int) (y + height - renderAmount);

        Minecraft.getInstance().getTextureManager().bindTexture(PlayerContainer.LOCATION_BLOCKS_TEXTURE);
        int color = fluid.getFluid().getAttributes().getColor();

        RenderSystem.pushMatrix();
        RenderSystem.color4f((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f, 1);
        RenderSystem.disableBlend();

        for(int i = 0; i < width; i += 16)
        {
            for(int j = 0; j < renderAmount; j += 16)
            {
                int drawWidth = (int) Math.min(width - i, 16);
                int drawHeight = Math.min(renderAmount - j, 16);

                int drawX = (int) (x + i);
                int drawY = posY + j;

                float minU;
                float minV;

                minU = icon.getMinU();
                minV = icon.getMinV();

                float maxU = icon.getMaxU();
                float maxV = icon.getMaxV();

                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder builder = tessellator.getBuffer();
                builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
                builder.pos(drawX, drawY + drawHeight, 0).tex(minU, minV + (maxV - minV) * (float)drawHeight / 16F).endVertex();
                builder.pos(drawX + drawWidth, drawY + drawHeight, 0).tex(minU + (maxU - minU) * (float)drawWidth / 16F, minV + (maxV - minV) * drawHeight / 16F).endVertex();
                builder.pos(drawX + drawWidth, drawY, 0).tex(minU + (maxU - minU) * drawWidth / 16F, minV).endVertex();
                builder.pos(drawX, drawY, 0).tex(minU, minV).endVertex();
                tessellator.draw();
            }
        }
        RenderSystem.enableBlend();
        RenderSystem.color4f(1, 1, 1, 1);
        RenderSystem.popMatrix();
    }


    //CyclopsCore part https://github.com/CyclopsMC/EvilCraft/blob/master-1.12/src/main/java/org/cyclops/evilcraft/client/render/tileentity/RenderTileEntityDarkTank.java#L87
    //https://minecraft.curseforge.com/projects/cyclops-core
    //https://github.com/CyclopsMC/CyclopsCore
    //Author: https://minecraft.curseforge.com/members/kroeser

    private static final float OFFSET = 0.01F;
    private static final float MINY = OFFSET;
    private static final float MIN = 0.125F + OFFSET;
    private static final float MAX = 0.3F - OFFSET;
    private static final float[][][] coordinates = {
            { // DOWN
                    {MIN, MINY, MAX},
                    {MIN, MINY, MIN},
                    {MAX, MINY, MIN},
                    {MAX, MINY, MAX}
            },
            { // UP
                    {MAX, MAX, MAX},
                    {MAX, MAX, MIN},
                    {MIN, MAX, MIN},
                    {MIN, MAX, MAX}
            },
            { // NORTH
                    {MIN, MINY, MIN},
                    {MIN, MAX, MIN},
                    {MAX, MAX, MIN},
                    {MAX, MINY, MIN}
            },
            { // SOUTH
                    {MAX, MINY, MAX},
                    {MAX, MAX, MAX},
                    {MIN, MAX, MAX},
                    {MIN, MINY, MAX}
            },
            { // WEST
                    {MIN, MINY, MAX},
                    {MIN, MAX, MAX},
                    {MIN, MAX, MIN},
                    {MIN, MINY, MIN}
            },
            { // EAST
                    {MAX, MINY, MIN},
                    {MAX, MAX, MIN},
                    {MAX, MAX, MAX},
                    {MAX, MINY, MAX}
            }
    };

    public static void renderFluidSides(ITravelersBackpack inv, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, float height, FluidStack fluid, int brightness)
    {
        Triple<Float, Float, Float> colorParts = getFluidVertexBufferColor(fluid);
        float r = colorParts.getLeft();
        float g = colorParts.getMiddle();
        float b = colorParts.getRight();
        float a = 1.0F;

        Matrix4f matrix4f = matrixStackIn.getLast().getMatrix();

        for(Direction direction : Direction.values())
        {
            TextureAtlasSprite icon = getFluidIcon(inv, fluid, direction);

            IVertexBuilder renderer = bufferIn.getBuffer(RenderType.getText(icon.getAtlasTexture().getTextureLocation()));

            float[][] c = coordinates[direction.ordinal()];
            float replacedMaxV = (direction == Direction.UP || direction == Direction.DOWN) ? icon.getInterpolatedV(4D) : ((icon.getMaxV() - icon.getMinV()) * height + icon.getMinV());
            float replacedU1 = (direction == Direction.UP || direction == Direction.DOWN) ? icon.getInterpolatedU(4D) : icon.getInterpolatedU(7D);
            float replacedU2 = (direction == Direction.UP || direction == Direction.DOWN) ? icon.getInterpolatedU(8D) : icon.getInterpolatedU(8D);

            renderer.pos(matrix4f, c[0][0], getHeight(c[0][1], height), c[0][2]).color(r, g, b, a).tex(replacedU1, replacedMaxV).lightmap(brightness).endVertex();
            renderer.pos(matrix4f, c[1][0], getHeight(c[1][1], height), c[1][2]).color(r, g, b, a).tex(replacedU1, icon.getMinV()).lightmap(brightness).endVertex();
            renderer.pos(matrix4f, c[2][0], getHeight(c[2][1], height), c[2][2]).color(r, g, b, a).tex(replacedU2, icon.getMinV()).lightmap(brightness).endVertex();
            renderer.pos(matrix4f, c[3][0], getHeight(c[3][1], height), c[3][2]).color(r, g, b, a).tex(replacedU2, replacedMaxV).lightmap(brightness).endVertex();
        }
    }

    private static float getHeight(float height, float replaceHeight)
    {
        if(height == MAX)
        {
            return replaceHeight;
        }
        return height;
    }

    public static void renderFluidInTank(ITravelersBackpack inv, FluidTank tank, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, float x, float y, float z)
    {
        matrixStackIn.push();
        matrixStackIn.rotate(Vector3f.ZP.rotationDegrees(180F));

        renderFluidContext(tank.getFluid(), matrixStackIn, x, y, z, fluid -> {
            float height = getTankFillRatio(tank) * 0.99F;
            RenderUtils.renderFluidSides(inv, matrixStackIn, bufferIn, height, fluid, WorldRenderer.getCombinedLight(Minecraft.getInstance().world, inv.getPosition()));
        });

        matrixStackIn.pop();
    }

    public static TextureAtlasSprite getFluidIcon(ITravelersBackpack inv, FluidStack fluidstack, Direction direction)
    {
        Block defaultBlock = Blocks.WATER;
        Block block = defaultBlock;

        if(fluidstack.getFluid().getAttributes().getBlock(Minecraft.getInstance().world, inv.getPosition(), fluidstack.getFluid().getDefaultState()) != null)
        {
            block = fluidstack.getFluid().getAttributes().getBlock(Minecraft.getInstance().world, inv.getPosition(), fluidstack.getFluid().getDefaultState()).getBlock();
        }

        if(direction == null)
        {
            direction = Direction.UP;
        }

        TextureAtlasSprite icon = Minecraft.getInstance().getAtlasSpriteGetter(PlayerContainer.LOCATION_BLOCKS_TEXTURE).apply(fluidstack.getFluid().getAttributes().getFlowingTexture());

        if(icon == null || (direction == Direction.UP || direction == Direction.DOWN))
        {
            icon = Minecraft.getInstance().getAtlasSpriteGetter(PlayerContainer.LOCATION_BLOCKS_TEXTURE).apply(fluidstack.getFluid().getAttributes().getStillTexture());
        }

        if(icon == null)
        {
            icon = getBlockIcon(block);

            if(icon == null)
            {
                icon = getBlockIcon(defaultBlock);
            }
        }

        return icon;
    }

    public static TextureAtlasSprite getBlockIcon(Block block)
    {
        return Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelShapes().getTexture(block.getDefaultState());
    }

    public static float getTankFillRatio(FluidTank tank)
    {
        return Math.min(1.0F, ((float)tank.getFluidAmount()) / (float)tank.getCapacity()) * 0.5F;
    }

    public static void renderFluidContext(FluidStack fluid, MatrixStack matrixStackIn, float x, float y, float z, IFluidContextRender render)
    {
        if(fluid != null && fluid.getAmount() > 0)
        {
            matrixStackIn.push();

            matrixStackIn.translate(x,y,z);
            render.renderFluid(fluid);

            matrixStackIn.pop();
        }
    }

    public static Triple<Float, Float, Float> getFluidVertexBufferColor(FluidStack fluidStack)
    {
        int color = fluidStack.getFluid().getAttributes().getColor(fluidStack);
        return intToRGB(color);
    }

    public static Triple<Float, Float, Float> intToRGB(int color)
    {
        float red, green, blue;
        red = (float)(color >> 16 & 255) / 255.0F;
        green = (float)(color >> 8 & 255) / 255.0F;
        blue = (float)(color & 255) / 255.0F;
        return Triple.of(red, green, blue);
    }

    public interface IFluidContextRender
    {
        void renderFluid(FluidStack fluid);
    }
}