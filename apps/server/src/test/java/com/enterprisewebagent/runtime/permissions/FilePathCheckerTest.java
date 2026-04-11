package com.enterprisewebagent.runtime.permissions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilePathCheckerTest {

    private FilePathChecker checker;

    @BeforeEach
    void setUp() {
        checker = new FilePathChecker();
    }

    @Test
    void normalPathIsAllowed() {
        assertTrue(checker.isAllowed("/home/user/project/file.java"));
    }

    @Test
    void sshPathIsBlocked() {
        assertFalse(checker.isAllowed("/home/user/.ssh/id_rsa"));
    }

    @Test
    void awsPathIsBlocked() {
        assertFalse(checker.isAllowed("/home/user/.aws/credentials"));
    }

    @Test
    void dotEnvIsBlocked() {
        assertFalse(checker.isAllowed("/home/user/.env"));
    }

    @Test
    void etcPasswdIsBlocked() {
        assertFalse(checker.isAllowed("/etc/passwd"));
    }

    @Test
    void gnupgPathIsBlocked() {
        assertFalse(checker.isAllowed("/home/user/.gnupg/secring.gpg"));
    }

    @Test
    void kubeConfigIsBlocked() {
        assertFalse(checker.isAllowed("/home/user/.kube/config"));
    }

    @Test
    void nullPathIsBlocked() {
        assertFalse(checker.isAllowed(null));
    }

    @Test
    void emptyPathIsBlocked() {
        assertFalse(checker.isAllowed(""));
    }

    @Test
    void blankPathIsBlocked() {
        assertFalse(checker.isAllowed("   "));
    }

    @Test
    void tildeSshIsBlocked() {
        assertFalse(checker.isAllowed("~/.ssh/id_rsa"));
    }

    @Test
    void tildeAwsIsBlocked() {
        assertFalse(checker.isAllowed("~/.aws/credentials"));
    }

    // ---- isWithinProjectRoot ----

    @Test
    void pathWithinProjectRoot() {
        assertTrue(checker.isWithinProjectRoot("/home/user/project/src/Main.java", "/home/user/project"));
    }

    @Test
    void pathOutsideProjectRoot() {
        assertFalse(checker.isWithinProjectRoot("/etc/config", "/home/user/project"));
    }

    @Test
    void pathAtProjectRoot() {
        assertTrue(checker.isWithinProjectRoot("/home/user/project", "/home/user/project"));
    }

    @Test
    void nullPathNotWithinRoot() {
        assertFalse(checker.isWithinProjectRoot(null, "/home/user/project"));
    }

    @Test
    void nullRootNotWithinRoot() {
        assertFalse(checker.isWithinProjectRoot("/home/user/project/file.java", null));
    }

    // ---- Explicit rules ----

    @Test
    void explicitAllowRuleOverridesSensitiveBlock() {
        PermissionRule allowSsh = new PermissionRule("allow-ssh", PermissionType.ALLOW,
            PermissionScope.FILE_PATH, "\\.ssh/", "Allow SSH access");
        FilePathChecker checkerWithRules = new FilePathChecker(List.of(allowSsh));
        assertTrue(checkerWithRules.isAllowed("/home/user/.ssh/id_rsa"));
    }

    @Test
    void explicitDenyRuleBlocksNormalPath() {
        PermissionRule denyBuild = new PermissionRule("deny-build", PermissionType.DENY,
            PermissionScope.FILE_PATH, "/build/", "Deny build dir");
        FilePathChecker checkerWithRules = new FilePathChecker(List.of(denyBuild));
        assertFalse(checkerWithRules.isAllowed("/home/user/project/build/output.jar"));
    }
}
