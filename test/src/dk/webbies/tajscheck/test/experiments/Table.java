package dk.webbies.tajscheck.test.experiments;

import dk.webbies.tajscheck.util.Util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by erik1 on 16-01-2017.
 */
public class Table {
    private final List<List<String>> table = Collections.synchronizedList(new ArrayList<>());
    private int size = -1;

    public synchronized void addRow(List<String> objects) {
        assertSize(objects.size());
        table.add(objects);
    }

    private void assertSize(int newSize) {
        if (size == -1) {
            size = newSize;
        } else {
            assert size == newSize;
        }
    }

    public synchronized void setRow(int index, List<String> objects) {
        Util.ensureSize(table, index + 1);
        table.set(index, objects);
    }

    public synchronized void consistencyCheck(int rowIndex) {
        assertSize(table.get(rowIndex).size());
    }

    private synchronized String print(String columnSeparator, String rowSeparator) {
        List<List<String>> table = new ArrayList<>(this.table);
        table.add(totalRow());

        List<String> rows = table.stream()
                .filter(Objects::nonNull)
                .map(row -> String.join(columnSeparator, Util.replaceNulls(row, "-")))
                .collect(Collectors.toList());

        return String.join(rowSeparator, rows);
    }

    private List<String> totalRow() {
        List<List<String>> table = this.table.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (table.size() == 0) {
            return null;
        }
        List<String> result = new ArrayList<>();
        result.add("Total");
        for (int column = 1; column < table.iterator().next().size(); column++) {
            double total = 0;
            for (int row = 1; row < table.size(); row++) {
                List<String> rowList = table.get(row);
                if (rowList.size() > column) {
                    String value = rowList.get(column);
                    try {
                        total += Double.parseDouble(onlyNumeric(value));
                    } catch (NumberFormatException | NullPointerException e) {
                        // Ignored, continue.
                    }
                }
            }
            if (total == 0) {
                result.add("-");
            } else {
                if ((long)total == total) {
                    result.add(Long.toString((long) total));
                } else {
                    result.add(Double.toString(total).replace('.', ','));
                }
            }
        }
        return result;
    }

    private String onlyNumeric(String value) {
        StringBuilder result = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '0':
                case ',':
                case '.':
                    result.append(c);
            }
        }

        return result.toString().replace(",", ".");
    }

    public String toCSV() {
        return print("\t", "\n");
    }

    public String toLatex() {
        return print(" & ", "\\\\ \\hline \n");
    }

    public Table transpose() {
        Table result = new Table();
        for (int i = 0; i < size; i++) {
            List<String> row = new ArrayList<>(table.size());
            for (List<String> list : table) {
                row.add(list.get(i));
            }
            result.addRow(row);
        }
        return result;
    }
}
