package com.petitioner0.divinecore.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.LightTexture;

public class StormFunnelParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;

    private final double centerX;
    private final double centerYBase;
    private final double centerZ;

    private final double radialOffset;
    private double theta;

    public StormFunnelParticle(
            ClientLevel level,
            double cx, double cy, double cz,
            double dx, double dy, double dz,
            SpriteSet spriteSet
    ) {
        // 位置参数用作初始中心点，速度参数你自己看需不需要
        super(level, cx, cy, cz, dx, dy, dz);
        this.spriteSet = spriteSet;

        this.centerX = cx;
        this.centerYBase = cy;
        this.centerZ = cz;

        RandomSource random = level.random;

        double yRel = random.nextDouble() * 150.0;
        this.y = this.centerYBase + yRel;

        double t = Math.pow(random.nextDouble(), 1.5);
        this.radialOffset = Mth.lerp(t, -0.2, 1.5);

        this.theta = random.nextDouble() * Math.PI * 2.0;

        double rInit = radiusForHeight(yRel) + this.radialOffset;
        this.x = this.centerX + rInit * Math.cos(theta);
        this.z = this.centerZ + rInit * Math.sin(theta);

        this.lifetime = 200 + random.nextInt(40);
        this.quadSize *= 2F;

        if (random.nextBoolean()) {
            this.setColor(1.0F, 0.85F, 0.2F);
        } else {
            this.setColor(0.05F, 0.05F, 0.08F);
        }

        this.gravity = 0.0F;

        // ✅ 一定要在构造时先设一次 sprite，防止渲染时还没贴图
        this.setSpriteFromAge(this.spriteSet);
    }

    private static double radiusForHeight(double yRel) {
        double t = Mth.clamp(yRel / 150.0, 0.0, 1.0);
        double k = 3.0;
        double exp = Math.exp(k * t);

        double expMin = 1.0;
        double expMax = Math.exp(k);

        return Mth.lerp(
                (exp - expMin) / (expMax - expMin),
                1.0,
                130.0
        );
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // ✅ 每 tick 更新一次 sprite（如果是多帧动画）
        this.setSpriteFromAge(this.spriteSet);

        double yRel = this.y - this.centerYBase;
        yRel = Mth.clamp(yRel, 0.0, 150.0);

        double heightFactor = 1.0 - (yRel / 150.0);
        double k = 0.25 * heightFactor;

        double rWanted = radiusForHeight(yRel) + this.radialOffset;

        double dx = this.x - this.centerX;
        double dz = this.z - this.centerZ;
        double r = Math.sqrt(dx * dx + dz * dz);
        if (r < 1e-5) r = 1e-5;

        double r0 = radiusForHeight(yRel);
        double vTheta = (r < r0) ? k * r : k * r0 * r0 / r;

        double maxV = 0.65;
        if (vTheta > maxV) vTheta = maxV;

        this.theta += vTheta / r;
        r += (rWanted - r) * 0.05;

        this.x = this.centerX + r * Math.cos(this.theta);
        this.z = this.centerZ + r * Math.sin(this.theta);

        this.y += (this.random.nextDouble() - 0.5) * 0.01;

        float lifeRatio = (float) this.age / (float) this.lifetime;
        this.alpha = 1.0F - lifeRatio;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    // =============== Provider（配合 registerSpriteSet 的正式写法）================

    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type,
                                       ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            // 这里用 Level 传进来的坐标作为粒子初始位置
            return new StormFunnelParticle(
                    level,
                    x, y, z,
                    xSpeed, ySpeed, zSpeed,
                    this.sprites
            );
        }
    }
}
