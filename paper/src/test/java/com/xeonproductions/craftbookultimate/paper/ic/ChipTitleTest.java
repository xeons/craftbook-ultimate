package com.xeonproductions.craftbookultimate.paper.ic;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.ic.ICDefinition;
import com.xeonproductions.craftbookultimate.core.ic.ICRegistry;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.paper.ICCatalogue;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Marking the title of a chip that cannot work")
class ChipTitleTest {

    private static final ICRegistry REGISTRY = ICCatalogue.build();

    /** The melody chip, which does nothing at all until its sign names a file. */
    private static final ICDefinition MELODY = REGISTRY.byModel("MCU700").orElseThrow();

    private static SignLines sign(Component title, String third) {
        return SignLines.of(List.of(
                title, Component.text("[MCU700]"), Component.text(third), Component.empty()));
    }

    private static Component plain() {
        return Component.text("MELODY");
    }

    private static Component red() {
        return Component.text("MELODY", NamedTextColor.RED);
    }

    @Nested
    @DisplayName("A chip whose sign leaves out a line it cannot work without")
    class Broken {

        @Test
        void isMarkedWhenItsTitleIsStillPlain() {
            assertThat(ChipTitle.wouldChange(sign(plain(), ""), MELODY)).isTrue();
        }

        @Test
        void isLeftAloneWhenItIsMarkedAlready() {
            assertThat(ChipTitle.wouldChange(sign(red(), ""), MELODY)).isFalse();
        }
    }

    @Nested
    @DisplayName("A chip whose sign says everything it needs")
    class Working {

        @Test
        void isLeftAloneWhenItsTitleIsPlain() {
            assertThat(ChipTitle.wouldChange(sign(plain(), "tune.mid"), MELODY)).isFalse();
        }

        @Test
        void hasItsMarkTakenOffWhenItWasBrokenBefore() {
            assertThat(ChipTitle.wouldChange(sign(red(), "tune.mid"), MELODY)).isTrue();
        }
    }

    @Test
    void leavesEveryChipInTheCatalogueUnmarkedWhenItsLinesAreFilledIn() {
        SignLines filled = SignLines.of("TITLE", "[MODEL]", "something", "something");

        for (ICDefinition chip : REGISTRY.definitions()) {
            assertThat(ChipTitle.wouldChange(filled, chip))
                    .as("%s with both lines written", chip.model())
                    .isFalse();
        }
    }

    @Test
    void marksOnlyTheChipsThatSayTheyNeedALine() {
        SignLines blank = SignLines.of("TITLE", "[MODEL]", "", "");

        for (ICDefinition chip : REGISTRY.definitions()) {
            boolean needsOne = chip.lineSpec(ICDefinition.THIRD_LINE)
                            .filter(spec -> spec.required()).isPresent()
                    || chip.lineSpec(ICDefinition.FOURTH_LINE)
                            .filter(spec -> spec.required()).isPresent();

            assertThat(ChipTitle.wouldChange(blank, chip))
                    .as("%s with both lines blank", chip.model())
                    .isEqualTo(needsOne);
        }
    }
}
