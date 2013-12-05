package diceroller;

import java.util.Iterator;

/**
 * DiceRoll represents a dice roll. It holds the results of a dice roll, and
 * provides methods for retrieve information about the roll.
 *
 * @author Jacob Dorman
 */
public class DiceRoll implements Iterable<Integer> {

    private int[] rolls;

    public DiceRoll(int[] rolls) {
        this.rolls = rolls;
    }

    /**
     * @return the number of dice contained in this roll
     */
    public int getDiceCount() {
        return rolls.length;
    }

    /**
     * @return the total of this dice roll
     */
    public int getTotalRoll() {
        int total = 0;
        for (int i = 0; i < rolls.length; i++) {
            total += rolls[i];
        }
        return total;
    }

    /**
     * @return the lowest roll
     */
    public int getLowestRoll() {
        int lowest = 100;
        for (int i = 0; i < rolls.length; i++) {
            if (rolls[i] < lowest) {
                lowest = rolls[i];
            }
        }
        return lowest;
    }

    /**
     * @return the highest roll
     */
    public int getHighestRoll() {
        int highest = 0;
        for (int i = 0; i < rolls.length; i++) {
            if (rolls[i] > highest) {
                highest = rolls[i];
            }
        }
        return highest;
    }

    /**
     * @return the average of the rolls
     */
    public int getAverageRoll() {
        int total = 0;
        for (int i = 0; i < rolls.length; i++) {
            total += rolls[i];
        }
        return total / rolls.length;
    }

    /**
     * @return the int values of the rolls, comma delimited and enclosed in
     * brackets. Ex. [3, 8, 4]
     */
    @Override
    public String toString() {
        String result = "[";
        for (int i = 0; i < rolls.length; i++) {
            result += rolls[i];
            if (i + 1 < rolls.length) {
                result += ", ";
            }
        }
        result += "]";
        return result;
    }

    @Override
    public Iterator<Integer> iterator() {
        Iterator<Integer> it = new Iterator<Integer>() {
            private int currentIndex = 0;

            @Override
            public boolean hasNext() {
                return currentIndex < rolls.length;
            }

            @Override
            public Integer next() {
                return rolls[currentIndex++];
            }

            @Deprecated
            @Override
            /**
             * Don't use this, it doesn't do anything.
             */
            public void remove() {
            }
        };
        return it;
    }
}
