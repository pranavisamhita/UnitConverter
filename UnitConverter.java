import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class UnitConverter extends Frame implements ActionListener, ItemListener {
    Choice categoryChoice, fromUnitChoice, toUnitChoice;
    TextField inputField, outputField;
    Button convertButton, clearHistoryButton;
    TextArea historyArea;

    Map<String, String[]> unitMap = new HashMap<>();
    Map<String, Double> currencyRates = new HashMap<>();

    public UnitConverter() {
        setTitle("Advanced Unit Converter");
        setSize(500, 600);
        setLayout(new BorderLayout());

        // Populate unit categories
        unitMap.put("Length", new String[]{"Kilometers", "Miles", "Meters", "Feet"});
        unitMap.put("Weight", new String[]{"Kilograms", "Pounds", "Grams", "Ounces"});
        unitMap.put("Temperature", new String[]{"Celsius", "Fahrenheit", "Kelvin"});
        unitMap.put("Currency", new String[]{"INR", "USD", "EUR", "GBP"});

        // Top Panel for input
        Panel inputPanel = new Panel(new GridLayout(6, 2, 10, 5));

        inputPanel.add(new Label("Select Category:"));
        categoryChoice = new Choice();
        for (String category : unitMap.keySet()) {
            categoryChoice.add(category);
        }
        categoryChoice.addItemListener(this);
        inputPanel.add(categoryChoice);

        inputPanel.add(new Label("From Unit:"));
        fromUnitChoice = new Choice();
        inputPanel.add(fromUnitChoice);

        inputPanel.add(new Label("To Unit:"));
        toUnitChoice = new Choice();
        inputPanel.add(toUnitChoice);

        inputPanel.add(new Label("Enter Value:"));
        inputField = new TextField();
        inputPanel.add(inputField);

        // 👉 Move Convert Button here
        inputPanel.add(new Label("")); // for layout alignment
        convertButton = new Button("Convert");
        convertButton.setPreferredSize(new Dimension(100, 30));
        convertButton.addActionListener(this);
        inputPanel.add(convertButton);

        inputPanel.add(new Label("Converted Value:"));
        outputField = new TextField();
        outputField.setEditable(false);
        inputPanel.add(outputField);


        add(inputPanel, BorderLayout.NORTH);



        // History Area in ScrollPane
        historyArea = new TextArea("Conversion History:\n", 10, 40, TextArea.SCROLLBARS_VERTICAL_ONLY);
        historyArea.setEditable(false);
        add(historyArea, BorderLayout.SOUTH);

        // Bottom Panel for Clear History Button
        Panel bottomPanel = new Panel();
        clearHistoryButton = new Button("Clear History");
        clearHistoryButton.setPreferredSize(new Dimension(120, 30));
        clearHistoryButton.addActionListener(this);
        bottomPanel.add(clearHistoryButton);
        add(bottomPanel, BorderLayout.PAGE_END);

        // Load currency rates
        loadCurrencyRates();

        // Load history from file
        loadHistoryFromFile();

        // Initial fill
        updateUnitChoices();

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                dispose();
            }
        });

        setVisible(true);
    }

    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == categoryChoice) {
            updateUnitChoices();
        }
    }

    private void updateUnitChoices() {
        fromUnitChoice.removeAll();
        toUnitChoice.removeAll();
        String selectedCategory = categoryChoice.getSelectedItem();
        String[] units = unitMap.get(selectedCategory);
        for (String unit : units) {
            fromUnitChoice.add(unit);
            toUnitChoice.add(unit);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == convertButton) {
            try {
                double inputValue = Double.parseDouble(inputField.getText());
                String fromUnit = fromUnitChoice.getSelectedItem();
                String toUnit = toUnitChoice.getSelectedItem();
                String category = categoryChoice.getSelectedItem();

                double result = convertUnits(category, fromUnit, toUnit, inputValue);
                outputField.setText(String.format("%.4f", result));

                // Prepare history entry
                String historyEntry = String.format("%.2f %s = %.4f %s\n", inputValue, fromUnit, result, toUnit);

                // Update history on the screen
                historyArea.append(historyEntry);

                // Save the history to the file
                appendHistoryToFile(historyEntry);
            } catch (Exception ex) {
                outputField.setText("Invalid Input");
            }
        } else if (e.getSource() == clearHistoryButton) {
            // Clear the history from the file
            clearHistoryFile();

            // Optionally reset the historyArea (comment this line if you want to keep history displayed)
            historyArea.setText("Conversion History:\n");
        }
    }


    private double convertUnits(String category, String from, String to, double value) throws Exception {
        if (from.equals(to)) return value;

        return switch (category) {
            case "Length" -> convertLength(from, to, value);
            case "Weight" -> convertWeight(from, to, value);
            case "Temperature" -> convertTemperature(from, to, value);
            case "Currency" -> convertCurrency(from, to, value);
            default -> value;
        };
    }

    private double convertLength(String from, String to, double value) {
        double inMeters = switch (from) {
            case "Kilometers" -> value * 1000;
            case "Miles" -> value * 1609.34;
            case "Feet" -> value * 0.3048;
            default -> value;
        };

        return switch (to) {
            case "Kilometers" -> inMeters / 1000;
            case "Miles" -> inMeters / 1609.34;
            case "Feet" -> inMeters / 0.3048;
            default -> inMeters;
        };
    }

    private double convertWeight(String from, String to, double value) {
        double inGrams = switch (from) {
            case "Kilograms" -> value * 1000;
            case "Pounds" -> value * 453.592;
            case "Ounces" -> value * 28.3495;
            default -> value;
        };

        return switch (to) {
            case "Kilograms" -> inGrams / 1000;
            case "Pounds" -> inGrams / 453.592;
            case "Ounces" -> inGrams / 28.3495;
            default -> inGrams;
        };
    }

    private double convertTemperature(String from, String to, double value) {
        double inCelsius = switch (from) {
            case "Fahrenheit" -> (value - 32) * 5 / 9;
            case "Kelvin" -> value - 273.15;
            default -> value;
        };

        return switch (to) {
            case "Fahrenheit" -> (inCelsius * 9 / 5) + 32;
            case "Kelvin" -> inCelsius + 273.15;
            default -> inCelsius;
        };
    }

    private double convertCurrency(String from, String to, double value) throws Exception {
        Double fromRate = currencyRates.get(from);
        Double toRate = currencyRates.get(to);
        if (fromRate == null || toRate == null) return value;
        return (value / fromRate) * toRate;
    }

    private void loadCurrencyRates() {
        // Static dummy rates for now (real API integration requires JSON parsing)
        currencyRates.put("INR", 83.0);
        currencyRates.put("USD", 1.0);
        currencyRates.put("EUR", 0.92);
        currencyRates.put("GBP", 0.78);
    }

    private void appendHistoryToFile(String line) {
        try (FileWriter fw = new FileWriter("conversion_history.txt", true)) {
            fw.write(line);
        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        }
    }

    private void loadHistoryFromFile() {
        try (BufferedReader br = new BufferedReader(new FileReader("conversion_history.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                historyArea.append(line + "\n");
            }
        } catch (IOException e) {
            System.out.println("History file not found. Starting fresh.");
        }
    }

    private void clearHistoryFile() {
        try (FileWriter fw = new FileWriter("conversion_history.txt")) {
            fw.write("");
        } catch (IOException e) {
            System.out.println("Error clearing history: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new UnitConverter();
    }
}
