package de.emlidhelper.app.shizuku;

interface IPrivilegedShellService {
    String exec(String command);
    void destroy();
}
