package com.omnimine.client;

import com.omnimine.net.VfxPacket;
import net.minecraft.world.phys.Vec3;

/**
 * Effets du minage : fissures qui progressent, absorption, trou dans le ciel, dissolution d'une structure,
 * eclair gele et sa destruction, ceremonie des structures evoluees.
 */
public final class FxMining {

    // =====================================================================================================
    // Fissures de minage (mises a jour sur place a chaque nouveau paquet : l'animation ne saute pas)
    // =====================================================================================================

    public static final class MineCrack extends Fx {
        private Vec3 center;
        private float size;
        private int kind;
        private float progress;
        private float prev;
        private int updateAge;

        public MineCrack(VfxPacket p) {
            super(p);
            read(p);
            prev = progress;
        }

        private void read(VfxPacket p) {
            center = new Vec3(p.x, p.y, p.z);
            size = (float) Math.max(0.5D, p.x2);
            kind = (int) Math.round(p.y2);
            progress = Rnd.clamp01(p.param);
        }

        /** Nouveau paquet pour la meme cible : on glisse vers la nouvelle progression. */
        public void refresh(VfxPacket p) {
            float cur = shown(age);
            read(p);
            prev = cur;
            updateAge = age;
            duration = Math.max(duration, age + Math.max(2, p.duration));
        }

        private float shown(float t) {
            return prev + (progress - prev) * Rnd.clamp01((t - updateAge) / 2.0F);
        }

        private boolean sky() {
            return kind == 1 || kind == 2;
        }

        /** Une fissure : ligne brisee qui part du centre dans le plan de l'ecran. */
        private Vec3[] arm(int k, float g, double radius, Vec3 c, Vec3 ex, Vec3 ey) {
            double ang = k * 2.399963D + Rnd.s(seed, k) * 0.35D;
            Vec3 dir = ex.scale(Math.cos(ang)).add(ey.scale(Math.sin(ang)));
            Vec3 perp = ex.scale(-Math.sin(ang)).add(ey.scale(Math.cos(ang)));
            double len = radius * (0.55D + 0.6D * Rnd.f(seed, 100 + k)) * g;
            int n = 8;
            Vec3[] pts = new Vec3[n + 1];
            double wob = 0.0D;
            for (int j = 0; j <= n; j++) {
                double f = j / (double) n;
                wob = wob * 0.75D + Rnd.s(seed, 200 + k * 16 + j) * 0.11D * radius;
                pts[j] = c.add(dir.scale(radius * 0.04D + len * f)).add(perp.scale(wob * Math.min(1.0D, f * 3.0D)));
            }
            return pts;
        }

        /** Une sous-fissure qui part du milieu d'une fissure principale. */
        private Vec3[] branch(int k, float g, double radius, Vec3[] main, Vec3 ex, Vec3 ey) {
            int from = 3 + (k % 3);
            double side = Rnd.f(seed, 300 + k) < 0.5F ? 1.0D : -1.0D;
            double rot = side * (0.6D + 0.5D * Rnd.f(seed, 320 + k));
            Vec3 m = main[main.length - 1].subtract(main[0]);
            double mx = m.dot(ex);
            double my = m.dot(ey);
            double rx = mx * Math.cos(rot) - my * Math.sin(rot);
            double ry = mx * Math.sin(rot) + my * Math.cos(rot);
            Vec3 dir = ex.scale(rx).add(ey.scale(ry)).normalize();
            Vec3 perp = ex.scale(-ry).add(ey.scale(rx)).normalize();
            Vec3[] pts = new Vec3[5];
            double wob = 0.0D;
            for (int j = 0; j < 5; j++) {
                double f = j / 4.0D;
                wob = wob * 0.7D + Rnd.s(seed, 340 + k * 8 + j) * 0.09D * radius;
                pts[j] = main[from].add(dir.scale(radius * 0.5D * g * f)).add(perp.scale(wob * f));
            }
            return pts;
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            float pr = shown(t);
            Vec3 c;
            double radius;
            if (sky()) {
                Vec3 dir = kind == 1 ? FxUtil.sunDir(partial) : FxUtil.moonDir(partial);
                double dist = FxUtil.skyDistance();
                c = d.cam.add(dir.scale(dist));
                radius = dist * size * 0.5D / 90.0D;
            } else {
                c = center;
                radius = Math.max(0.45D, size * 0.55D);
            }
            float fade = 1.0F - Rnd.ramp(t, duration - 3.0F, duration);
            int arms = 5 + (int) (pr * 9.0F);
            Vec3 ex = d.left;
            Vec3 ey = d.up;

            Vec3[][] a = new Vec3[arms][];
            Vec3[][] br = new Vec3[arms][];
            for (int k = 0; k < arms; k++) {
                float st = k * 0.045F;
                float g = easeOut(Rnd.ramp(pr, st, st + 0.28F));
                if (g > 0.0F) {
                    a[k] = arm(k, g, radius, c, ex, ey);
                    float bs = 0.3F + k * 0.05F;
                    float bg = Rnd.ramp(pr, bs, bs + 0.2F);
                    if (k < 8 && bg > 0.0F) {
                        br[k] = branch(k, bg, radius, a[k], ex, ey);
                    }
                }
            }

            // 1. fissures sombres
            d.type(VfxRenderTypes.LINE_DARK);
            double w0 = Math.max(0.03D, radius * 0.07D);
            for (int k = 0; k < arms; k++) {
                strokes(d, a[k], w0, 0x000000, 0.9F * fade, 1.0F);
                strokes(d, br[k], w0 * 0.7D, 0x000000, 0.85F * fade, 1.0F);
            }

            // 2. lumiere qui sort des fissures
            d.type(VfxRenderTypes.LINE_ADD);
            float seam = (0.25F + 0.75F * pr) * fade;
            for (int k = 0; k < arms; k++) {
                strokes(d, a[k], w0 * 0.55D, color, seam, 1.0F);
                strokes(d, br[k], w0 * 0.4D, color, seam * 0.9F, 1.0F);
                if (pr > 0.6F) {
                    strokes(d, a[k], w0 * 0.2D, 0xFFFFFF, (pr - 0.6F) * 2.2F * fade, 1.0F);
                }
            }

            // 3. halo au centre
            d.type(VfxRenderTypes.GLOW_ADD);
            float breathe = 0.8F + 0.2F * (float) Math.sin(t * 0.9F);
            d.glow(c, (float) (radius * (0.8D + 1.4D * pr)), color, (0.08F + 0.5F * pr * pr) * breathe * fade);
            d.glow(c, (float) (radius * 0.5D * pr), 0xFFFFFF, 0.45F * pr * pr * fade);

            // 4. etincelles et eclats
            d.type(VfxRenderTypes.STAR_ADD);
            int n = 6 + (int) (pr * 22.0F);
            boolean sky = sky();
            for (int i = 0; i < n; i++) {
                float ph = fract(t * 0.05F + Rnd.f(seed, 500 + i));
                Vec3 dir = sphere(seed, 600 + i);
                Vec3 p = c.add(dir.scale(radius * (0.25D + 1.4D * ph)));
                if (!sky) {
                    p = p.add(0.0D, -radius * 1.3D * ph * ph, 0.0D);
                }
                float al = (1.0F - ph) * (0.25F + 0.75F * pr) * fade;
                d.glow(p, (float) (radius * 0.22D * (1.0F - ph)) + 0.03F, t * 0.2F + i, i % 3 == 0 ? 0xFFFFFF : color,
                        al);
            }
        }

