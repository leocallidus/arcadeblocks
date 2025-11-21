package com.arcadeblocks.bosses;

import com.arcadeblocks.gameplay.Boss;
import com.arcadeblocks.config.GameConfig;

/**
 * Босс "Архитектор Города"
 * Уровень 20 - Искусственный интеллект, контролирующий цифровую инфраструктуру города
 */
public class NeonArchitect extends Boss {
    
    public NeonArchitect() {
        super("NEON ARCHITECT", GameConfig.NEON_ARCHITECT_HP);
    }
    
    @Override
    public void onAdded() {
        super.onAdded();
        // TODO: Специфичная инициализация босса
    }
    
    @Override
    public void onUpdate(double tpf) {
        super.onUpdate(tpf);
        // TODO: Специфичное поведение босса
    }
}
