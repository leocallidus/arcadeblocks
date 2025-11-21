package com.arcadeblocks.bosses;

import com.arcadeblocks.gameplay.Boss;
import com.arcadeblocks.config.GameConfig;

/**
 * Босс "MONOLITH.exe"
 * Уровень 30 - Центральное ядро мегакорпорации, последний барьер системы
 */
public class MonolithExe extends Boss {
    
    public MonolithExe() {
        super("MONOLITH.EXE", GameConfig.MONOLITH_EXE_HP);
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