        private static void strokes(VfxDraw d, Vec3[] pts, double width, int rgb, float alpha, float taper) {
            if (pts == null || alpha <= 0.0F) {
                return;
            }
            int n = pts.length - 1;
            for (int j = 0; j < n; j++) {
                float wa = (float) (width * (1.0D - 0.75D * j / n * taper));
                float wb = (float) (width * (1.0D - 0.75D * (j + 1) / n * taper));
                d.ribbon(pts[j], pts[j + 1], wa, wb, rgb, alpha, alpha);
            }
        }

        @Override
        public float shake(Vec3 cam, float t) {
            float pr = shown(t);
            float s = 0.03F + 0.30F * pr * pr;
            return sky() ? s * 0.5F : s * falloff(cam, center, 24.0F);
        }
    }

    // =====================================================================================================
    // Absorption : la chose minee est aspiree vers le joueur
    // =====================================================================================================

    public static final class Absorb extends Fx {
        public Absorb(VfxPacket p) {
            super(p);
        }

        /** Point de la trajectoire en spirale entre a et b (s de 0 a 1). */
        private Vec3 head(Vec3 a, Vec3 b, float s, double swirl, int salt) {
            Vec3 axis = b.subtract(a);
            Vec3[] bs = FxUtil.basis(axis.lengthSqr() < 1.0E-6D ? new Vec3(0, 1, 0) : axis);
            double ph = s * 9.0D + Rnd.f(seed, salt) * 6.28D;
            double amp = swirl * Math.sin(Math.PI * Math.min(1.0F, s)) * (1.0D - s);
            return a.add(axis.scale(s)).add(bs[0].scale(Math.cos(ph) * amp)).add(bs[1].scale(Math.sin(ph) * amp));
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            Vec3 end = pos(partial).add(0.0D, 1.0D, 0.0D);
            Vec3 start = origin;
            float flight = duration * 0.72F;
            float x = Rnd.clamp01(t / flight);
            float s = x * x;
            float size = Math.min(param, 36.0F);
            float hs = 0.45F + size * 0.9F * (1.0F - s) * (1.0F - s);
            double swirl = 1.2D + Math.min(size, 20.0F) * 0.12D;

            if (t < flight) {
                Vec3 h = head(start, end, s, swirl, 1);
                // queue lumineuse
                d.type(VfxRenderTypes.LINE_ADD);
                Vec3 prevPt = h;
                for (int j = 1; j <= 26; j++) {
                    float sj = s - j * 0.014F * (0.6F + x);
                    if (sj < 0.0F) {
                        break;
                    }
                    Vec3 pj = head(start, end, sj, swirl, 1);
                    float f = 1.0F - j / 27.0F;
                    d.ribbon(prevPt, pj, hs * 0.55F * f, hs * 0.55F * (f - 0.04F), color, 0.55F * f, 0.5F * f);
                    prevPt = pj;
                }
                d.type(VfxRenderTypes.GLOW_ADD);
                d.glow(h, hs * 1.4F, color, 0.5F);
                d.glow(h, hs * 0.6F, 0xFFFFFF, 0.9F);
                for (int i = 0; i < 18; i++) {
                    double ang = t * 0.35D + i * 0.35D;
                    double rr = hs * (0.5D + 0.4D * Rnd.f(seed, 20 + i));
                    Vec3[] bs = FxUtil.basis(end.subtract(start));
                    Vec3 m = h.add(bs[0].scale(Math.cos(ang + i) * rr)).add(bs[1].scale(Math.sin(ang + i) * rr));
                    d.glow(m, 0.35F + hs * 0.05F, color, 0.7F);
                }
                d.type(VfxRenderTypes.STAR_ADD);
                d.glow(h, hs * 0.9F, t * 0.3F, 0xFFFFFF, 0.9F);
            }

            // eclat quand la chose entre dans le joueur
            float u = Rnd.clamp01((t - flight) / Math.max(1.0F, duration - flight));
            if (t >= flight && u < 1.0F) {
                float a = 1.0F - u;
                d.type(VfxRenderTypes.LINE_ADD);
                d.ring(end.add(0, -0.9D, 0), AXIS_X, AXIS_Z, 0.4D + 3.2D * easeOut(u), 0.5D * a + 0.1D, color, a * 0.9F, 40);
                d.ring(end, d.left, d.up, 0.3D + 1.8D * easeOut(u), 0.25D * a + 0.05D, 0xFFFFFF, a, 40);
                for (int i = 0; i < 16; i++) {
                    Vec3 dir = sphere(seed, 50 + i);
                    Vec3 p0 = end.add(dir.scale(0.4D + 1.2D * easeOut(u)));
                    Vec3 p1 = end.add(dir.scale(0.9D + 3.4D * easeOut(u)));
                    d.ribbon(p0, p1, 0.0F, 0.11F, color, 0.0F, a);
                }
                d.type(VfxRenderTypes.GLOW_ADD);
                d.glow(end, 3.5F * a, color, 0.8F * a);
                d.glow(end, 1.5F * a, 0xFFFFFF, a);
            }
        }

