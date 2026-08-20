package com.xeonproductions.craftbookultimate.core.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.xeonproductions.craftbookultimate.core.world.Blocks;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Describing a creature on a sign")
class EntitySpecTest {

    private static EntitySpec parse(String written) {
        Optional<EntitySpec> spec = EntitySpec.parse(written);
        assertThat(spec).as("parsing \"%s\"", written).isPresent();
        return spec.get();
    }

    @Nested
    @DisplayName("naming one kind")
    class NamingOneKind {

        @Test
        void matchesThatKindAndNothingElse() {
            EntitySpec spec = parse("zombie");

            assertThat(spec.matches(SimpleBystander.monster("zombie"))).isTrue();
            assertThat(spec.matches(SimpleBystander.monster("skeleton"))).isFalse();
        }

        @Test
        void ignoresTheCaseTheNameWasWrittenIn() {
            assertThat(parse("ZoMbIe").matches(SimpleBystander.monster("zombie"))).isTrue();
        }

        @Test
        void canBeSpawned() {
            assertThat(parse("zombie").isSpawnable()).isTrue();
        }

        @Test
        void acceptsAKindThisVersionHasNotHeardOf() {
            // Nothing here holds a list of what exists, so a mob added later needs no change.
            assertThat(parse("wandering_trader")).isInstanceOf(EntitySpec.OfType.class);
        }
    }

    @Nested
    @DisplayName("naming a group")
    class NamingAGroup {

        @Test
        void matchesEveryHostileMob() {
            EntitySpec spec = parse("mobs");

            assertThat(spec.matches(SimpleBystander.monster("creeper"))).isTrue();
            assertThat(spec.matches(SimpleBystander.animal("cow"))).isFalse();
        }

        @Test
        void matchesEveryAnimal() {
            EntitySpec spec = parse("animals");

            assertThat(spec.matches(SimpleBystander.animal("cow"))).isTrue();
            assertThat(spec.matches(SimpleBystander.monster("creeper"))).isFalse();
        }

        @Test
        void cannotBeSpawned() {
            // A group says what to look for, not what to make.
            assertThat(parse("mobs").isSpawnable()).isFalse();
        }
    }

    @Nested
    @DisplayName("naming a family")
    class NamingAFamily {

        @Test
        void matchesEveryKindOfMinecart() {
            EntitySpec spec = parse("minecart");

            assertThat(spec.matches(SimpleBystander.of("chest_minecart"))).isTrue();
            assertThat(spec.matches(SimpleBystander.of("minecart"))).isTrue();
            assertThat(spec.matches(SimpleBystander.of("boat"))).isFalse();
        }

        @Test
        void picksOneKindOutByNumber() {
            EntitySpec spec = parse("minecart:1");

            assertThat(spec.matches(SimpleBystander.of("chest_minecart"))).isTrue();
            assertThat(spec.matches(SimpleBystander.of("minecart"))).isFalse();
        }

        @Test
        void refusesAMinecartNumberThatNamesNothing() {
            assertThat(EntitySpec.parse("minecart:9")).isEmpty();
        }

        @Test
        void matchesEveryKindOfHorse() {
            assertThat(parse("entityhorse").matches(SimpleBystander.animal("donkey"))).isTrue();
        }
    }

    @Nested
    @DisplayName("naming a property")
    class NamingAProperty {

        @Test
        void picksASheepOutByColour() {
            EntitySpec spec = parse("sheep@13");

            assertThat(spec.matches(sheep("green"))).isTrue();
            assertThat(spec.matches(sheep("white"))).isFalse();
        }

        @Test
        void refusesAColourNumberThatNamesNothing() {
            assertThat(EntitySpec.parse("sheep@16")).isEmpty();
        }

        @Test
        void picksASaddledPigOut() {
            EntitySpec spec = parse("pig@1");

            assertThat(spec.matches(pig(true))).isTrue();
            assertThat(spec.matches(pig(false))).isFalse();
        }

