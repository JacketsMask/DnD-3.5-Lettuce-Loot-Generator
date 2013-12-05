package loot.generator;

import diceroller.DiceRoller;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * This class is meant to follow the loot table, gathering information and
 * jumping into other tables when necessary to calculate and generate loot.
 *
 * The crawler begins with a reference to a JTable that holds level information
 * the encounter...
 *
 * @author Jacob
 */
public class LootTableCrawler {

    private HashMap<String, JTable> tables;
    private String lastItem;
    private JDialog debugWindow;
    private JTextArea debugText;

    public LootTableCrawler(HashMap<String, JTable> tables) {
        this.tables = tables;
        debugWindow = new JDialog();
        debugText = new JTextArea();
        debugWindow.add(debugText);
        debugWindow.pack();
        lastItem = null;
    }

    public ArrayList<Object> generateLootFromTable(double level, boolean coins, boolean goods, boolean items) {
        ArrayList<Object> results = new ArrayList<>();
        JTable levelTable = tables.get("Level " + (int) level);
        System.out.println("Attempting to retrieve data from level " + (int) level + ".");
        if (coins) {
            System.out.println("Coins:");
            //Get row index resulting from rolling the column
            int rowIndex = rollColumn(levelTable, 0);
            //Get text from cell
            String text = levelTable.getValueAt(rowIndex, 1).toString();
            //Issolate currency from cell
            Currency coinResult = rollCoinsFromCell(text);
            if (coinResult != null) {
                results.add(coinResult);
            } else {
                System.out.println("No coins found.");
            }
        }
        if (goods) {
            System.out.println("\nGoods:");
            //Get row index resulting from rolling the column
            int rowIndex = rollColumn(levelTable, 2);
            String cellObject = levelTable.getValueAt(rowIndex, 3).toString();
            //Make sure the cell isn't empty
            if (!cellObject.isEmpty()) {
                String text = levelTable.getValueAt(rowIndex, 3).toString();
                ArrayList<Object> goodsResult = rollGoods(text);
                for (Object o : goodsResult) {
                    results.add(o);
                }
            } else {
                System.out.println("No goods found.");
            }
        }
        if (items) {
            System.out.println("\nItems:");
            //Get row index resulting from rolling the column
            int rowIndex = rollColumn(levelTable, 4);
            //Get text from cell
            Object value = levelTable.getValueAt(rowIndex, 5);
            String text = null;
            if (value != null) {
                text = (String) value;
            }
            if (!isEmpty(text)) {
                ArrayList<String> item = rollItems(text);
                for (String s : item) {
                    results.add(s);
                }
            } else {
                System.out.println("No items found.");
            }
        }
        return results;
    }

    private ArrayList<String> rollItems(String cellText) {
        ArrayList<String> results = new ArrayList<>();
        int quantity = calculateRollsFromString(cellText);
        System.out.println(quantity + " items found.");
        JTable targetTable = null;
        String columnToRoll = "Roll";
        if (cellText.contains("mundane")) {
            //Generate mundane items
            System.out.println("insert mundane item here");
            targetTable = tables.get("Mundane Types");
        } else if (cellText.contains("minor")) {
            //Generate minor items
            targetTable = tables.get("Magic Item Generation");
            columnToRoll = "Minor Roll";
            System.out.println("insert minor item here");
        } else if (cellText.contains("medium")) {
            //Generate medium items
            targetTable = tables.get("Magic Item Generation");
            columnToRoll = "Medium Roll";
            System.out.println("insert medium item here");
        } else if (cellText.contains("major")) {
            //Generate major
            targetTable = tables.get("Magic Item Generation");
            columnToRoll = "Major Roll";
            System.out.println("insert major item here");
        } else {
            throw new RuntimeException("Item quality not recognized: " + cellText);
        }
        if (targetTable != null) {
            //Begin the machine
            for (int i = 0; i < quantity; i++) {
                String result = startCrawler(targetTable, columnToRoll, null).trim();
                results.add(result + ".");
            }
        } else {
            throw new RuntimeException("Table not set yet.");
        }
        return results;
    }

