package io.sc3.peripherals.posters

import io.sc3.library.ext.optString
import io.sc3.library.ext.putOptString
import io.sc3.peripherals.config.ScPeripheralsConfig.config
import io.sc3.peripherals.mixin.MapColorAccessor
import io.sc3.peripherals.posters.PosterItem.Companion.POSTER_KEY
import net.minecraft.block.MapColor
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import kotlin.math.roundToInt
import kotlin.math.sign

const val MAX_LABEL_LENGTH = 48
const val MAX_TOOLTIP_LENGTH = 256

fun getDefaultPalette() = MapColorAccessor.getColors().map { it?.color ?: MapColor.CLEAR.color }.toMutableList()

data class PosterPrintData(
  private val initialLabel: String? = null,
  var tooltip: String? = null,
  val colors: MutableList<Int> = MutableList(128 * 128) { 0 },
  var palette: MutableList<Int> = getDefaultPalette(),
  var posterId: String? = null,
) {
  var label: String? = initialLabel
    set(value) {
      field = value?.takeIf { it.isValidLabel() }
      labelText = field?.let { Text.of(it) }
    }

  var labelText: Text? = initialLabel?.let { Text.of(it) }
    private set

  fun computeCosts(): Int {
    val pixels = colors.count { it != 0 }
    return (posterInkCost*(pixels / 16384.0)).roundToInt().coerceAtLeast(pixels.sign)
  }

  fun toNbt(): NbtCompound {
    val nbt = toItemNbt()
    nbt.putIntArray("colors", colors.toIntArray())
    nbt.putIntArray("palette", palette.toIntArray())
    return nbt
  }

  fun toItemNbt(): NbtCompound {
    val nbt = NbtCompound()
    nbt.putOptString("label", label?.takeIf { it.isValidLabel() })
    nbt.putOptString("tooltip", tooltip?.takeIf { it.isValidTooltip() })
    posterId?.let { nbt.putString(POSTER_KEY, it) }

    return nbt
  }

  companion object {
    val posterInkCost: Int = config.get("poster_printer.ink_cost")

    fun fromNbt(nbt: NbtCompound) = PosterPrintData(
      initialLabel = nbt.optString("label")?.takeIf { it.isValidLabel() },
      tooltip = nbt.optString("tooltip"),
      colors = nbt.getIntArray("colors").takeIf { it.size == 16384 }?.toMutableList() ?: MutableList(128 * 128) { 0 },
      palette = nbt.getIntArray("palette").takeIf { it.size == 64 }?.toMutableList() ?: getDefaultPalette(),
      posterId = nbt.getString(POSTER_KEY).ifEmpty { null },
    )

    private fun String?.isValidLabel() = this?.length in 1..MAX_LABEL_LENGTH
    private fun String?.isValidTooltip() = this?.length in 1..MAX_TOOLTIP_LENGTH
  }
}
