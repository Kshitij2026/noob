package com.noobfree.android;

import com.noobfree.android.ShellCommandResult;

interface IShizukuShellService {
    ShellCommandResult executeCommand(String command, long timeoutMs);
    int getUid();
    void destroy();
}