    /**
     * Attempts to isolate a #d# formatted roll from the passed String, and
     * calculate the roll.
     *
     * @param text
     * @return the result of the roll
     */
    private int calculateRollsFromString(String text) {
        String firstNum = "";
        String secondNum = "";
        boolean numberStarted = false;
        int i;
        for (i = 0; i < text.length(); i++) {
            char nextChar = text.charAt(i);
            if (!numberStarted && Character.isDigit(nextChar)) {
                numberStarted = true;
                firstNum += nextChar;
            } else if (numberStarted && Character.isDigit(nextChar)) {
                firstNum += nextChar;
            } else if (numberStarted && !Character.isDigit(nextChar)) {
                if (nextChar == 'd') {
                    i++;
                    break;
                } else {
                    return Integer.parseInt(firstNum);
                }
            }
        }
        numberStarted = false;
        for (; i < text.length(); i++) {
            char nextChar = text.charAt(i);
            if (!numberStarted && Character.isDigit(nextChar)) {
                numberStarted = true;
                secondNum += nextChar;
            } else if (numberStarted && Character.isDigit(nextChar)) {
                secondNum += nextChar;
            } else if (numberStarted && !Character.isDigit(nextChar)) {
                break;
            }
        }
        return DiceRoller.rollDice(Integer.parseInt(firstNum), Integer.parseInt(secondNum)).getTotalRoll();
    }

    /**
     * This method is meant to be used to to crawl through dynamic tables,
     * interpreting scripts and ultimately coming to a result.
     *
     * First of all, roll a d100 to determine target row. If there is a "Next
     * Table" column, follow parse the contents of that. Otherwise, look for a
     * results column, and return the contents of that.
     *
     * @param table the table in which to start crawling
     * @return a String representing the results of the table crawl
     */
    private String startCrawler(JTable table, String columnNameToRoll, String resultColumnName) {
        //Make sure there's a column roll name
        if (columnNameToRoll == null) {
            columnNameToRoll = "Roll";
        }
        System.out.println("Now in table: " + table.getName());
        //Find the roll column
        TableColumn columnToRoll = findColumn(table.getColumnModel(), columnNameToRoll);
        if (columnToRoll == null) {
            //Try standard roll
            columnToRoll = findColumn(table.getColumnModel(), "Roll");
            System.out.println("Looking for roll column");
        }
        if (columnToRoll == null) {
            throw new RuntimeException("\"" + columnNameToRoll + "\" column not found.");
        }
        //Roll in that column
        int targetRowIndex = rollColumn(table, columnToRoll.getModelIndex());
        //Create a new string to store the result as we go
        String result = "";
        TableColumn targetColumn = null;
        //Begin crawl loop
        if (resultColumnName != null) {
            //Look for custom column name
            targetColumn = findColumn(table.getColumnModel(), resultColumnName);
        }
        //If a suitable column hasn't been found
        if (targetColumn == null) {
            //Look for a "Next Table"
            targetColumn = findColumn(table.getColumnModel(), "Next Table");
        }
        //Check if next table is found and we weren't expecting a different column
        if (targetColumn != null && resultColumnName == null) {
            //Get text from corresponding cell
            Object object = table.getValueAt(targetRowIndex, targetColumn.getModelIndex());
            //Make sure the cell isn't empty
            String cellText = null;
            if (object != null) {
                cellText = object.toString();
            }
            //If the cell is not empty, parse cell logic
            if (!isEmpty(cellText)) {
                ArrayList<Object> crawlerParse = crawlerParse(cellText);
                for (Object o : crawlerParse) {
                    //String, return as part of result
                    if (o instanceof String) {
                        result += o;
                        //Table, recursively follow reference
                    } else if (o instanceof JTable) {
                        //Add space for next result
                        result += " ";
                        JTable nextTable = (JTable) o;
                        result += startCrawler(nextTable, columnNameToRoll, null);
                    } else if (o instanceof CrawlerCommands) {
                        if (o.equals(CrawlerCommands.SCROLL)) {
                            result = "Scroll: " + (commandScroll(columnNameToRoll));
                        } else if (o.equals(CrawlerCommands.WONDER)) {
                            result = "Wonderous item: " + (commandWonder(columnNameToRoll));
                        } else if (o.equals(CrawlerCommands.SPECIAL_ABILITY)) {
                            String newResult = (commandSpecialAbility(columnNameToRoll, result)).trim();
                            if (!newResult.contains("Magic equipment: ")) {
                                result = "Magic equipment: " + newResult;
                            } else {
                                result = newResult;
                            }
                        }
                    }
                }
            } else {
                TableColumn resultColumn = findColumn(table.getColumnModel(), "Result");
                //Return the value in the results table
                result += table.getValueAt(targetRowIndex, resultColumn.getModelIndex()).toString(); //null pointer
            }
        } //Next table not found
        else {
            //Look for results cell to end crawl
            TableColumn resultColumn;
            if (resultColumnName != null) {
                resultColumn = findColumn(table.getColumnModel(), resultColumnName);
            } else {
                resultColumn = findColumn(table.getColumnModel(), "Result");
            }
            //Return the value in the results table
            try {
                //If errors are thrown here, a table is probably not 1-100
                result += table.getValueAt(targetRowIndex, resultColumn.getModelIndex()).toString();
            } catch (Exception e) {
                System.err.println("Exception in table: " + table.getName() + " at row: " + targetRowIndex + ", column: " + resultColumn.getModelIndex());
                e.printStackTrace();
                System.exit(1);
            }
        }
        return result;
    }

