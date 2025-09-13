package net.ccbluex.liquidbounce.render.ui

import com.mojang.blaze3d.systems.ProjectionType
import com.mojang.blaze3d.systems.RenderSystem
<<<<<<< HEAD
=======
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
>>>>>>> upstream/nextgen
import net.ccbluex.liquidbounce.common.GlobalFramebuffer
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ResourceReloadEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.client.mc
<<<<<<< HEAD
import net.ccbluex.liquidbounce.utils.kotlin.toTypedArray
import net.ccbluex.liquidbounce.utils.math.Vec2i
=======
import net.ccbluex.liquidbounce.utils.render.toBufferedImage
import net.ccbluex.liquidbounce.utils.render.toNativeImage
>>>>>>> upstream/nextgen
import net.minecraft.client.gl.Framebuffer
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.texture.NativeImage
<<<<<<< HEAD
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import org.joml.Matrix4f
=======
import net.minecraft.client.util.math.Rect2i
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import org.joml.Matrix4f
import org.joml.Vector2i
>>>>>>> upstream/nextgen
import java.awt.image.BufferedImage
import kotlin.math.ceil
import kotlin.math.sqrt

private const val NATIVE_ITEM_SIZE: Int = 16

private class Atlas(
<<<<<<< HEAD
    val map: Map<Item, Pair<Vec2i, Vec2i>>,
=======
    val map: Map<Item, Rect2i>,
>>>>>>> upstream/nextgen
    val image: BufferedImage,
    /**
     * Contains aliases. For example `minecraft:blue_wall_banner` -> `minecraft:wall_banner` which is necessary since
     * `minecraft:blue_wall_banner` has no texture.
     */
    val aliasMap: Map<Identifier, Identifier>
)

/**
 *
 */
object ItemImageAtlas : EventListener {

    private var atlas: Atlas? = null

    fun updateAtlas(drawContext: DrawContext) {
        if (this.atlas != null) {
            return
        }

        val renderer = ItemFramebufferRenderer(
<<<<<<< HEAD
            Registries.ITEM.stream().toTypedArray(),
=======
            Registries.ITEM,
>>>>>>> upstream/nextgen
            4
        )

        val items = renderer.render(drawContext)

<<<<<<< HEAD
        val image = renderer.getImage()

        val img = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)

        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                img.setRGB(x, y, image.getColorArgb(x, y))
            }
        }

        renderer.deleteFramebuffer()

        this.atlas = Atlas(items, img, findAliases())
=======
        val image = renderer.getImage().toBufferedImage()

        renderer.deleteFramebuffer()

        this.atlas = Atlas(items, image, findAliases())
>>>>>>> upstream/nextgen
    }

    private fun findAliases(): Map<Identifier, Identifier> {
        val map = hashMapOf<Identifier, Identifier>()

        Registries.BLOCK.forEach {
            val pickUpState = it.getPickStack(mc.world!!, BlockPos.ORIGIN, it.defaultState, false)

            if (pickUpState.item != it) {
                val blockId = Registries.BLOCK.getId(it)
                val itemId = Registries.ITEM.getId(pickUpState.item)

                map[blockId] = itemId
            }
        }

        return map
    }

    @Suppress("unused")
    private val resourceReloadHandler = handler<ResourceReloadEvent> {
        this.atlas = null
    }

    val isAtlasAvailable
        get() = this.atlas != null

    fun resolveAliasIfPresent(name: Identifier): Identifier {
        return atlas!!.aliasMap[name] ?: return name
    }

    fun getItemImage(item: Item): BufferedImage? {
        val atlas = requireNotNull(this.atlas) { "Atlas is not available yet" }
<<<<<<< HEAD
        val (atlasStart, atlasEnd) = atlas.map[item] ?: return null

        return atlas.image.getSubimage(
            atlasStart.x,
            atlasStart.y,
            atlasEnd.x - atlasStart.x,
            atlasEnd.y - atlasStart.y,
=======
        val rect = atlas.map[item] ?: return null

        return atlas.image.getSubimage(
            rect.x,
            rect.y,
            rect.width,
            rect.height,
>>>>>>> upstream/nextgen
        )!!
    }
}

<<<<<<< HEAD