        @Override
        public float flash(Vec3 cam, float t) {
            float flight = duration * 0.72F;
            return 0.3F * (1.0F - Rnd.ramp(t, flight, flight + 6.0F)) * Rnd.ramp(t, flight - 2.0F, flight)
                    * falloff(cam, pos(0.0F), 5.0F);
        }

        @Override
        public int flashColor() {
            return FxUtil.mix(color, 0xFFFFFF, 0.5F);
        }
    }

    // =====================================================================================================
    // Trou dans le ciel : le soleil ou la lune ont ete mines
    // =====================================================================================================

    private static final double SUN_HOLE_ANGLE = Math.toRadians(13.0D);
    private static final double MOON_HOLE_ANGLE = Math.toRadians(10.5D);

    public static final class SkyHole extends Fx {
        private final int which;
        private Vec3 fixed;

        public SkyHole(VfxPacket p) {
            super(p);
            this.which = p.param >= 0.5F ? 1 : 0;
            this.fixed = fixedOf(p);
        }

        private static Vec3 fixedOf(VfxPacket p) {
            Vec3 v = new Vec3(p.x2, p.y2, p.z2);
            return v.lengthSqr() > 0.25D ? v.normalize() : null;
        }

        public int which() {
            return which;
        }

        /** Un nouveau paquet pour le meme trou : on le prolonge, l'animation ne repart pas. */
        public void refresh(VfxPacket p) {
            fixed = fixedOf(p);
            duration = Math.max(duration, age + Math.max(2, p.duration));
        }

        private Vec3 dir(float partial) {
            if (fixed != null) {
                return fixed;
            }
            return which == 0 ? FxUtil.sunDir(partial) : FxUtil.moonDir(partial);
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            Vec3 dir = dir(partial);
            if (dir.y < -0.55D) {
                return;
            }
            double dist = FxUtil.skyDistance();
            double radius = dist * Math.tan(which == 0 ? SUN_HOLE_ANGLE : MOON_HOLE_ANGLE);
            float open = easeOut(Rnd.ramp(t, 0.0F, 26.0F));
            float fade = 1.0F - Rnd.ramp(t, duration - 8.0F, duration);
            Vec3 c = d.cam.add(dir.scale(dist));
            Vec3[] b = FxUtil.basis(dir);
            int rim = which == 0 ? 0xFF8F1F : 0xBBD6FF;
            int core = which == 0 ? 0xFFE2A8 : 0xFFFFFF;
            float pulse = 0.78F + 0.22F * (float) Math.sin(t * 0.13F);
            double r0 = radius * open;
            int sd = 7000 + which * 131;
            if (r0 < 1.0D) {
                return;
            }

            // le vide
            d.type(VfxRenderTypes.GLOW_DARK);
            d.glow(c, (float) (r0 * 3.4D), 0x000000, 0.5F * fade);
            d.type(VfxRenderTypes.DISC_DARK);
            d.glow(c, (float) (r0 * 2.0D), 0x000000, 0.98F * fade);

            // lueur de couronne
            d.type(VfxRenderTypes.GLOW_ADD);
            d.glow(c, (float) (r0 * 2.7D), rim, 0.2F * pulse * fade);
            d.glow(c, (float) (r0 * 5.5D), rim, 0.06F * fade);

            d.type(VfxRenderTypes.LINE_ADD);
            // anneau de bord
            d.ring(c, b[0], b[1], r0, radius * 0.11D * (0.85D + 0.15D * Math.sin(t * 0.2D)), rim, 0.95F * pulse * fade, 96);
            d.ring(c, b[0], b[1], r0 * 1.004D, radius * 0.035D, core, 0.95F * fade, 96);

            // fissures qui partent du bord et zebrent le ciel
            int arms = 18;
            for (int k = 0; k < arms; k++) {
                double a = k * (Math.PI * 2.0D / arms) + Rnd.s(sd, k) * 0.12D;
                float grow = easeOut(Rnd.ramp(t, 3.0F + k * 0.5F, 22.0F + k * 0.5F));
                if (grow <= 0.0F) {
                    continue;
                }
                double len = radius * (0.7D + 2.6D * Rnd.f(sd, 40 + k)) * grow;
                Vec3 dv = b[0].scale(Math.cos(a)).add(b[1].scale(Math.sin(a)));
                Vec3 pv = b[0].scale(-Math.sin(a)).add(b[1].scale(Math.cos(a)));
                float flick = 0.55F + 0.45F * (float) Math.sin(t * 0.35F + k * 1.7F);
                Vec3 prev = c.add(dv.scale(r0 * 0.99D));
                double wob = 0.0D;
                for (int j = 1; j <= 8; j++) {
                    double f = j / 8.0D;
                    wob = wob * 0.7D + Rnd.s(sd + k, j) * 0.09D * radius;
                    Vec3 nxt = c.add(dv.scale(r0 * 0.99D + len * f)).add(pv.scale(wob * Math.min(1.0D, f * 2.0D)));
                    float w0 = (float) (radius * 0.05D * (1.0D - (j - 1) / 8.0D));
                    float w1 = (float) (radius * 0.05D * (1.0D - j / 8.0D));
                    float al = 0.9F * flick * fade;
                    d.ribbon(prev, nxt, w0, w1, rim, al, al * (float) (1.0D - f * 0.7D));
                    d.ribbon(prev, nxt, w0 * 0.4F, w1 * 0.4F, core, al, al * (float) (1.0D - f * 0.7D));
                    prev = nxt;
                }
            }
            // rayons de couronne (comme une eclipse)
            for (int k = 0; k < 28; k++) {
                double a = Rnd.f(sd, 80 + k) * Math.PI * 2.0D;
                Vec3 dv = b[0].scale(Math.cos(a)).add(b[1].scale(Math.sin(a)));
                double l0 = r0 * 1.05D;
                double l1 = r0 * (1.15D + 0.9D * Rnd.f(sd, 120 + k));
                float al = 0.3F * pulse * fade * (0.6F + 0.4F * (float) Math.sin(t * 0.2F + k));
                d.ribbon(c.add(dv.scale(l0)), c.add(dv.scale(l1)), (float) (radius * 0.05D), 0.0F, rim, al, 0.0F);
            }

            // braises qui s'echappent du bord
            d.type(VfxRenderTypes.STAR_ADD);
            for (int i = 0; i < 36; i++) {
                float ph = fract(t * 0.012F + Rnd.f(sd, 200 + i));
                double ang = Rnd.f(sd, 240 + i) * Math.PI * 2.0D + ph * 0.5D;
                double rr = r0 * (1.05D + 2.8D * ph);
                Vec3 p = c.add(b[0].scale(Math.cos(ang) * rr)).add(b[1].scale(Math.sin(ang) * rr));
                d.glow(p, (float) (radius * 0.07D * (1.0F - ph)), t * 0.1F + i, i % 3 == 0 ? 0xFFFFFF : rim,
                        (1.0F - ph) * 0.9F * fade);
            }
        }