    private ArrayList<Object> rollGoods(String text) {
        ArrayList<Object> goods = new ArrayList<>();
        String cellText = text;
        if (isEmpty(cellText)) {
            return goods;
        }
        //Get goods type
        String type = "";
        if (cellText.contains("gem")) {
            cellText = cellText.replace("gems", "");
            cellText = cellText.replace("gem", "");
            type = "gems";
        } else if (cellText.contains("art")) {
            cellText = cellText.replace("art", "");
            type = "art";
        } else {
            throw new RuntimeException("Can't detect goods type, expected gems or art.");
        }
        int quantity;
        //Look for roll
        if (cellText.contains("d")) {
            String[] split = cellText.split(" ");
            quantity = rollDice(split[0]);
            //No roll, must be just a number
        } else {
            quantity = Integer.parseInt(cellText.trim());
        }
        System.out.println("Found " + quantity + " goods.");
        //Iterate over the quantity of goods found
        for (int i = 0; i < quantity; i++) {
            if (type.equals("gems")) {
                System.out.println("Goods found: gems");
                String gem = rollGem();
                goods.add(gem);
            } else {
                System.out.println("Goods found: art");
                String art = rollArt();
                goods.add(art);
            }
        }
        return goods;
    }

    private String rollArt() {
        JTable artTable = tables.get("Art");
        int rowIndex = rollColumn(artTable, 0);
        String cellText = artTable.getValueAt(rowIndex, 1).toString();
        Currency value = rollCoinsFromCell(cellText);
//        String examples = artTable.getValueAt(rowIndex, 3).toString();
        return "Art worth " + value + ". ";
    }

    private String rollGem() {
        JTable gemTable = tables.get("Gems");
        int rowIndex = rollColumn(gemTable, 0);
        String cellText = gemTable.getValueAt(rowIndex, 1).toString();
        Currency value = rollCoinsFromCell(cellText);
//        String examples = gemTable.getValueAt(rowIndex, 3).toString();
        return "Gem worth " + value + ". ";
    }

    private Currency rollCoinsFromCell(String text) {
        String cellText = text;
        if (isEmpty(cellText)) {
            return null;
        }
        Currency.CurrencyType type;
        //Find currency type
        if (cellText.contains("cp")) {
            type = Currency.CurrencyType.COPPER;
            cellText = cellText.replace("cp", "");
        } else if (cellText.contains("sp")) {
            type = Currency.CurrencyType.SILVER;
            cellText = cellText.replace("sp", "");
        } else if (cellText.contains("gp")) {
            type = Currency.CurrencyType.GOLD;
            cellText = cellText.replace("gp", "");
        } else if (cellText.contains("pp")) {
            type = Currency.CurrencyType.PLATINUM;
            cellText = cellText.replace("pp", "");
        } else {
            throw new RuntimeException("Can't detect currency type, expected cp, sp, gp, or pp.");
        }
        //If there's a multiplier, split on it
        if (cellText.contains("x")) {
            //Split on the multiplier
            String[] split = cellText.split("x");
            String coinRoll = split[0];
            //Remove commas
            split[1] = split[1].replaceAll(",", "");
            int multiplier = Integer.parseInt(split[1].trim());
            int rollDice = rollDice(coinRoll);
            return new Currency(multiplier * rollDice, type);
        } else {
            cellText = cellText.replaceAll(",", "");
            int rollDice = rollDice(cellText.trim());
            return new Currency(rollDice, type);
        }

    }

    private int rollDice(String diceRoll) {
        String[] split = diceRoll.trim().split("d");
        return DiceRoller.rollDice(Integer.parseInt(split[0].trim()), Integer.parseInt(split[1].trim())).getTotalRoll();
    }

