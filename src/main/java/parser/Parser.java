package parser;

import command.*;
import exceptions.NullException;
import exceptions.InvalidCommand;
import seedu.duke.FinancialGoal;
import seedu.duke.TransactionManager;
import seedu.duke.Storage;
import seedu.duke.SavingMode;
import enumStructure.Category;
import ui.Ui;
import java.util.Scanner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static constant.Constant.*;

public class Parser {
    /**
     * Parses the user input and returns the corresponding command.
     *
     * @param userInput The raw user input string.
     * @throws NullException If the input is invalid or missing required details.
     */
    public static void parser(String userInput, Ui ui, TransactionManager transactions,
                              FinancialGoal goal, Storage storage) {
        String[] parts = userInput.toLowerCase().split(" ", 2);
        String commandType = parts[0];
        String[] details;
        double amount;
        int index;
        int id;
        String[] fields;

        try {
            switch (commandType) {
            case COMMAND_HELP:
                ui.help();
                break;
            case COMMAND_ADD:
                fields = new String[]{"description", "amount", "category"};
                String[] patterns = {
                        "d/(.*?)(?:\\s+[ace]/|$)", // d/
                        "a/(.*?)(?:\\s+[dce]/|$)", // a/
                        "c/(.*?)(?:\\s+[dae]/|$)"  // c/
                };

                String[] results = new String[fields.length];
                //match pattern
                for (int i = 0; i < fields.length; i++) {
                    Pattern pattern = Pattern.compile(patterns[i]);
                    Matcher matcher = pattern.matcher(parts[1]);

                    if (matcher.find()) {
                        results[i] = matcher.group(1).trim();
                    } else {
                        throw new InvalidCommand("No " + fields[i] + " found");
                    }
                }
                amount = Double.parseDouble(results[1]);
                Category category = parseCategory(results[2], ui);

                boolean success = transactions.addTransaction(transactions.getNum() + 1, results[0], amount, category);

                if (success) {
                    ui.add(transactions.searchTransaction(transactions.getNum()));
                    storage.saveTransactions(transactions.getTransactions());
                }
                break;
            case COMMAND_LIST:
                if (parts.length > 1) {
                    throw new InvalidCommand("Invalid command");
                }
                ui.printTransactions(transactions.getTransactions());
                break;
            case COMMAND_TICK:
                id = Integer.parseInt(parts[1]);
                transactions.tickTransaction(id);
                ui.tickTransaction(transactions.searchTransaction(id));
                storage.saveTransactions(transactions.getTransactions());
                break;
            case COMMAND_UNTICK:
                id = Integer.parseInt(parts[1]);
                transactions.unTickTransaction(id);
                ui.unTickTransaction(transactions.searchTransaction(id));
                storage.saveTransactions(transactions.getTransactions());
                break;
            case COMMAND_SEARCH:
                boolean isIndex = parts[1].startsWith("id-");
                String keyWord = isIndex ? parts[1].substring(3) : parts[1];
                ui.search(isIndex);
                ui.printTransactions(transactions.searchTransactionList(isIndex, keyWord));
                break;
            case COMMAND_EDIT:
                try {
                    parseEditCommands(parts[1], ui, transactions);
                } catch (Exception e) {
                    throw new InvalidCommand("Format invalid, try again! (edit [attribute] [id] [value])");
                }
                storage.saveTransactions(transactions.getTransactions());
                break;
            case COMMAND_DELETE:
                index = Integer.parseInt(parts[1]);
                new DeleteCommand(index - 1, transactions, ui);
                storage.saveTransactions(transactions.getTransactions());
                break;
            case COMMAND_CLEAR:
                transactions.clear();
                storage.saveTransactions(transactions.getTransactions());
                ui.PrintClear();
                break;
            case COMMAND_SET_BUDGET:
                details = parts[1].split(IDENTIFIER_AMOUNT, 2);
                amount = Integer.parseInt(details[1]);
                new SetBudgetCommand(amount, transactions, ui);
                storage.saveTransactions(transactions.getTransactions());
                break;
            case FIND_DATE:
                String time = parts[1];
                transactions.getUpcomingTransactions(time);
                break;
            case COMMAND_NOTIFY:
                String[] detail = {"description", "amount", "category", "date"};
                String[] notifyPatterns = {
                        "d/(.*?)(?:\\s+[ac]/|$)", // d/
                        "a/(.*?)(?:\\s+[dc]/|$)", // a/
                        "c/(.*?)(?:\\s+[at]/|$)",  // c/
                        "t/(.*?)(?:\\s+[da]/|$)"  // t/
                };

                String[] result = new String[detail.length];
                //match pattern
                for (int i = 0; i < detail.length; i++) {
                    Pattern pattern = Pattern.compile(notifyPatterns[i]);
                    Matcher matcher = pattern.matcher(parts[1]);

                    if (matcher.find()) {
                        result[i] = matcher.group(1).trim();
                    } else {
                        throw new InvalidCommand("No " + detail[i] + " found");
                    }
                }

                amount = Double.parseDouble(result[1]);
                String categoryString = result[2].toUpperCase();
                String date = result[3];

                new NotifyCommand(result[0], amount, categoryString, date, transactions, ui);
                storage.saveTransactions(transactions.getTransactions());
                break;
            case COMMAND_ALERT:
                if (parts.length > 1) {
                    throw new InvalidCommand("Invalid command");
                }
                new AlertCommand(transactions, ui);
                storage.saveTransactions(transactions.getTransactions());
                break;

            case COMMAND_SET_PRIORITY:
                try {
                    details = parts[1].split(" ", 2);
                    index = Integer.parseInt(details[0]);

                    new SetPriorityCommand(index - 1, details[1], transactions, ui);
                } catch (NullException e) {
                    throw new InvalidCommand("Invalid input format, should be (priority [id] [priority_level])");
                }
                storage.saveTransactions(transactions.getTransactions());
                break;
            case COMMAND_RECUR:
                int slashIndex = parts[1].indexOf("/");
                try {
                    int transactionId = Integer.parseInt(parts[1].substring(0, slashIndex).trim());
                    int recurringPeriod = Integer.parseInt(parts[1].substring(slashIndex + 1).trim());
                    transactions.setRecur(transactionId, recurringPeriod);
                    ui.setPeriod(transactions.searchTransaction(transactionId), recurringPeriod);
                } catch (Exception e) {
                    throw new InvalidCommand("Format invalid, try again! (recur [id]/[period])");
                }
                storage.saveTransactions(transactions.getTransactions());
                break;
            case COMMAND_EXIT:
                ui.printExit();
                storage.saveTransactions(transactions.getTransactions());
                System.exit(0);
                break;
            case "saving":
                    SavingMode.enter(ui, goal, storage);
                    break;
            case COMMAND_GOAL:
                goal.updateExpenses(transactions);
                try {
                    String goalTag = parts.length < 2 ? "placeholder" : parts[1];
                    parseGoalCommands(goalTag, ui, goal);
                } catch (Exception e) {
                    throw new InvalidCommand("Format invalid, try again!");
                }
                break;
            default:
                throw new InvalidCommand(INVALID_INPUT);
            }
        } catch (Exception e) {
            ui.showError(e.getMessage());
        }
    }