        @Override
        public float shake(Vec3 cam, float t) {
            return 0.55F * (1.0F - Rnd.ramp(t, 0.0F, 36.0F));
        }

        @Override
        public float flash(Vec3 cam, float t) {
            return 0.4F * (1.0F - Rnd.ramp(t, 0.0F, 9.0F));
        }

        @Override
        public int flashColor() {
            return which == 0 ? 0xFFC070 : 0xDDEBFF;
        }
    }

    /** Le ciel se referme : le disque noir vole en eclats et l'astre revient dans une explosion de lumiere. */
    public static final class SkyReturn extends Fx {
        private final int which;
        private final Vec3 fixed;

        public SkyReturn(VfxPacket p) {
            super(p);
            this.which = p.param >= 0.5F ? 1 : 0;
            Vec3 v = new Vec3(p.x2, p.y2, p.z2);
            this.fixed = v.lengthSqr() > 0.25D ? v.normalize() : null;
        }

        public int which() {
            return which;
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            Vec3 dir = fixed != null ? fixed : (which == 0 ? FxUtil.sunDir(partial) : FxUtil.moonDir(partial));
            double dist = FxUtil.skyDistance();
            double radius = dist * Math.tan(which == 0 ? SUN_HOLE_ANGLE : MOON_HOLE_ANGLE);
            Vec3 c = d.cam.add(dir.scale(dist));
            Vec3[] b = FxUtil.basis(dir);
            int rim = which == 0 ? 0xFFB040 : 0xBBD6FF;
            int core = which == 0 ? 0xFFF0C8 : 0xFFFFFF;
            int sd = 7300 + which * 97;
            float shrink = 1.0F - easeOut(Rnd.ramp(t, 0.0F, 16.0F));
            float boom = easeOut(Rnd.ramp(t, 4.0F, 34.0F));
            float fadeOut = 1.0F - Rnd.ramp(t, 40.0F, duration);

            if (shrink > 0.01F) {
                d.type(VfxRenderTypes.DISC_DARK);
                d.glow(c, (float) (radius * 2.0D * shrink), 0x000000, 0.98F);
            }

            // eclats sombres du disque brise
            d.type(VfxRenderTypes.LINE_DARK);
            for (int i = 0; i < 60; i++) {
                float life = Rnd.ramp(t, 0.0F, 26.0F);
                if (life >= 1.0F) {
                    break;
                }
                double a = Rnd.f(sd, i) * Math.PI * 2.0D;
                Vec3 dv = b[0].scale(Math.cos(a)).add(b[1].scale(Math.sin(a)));
                double r = radius * (0.9D + 3.2D * easeOut(life) * (0.4D + 0.6D * Rnd.f(sd, 70 + i)));
                Vec3 p0 = c.add(dv.scale(r));
                Vec3 p1 = c.add(dv.scale(r + radius * 0.22D));
                d.ribbon(p0, p1, (float) (radius * 0.05D), (float) (radius * 0.03D), 0x000000, 0.9F * (1.0F - life),
                        0.9F * (1.0F - life));
            }

            d.type(VfxRenderTypes.LINE_ADD);
            // ondes de choc
            for (int k = 0; k < 3; k++) {
                float tk = t - k * 4.0F;
                if (tk < 0.0F) {
                    continue;
                }
                float al = (1.0F - Rnd.ramp(tk, 10.0F, 40.0F)) * 0.9F;
                d.ring(c, b[0], b[1], radius * (0.9D + 7.0D * easeOut(tk / 36.0F)), radius * (0.16D + 0.1D * k),
                        k == 0 ? core : rim, al, 96);
            }
            // rayons
            for (int k = 0; k < 30; k++) {
                double a = Rnd.f(sd, 150 + k) * Math.PI * 2.0D;
                Vec3 dv = b[0].scale(Math.cos(a)).add(b[1].scale(Math.sin(a)));
                double len = radius * (1.5D + 4.5D * Rnd.f(sd, 190 + k)) * easeOut(Rnd.ramp(t, 2.0F, 20.0F));
                float al = 0.7F * (1.0F - Rnd.ramp(t, 12.0F, 60.0F));
                d.ribbon(c.add(dv.scale(radius * 0.4D)), c.add(dv.scale(radius * 0.4D + len)),
                        (float) (radius * 0.09D), 0.0F, rim, al, 0.0F);
            }

            d.type(VfxRenderTypes.GLOW_ADD);
            d.glow(c, (float) (radius * (2.2D + 7.0D * boom)), rim, 0.85F * (1.0F - boom * 0.6F) * fadeOut);
            d.glow(c, (float) (radius * (1.2D + 2.5D * boom)), 0xFFFFFF, 0.9F * (1.0F - boom) * fadeOut);
            d.glow(c, (float) (radius * 4.0D), core, 0.25F * (1.0F - Rnd.ramp(t, 20.0F, 60.0F)));
        }

