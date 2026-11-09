package com.minecraft_terraria.mod;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * Музыкальный трек с управляемой громкостью — основа "террариевского" плавного перехода.
 *
 * Почему именно TickableSoundInstance:
 * ванильный SoundEngine читает getVolume() у ОБЫЧНОГО SoundInstance ровно один раз —
 * в момент play() — и дальше держит это значение на аудиоканале. То есть поменять
 * громкость уже играющего трека нельзя, отсюда и резкий обрыв.
 * А у TickableSoundInstance громкость пересчитывается КАЖДЫЙ тик, поэтому трек
 * можно плавно гасить и плавно наращивать.
 *
 * Схема работы:
 *   1. трек стартует с громкостью 0 (fadeFactor = 0) и мы вызываем fadeTo(1.0, N);
 *   2. каждый тик tick() подтягивает громкость к цели на fadeStep;
 *   3. когда трек нужно убрать, вызываем fadeTo(0.0, N) — он сам затихнет и пометится stopped.
 *
 * ВАЖНО (почему наследуемся от AbstractSoundInstance, а не от SimpleSoundInstance):
 * в 1.21.1 у SimpleSoundInstance конструктор, принимающий SoundEvent + looping + attenuation,
 * объявлен ПРИВАТНЫМ (публичный вариант принимает ResourceLocation, а не SoundEvent).
 * Наследник не может вызвать приватный конструктор суперкласса, поэтому берём
 * AbstractSoundInstance — его protected-конструктор как раз принимает SoundEvent,
 * а нужные поля (volume, looping, attenuation, relative) выставляем сами.
 */
public class FadeMusicSound extends AbstractSoundInstance implements TickableSoundInstance {

    /** Текущая громкость как множитель 0..1 (реальная громкость = базовая * этот множитель). */
    private float fadeFactor;
    /** К чему стремимся. */
    private float fadeTarget;
    /** Шаг изменения за один тик. */
    private float fadeStep;

    private boolean stopped;
    private boolean playing;
    private int age;

    /**
     * @param sound       звуковое событие трека
     * @param startVolume стартовая громкость 0..1
     * @param looping     true — трек зацикливается бесконечно (тема биома), false — играет один раз (трек меню)
     */
    public FadeMusicSound(SoundEvent sound, float startVolume, boolean looping) {
        super(sound, SoundSource.MUSIC, SoundInstance.createUnseededRandom());

        this.volume = 1.0F;                     // базовая громкость: фейд считаем множителем к ней
        this.pitch = 1.0F;
        this.looping = looping;
        this.delay = 0;
        this.attenuation = SoundInstance.Attenuation.NONE; // музыка не затухает по расстоянию
        this.relative = true;                   // играет "в голове" игрока

        this.fadeFactor = Mth.clamp(startVolume, 0.0F, 1.0F);
        this.fadeTarget = this.fadeFactor;
    }

    /** Плавно вести громкость к target за ticks тиков. */
    public void fadeTo(float target, int ticks) {
        this.fadeTarget = Mth.clamp(target, 0.0F, 1.0F);
        this.fadeStep = ticks <= 0 ? 1.0F : 1.0F / (float) ticks;
    }

    /** Поставить громкость мгновенно (без фейда). */
    public void setVolumeInstant(float volume) {
        this.fadeFactor = Mth.clamp(volume, 0.0F, 1.0F);
        this.fadeTarget = this.fadeFactor;
    }

    public int getAge() {
        return this.age;
    }

    /** Трек реально звучал хотя бы один тик (защита от бесконечных перезапусков битого звука). */
    public void markPlaying() {
        this.playing = true;
    }

    public boolean wasPlaying() {
        return this.playing;
    }

    /** Реальная громкость: базовое значение трека, умноженное на текущий коэффициент фейда. */
    @Override
    public float getVolume() {
        return super.getVolume() * this.fadeFactor;
    }

    @Override
    public void tick() {
        this.age++;

        if (this.fadeFactor < this.fadeTarget) {
            this.fadeFactor = Math.min(this.fadeTarget, this.fadeFactor + this.fadeStep);
        } else if (this.fadeFactor > this.fadeTarget) {
            this.fadeFactor = Math.max(this.fadeTarget, this.fadeFactor - this.fadeStep);
        }

        // Полностью затихли — говорим движку, что трек можно выбрасывать
        if (this.fadeTarget <= 0.0F && this.fadeFactor <= 0.0F) {
            this.stopped = true;
        }
    }

    @Override
    public boolean isStopped() {
        return this.stopped;
    }

    /**
     * ВАЖНО: без этого SoundEngine отбросит трек, который стартует с нулевой громкости
     * ("Skipped playing sound ..., volume was zero") — и плавного нарастания просто не будет.
     */
    @Override
    public boolean canStartSilent() {
        return true;
    }
}
