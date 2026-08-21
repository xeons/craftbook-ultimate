package com.xeonproductions.craftbookultimate.paper.ic;

import com.xeonproductions.craftbookultimate.core.ic.AreaAwareICLogic;
import com.xeonproductions.craftbookultimate.core.ic.ChipReport;
import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.LineReview;
import com.xeonproductions.craftbookultimate.core.ic.PinLayout;
import com.xeonproductions.craftbookultimate.core.math.Bounds;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.paper.adapter.Positions;
import com.xeonproductions.craftbookultimate.paper.adapter.Signs;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.block.Block;
import org.jspecify.annotations.NullMarked;

/**
 * Reads a loaded chip and says what the plugin currently makes of it.
 *
 * <p>Everything here is read rather than worked out: which block sits on each pin, what that block
 * is emitting, what the sign says now. That is the point — a chip that is not doing what its
 * builder expects is nearly always a disagreement between what they think is wired and what the
 * plugin thinks is wired, and only one of those two can be printed.
 *
 * <p>Must run on the region owning the chip's sign, since it reads blocks around it.
 */
@NullMarked
public final class ChipInspector {

    private ChipInspector() {}

    /** Everything worth knowing about a chip, as it stands right now. */
    public static ChipReport inspect(ICInstance chip) {
        ICDefinition definition = chip.definition();
        PinLayout layout = chip.layout();
        BlockChipState state = chip.inspectionState();

        List<ChipReport.Pin> pins = new ArrayList<>(layout.pinCount());
        for (int input = 0; input < layout.inputCount(); input++) {
            pins.add(pin(state, input, true));
        }
        for (int output = 0; output < layout.outputCount(); output++) {
            pins.add(pin(state, output, false));
        }

        SignLines sign = Signs.at(Positions.toBlock(chip.world(), chip.signPosition()))
                .map(Signs::read)
                .orElse(SignLines.EMPTY);

        return new ChipReport(
                definition.model(),
                definition.shorthand(),
                definition.name(),
                chip.world().getName(),
                chip.signPosition(),
                layout,
                chip.mode(),
                chip.isSelfTriggering(),
                definition.supportsSelfTriggering(),
                pins,
                state.hasPowerSourceBehind(),
                LineReview.of(definition, sign),
                sign,
                area(chip, state));
    }

    /**
     * The stretch of world a chip is working on, where the chip is one that can say.
     *
     * <p>Asked of the chip's own running logic rather than of a fresh one, so that a chip which
     * decides its area from something it has already read answers with what it is actually using.
     */
    public static Optional<Bounds> area(ICInstance chip) {
        return area(chip, chip.inspectionState());
    }

    private static Optional<Bounds> area(ICInstance chip, BlockChipState state) {
        return chip.logic() instanceof AreaAwareICLogic aware ? aware.area(state) : Optional.empty();
    }

    /** One pin, as the block sitting on it currently reads. */
    private static ChipReport.Pin pin(BlockChipState state, int index, boolean input) {
        int slot = input ? index : state.layout().outputPin(index);
        Block block = state.pinBlock(slot);
        Vec3i position = Positions.toDomain(block);

        boolean wired = Redstone.isPowerSource(block);
        int power = Redstone.powerLevel(block);
        return new ChipReport.Pin(index, input, position, wired, wired && power > 0, power);
    }
}