        @Override
        public float shake(Vec3 cam, float t) {
            return 0.4F * (1.0F - Rnd.ramp(t, 0.0F, 24.0F));
        }

        @Override
        public float flash(Vec3 cam, float t) {
            return 0.45F * (1.0F - Rnd.ramp(t, 0.0F, 10.0F));
        }

        @Override
        public int flashColor() {
            return which == 0 ? 0xFFEBC0 : 0xDDEEFF;
        }
    }

    // =====================================================================================================
    // Dissolution d'une structure
    // =====================================================================================================

    public static final class Dissolve extends Fx {
        private final double hx;
        private final double hy;
        private final double hz;

        public Dissolve(VfxPacket p) {
            super(p);
            this.hx = Math.max(2.0D, p.x2);
            this.hy = Math.max(3.0D, p.y2);
            this.hz = Math.max(2.0D, p.z2);
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            float u = Rnd.clamp01(t / duration);
            float fade = 1.0F - Rnd.ramp(t, duration - 10.0F, duration);
            Vec3 base = origin;
            int light = FxUtil.mix(color, 0xFFFFFF, 0.55F);

            d.type(VfxRenderTypes.LINE_ADD);
            // ondes de choc au sol
            for (int k = 0; k < 3; k++) {
                float tk = t - k * 7.0F;
                if (tk < 0.0F) {
                    continue;
                }
                double r = Math.max(hx, hz) * (0.2D + 1.1D * easeOut(tk / 50.0F));
                float al = (1.0F - Rnd.ramp(tk, 6.0F, 50.0F)) * 0.8F;
                d.ring(base.add(0, 0.2D, 0), AXIS_X, AXIS_Z, r, 1.6D + k * 0.6D, k == 0 ? light : color, al, 72);
            }
            // cadre au sol qui pulse
            float pulse = 0.5F + 0.5F * (float) Math.sin(t * 0.4F);
            Vec3 c00 = base.add(-hx, 0.3D, -hz);
            Vec3 c10 = base.add(hx, 0.3D, -hz);
            Vec3 c11 = base.add(hx, 0.3D, hz);
            Vec3 c01 = base.add(-hx, 0.3D, hz);
            float fa = (0.35F + 0.4F * pulse) * fade;
            d.ribbon(c00, c10, 0.9F, 0.9F, color, fa, fa);
            d.ribbon(c10, c11, 0.9F, 0.9F, color, fa, fa);
            d.ribbon(c11, c01, 0.9F, 0.9F, color, fa, fa);
            d.ribbon(c01, c00, 0.9F, 0.9F, color, fa, fa);

            // piliers de lumiere qui jaillissent au hasard
            for (int i = 0; i < 40; i++) {
                float tb = Rnd.f(seed, 2000 + i) * Math.max(8.0F, duration - 14.0F);
                float age2 = t - tb;
                if (age2 < 0.0F || age2 > 12.0F) {
                    continue;
                }
                float al = (1.0F - age2 / 12.0F) * fade;
                Vec3 p = base.add(Rnd.s(seed, 2100 + i) * hx, 0.0D, Rnd.s(seed, 2200 + i) * hz);
                double h = hy * (0.5D + 1.0D * Rnd.f(seed, 2300 + i)) * easeOut(age2 / 5.0F);
                d.pillar(p, h, 1.4F + 1.2F * Rnd.f(seed, 2400 + i), color, 0.7F * al, 0.0F);
                d.pillar(p, h * 0.9D, 0.5F, 0xFFFFFF, 0.5F * al, 0.0F);
            }

            // grande colonne finale au centre
            float fin = Rnd.ramp(u, 0.82F, 1.0F);
            if (fin > 0.0F) {
                d.pillar(base, hy * 2.5D, (float) (3.0D + Math.min(hx, hz) * 0.15D) * fin * (1.0F - fin * 0.6F), light,
                        0.8F * (1.0F - fin), 0.0F);
            }

            // particules de lumiere qui montent
            d.type(VfxRenderTypes.STAR_ADD);
            for (int i = 0; i < 160; i++) {
                float tb = Rnd.f(seed, i) * Math.max(8.0F, duration - 20.0F);
                float life = 22.0F + Rnd.f(seed, 300 + i) * 26.0F;
                float a2 = t - tb;
                if (a2 < 0.0F || a2 > life) {
                    continue;
                }
                float f = a2 / life;
                Vec3 p = base.add(Rnd.s(seed, 400 + i) * hx,
                        Rnd.f(seed, 500 + i) * hy + a2 * (0.1D + 0.2D * Rnd.f(seed, 600 + i)),
                        Rnd.s(seed, 700 + i) * hz);
                d.glow(p, 0.5F + 0.9F * Rnd.f(seed, 800 + i), t * 0.15F + i, i % 4 == 0 ? 0xFFFFFF : color,
                        (float) Math.sin(Math.PI * f) * fade);
            }
            d.type(VfxRenderTypes.GLOW_ADD);
            d.glow(base.add(0, hy * 0.5D, 0), (float) (Math.max(hx, hz) * 1.4D), color, 0.10F * fade);
        }

