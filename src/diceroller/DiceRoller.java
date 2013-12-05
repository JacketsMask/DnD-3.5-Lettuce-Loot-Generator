package diceroller;

import java.security.InvalidParameterException;
import java.util.Random;

/**
 * A die roller capable of rolling dice and returning the result.
 *
 * @author Jacob Dorman
 */
public class DiceRoller {

    private static final Random random = new Random();

    /**
     * Rolls the given number of die, each with the given number of sides, and
     * returns the results in an int array.
     *
     * @param numberOfDice the number of die
     * @param numberOfSides the number of sides to each dice
     * @return int[] of resulting values
     */
    public static DiceRoll rollDice(int numberOfDice, int numberOfSides) {
        if (numberOfDice < 1) {
            throw new InvalidParameterException("Invalid number of dice.");
        }
        int[] result = new int[numberOfDice];
        for (int i = 0; i < numberOfDice; i++) {
            result[i] = random.nextInt(numberOfSides) + 1;
        }
        return new DiceRoll(result);
    }

    /**
     * Rolls 3d6, and returns the sum of the larger two rolls.
     * @return the sum of the larger two rolls
     */
    public static int rollAbilityScore() {
        int result = 0;
        int smallestRoll = 100;
        for (int i = 0; i < 4; i++) {
            int roll = random.nextInt(6) + 1;
            //Check to see if this is the new smallest roll
            if (roll < smallestRoll) {
                smallestRoll = roll;
            }
            result += roll;
        }
        //Subtract the smallest roll
        result -= smallestRoll;
        return result;
    }
    
    /**
     * Rolls a single twenty sided die and returns the result.
     * @return the result
     */
    public static int rollD20() {
        return (random.nextInt(20) + 1);
    }
}
