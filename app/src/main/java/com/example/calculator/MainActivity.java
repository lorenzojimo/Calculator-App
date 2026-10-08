package com.example.calculator;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal;

/**
 * Main screen of the calculator app.
 * Handles key presses, maintains the input panel text and evaluates expressions.
 *
 * @author Omar Lorenzo Jimenez
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Developer's last name and CWU ID, shown when the app starts.
     */
    private static final String INITIAL_TEXT = "Lorenzo 1098332182";

    /**
     * Text shown when an expression cannot be evaluated.
     */
    private static final String ERROR_TEXT = "Error";

    /**
     * The input panel.
     */
    private TextView display;

    /**
     * Current expression being typed.
     */
    private StringBuilder expression = new StringBuilder();

    /**
     * True when the panel shows the initial text, a result, or an error (next digit starts fresh).
     */
    private boolean showingPlaceholder = true;

    /**
     * Initializes the layout and the personalized default display.
     *
     * @param savedInstanceState previously saved state, or null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        display = findViewById(R.id.display);
        display.setText(INITIAL_TEXT);
    }

    /**
     * Click handler shared by every key. The key's text identifies the action.
     *
     * @param v the pressed key (a {@link Button})
     */
    @SuppressWarnings("unused")

    public void onKeyClick(View v) {
        String key = ((Button) v).getText().toString();
        switch (key) {
            case "AC":
                clearAll();
                return;
            case "C":
                deleteLast();
                return;
            case "=":
                calculate();
                return;
            default:
                append(key);
        }
    }

    /**
     * Appends a digit, dot, operator or bracket to the expression.
     * Typing a digit after a result/placeholder starts a new expression;
     * typing an operator after a result continues from it.
     *
     * @param key the key text
     */
    private void append(String key) {
        boolean isOperator = "+-*/".contains(key);
        if (showingPlaceholder) {
            boolean keepResult = isOperator && expression.length() > 0;
            if (!keepResult) {
                expression.setLength(0);
            }
            showingPlaceholder = false;
        }
        expression.append(key);
        display.setText(expression);
    }

    /**
     * Clears the whole input panel (AC key).
     */
    private void clearAll() {
        expression.setLength(0);
        showingPlaceholder = false;
        display.setText("0");
    }

    /**
     * Deletes the most recently entered character (C key).
     */
    private void deleteLast() {
        if (showingPlaceholder) {
            clearAll();
            return;
        }
        if (expression.length() > 0) {
            expression.deleteCharAt(expression.length() - 1);
        }
        display.setText(expression.length() == 0 ? "0" : expression);
    }

    /**
     * Evaluates the expression and shows the result or an error message.
     */
    private void calculate() {
        if (expression.length() == 0) {
            return;
        }
        try {
            double value = new Parser(expression.toString()).parse();
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new ArithmeticException("Invalid result");
            }
            String result = format(value);
            display.setText(expression + " = " + result);
            expression = new StringBuilder(result);
        } catch (RuntimeException e) {
            display.setText(ERROR_TEXT);
            expression.setLength(0);
        }
        showingPlaceholder = true; // next digit starts a new calculation
    }

    /**
     * Formats a double without trailing zeros and without floating-point noise.
     *
     * @param value the number to format
     * @return plain-text representation
     */
    private static String format(double value) {
        double rounded = Math.round(value * 1e10) / 1e10;
        return BigDecimal.valueOf(rounded).stripTrailingZeros().toPlainString();
    }

    /**
     * Recursive-descent parser supporting + - * /, brackets, decimals and unary minus.
     * Grammar: expr = term {(+|-) term}; term = factor {(*|/) factor};
     * factor = [-] number | [-] "(" expr ")".
     */
    private static class Parser {

        /**
         * The expression text being parsed.
         */
        private final String s;
        /**
         * Index of the next character to read.
         */
        private int pos = 0;

        /**
         * @param s the expression text
         */
        Parser(String s) {
            this.s = s;
        }

        /**
         * Parses the entire expression.
         *
         * @return the computed value
         * @throws IllegalArgumentException if the expression is malformed
         */
        double parse() {
            double v = expr();
            if (pos != s.length()) {
                throw new IllegalArgumentException("Unexpected '" + s.charAt(pos) + "'");
            }
            return v;
        }

        /**
         * @return the char at the current position, or 0 at end of input
         */
        private char peek() {
            return pos < s.length() ? s.charAt(pos) : 0;
        }

        /**
         * @return value of an addition/subtraction chain
         */
        private double expr() {
            double v = term();
            while (peek() == '+' || peek() == '-') {
                char op = s.charAt(pos++);
                double r = term();
                v = (op == '+') ? v + r : v - r;
            }
            return v;
        }

        /**
         * @return value of a multiplication/division chain
         */
        private double term() {
            double v = factor();
            while (peek() == '*' || peek() == '/') {
                char op = s.charAt(pos++);
                double r = factor();
                if (op == '/' && r == 0) {
                    throw new ArithmeticException("Divide by zero");
                }
                v = (op == '*') ? v * r : v / r;
            }
            return v;
        }

        /**
         * @return value of a number, bracketed expression, or negated factor
         */
        private double factor() {
            if (peek() == '-') {
                pos++;
                return -factor();
            }
            if (peek() == '(') {
                pos++;
                double v = expr();
                if (peek() != ')') {
                    throw new IllegalArgumentException("Missing )");
                }
                pos++;
                return v;
            }
            int start = pos;
            while (Character.isDigit(peek()) || peek() == '.') {
                pos++;
            }
            if (start == pos) {
                throw new IllegalArgumentException("Number expected");
            }
            return Double.parseDouble(s.substring(start, pos));
        }
    }
}