        @Override
        public float shake(Vec3 cam, float t) {
            return 0.45F * (1.0F - Rnd.ramp(t, 0.0F, duration)) * falloff(cam, origin, 90.0F);
        }
    }

    // =====================================================================================================
    // Eclair gele et sa destruction
    // =====================================================================================================

    /** Hauteur de l'eclair gele (identique au serveur). */
    private static final double BOLT_HEIGHT = 96.0D;

    /** Forme de l'eclair : le tronc (borne a +-1 bloc de l'axe) puis des branches. Meme forme pour le gele et l'eclat. */
    private static Vec3[][] boltShape(int seed, Vec3 base) {
        Vec3[] main = FxUtil.verticalBolt(seed, base, BOLT_HEIGHT, 1.7D, 0.9D);
        int branches = 9;
        Vec3[][] all = new Vec3[branches + 1][];
        all[0] = main;
        for (int b = 0; b < branches; b++) {
            int idx = 6 + (int) (Rnd.f(seed, 3000 + b) * (main.length - 14));
            Vec3 from = main[idx];
            double ang = Rnd.f(seed, 3100 + b) * Math.PI * 2.0D;
            Vec3 dir = new Vec3(Math.cos(ang), -0.25D - 0.35D * Rnd.f(seed, 3200 + b), Math.sin(ang)).normalize();
            double len = 6.0D + 12.0D * Rnd.f(seed, 3300 + b);
            int n = 5;
            Vec3[] pts = new Vec3[n + 1];
            pts[0] = from;
            for (int j = 1; j <= n; j++) {
                Vec3 jit = new Vec3(Rnd.s(seed, 3400 + b * 8 + j), Rnd.s(seed, 3500 + b * 8 + j) * 0.5D,
                        Rnd.s(seed, 3600 + b * 8 + j)).scale(len * 0.12D);
                pts[j] = from.add(dir.scale(len * j / n)).add(jit);
            }
            all[b + 1] = pts;
        }
        return all;
    }

    public static final class FrozenBolt extends Fx {
        private final Vec3[][] shape;

        public FrozenBolt(VfxPacket p) {
            super(p);
            this.shape = boltShape(p.seed, new Vec3(p.x, p.y, p.z));
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            float remaining = duration - t;
            float warn = 1.0F - Rnd.ramp(remaining, 0.0F, 50.0F);
            float intro = Rnd.ramp(t, 0.0F, 6.0F);
            float outro = Rnd.ramp(remaining, 0.0F, 2.0F);
            float flicker = 0.85F + 0.15F * (float) Math.sin(t * (0.9F + 2.5F * warn) + Rnd.f(seed, 3) * 6.0F);
            float blink = warn > 0.0F ? 0.65F + 0.35F * (float) Math.sin(t * 1.9F * (1.0F + warn)) : 1.0F;
            float a = intro * outro * flicker * blink;
            Vec3 base = origin;
            Vec3[] main = shape[0];

            d.type(VfxRenderTypes.LINE_ADD);
            d.strip(main, 0.3F, color, a);
            for (int b = 1; b < shape.length; b++) {
                d.strip(shape[b], 0.15F, color, a * 0.7F);
            }
            d.ring(base.add(0, 0.12D, 0), AXIS_X, AXIS_Z, 1.8D - 0.8D * warn + 0.2D * Math.sin(t * 0.3D), 0.5D, color,
                    0.8F * a, 40);
            d.ring(base.add(0, 0.14D, 0), AXIS_X, AXIS_Z, 3.2D - 1.4D * warn, 0.25D, 0xFFFFFF, 0.5F * a, 48);

            d.type(VfxRenderTypes.GLOW_ADD);
            d.glow(base.add(0, 0.6D, 0), 6.0F + 2.0F * warn, color, 0.5F * a);
            d.glow(base.add(0, 0.6D, 0), 2.4F, 0xFFFFFF, 0.8F * a);
            d.glow(main[main.length - 1], 14.0F, color, 0.35F * a);
            // etincelles qui grimpent le long de l'eclair
            for (int i = 0; i < 7; i++) {
                float s = fract(t * (0.012F + 0.004F * warn) + i / 7.0F);
                int idx = Math.min(main.length - 1, (int) (s * (main.length - 1)));
                d.glow(main[idx], 1.2F, color, 0.8F * a);
                d.glow(main[idx], 0.5F, 0xFFFFFF, a);
            }

            // eclats de glace-lumiere autour du tronc
            d.type(VfxRenderTypes.STAR_ADD);
            for (int i = 0; i < 44; i++) {
                int idx = 2 + (int) (Rnd.f(seed, 3700 + i) * (main.length - 4));
                Vec3 p = main[idx].add(Rnd.s(seed, 3800 + i) * 1.6D, Rnd.s(seed, 3900 + i) * 0.6D,
                        Rnd.s(seed, 4000 + i) * 1.6D);
                float tw = 0.5F + 0.5F * (float) Math.sin(t * 0.3F + i);
                d.glow(p, 0.45F + 0.5F * tw, t * 0.05F + i, 0xFFFFFF, 0.8F * tw * a);
            }
        }

