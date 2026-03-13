package com.girlkun.tool.screens.mob_scr;

import java.awt.*;
import java.awt.image.BufferedImage;

public class EditorUtils {

    public static int clamp(int v, int a, int b) {
        return Math.max(a, Math.min(b, v));
    }

    public static double clamp(double v, double a, double b) {
        return Math.max(a, Math.min(b, v));
    }

    public static BufferedImage createCheckerboard(int w, int h, int step, Color c1, Color c2) {
        if (w <= 0) w = 1;
        if (h <= 0) h = 1;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(c1);
        g.fillRect(0, 0, w, h);
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                if ((x / step + y / step) % 2 == 1) {
                    g.setColor(c2);
                    g.fillRect(x, y, step, step);
                }
            }
        }
        g.dispose();
        return img;
    }

    public static BufferedImage createCheckerboard(int w, int h) {
        return createCheckerboard(w, h, 10, new Color(200, 200, 200), new Color(160, 160, 160));
    }

    public static BufferedImage resizeNearest(BufferedImage src, int w, int h) {
        if (w <= 0) w = 1;
        if (h <= 0) h = 1;
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return dst;
    }

    public static BufferedImage setAlpha(BufferedImage src, int alpha) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int argb = src.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                a = Math.min(a, alpha);
                dst.setRGB(x, y, (a << 24) | (argb & 0x00FFFFFF));
            }
        }
        return dst;
    }

    /** Simple flood-fill connected component bounding box at (clickX, clickY) based on alpha > 0 */
    public static int[] findConnectedBBox(BufferedImage img, int clickX, int clickY) {
        int w = img.getWidth(), h = img.getHeight();
        if (clickX < 0 || clickX >= w || clickY < 0 || clickY >= h) return null;
        int clickAlpha = (img.getRGB(clickX, clickY) >> 24) & 0xFF;
        if (clickAlpha == 0) return null;

        // BFS/flood fill
        boolean[][] visited = new boolean[h][w];
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();
        queue.add(new int[]{clickX, clickY});
        visited[clickY][clickX] = true;

        int minX = clickX, minY = clickY, maxX = clickX, maxY = clickY;
        int[][] dirs = {{0,1},{0,-1},{1,0},{-1,0},{1,1},{1,-1},{-1,1},{-1,-1}};

        while (!queue.isEmpty()) {
            int[] pt = queue.poll();
            int px = pt[0], py = pt[1];
            minX = Math.min(minX, px);
            minY = Math.min(minY, py);
            maxX = Math.max(maxX, px);
            maxY = Math.max(maxY, py);

            for (int[] d : dirs) {
                int nx = px + d[0], ny = py + d[1];
                if (nx >= 0 && nx < w && ny >= 0 && ny < h && !visited[ny][nx]) {
                    int a = (img.getRGB(nx, ny) >> 24) & 0xFF;
                    if (a > 0) {
                        visited[ny][nx] = true;
                        queue.add(new int[]{nx, ny});
                    }
                }
            }
        }

        int bw = maxX - minX + 1;
        int bh = maxY - minY + 1;
        if (bw <= 0 || bh <= 0) return null;
        return new int[]{minX, minY, bw, bh};
    }
}