    public static void parseGoalCommands(String command, Ui ui, FinancialGoal goal) throws Exception {
        String[] parts = command.toLowerCase().split(" ", 2);

        switch (parts[0]) {
        case GOAL_TARGET:
            goal.setTargetAmount(Integer.parseInt(parts[1]));
            ui.setGoalTarget(goal);
            break;
        case GOAL_DESC:
            goal.setDescription(parts[1]);
            ui.setGoalDescription(goal);
            break;
        case GOAL_TITLE:
            goal.setGoal(parts[1]);
            ui.setGoalTitle(goal);
            break;
        case GOAL_STATUS:
            if (goal.isBlank()) {
                throw new InvalidCommand("Empty goal.");
            } else {
                goal.checkGoalStatus();
            }
            break;
        case GOAL_NEW:
            goal.createNewGoal(ui);
            break;
        default:
            if (goal.isBlank()) {
                goal.createNewGoal(ui);
            } else {
                ui.printGoal(goal);
            }
        }
    }

    public static void parseEditCommands(String command, Ui ui, TransactionManager transactions) throws Exception {
        String[] fields = command.toLowerCase().split(" ", 3);
        String attribute = fields[0];
        int id = Integer.parseInt(fields[1]);
        if (id >= transactions.getNum() || id < 0) {
            throw new InvalidCommand("ID is out of range!");
        }
        String value = fields[2];

        switch (attribute) {
        case EDIT_DESC:
            transactions.editInfo(id, value, 0);
            ui.printEdited(value, 0);
            break;
        case EDIT_CAT:
            try {
                transactions.editInfo(id, value, 1);
            } catch (Exception e) {
                throw new InvalidCommand("Unknown category, try again!");
            }
            ui.printEdited(value, 1);
            break;
        case EDIT_AM:
            transactions.editInfo(id, value, 2);
            ui.printEdited(value, 2);
            break;
        case EDIT_CURR:
            try {
                transactions.editInfo(id, value, 3);
            } catch (Exception e) {
                throw new InvalidCommand("Unknown currency, try again!");
            }
            ui.printEdited(value, 3);
            break;
        default:
            throw new InvalidCommand("Unknown attribute!");
        }
    }

    /**
     * Tries to parse a string into a valid Category enum, case-insensitively.
     * If the input is invalid, shows a list of valid categories and asks user to choose one.
     */
    public static Category parseCategory(String userInput, Ui ui) {
        String trimmedInput = userInput.trim();
        for (Category category : Category.values()) {
            if (category.name().equalsIgnoreCase(trimmedInput)) {
                return category;
            }
        }

        // Invalid category
        ui.showError("Invalid category: \"" + userInput + "\"");

        // Show available options
        ui.showLine();
        System.out.println("Please choose a valid category from the list below:");
        int index = 1;
        for (Category category : Category.values()) {
            System.out.println(index + ". " + category.name());
            index++;
        }
        ui.showLine();

        // Prompt for selection
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Enter category number (1-" + Category.values().length + "): ");
            String choice = scanner.nextLine();
            try {
                int selected = Integer.parseInt(choice);
                if (selected >= 1 && selected <= Category.values().length) {
                    return Category.values()[selected - 1];
                }
            } catch (NumberFormatException ignored) {}
            System.out.println("Invalid selection. Please enter a number between 1 and " + Category.values().length + ".");
        }
    }

}