        @Override
        public float shake(Vec3 cam, float t) {
            float remaining = duration - t;
            return 0.18F * (1.0F - Rnd.ramp(remaining, 0.0F, 30.0F)) * falloff(cam, origin, 60.0F);
        }
    }

    public static final class BoltShatter extends Fx {
        private final Vec3[][] shape;

        public BoltShatter(VfxPacket p) {
            super(p);
            this.shape = boltShape(p.seed, new Vec3(p.x, p.y, p.z));
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            Vec3 base = origin;
            Vec3[] main = shape[0];
            float flash = 1.0F - Rnd.ramp(t, 0.0F, 7.0F);

            d.type(VfxRenderTypes.LINE_ADD);
            if (flash > 0.0F) {
                d.strip(main, 0.5F + 0.4F * (1.0F - flash), 0xFFFFFF, flash);
                for (int b = 1; b < shape.length; b++) {
                    d.strip(shape[b], 0.22F, color, flash * 0.8F);
                }
            }
            // morceaux de l'eclair qui volent en eclats
            float ts = t / 20.0F;
            for (int i = 0; i < main.length - 1; i += 1) {
                float life = 0.55F + 0.4F * Rnd.f(seed, 10 + i);
                if (ts > life) {
                    continue;
                }
                float f = ts / life;
                Vec3 out = new Vec3(Rnd.s(seed, 100 + i), Rnd.s(seed, 200 + i) * 0.3D + 0.2D, Rnd.s(seed, 300 + i))
                        .normalize().scale((3.0D + 9.0D * Rnd.f(seed, 400 + i)) * ts);
                Vec3 g = new Vec3(0.0D, -6.0D * ts * ts, 0.0D);
                Vec3 p0 = main[i].add(out).add(g);
                Vec3 p1 = main[i + 1].add(out.scale(1.0D + 0.2D * Rnd.f(seed, 500 + i))).add(g);
                d.ribbon(p0, p1, 0.35F * (1.0F - f), 0.3F * (1.0F - f), i % 3 == 0 ? 0xFFFFFF : color,
                        0.9F * (1.0F - f), 0.9F * (1.0F - f));
            }
            // onde au sol
            for (int k = 0; k < 2; k++) {
                float tk = t - k * 3.0F;
                if (tk < 0.0F) {
                    continue;
                }
                d.ring(base.add(0, 0.15D, 0), AXIS_X, AXIS_Z, 1.0D + 12.0D * easeOut(tk / 22.0F),
                        1.2D * (1.0F - tk / 34.0F) + 0.2D, k == 0 ? 0xFFFFFF : color,
                        (1.0F - Rnd.ramp(tk, 3.0F, 26.0F)) * 0.9F, 56);
            }

            d.type(VfxRenderTypes.GLOW_ADD);
            d.glow(base.add(0, 0.6D, 0), 18.0F * flash + 3.0F, color, 0.8F * flash);
            d.glow(base.add(0, 0.6D, 0), 7.0F * flash, 0xFFFFFF, flash);

            d.type(VfxRenderTypes.STAR_ADD);
            for (int i = 0; i < 60; i++) {
                float life = 0.5F + Rnd.f(seed, 600 + i) * 0.6F;
                if (ts > life) {
                    continue;
                }
                Vec3 dir = sphere(seed, 700 + i);
                dir = new Vec3(dir.x, Math.abs(dir.y) * 0.9D + 0.2D, dir.z).normalize();
                Vec3 p = base.add(dir.scale((4.0D + 12.0D * Rnd.f(seed, 800 + i)) * ts)).add(0.0D, -4.0D * ts * ts, 0.0D);
                d.glow(p, 0.55F * (1.0F - ts / life), t * 0.2F + i, 0xFFFFFF, 1.0F - ts / life);
            }
        }

        @Override
        public float shake(Vec3 cam, float t) {
            return 0.6F * (1.0F - Rnd.ramp(t, 0.0F, 16.0F)) * falloff(cam, origin, 60.0F);
        }

        @Override
        public float flash(Vec3 cam, float t) {
            return 0.5F * (1.0F - Rnd.ramp(t, 0.0F, 6.0F)) * falloff(cam, origin, 35.0F);
        }

        @Override
        public int flashColor() {
            return 0xDDEEFF;
        }
    }

    // =====================================================================================================
    // Ceremonie d'une structure evoluee
    // =====================================================================================================

