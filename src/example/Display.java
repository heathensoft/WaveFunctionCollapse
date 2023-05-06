package example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;

import static java.lang.System.nanoTime;

/**
 * Utility class do display the wave function.
 * Running on separate thread at 60 frames / second.
 *
 * @author Frederik Dahl
 * 05/05/2023
 */


public class Display extends JFrame implements Runnable {

    private Color background_color;
    private final Canvas canvas;
    private final BufferedImage image;
    private final int[][] pixels;
    private final int scale;
    private boolean active;
    
    
    public Display(final int[][] pixels, int scale, KeyListener listener) {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        addKeyListener(listener);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { active = false; }});
        int cols = pixels[0].length;
        int rows = pixels.length;
        this.pixels = pixels;
        this.scale = scale;
        active = true;
        background_color = new Color(0,0,0);
        image = new BufferedImage(cols,rows,BufferedImage.TYPE_INT_ARGB);
        canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(cols * scale,rows * scale));
        canvas.setFocusable(false);
        add(canvas);
        pack();
        canvas.createBufferStrategy(2);
    }
    
    public void run() {
        setVisible(true);
        double interval = 1 / 60.0d;
        double start_time = time();
        double end_time;
        double delta_time = 0.0d;
        double accumulator = 0.0d;
        while (active) {
            accumulator += delta_time;
            while (accumulator > interval) {
                accumulator -= interval;
                refresh();
            } end_time = time();
            delta_time = end_time - start_time;
            start_time = end_time;
        }
    }
    
    public synchronized boolean isActive() {
        return active;
    }
    
    public void setBackgroundColor(Color color) {
        background_color = color;
    }
    
    private void refresh() {
        int width = pixels[0].length;
        int height = pixels.length;
        BufferStrategy bufferStrategy = canvas.getBufferStrategy();
        Graphics graphics = bufferStrategy.getDrawGraphics();
        graphics.setColor(background_color);
        graphics.fillRect(0,0,width * scale,height * scale);
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                image.setRGB(c,r,pixels[r][c]);
            }
        }
        graphics.drawImage(image,0,0, width * scale, height * scale,null);
        graphics.dispose();
        bufferStrategy.show();
    }
    
    private double time() {
        return nanoTime() / 1_000_000_000.0d;
    }
}
