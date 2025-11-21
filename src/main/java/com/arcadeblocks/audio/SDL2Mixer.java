package com.arcadeblocks.audio;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * JNA interface для SDL2_mixer библиотеки
 * Предоставляет доступ к функциям воспроизведения музыки и звуковых эффектов
 */
public interface SDL2Mixer extends Library {
    
    // Загружаем библиотеку SDL2_mixer динамически
    static SDL2Mixer getInstance() {
        try {
            // Сначала пытаемся загрузить из временной директории (приоритет)
            return Native.load("SDL2_mixer", SDL2Mixer.class);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Не удалось загрузить SDL2_mixer из временной директории: " + e.getMessage());
            
            // Если не получилось, пробуем загрузить из системных путей
            try {
                return SystemSDL2Loader.loadSDL2Mixer();
            } catch (Exception e2) {
                System.err.println("Не удалось загрузить SDL2_mixer из системных путей: " + e2.getMessage());
                throw new RuntimeException("Не удалось загрузить библиотеку SDL2_mixer. Проверьте установку SDL2_mixer в системе.", e2);
            }
        }
    }
    
    // Константы для форматов аудио - высокое качество (320 kbps эквивалент)
    int MIX_DEFAULT_FORMAT = 0x8010; // AUDIO_S16SYS (16-bit signed)
    int MIX_DEFAULT_FREQUENCY = 48000; // 48kHz для высокого качества
    int MIX_DEFAULT_CHANNELS = 2; // Стерео
    
    // Дополнительные форматы для высокого качества
    int AUDIO_U8 = 0x0008; // 8-bit unsigned
    int AUDIO_S8 = 0x8008; // 8-bit signed
    int AUDIO_U16LSB = 0x0010; // 16-bit unsigned little-endian
    int AUDIO_S16LSB = 0x8010; // 16-bit signed little-endian
    int AUDIO_U16MSB = 0x1010; // 16-bit unsigned big-endian
    int AUDIO_S16MSB = 0x9010; // 16-bit signed big-endian
    int AUDIO_S32LSB = 0x8020; // 32-bit signed little-endian
    int AUDIO_S32MSB = 0x9020; // 32-bit signed big-endian
    int AUDIO_F32LSB = 0x8120; // 32-bit float little-endian
    int AUDIO_F32MSB = 0x9120; // 32-bit float big-endian
    
    // Константы для каналов
    int MIX_CHANNEL_POST = -2;
    int MIX_CHANNEL_FREE = -1;
    
    // Константы для эффектов
    int MIX_EFFECTSMAXSPEED = 128;
    
    /**
     * Инициализация SDL2_mixer
     * @param flags флаги инициализации (обычно 0)
     * @return 0 при успехе, -1 при ошибке
     */
    int Mix_Init(int flags);
    
    /**
     * Открытие аудио устройства
     * @param frequency частота дискретизации
     * @param format формат аудио
     * @param channels количество каналов
     * @param chunksize размер чанка
     * @return 0 при успехе, -1 при ошибке
     */
    int Mix_OpenAudio(int frequency, int format, int channels, int chunksize);
    
    /**
     * Открытие аудио устройства с указанием количества аудио драйверов
     * @param frequency частота дискретизации
     * @param format формат аудио
     * @param channels количество каналов
     * @param chunksize размер чанка
     * @param numdrivers количество драйверов
     * @return 0 при успехе, -1 при ошибке
     */
    int Mix_OpenAudioDevice(int frequency, int format, int channels, 
                           int chunksize, int numdrivers);
    
    /**
     * Закрытие аудио устройства
     */
    void Mix_CloseAudio();
    
    /**
     * Загрузка музыки из файла
     * @param file путь к файлу
     * @return указатель на загруженную музыку или null при ошибке
     */
    Pointer Mix_LoadMUS(String file);
    
    /**
     * Загрузка звукового эффекта из файла
     * @param file путь к файлу
     * @return указатель на загруженный звук или null при ошибке
     */
    Pointer Mix_LoadWAV(String file);
    
    /**
     * Загрузка звукового эффекта из памяти
     * @param mem указатель на данные в памяти
     * @param freesrc освободить источник после загрузки (1) или нет (0)
     * @return указатель на загруженный звук или null при ошибке
     */
    Pointer Mix_LoadWAV_RW(Pointer mem, int freesrc);
    
    /**
     * Воспроизведение музыки
     * @param music указатель на музыку
     * @param loops количество повторов (-1 для бесконечного воспроизведения)
     * @return 0 при успехе, -1 при ошибке
     */
    int Mix_PlayMusic(Pointer music, int loops);
    
