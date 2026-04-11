package com.enterprisewebagent.runtime.permissions;

import java.util.List;
import java.util.regex.Pattern;

public class BashSafetyAnalyzer {

    public enum SafetyLevel { SAFE, MODERATE, DANGEROUS }

    private static final List<Pattern> DANGEROUS_PATTERNS = List.of(
        Pattern.compile("rm\\s+-rf\\b"),
        Pattern.compile("rm\\s+-r\\b"),
        Pattern.compile(">\\s*/dev/"),
        Pattern.compile("\\bdd\\s+if="),
        Pattern.compile("\\bmkfs\\b"),
        Pattern.compile("\\bchmod\\s+777\\b"),
        Pattern.compile(":\\(\\)\\s*\\{\\s*:\\|:\\s*&\\s*\\}\\s*;\\s*:"),
        Pattern.compile("\\bshutdown\\b"),
        Pattern.compile("\\breboot\\b"),
        Pattern.compile("\\bkill\\s+-9\\b"),
        Pattern.compile("\\bDROP\\s+TABLE\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bDELETE\\s+FROM\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bTRUNCATE\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bgit\\s+push\\s+--force\\b"),
        Pattern.compile("\\bgit\\s+reset\\s+--hard\\b"),
        Pattern.compile("\\|\\s*sh\\b"),
        Pattern.compile("\\|\\s*bash\\b"),
        Pattern.compile("\\|\\s*eval\\b")
    );

    private static final List<Pattern> MODERATE_PATTERNS = List.of(
        Pattern.compile("\\brm\\b"),
        Pattern.compile("\\bmv\\b"),
        Pattern.compile("\\bchmod\\b"),
        Pattern.compile("\\bchown\\b"),
        Pattern.compile("\\bgit\\s+push\\b"),
        Pattern.compile("\\bnpm\\s+publish\\b"),
        Pattern.compile("\\bdocker\\s+rm\\b"),
        Pattern.compile("\\bkubectl\\s+delete\\b")
    );

    public SafetyLevel analyze(String command) {
        if (command == null || command.isBlank()) {
            return SafetyLevel.SAFE;
        }

        for (Pattern p : DANGEROUS_PATTERNS) {
            if (p.matcher(command).find()) {
                return SafetyLevel.DANGEROUS;
            }
        }

        for (Pattern p : MODERATE_PATTERNS) {
            if (p.matcher(command).find()) {
                return SafetyLevel.MODERATE;
            }
        }

        return SafetyLevel.SAFE;
    }

    public String explain(String command) {
        if (command == null || command.isBlank()) {
            return "Empty command";
        }

        SafetyLevel level = analyze(command);
        return switch (level) {
            case DANGEROUS -> "DANGEROUS: Command contains destructive operations that could cause data loss or system damage";
            case MODERATE -> "MODERATE: Command modifies files or system state and should be reviewed";
            case SAFE -> "SAFE: Command is read-only or low-risk";
        };
    }
}
