package com.github.mike10004.nativehelper.subprocess;

class ShutdownHookProcessContext implements ProcessContext {

    private final ProcessDestroyer destroyer = new ProcessDestroyer();

    @Override
    public void add(Process process) {
        destroyer.add(process);
    }

    @Override
    public boolean remove(Process process) {
        return destroyer.remove(process);
    }
}