    public static final class Reward extends Fx {
        public Reward(VfxPacket p) {
            super(p);
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            Vec3 base = pos(partial);
            int gold = 0xFFD76A;
            int tint = FxUtil.mix(color, gold, 0.4F);
            float rise = easeOut(Rnd.ramp(t, 6.0F, 26.0F));
            float fade = 1.0F - Rnd.ramp(t, 52.0F, duration);
            Vec3 chest = base.add(0.0D, 1.0D, 0.0D);

            d.type(VfxRenderTypes.LINE_ADD);
            // des fils de lumiere convergent vers le joueur
            float conv = Rnd.ramp(t, 0.0F, 14.0F);
            if (conv < 1.0F) {
                for (int i = 0; i < 30; i++) {
                    Vec3 dir = sphere(seed, i);
                    double r = 7.0D * (1.0F - easeIn(conv));
                    Vec3 p1 = chest.add(dir.scale(r));
                    Vec3 p0 = chest.add(dir.scale(r + 1.8D));
                    d.ribbon(p0, p1, 0.0F, 0.12F, i % 2 == 0 ? tint : 0xFFFFFF, 0.0F, 0.9F * (1.0F - conv * 0.5F));
                }
            }
            // colonne de lumiere
            if (t > 8.0F) {
                float w = 2.6F * rise * (1.0F - 0.3F * Rnd.ramp(t, 20.0F, 60.0F));
                d.pillar(base, 46.0D * rise, w, tint, 0.8F * fade, 0.1F * fade);
                d.pillar(base, 46.0D * rise, w * 0.4F, 0xFFFFFF, 0.9F * fade, 0.15F * fade);
            }
            // trois anneaux tournants qui montent
            for (int k = 0; k < 3; k++) {
                float rk = Rnd.ramp(t, 8.0F + k * 3.0F, 30.0F + k * 3.0F);
                if (rk <= 0.0F) {
                    continue;
                }
                double y = 0.15D + easeOut(rk) * (1.2D + k * 1.0D);
                double rad = 1.3D + k * 0.55D + 0.2D * Math.sin(t * 0.2D + k);
                double spin = t * (0.14D + 0.05D * k) * (k % 2 == 0 ? 1.0D : -1.0D);
                d.arc(base.add(0, y, 0), AXIS_X, AXIS_Z, rad, 0.22D, spin, spin + Math.PI * 1.4D, k == 1 ? 0xFFFFFF : tint,
                        0.9F * fade, 36);
                d.arc(base.add(0, y, 0), AXIS_X, AXIS_Z, rad, 0.22D, spin + Math.PI, spin + Math.PI * 1.7D, tint,
                        0.6F * fade, 24);
            }
            // onde au sol quand la structure s'ouvre
            for (int k = 0; k < 3; k++) {
                float tk = t - 12.0F - k * 5.0F;
                if (tk < 0.0F) {
                    continue;
                }
                d.ring(base.add(0, 0.12D, 0), AXIS_X, AXIS_Z, 0.6D + 9.0D * easeOut(tk / 30.0F),
                        0.9D * (1.0F - tk / 44.0F) + 0.15D, k == 0 ? 0xFFFFFF : tint, (1.0F - Rnd.ramp(tk, 6.0F, 36.0F)) * 0.9F, 56);
            }

            d.type(VfxRenderTypes.GLOW_ADD);
            float burst = (1.0F - Rnd.ramp(t, 12.0F, 30.0F)) * Rnd.ramp(t, 8.0F, 12.0F);
            d.glow(chest, 9.0F * burst + 2.0F * rise, tint, 0.8F * burst + 0.3F * fade * rise);
            d.glow(chest, 4.0F * burst, 0xFFFFFF, burst);

            // etoiles en spirale qui s'elevent
            d.type(VfxRenderTypes.STAR_ADD);
            for (int i = 0; i < 40; i++) {
                float ph = fract(t * 0.022F + Rnd.f(seed, 100 + i));
                double ang = t * 0.16D + i * 2.399963D;
                double rr = 0.9D + 1.2D * Rnd.f(seed, 200 + i) + 0.8D * ph;
                Vec3 p = base.add(Math.cos(ang) * rr, 0.1D + ph * 7.0D, Math.sin(ang) * rr);
                float al = (float) Math.sin(Math.PI * ph) * fade * Rnd.ramp(t, 6.0F, 12.0F);
                d.glow(p, 0.4F + 0.5F * Rnd.f(seed, 300 + i), t * 0.2F + i, i % 3 == 0 ? 0xFFFFFF : tint, al);
            }
            // gerbe d'etincelles au moment de l'ouverture
            float ts = Math.max(0.0F, (t - 12.0F) / 20.0F);
            if (ts > 0.0F && ts < 1.0F) {
                for (int i = 0; i < 50; i++) {
                    Vec3 dir = sphere(seed, 400 + i);
                    dir = new Vec3(dir.x, Math.abs(dir.y) * 0.8D + 0.3D, dir.z).normalize();
                    Vec3 p = chest.add(dir.scale((3.0D + 7.0D * Rnd.f(seed, 500 + i)) * easeOut(ts)))
                            .add(0.0D, -2.5D * ts * ts, 0.0D);
                    d.glow(p, 0.5F * (1.0F - ts), t * 0.3F + i, i % 2 == 0 ? 0xFFFFFF : gold, 1.0F - ts);
                }
            }
        }

        @Override
        public float flash(Vec3 cam, float t) {
            return 0.5F * Rnd.ramp(t, 8.0F, 11.0F) * (1.0F - Rnd.ramp(t, 11.0F, 22.0F)) * falloff(cam, origin, 8.0F);
        }

        @Override
        public int flashColor() {
            return 0xFFF2C0;
        }

        @Override
        public float shake(Vec3 cam, float t) {
            return 0.25F * Rnd.ramp(t, 8.0F, 11.0F) * (1.0F - Rnd.ramp(t, 11.0F, 30.0F)) * falloff(cam, origin, 30.0F);
        }
    }

    private FxMining() {}
}