        @Test
        void picksAnUnsaddledPigOut() {
            assertThat(parse("pig@0").matches(pig(false))).isTrue();
            assertThat(parse("pig@0").matches(pig(true))).isFalse();
        }

        @Test
        void picksAChargedCreeperOut() {
            EntitySpec spec = parse("creeper@1");

            assertThat(spec.matches(creeper(true))).isTrue();
            assertThat(spec.matches(creeper(false))).isFalse();
        }

        private static SimpleBystander sheep(String colour) {
            return SimpleBystander.animal("sheep").withTraits(Bystander.Traits.ofSheep(colour));
        }

        private static SimpleBystander pig(boolean saddled) {
            return SimpleBystander.animal("pig").withTraits(Bystander.Traits.ofPig(saddled));
        }

        private static SimpleBystander creeper(boolean charged) {
            return SimpleBystander.monster("creeper").withTraits(Bystander.Traits.ofCreeper(charged));
        }
    }

    @Nested
    @DisplayName("naming a player")
    class NamingAPlayer {

        @Test
        void matchesAnybodyAtAll() {
            EntitySpec spec = parse("player");

            assertThat(spec.matches(SimpleBystander.player("Notch"))).isTrue();
            assertThat(spec.matches(SimpleBystander.monster("zombie"))).isFalse();
        }

        @Test
        void matchesOneAccountName() {
            EntitySpec spec = parse("p:Notch");

            assertThat(spec.matches(SimpleBystander.player("Notch"))).isTrue();
            assertThat(spec.matches(SimpleBystander.player("notch"))).isFalse();
        }

        @Test
        void matchesEverybodyExceptOne() {
            EntitySpec spec = parse("p:!Notch");

            assertThat(spec.matches(SimpleBystander.player("Notch"))).isFalse();
            assertThat(spec.matches(SimpleBystander.player("Herobrine"))).isTrue();
        }

        @Test
        void matchesAPermissionGroup() {
            EntitySpec spec = parse("g:admin");

            assertThat(spec.matches(SimpleBystander.player("Notch").inGroup("admin"))).isTrue();
            assertThat(spec.matches(SimpleBystander.player("Notch"))).isFalse();
        }

        @Test
        void matchesPartOfAName() {
            EntitySpec spec = parse("m:otc");

            assertThat(spec.matches(SimpleBystander.player("Notch"))).isTrue();
            assertThat(spec.matches(SimpleBystander.player("Herobrine"))).isFalse();
        }

        @Test
        void neverMatchesSomethingThatIsNotAPlayer() {
            // Turning a match around must not make it start matching zombies.
            assertThat(parse("p:!Notch").matches(SimpleBystander.monster("zombie"))).isFalse();
        }

        @Test
        void refusesAMatchWithNothingToMatchOn() {
            assertThat(EntitySpec.parse("p:")).isEmpty();
        }
    }

    @Nested
    @DisplayName("naming a dropped stack")
    class NamingADroppedStack {

        @Test
        void matchesAnyStackAtAll() {
            assertThat(parse("item").matches(droppedStack(Blocks.key("stone")))).isTrue();
        }

        @Test
        void matchesOneKindOfStack() {
            EntitySpec spec = parse("item:stone");

            assertThat(spec.matches(droppedStack(Blocks.key("stone")))).isTrue();
            assertThat(spec.matches(droppedStack(Blocks.key("dirt")))).isFalse();
        }

        @Test
        void handsTheItemNameToWhoeverCanResolveIt() {
            // Only the server can turn a name written before the flattening into an item, so the
            // text after the colon goes through untouched to whatever was given the job.
            Optional<EntitySpec> spec =
                    EntitySpec.parse(
                            "item:35@14",
                            written ->
                                    written.equals("35@14")
                                            ? Optional.of(Blocks.key("red_wool"))
                                            : Optional.empty());

            assertThat(spec).contains(new EntitySpec.Dropped(Optional.of(Blocks.key("red_wool"))));
        }

        @Test
        void refusesAnItemNameNothingCanResolve() {
            assertThat(EntitySpec.parse("item:nonsense", written -> Optional.empty())).isEmpty();
        }