    /**
     * Воспроизведение звукового эффекта
     * @param chunk указатель на звук
     * @param channel канал для воспроизведения (-1 для любого доступного)
     * @param loops количество повторов (0 = один раз)
     * @return канал, на котором воспроизводится звук, или -1 при ошибке
     */
    int Mix_PlayChannel(int channel, Pointer chunk, int loops);
    
    /**
     * Воспроизведение звукового эффекта с таймером
     * @param channel канал для воспроизведения (-1 для любого доступного)
     * @param chunk указатель на звук
     * @param loops количество повторов
     * @param ticks длительность в миллисекундах (-1 для полного воспроизведения)
     * @return канал, на котором воспроизводится звук, или -1 при ошибке
     */
    int Mix_PlayChannelTimed(int channel, Pointer chunk, int loops, int ticks);
    
    /**
     * Пауза музыки
     */
    void Mix_PauseMusic();
    
    /**
     * Возобновление музыки
     */
    void Mix_ResumeMusic();
    
    /**
     * Остановка музыки
     */
    void Mix_HaltMusic();
    
    /**
     * Остановка всех звуков на канале
     * @param channel канал для остановки (-1 для всех каналов)
     */
    int Mix_HaltChannel(int channel);
    
    /**
     * Остановка всех звуков
     */
    int Mix_HaltGroup(int tag);
    
    /**
     * Освобождение музыки
     * @param music указатель на музыку
     */
    void Mix_FreeMusic(Pointer music);
    
    /**
     * Освобождение звукового эффекта
     * @param chunk указатель на звук
     */
    void Mix_FreeChunk(Pointer chunk);
    
    /**
     * Установка громкости музыки
     * @param volume громкость от 0 до 128
     * @return текущая громкость
     */
    int Mix_VolumeMusic(int volume);
    
    /**
     * Установка громкости звукового эффекта
     * @param chunk указатель на звук
     * @param volume громкость от 0 до 128
     * @return текущая громкость
     */
    int Mix_VolumeChunk(Pointer chunk, int volume);
    
    /**
     * Установка громкости канала
     * @param channel канал
     * @param volume громкость от 0 до 128
     * @return текущая громкость
     */
    int Mix_Volume(int channel, int volume);
    
    /**
     * Проверка, играет ли музыка
     * @return 1 если играет, 0 если нет
     */
    int Mix_PlayingMusic();
    
    /**
     * Проверка, приостановлена ли музыка
     * @return 1 если приостановлена, 0 если нет
     */
    int Mix_PausedMusic();
    
    /**
     * Проверка, играет ли звук на канале
     * @param channel канал
     * @return 1 если играет, 0 если нет
     */
    int Mix_Playing(int channel);
    
    /**
     * Проверка, приостановлен ли звук на канале
     * @param channel канал
     * @return 1 если приостановлен, 0 если нет
     */
    int Mix_Paused(int channel);
    
    /**
     * Получение количества доступных каналов
     * @return количество каналов
     */
    int Mix_AllocateChannels(int numchans);
    
    /**
     * Получение количества каналов
     * @return количество каналов
     */
    int Mix_QuerySpec(IntByReference frequency, IntByReference format, IntByReference channels);
    
    /**
     * Очистка всех загруженных ресурсов
     */
    void Mix_Quit();
    
    // Примечание: Mix_GetError не существует в SDL2_mixer
    // Используйте SDL_GetError из основной библиотеки SDL2
    
    /**
     * Установка callback функции для завершения музыки
     * @param music_finished callback функция
     * @param arg аргумент для callback функции
     */
    void Mix_HookMusicFinished(Pointer music_finished, Pointer arg);
    
    /**
     * Установка callback функции для завершения звукового эффекта
     * @param channel_finished callback функция
     * @param arg аргумент для callback функции
     */
    void Mix_ChannelFinished(Pointer channel_finished, Pointer arg);
    
    /**
     * Получение информации о музыке
     * @param music указатель на музыку
     * @param type тип музыки (возвращается)
     * @return 1 при успехе, 0 при ошибке
     */
    int Mix_GetMusicType(Pointer music);
    
    /**
     * Установка позиции в музыке
     * @param music указатель на музыку
     * @param position позиция в секундах
     * @return 0 при успехе, -1 при ошибке
     */
    int Mix_SetMusicPosition(double position);
    
    /**
     * Получение позиции в музыке
     * @param music указатель на музыку
     * @return позиция в секундах
     */
    double Mix_GetMusicPosition(Pointer music);
    
    /**
     * Получение длины музыки
     * @param music указатель на музыку
     * @return длина в секундах
     */
    double Mix_MusicDuration(Pointer music);
}
