package com.xeonproductions.craftbookultimate.core.ic.gate;

import com.xeonproductions.craftbookultimate.core.ic.ChipServices;
import com.xeonproductions.craftbookultimate.core.ic.ChipState;
import com.xeonproductions.craftbookultimate.core.ic.ICLogic;
import com.xeonproductions.craftbookultimate.core.ic.SelfTriggeringICLogic;
import com.xeonproductions.craftbookultimate.core.mechanic.Actor;
import com.xeonproductions.craftbookultimate.core.math.Vec3i;
import com.xeonproductions.craftbookultimate.core.sign.SignLines;
import com.xeonproductions.craftbookultimate.core.stock.Stockpile;
import com.xeonproductions.craftbookultimate.core.variable.VariableName;
import com.xeonproductions.craftbookultimate.core.variable.Variables;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.NullMarked;

/**
 * The chips that read and change the values everything on the server shares.
 *
 * <p>A variable is a number kept somewhere other than the world — see {@link Variables} — so these
 * three are how redstone reaches it. One changes a variable by arithmetic, one drives its output
 * from a comparison against one, and one counts what is in a chest and adds the total to one.
 * Between them a build can keep a score, a tally or a stock level that outlives the chunk it was
 * set in.
 *
 * <p>All three name their variable on line 3, in the grammar {@link VariableName} defines:
 * {@code score} for a shared one, {@code alice|score} for one of Alice's.
 *
 * <p>Every one of them refuses its sign when the variable named does not exist. What a variable is
 * called lives in the store rather than in the blocks beside the sign, so a sign naming one nobody
 * has made would be silently dead, and its builder would have nothing to tell that from a wiring
 * fault. Refusing at the moment it is written puts the problem in front of somebody standing at
 * the sign with the means to fix it.
 *
 * <p>A variable that goes away <em>afterwards</em> is a different matter and is not an error: the
 * chip finds nothing to read, does nothing, and drives its output low. A chip cannot be refused
 * retrospectively, and throwing from inside a chip that ticks would take the region down with it.
 */
@NullMarked
public final class VariableChips {

    /** The sign line naming the variable. */
    public static final int VARIABLE_LINE = 2;

    /** The sign line carrying whatever else the chip needs told. */
    public static final int ARGUMENT_LINE = 3;

    /** Separates a function from the amount it works with. */
    private static final char ARGUMENT_SEPARATOR = ':';

    /** How far above the block behind the sign an item counter looks for its container. */
    private static final int CONTAINER_HEIGHT = 1;

    /** The permission to use a variable belonging to somebody else. */
    public static final String OTHER_NAMESPACE_PERMISSION = "craftbook.variables.use.other";

    private VariableChips() {}

    /**
     * Changes a variable by arithmetic.
     *
     * <p>Line 3 names the variable and line 4 is the sum to do to it, written as the function and
     * the amount separated by a colon: {@code +:1} counts up by one, {@code *:2} doubles,
     * {@code %:10} keeps only the remainder. The functions are those in {@link Function}, each of
     * which takes a symbol or its name.
     *
     * <p>The output goes high when the sum was done and low when it was not, which covers a
     * variable that has gone away, one holding something that is not a number, and a sign whose
     * fourth line does not read as a function and an amount.
     */
    public static ICLogic modifier() {
        return new Modifier();
    }

    /**
     * Drives its output from a variable being at least a number.
     *
     * <p>Line 3 names the variable and line 4 is the number to compare it against. The output is
     * high while the variable is that number or greater.
     *
     * <p>Ticking, it follows the variable whatever its inputs are doing, which is what a chip
     * watching a score wants. Not ticking, it reads the variable each time its input goes high, so
     * a clock can drive it as often as a build needs rather than every tick.
     */
    public static SelfTriggeringICLogic isAtLeast() {
        return new IsAtLeast();
    }

    /**
     * Counts what is in the container above it and adds the total to a variable.
     *
     * <p>Line 3 names the variable and line 4 says what to count, or is left blank to count
     * everything. The container is the block above the one the sign hangs on, which is where these
     * chips have always looked for one.
     *
     * <p>The output goes high when anything was counted. The variable gains the total whether or
     * not the output went high, so counting an empty chest adds nothing rather than doing nothing.
     */
    public static ICLogic itemCounter() {
        return new ItemCounter();
    }

    /** What a modifier does to a variable. */
    public enum Function {

        /** Adds the amount. */
        ADD("+"),

        /** Takes the amount away. */
        SUBTRACT("-"),

        /** Multiplies by the amount. */
        MULTIPLY("*", "x"),