        private static SimpleBystander droppedStack(Key item) {
            return SimpleBystander.of("item").asObject().carrying(item);
        }
    }

    @Nested
    @DisplayName("stacking riders")
    class StackingRiders {

        @Test
        void putsOneThingOnAnother() {
            EntitySpec spec = parse("pig+zombie");

            SimpleBystander ridden =
                    SimpleBystander.animal("pig").carrying(SimpleBystander.monster("zombie"));

            assertThat(spec.matches(ridden)).isTrue();
            assertThat(spec.matches(SimpleBystander.animal("pig"))).isFalse();
        }

        @Test
        void allowsMoreRidersThanWereAskedFor() {
            SimpleBystander ridden = SimpleBystander.animal("pig")
                    .carrying(SimpleBystander.monster("zombie"))
                    .carrying(SimpleBystander.animal("chicken"));

            assertThat(parse("pig+zombie").matches(ridden)).isTrue();
        }

        @Test
        void stacksThreeDeep() {
            SimpleBystander tower = SimpleBystander.animal("pig")
                    .carrying(SimpleBystander.animal("cow")
                            .carrying(SimpleBystander.monster("zombie")));

            assertThat(parse("pig+cow+zombie").matches(tower)).isTrue();
            assertThat(parse("pig+cow+creeper").matches(tower)).isFalse();
        }

        @Test
        void stepsBackDownToPutTwoThingsOnTheSameMount() {
            SimpleBystander pair = SimpleBystander.animal("pig")
                    .carrying(SimpleBystander.animal("cow"))
                    .carrying(SimpleBystander.monster("zombie"));

            assertThat(parse("pig+cow\\zombie").matches(pair)).isTrue();
        }

        @Test
        void refusesToStepBackPastTheBottomOfTheStack() {
            assertThat(EntitySpec.parse("pig\\cow")).isEmpty();
        }

        @Test
        void refusesToStartWithARiderMarker() {
            assertThat(EntitySpec.parse("+zombie")).isEmpty();
        }

        @Test
        void refusesAStackWithNothingBetweenTwoMarkers() {
            assertThat(EntitySpec.parse("pig++zombie")).isEmpty();
        }

        @Test
        void canBeSpawnedOnlyIfEveryPartCan() {
            assertThat(parse("pig+zombie").isSpawnable()).isTrue();
            assertThat(parse("pig+mobs").isSpawnable()).isFalse();
        }
    }

    @Nested
    @DisplayName("carrying extra data")
    class CarryingExtraData {

        @Test
        void keepsTheBracesAndEverythingInThem() {
            EntitySpec spec = parse("zombie{IsBaby:1b}");

            assertThat(spec).isInstanceOf(EntitySpec.WithData.class);
            assertThat(((EntitySpec.WithData) spec).data()).isEqualTo("{IsBaby:1b}");
        }

        @Test
        void keepsMatchingOnTheThingItselfWhileIgnoringTheData() {
            assertThat(parse("zombie{IsBaby:1b}").matches(SimpleBystander.monster("zombie"))).isTrue();
        }

        @Test
        void survivesBracesInsideBraces() {
            EntitySpec spec = parse("zombie{a:{b:1}}");

            assertThat(((EntitySpec.WithData) spec).data()).isEqualTo("{a:{b:1}}");
        }

        @Test
        void letsARiderFollowTheClosingBrace() {
            EntitySpec spec = parse("pig{Saddle:1b}+zombie");

            assertThat(spec).isInstanceOf(EntitySpec.Mounted.class);
            assertThat(((EntitySpec.Mounted) spec).riders()).hasSize(1);
        }

        @Test
        void refusesBracesThatNeverClose() {
            assertThat(EntitySpec.parse("zombie{IsBaby:1b")).isEmpty();
        }
    }

    @Test
    void refusesAnEmptyDescription() {
        assertThat(EntitySpec.parse("")).isEmpty();
        assertThat(EntitySpec.parse("   ")).isEmpty();
    }
}
