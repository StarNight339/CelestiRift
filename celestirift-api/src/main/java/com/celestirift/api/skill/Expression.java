package com.celestirift.api.skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToDoubleFunction;

/**
 * A small arithmetic expression compiled once and evaluated per cast, e.g.
 * {@code 6 + <caster.level> * 0.5} or {@code max(2, <target.mhp> * 0.1)}.
 *
 * <p>Operators: {@code + - * / % ^} and parentheses. Functions: {@code min, max, abs,
 * floor, ceil, round, sqrt, clamp(x, lo, hi), random(lo, hi)}. Variables are written
 * {@code <name>} and resolved by the caller at evaluation time.
 */
public final class Expression {

    /** Thrown for malformed expressions; the message says what is wrong. */
    public static final class ParseException extends RuntimeException {
        public ParseException(String message) {
            super(message);
        }
    }

    private interface Node {
        double eval(ToDoubleFunction<String> vars);
    }

    private final String source;
    private final Node root;
    private final Set<String> variables;
    private final boolean random;

    private Expression(String source, Node root, Set<String> variables, boolean random) {
        this.source = source;
        this.root = root;
        this.variables = Collections.unmodifiableSet(variables);
        this.random = random;
    }

    /**
     * @param source expression text
     * @return the compiled expression
     * @throws ParseException when the text is not a valid expression
     */
    public static Expression compile(String source) {
        Parser p = new Parser(source);
        Node n = p.expr();
        p.skipSpace();
        if (p.pos < source.length()) {
            throw new ParseException("unexpected '" + source.charAt(p.pos) + "' at position " + (p.pos + 1));
        }
        return new Expression(source, n, p.vars, p.random);
    }

    /**
     * @param vars resolves a variable name (without the angle brackets) to its value
     * @return the result
     */
    public double eval(ToDoubleFunction<String> vars) {
        return root.eval(vars);
    }

    /** @return variable names used, without angle brackets */
    public Set<String> variables() {
        return variables;
    }

    /** @return true when the result never changes (no variables, no random) */
    public boolean isConstant() {
        return variables.isEmpty() && !random;
    }

    @Override
    public String toString() {
        return source;
    }

    // ------------------------------------------------------------------ parser

    private static final class Parser {
        private final String s;
        private int pos;
        private final Set<String> vars = new LinkedHashSet<>();
        private boolean random;

        Parser(String s) {
            this.s = s;
        }

        void skipSpace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        boolean eat(char c) {
            skipSpace();
            if (pos < s.length() && s.charAt(pos) == c) {
                pos++;
                return true;
            }
            return false;
        }

        Node expr() {
            Node left = term();
            while (true) {
                if (eat('+')) {
                    Node l = left, r = term();
                    left = v -> l.eval(v) + r.eval(v);
                } else if (eat('-')) {
                    Node l = left, r = term();
                    left = v -> l.eval(v) - r.eval(v);
                } else {
                    return left;
                }
            }
        }

        Node term() {
            Node left = power();
            while (true) {
                if (eat('*')) {
                    Node l = left, r = power();
                    left = v -> l.eval(v) * r.eval(v);
                } else if (eat('/')) {
                    Node l = left, r = power();
                    left = v -> {
                        double d = r.eval(v);
                        return d == 0 ? 0 : l.eval(v) / d;
                    };
                } else if (eat('%')) {
                    Node l = left, r = power();
                    left = v -> {
                        double d = r.eval(v);
                        return d == 0 ? 0 : l.eval(v) % d;
                    };
                } else {
                    return left;
                }
            }
        }

        Node power() {
            Node base = unary();
            if (eat('^')) {
                Node exp = power(); // right-associative
                return v -> Math.pow(base.eval(v), exp.eval(v));
            }
            return base;
        }

        Node unary() {
            if (eat('-')) {
                Node n = unary();
                return v -> -n.eval(v);
            }
            if (eat('+')) {
                return unary();
            }
            return primary();
        }

        Node primary() {
            skipSpace();
            if (pos >= s.length()) {
                throw new ParseException("expression ends too early");
            }
            char c = s.charAt(pos);
            if (eat('(')) {
                Node n = expr();
                if (!eat(')')) throw new ParseException("missing ')'");
                return n;
            }
            if (c == '<') {
                int end = s.indexOf('>', pos);
                if (end < 0) throw new ParseException("missing '>' after '<'");
                String name = s.substring(pos + 1, end).trim().toLowerCase(Locale.ROOT);
                if (name.isEmpty()) throw new ParseException("empty variable '<>'");
                pos = end + 1;
                vars.add(name);
                return v -> v.applyAsDouble(name);
            }
            if (Character.isDigit(c) || c == '.') {
                int start = pos;
                while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
                String num = s.substring(start, pos);
                try {
                    double d = Double.parseDouble(num);
                    return v -> d;
                } catch (NumberFormatException e) {
                    throw new ParseException("bad number '" + num + "'");
                }
            }
            if (Character.isLetter(c)) {
                int start = pos;
                while (pos < s.length() && Character.isLetterOrDigit(s.charAt(pos))) pos++;
                String fn = s.substring(start, pos).toLowerCase(Locale.ROOT);
                if (fn.equals("pi")) return v -> Math.PI;
                if (!eat('(')) throw new ParseException("unknown word '" + fn + "' (variables are written <caster.level>)");
                List<Node> args = new ArrayList<>();
                if (!eat(')')) {
                    do {
                        args.add(expr());
                    } while (eat(','));
                    if (!eat(')')) throw new ParseException("missing ')' after " + fn + "(...");
                }
                return function(fn, args);
            }
            throw new ParseException("unexpected '" + c + "' at position " + (pos + 1));
        }

        Node function(String fn, List<Node> a) {
            return switch (fn) {
                case "min" -> { need(fn, a, 2); yield v -> Math.min(a.get(0).eval(v), a.get(1).eval(v)); }
                case "max" -> { need(fn, a, 2); yield v -> Math.max(a.get(0).eval(v), a.get(1).eval(v)); }
                case "abs" -> { need(fn, a, 1); yield v -> Math.abs(a.get(0).eval(v)); }
                case "floor" -> { need(fn, a, 1); yield v -> Math.floor(a.get(0).eval(v)); }
                case "ceil" -> { need(fn, a, 1); yield v -> Math.ceil(a.get(0).eval(v)); }
                case "round" -> { need(fn, a, 1); yield v -> Math.round(a.get(0).eval(v)); }
                case "sqrt" -> { need(fn, a, 1); yield v -> Math.sqrt(Math.max(0, a.get(0).eval(v))); }
                case "clamp" -> {
                    need(fn, a, 3);
                    yield v -> Math.max(a.get(1).eval(v), Math.min(a.get(2).eval(v), a.get(0).eval(v)));
                }
                case "random" -> {
                    need(fn, a, 2);
                    random = true;
                    yield v -> {
                        double lo = a.get(0).eval(v), hi = a.get(1).eval(v);
                        return hi <= lo ? lo : ThreadLocalRandom.current().nextDouble(lo, hi);
                    };
                }
                default -> throw new ParseException("unknown function '" + fn
                        + "' (min, max, abs, floor, ceil, round, sqrt, clamp, random)");
            };
        }

        static void need(String fn, List<Node> args, int n) {
            if (args.size() != n) {
                throw new ParseException(fn + "() takes " + n + " argument(s), got " + args.size());
            }
        }
    }
}