        /** Divides by the amount. */
        DIVIDE("/"),

        /** Keeps the remainder after dividing by the amount. */
        MOD("%");

        private final String[] symbols;

        Function(String... symbols) {
            this.symbols = symbols;
        }

        /**
         * Reads a function off a sign, by symbol or by name.
         *
         * @return the function, or empty if what was written is not one
         */
        public static Optional<Function> parse(String written) {
            String trimmed = written.trim();
            for (Function function : values()) {
                if (function.name().equalsIgnoreCase(trimmed)) {
                    return Optional.of(function);
                }
                for (String symbol : function.symbols) {
                    if (symbol.equalsIgnoreCase(trimmed)) {
                        return Optional.of(function);
                    }
                }
            }
            return Optional.empty();
        }

        /**
         * Works this function on a number.
         *
         * <p>Dividing by zero and taking a remainder by zero both leave the number alone. Neither
         * has an answer, and leaving a counter where it was is the one outcome a build can carry
         * on from — where the alternative is a variable holding something no arithmetic will ever
         * move off again.
         */
        public double apply(double initial, double amount) {
            return switch (this) {
                case ADD -> initial + amount;
                case SUBTRACT -> initial - amount;
                case MULTIPLY -> initial * amount;
                case DIVIDE -> amount == 0 ? initial : initial / amount;
                case MOD -> amount == 0 ? initial : initial % amount;
            };
        }

        /** The symbol a builder is most likely to write. */
        public String symbol() {
            return symbols[0];
        }
    }

    /** VAR100, which does a sum to a variable each time its input goes high. */
    private static final class Modifier implements ICLogic {

        @Override
        public void trigger(ChipState state) {
            if (!state.mainInput()) {
                return;
            }
            state.setMainOutput(apply(state));
        }

        private static boolean apply(ChipState state) {
            Optional<VariableName> name = variableOn(state.sign());
            Optional<Sum> sum = sumOn(state.sign());
            if (name.isEmpty() || sum.isEmpty()) {
                return false;
            }

            Variables variables = state.services().variables();
            OptionalDouble held = variables.number(name.get());
            if (held.isEmpty()) {
                return false;
            }

            return variables.setNumber(
                    name.get(), sum.get().function().apply(held.getAsDouble(), sum.get().amount()));
        }

        @Override
        public Optional<String> reviewSign(SignLines lines, ChipServices services, Actor builder) {
            Optional<String> problem = reviewVariable(lines, services, builder);
            if (problem.isPresent()) {
                return problem;
            }
            if (sumOn(lines).isEmpty()) {
                return Optional.of(
                        "Line 4 is the sum to do, written as a function and an amount with a colon "
                                + "between them, such as +:1 or *:2. The functions are "
                                + functionList() + ".");
            }
            return Optional.empty();
        }
    }

    /** VAR170, whose output follows whether a variable has reached a number. */
    private static final class IsAtLeast implements SelfTriggeringICLogic {

        @Override
        public void trigger(ChipState state) {
            if (state.mainInput()) {
                state.setMainOutput(reached(state));
            }
        }

        @Override
        public void tick(ChipState state) {
            state.setMainOutput(reached(state));
        }

        /**
         * Whether the variable has reached the number on the sign.
         *
         * <p>A variable that does not exist, or holds something that is not a number, has not
         * reached anything. That is a plainer answer than holding the output where it was, because
         * a comparison chip's output means the comparison came out true, and it did not.
         */
        private static boolean reached(ChipState state) {
            Optional<VariableName> name = variableOn(state.sign());
            Optional<Double> target = numberOn(state.sign(), ARGUMENT_LINE);
            if (name.isEmpty() || target.isEmpty()) {
                return false;
            }

            OptionalDouble held = state.services().variables().number(name.get());
            return held.isPresent() && held.getAsDouble() >= target.get();
        }

        @Override
        public Optional<String> reviewSign(SignLines lines, ChipServices services, Actor builder) {
            Optional<String> problem = reviewVariable(lines, services, builder);
            if (problem.isPresent()) {
                return problem;
            }
            if (numberOn(lines, ARGUMENT_LINE).isEmpty()) {
                return Optional.of("Line 4 is the number to compare the variable against.");
            }
            return Optional.empty();
        }
    }

    /** VAR200, which counts a container's contents into a variable. */
    private static final class ItemCounter implements ICLogic {