private class ItemFramebufferRenderer(
    val items: Array<Item>,
    val scale: Int,
): MinecraftShortcuts {
    val itemsPerDimension = ceil(sqrt(items.size.toDouble())).toInt()

    val framebuffer: Framebuffer = run {
        val fb = SimpleFramebuffer(
            NATIVE_ITEM_SIZE * scale * itemsPerDimension,
            NATIVE_ITEM_SIZE * scale * itemsPerDimension,
            true
        )

        fb.setClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        fb
    }

    val itemPixelSizeOnFramebuffer = NATIVE_ITEM_SIZE * scale

    fun render(ctx: DrawContext): Map<Item, Pair<Vec2i, Vec2i>> {
=======
private class ItemFramebufferRenderer(
    val items: Registry<Item>,
    val scale: Int,
) : MinecraftShortcuts {
    private val itemsPerDimension = ceil(sqrt(items.size().toDouble())).toInt()

    private val framebuffer: Framebuffer = SimpleFramebuffer(
        NATIVE_ITEM_SIZE * scale * itemsPerDimension,
        NATIVE_ITEM_SIZE * scale * itemsPerDimension,
        true
    ).apply {
        setClearColor(0.0f, 0.0f, 0.0f, 0.0f)
    }

    private val itemPixelSizeOnFramebuffer = NATIVE_ITEM_SIZE * scale

    fun render(ctx: DrawContext): Map<Item, Rect2i> {
>>>>>>> upstream/nextgen
        this.framebuffer.beginWrite(true)

        ctx.matrices.push()

        ctx.matrices.loadIdentity()

        ctx.matrices.scale(scale.toFloat(), scale.toFloat(), 1.0f)

        val projectionMatrix = RenderSystem.getProjectionMatrix()

        val matrix4f = Matrix4f().setOrtho(
            0.0f,
            this.framebuffer.textureWidth.toFloat(),
            this.framebuffer.textureHeight.toFloat(),
            0.0f,
            1000.0f,
            21000.0f
        )

        RenderSystem.setProjectionMatrix(matrix4f, ProjectionType.ORTHOGRAPHIC)
        GlobalFramebuffer.push(framebuffer)

<<<<<<< HEAD
        val map = this.items.mapIndexed { idx, item ->
            val from = Vec2i(
                (idx % this.itemsPerDimension) * NATIVE_ITEM_SIZE,
                (idx / this.itemsPerDimension) * NATIVE_ITEM_SIZE
            )

            ctx.drawItem(ItemStack(item), from.x, from.y)

            val fbFrom = Vec2i(from.x * this.scale, from.y * this.scale)
            val fbTo = Vec2i(
                fbFrom.x + this.itemPixelSizeOnFramebuffer,
                fbFrom.y + this.itemPixelSizeOnFramebuffer
            )

            item to (fbFrom to fbTo)
        }.toMap()
=======
        val map = Reference2ObjectOpenHashMap<Item, Rect2i>(items.size())
        this.items.forEachIndexed { idx, item ->
            val fromX = (idx % this.itemsPerDimension) * NATIVE_ITEM_SIZE
            val fromY = (idx / this.itemsPerDimension) * NATIVE_ITEM_SIZE

            ctx.drawItem(item.defaultStack, fromX, fromY)

            map[item] = Rect2i(
                fromX * this.scale,
                fromY * this.scale,
                this.itemPixelSizeOnFramebuffer,
                this.itemPixelSizeOnFramebuffer
            )
        }
>>>>>>> upstream/nextgen

        ctx.matrices.pop()

        GlobalFramebuffer.pop()
        mc.framebuffer.beginWrite(true)

        RenderSystem.setProjectionMatrix(projectionMatrix, ProjectionType.ORTHOGRAPHIC)

        return map
    }

<<<<<<< HEAD
    fun getImage(): NativeImage {
        val ss = NativeImage(this.framebuffer.textureWidth, this.framebuffer.textureHeight, false)

        RenderSystem.bindTexture(this.framebuffer.colorAttachment)

        ss.loadFromTextureImage(0, false)
        ss.mirrorVertically()

        return ss
    }
=======
    fun getImage(): NativeImage = framebuffer.toNativeImage()
>>>>>>> upstream/nextgen

    fun deleteFramebuffer() {
        this.framebuffer.delete()
    }

}