    /**
     * Rolls a d100, and then finds the value in the column that fits within the
     * range of the result. Returns the row value of the cell.
     *
     * @param column the column to roll in
     * @return the row value of the cell
     */
    private int rollColumn(JTable table, int columnIndex) {
        int d100 = d100();
        System.out.println("Roll: " + d100);
        for (int i = 0; i < table.getRowCount(); i++) {
            //Make sure the cell isn't empty
            Object valueAt = table.getValueAt(i, columnIndex);
            if (valueAt != null && !isEmpty(valueAt.toString())) {
                String[] split = valueAt.toString().split("-");
                //If split is needed, do it and make sure it works
                if (!split[0].equals("") && split.length > 1 && !split[1].equals("")) {
                    int lowerBound = Integer.parseInt(split[0].trim());
                    int upperBound = Integer.parseInt(split[1].trim());
                    if (d100 >= lowerBound && d100 <= upperBound) {
                        System.out.println("Target row index: " + i);
                        return i;
                    }
                    //No split needed
                } else {
                    int cellValue = Integer.parseInt(valueAt.toString().trim());
                    if (d100 == cellValue) {
                        System.out.println("Target row index: " + i);
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private TableColumn findColumn(TableColumnModel model, String query) {
        Enumeration<TableColumn> columns = model.getColumns();
        while (columns.hasMoreElements()) {
            TableColumn nextElement = columns.nextElement();
            if (nextElement.getHeaderValue().toString().equalsIgnoreCase(query)) {
                return nextElement;
            }
        }
        return null;
    }

    private int d100() {
        return DiceRoller.rollDice(1, 100).getTotalRoll();
    }

    /**
     * Deciphers the meaning of the passed command(s). Strings will be added to
     * the ArrayList as String literals, and JTable names will be added to the
     * ArrayList as JTables.
     *
     * @param cellText
     * @return
     */
    private ArrayList<Object> crawlerParse(String cellText) {
        String parseText = cellText;
        ArrayList<Object> results = new ArrayList<>();
        //Tokenize if there are multiple commands
        String[] split;
        if (parseText.contains("AND")) {
            split = parseText.split("AND");
            for (String s : split) {
                results.add(crawlerSanitizeCommand(s));
            }
            //There's only one command
        } else {
            results.add(crawlerSanitizeCommand(parseText));
        }
        return results;
    }

    private Object crawlerSanitizeCommand(String command) {
        System.out.println("Sanitizing: " + command);
        String text = command;
        char firstChar = text.trim().charAt(0);
        if (firstChar == '[') {
            text = text.replace("[", "");
            text = text.replace("]", "");
            JTable table = tables.get(text.trim());
            if (table != null) {
                return table;
            } else {
                throw new RuntimeException("No table found: " + text);
            }
        } else if (firstChar == '"') {
            text = text.replace("\"", "");
            return text.trim();
        } else if (firstChar == '@') {
            if (command.trim().equals("@SCROLL")) {
                return CrawlerCommands.SCROLL;
            }
            if (command.trim().equals("@WONDER")) {
                return CrawlerCommands.WONDER;
            }
            if (command.trim().equals("@SPECIAL_ABILITY")) {
                return CrawlerCommands.SPECIAL_ABILITY;
            }
        }
        throw new RuntimeException("Invalid next table field detected: Can't process" + command + ".");

    }

    private String commandScroll(String rollColumn) {
        //Keep track of the final table name as we go
        String finalTableName = "Level";
        //Determine spell level
        String spellLevel = startCrawler(tables.get("Scroll Spell Levels"), rollColumn, "Scroll Caster Level").trim();
        finalTableName += " " + spellLevel;
        //Determine caster level
        String spellsCasterLevel = startCrawler(tables.get("Scroll Spell Levels"), rollColumn, "Spell's Caster Level").trim();
        //Determine scroll type
        finalTableName += " " + startCrawler(tables.get("Scroll Types"), null, null);
        finalTableName += " Scrolls";
        //Determine number of uses
        int uses = 0;
        if (rollColumn.contains("Minor")) {
            uses = DiceRoller.rollDice(1, 3).getTotalRoll();
        } else if (rollColumn.contains("Medium")) {
            uses = DiceRoller.rollDice(1, 4).getTotalRoll();
        } else if (rollColumn.contains("Major")) {
            uses = DiceRoller.rollDice(1, 6).getTotalRoll();
        }
        //Return final scroll type and amount of uses
        System.out.println("Going to scroll table: " + finalTableName);
        return startCrawler(tables.get(finalTableName), null, null).trim() + " (spell level: " + spellLevel + ")" + " (spell's caster level: " + spellsCasterLevel + ") (" + uses + " uses)";
    }

    private String commandWonder(String rollColumn) {
        if (rollColumn.contains("Minor")) {
            return startCrawler(tables.get("Minor Wonderous Items"), null, null);
        }
        if (rollColumn.contains("Medium")) {
            return startCrawler(tables.get("Medium Wonderous Items"), null, null);
        }
        if (rollColumn.contains("Major")) {
            return startCrawler(tables.get("Major Wonderous Items"), null, null);
        }
        return "???Unknown wonderous item???";
    }

    private String commandSpecialAbility(String rollColumn, String itemSoFar) {
        System.out.println("Attempting to create a special ability for: " + itemSoFar);
        System.out.println("Ability tier:" + rollColumn);
        //If the item so far is empty, it is because this is a double effect
        if (itemSoFar.equals("")) {
            itemSoFar = this.lastItem;
        }
        JTable specialAbilityTable;
        if (isMeleeWeapon(itemSoFar)) {
            specialAbilityTable = tables.get("Melee Special Abilities");
        } else if (isRangedWeapon(itemSoFar)) {
            specialAbilityTable = tables.get("Ranged Special Abilities");
        } else if (isArmor(itemSoFar)) {
            specialAbilityTable = tables.get("Armor Special Abilities");
        } else if (isShield(itemSoFar)) {
            specialAbilityTable = tables.get("Shield Special Abilities");
        } else {
            debugWindow.setVisible(true);
            debugText.setText(debugText.getText() + "\n" + "Unable to create special ability for: " + itemSoFar);
            return "";
//            throw new RuntimeException("Unable to create special ability for: " + itemSoFar);
        }
        String ability = startCrawler(specialAbilityTable, rollColumn, null);
        this.lastItem = itemSoFar;
        return itemSoFar + "(" + ability + ")";
    }

    private boolean isRangedWeapon(String item) {
        JTable get = tables.get("Common Ranged Weapons");
        int columnIndex = get.getColumnModel().getColumnIndex("Result");
        for (int row = 0; row < get.getRowCount(); row++) {
            //Get the contents of the cell
            Object valueAt = get.getValueAt(row, columnIndex);
            //If the cell isn't empty and the contents of the cell match part of
            //the string, return true
            if (valueAt != null && item.toLowerCase().contains(valueAt.toString().trim().toLowerCase())) {
                return true;
            }
        }
        //Exceptions
        String[] exceptions = {"Net", "Crossbow", "Shuriken", "Bolt", "Arrow",
            "Oathbow"};
        for (String s : exceptions) {
            if (item.toLowerCase().contains(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean isMeleeWeapon(String item) {
        JTable get = tables.get("Common Melee Weapons");
        int columnIndex = get.getColumnModel().getColumnIndex("Result");
        for (int row = 0; row < get.getRowCount(); row++) {
            //Get the contents of the cell
            Object valueAt = get.getValueAt(row, columnIndex);
            //If the cell isn't empty and the contents of the cell match part of
            //the string, return true
            if (valueAt != null && item.toLowerCase().contains(valueAt.toString().trim().toLowerCase())) {
                return true;
            }
        }
        //Exceptions
        String[] exceptions = {"Whip", "Flail", "Halfspear", "Sap",
            "Battleaxe", "Halberd", "Pick", "Axe", "Gauntlet", "Hammer",
            "Scythe", "Urgrosh", "Sickle", "Sword", "Longspear", "Morningstar",
            "Lance", "Ranseur", "Club", "Guisarme", "Falchion", "Glaive",
            "Kukri", "Trident", "Blade", "Nine Lives", "Flame tongue",
            "Shifter's Sorrow", "Mace", "Life-Drinker", "Holy avenger",
            "Frost Brand", "Dwarven thrower"};
        for (String s : exceptions) {
            if (item.toLowerCase().contains(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean isShield(String item) {
        return (item.toLowerCase().contains("Buckler".toLowerCase()) || item.toLowerCase().contains("Shield".toLowerCase()));
    }

    private boolean isArmor(String item) {
        JTable get = tables.get("Random Armor Type");
        int columnIndex = get.getColumnModel().getColumnIndex("Result");
        for (int row = 0; row < get.getRowCount(); row++) {
            //Get the contents of the cell
            Object valueAt = get.getValueAt(row, columnIndex);
            //If the cell isn't empty and the contents of the cell match part of
            //the string, return true
            if (valueAt != null && item.toLowerCase().contains(valueAt.toString().trim().toLowerCase())) {
                return true;
            }
        }
        //Exceptions
        String[] exceptions = {"Shirt", "Chain", "Plate", "Armor"};
        for (String s : exceptions) {
            if (item.toLowerCase().contains(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param string
     * @return true if the passed string is null or empty
     */
    private boolean isEmpty(String string) {
        return (string == null || string.equals("") || string.equals(" ") || string.equals("  "));
    }
}