        @Override
        public void trigger(ChipState state) {
            if (!state.mainInput()) {
                return;
            }

            int counted = count(state);
            state.setMainOutput(counted > 0);

            Optional<VariableName> name = variableOn(state.sign());
            if (name.isEmpty()) {
                return;
            }

            Variables variables = state.services().variables();
            OptionalDouble held = variables.number(name.get());
            if (held.isPresent()) {
                variables.setNumber(name.get(), held.getAsDouble() + counted);
            }
        }

        /** How much of what the sign asks for is in the container above the chip. */
        private static int count(ChipState state) {
            Vec3i container = state.backPosition().add(0, CONTAINER_HEIGHT, 0);
            Stockpile pile = state.stockpileNear(container, 0, Set.of());

            Optional<Key> wanted = wantedItem(state);
            if (wanted.isPresent()) {
                return pile.count(wanted.get());
            }

            int total = 0;
            for (Map.Entry<Key, Integer> held : pile.contents().entrySet()) {
                total += held.getValue();
            }
            return total;
        }

        /** What the sign says to count, empty when it says to count everything. */
        private static Optional<Key> wantedItem(ChipState state) {
            String written = state.sign().trimmedText(ARGUMENT_LINE);
            if (written.isEmpty()) {
                return Optional.empty();
            }
            return state.world().resolveItem(written);
        }

        @Override
        public Optional<String> reviewSign(SignLines lines, ChipServices services, Actor builder) {
            return reviewVariable(lines, services, builder);
        }
    }

    /** A function and the amount it works with, as line 4 of a modifier writes them. */
    private record Sum(Function function, double amount) {}

    /**
     * Checks the variable a sign names, which every one of these chips does the same way.
     *
     * @return what to tell the builder, or empty if the sign is fine
     */
    private static Optional<String> reviewVariable(
            SignLines lines, ChipServices services, Actor builder) {

        Optional<VariableName> name = variableOn(lines);
        if (name.isEmpty()) {
            return Optional.of(
                    "Line 3 names the variable, in letters, digits and underscores. Put a "
                            + "namespace and a " + VariableName.SEPARATOR + " before it to name "
                            + "somebody else's, such as alice" + VariableName.SEPARATOR + "score.");
        }

        if (!services.variables().has(name.get())) {
            return Optional.of("There is no variable called " + name.get()
                    + ". Make one with /var define " + name.get() + " 0.");
        }

        if (!mayUse(name.get(), builder)) {
            return Optional.of("The variable " + name.get() + " belongs to " + name.get().namespace()
                    + ", and you may only use your own and the shared ones.");
        }

        return Optional.empty();
    }

    /**
     * Whether somebody may build a chip on a variable.
     *
     * <p>Their own and the shared ones always; anybody else's only with the permission for it. A
     * chip is left running long after whoever wrote its sign has gone, so this is checked as the
     * sign is written rather than as the chip runs.
     */
    public static boolean mayUse(VariableName name, Actor builder) {
        return name.isShared()
                || name.namespace().equalsIgnoreCase(builder.name())
                || builder.mayUse(OTHER_NAMESPACE_PERMISSION);
    }

    /** The variable a sign names. */
    private static Optional<VariableName> variableOn(SignLines lines) {
        return VariableName.parse(lines.trimmedText(VARIABLE_LINE));
    }

    /** The sum a modifier's sign asks for. */
    private static Optional<Sum> sumOn(SignLines lines) {
        String written = lines.trimmedText(ARGUMENT_LINE);
        int separator = written.indexOf(ARGUMENT_SEPARATOR);
        if (separator < 0) {
            return Optional.empty();
        }

        Optional<Function> function = Function.parse(written.substring(0, separator));
        Optional<Double> amount = number(written.substring(separator + 1));
        if (function.isEmpty() || amount.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Sum(function.get(), amount.get()));
    }

    /** A number written on one of a sign's lines. */
    private static Optional<Double> numberOn(SignLines lines, int index) {
        return number(lines.trimmedText(index));
    }

    /** A number somebody has typed, which may not be one at all. */
    private static Optional<Double> number(String written) {
        try {
            double parsed = Double.parseDouble(written.trim());
            return Double.isFinite(parsed) ? Optional.of(parsed) : Optional.empty();
        } catch (NumberFormatException notANumber) {
            return Optional.empty();
        }
    }

    /** The functions, written out for somebody who has just got one wrong. */
    private static String functionList() {
        StringBuilder listed = new StringBuilder();
        for (Function function : Function.values()) {
            if (!listed.isEmpty()) {
                listed.append(", ");
            }
            listed.append(function.symbol())
                    .append(" (")
                    .append(function.name().toLowerCase(Locale.ROOT))
                    .append(')');
        }
        return listed.toString();
    }
}
