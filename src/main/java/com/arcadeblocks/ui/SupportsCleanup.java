package com.arcadeblocks.ui;

/**
 * Интерфейс для UI компонентов, которые поддерживают явную очистку ресурсов
 */
public interface SupportsCleanup {
    /**
     * Выполняет очистку ресурсов компонента
     */
    void cleanup();
}

