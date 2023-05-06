package example;

import wfc.WFC;

import javax.imageio.ImageIO;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Frederik Dahl
 * 06/05/2023
 */


public class Example {
    
    // Press any key to re-generate the output
    
    public static void main(String[] args) throws IOException {
        // -------------------------------------------------------------------------------------
        final String input_path = "img/Rooms.png";
        final int seed = (int)(System.currentTimeMillis());
        final int display_scale = 4;
        final int output_width = 128;
        final int output_height = 128;
        final int failure_limit = 100;
        final boolean allow_permutation = true;
        final boolean wrap_around = false;
        // -------------------------------------------------------------------------------------
        final int[][] output_data = new int[output_height][output_width];
        final int[][] training_data = from_buffered_image(load_buffered_image(input_path));
        final KeySignal any_key = new KeySignal();
        final Display display = new Display(output_data,display_scale,any_key);
        final WFC wfc = new WFC(training_data,seed,allow_permutation);
        new Thread(display).start();
        while (display.isActive()) {
            if (wfc.generate(output_data,failure_limit,wrap_around)) {
                while (!any_key.isPressed()) {/*intentional*/}
                for (int r = 0; r < output_height; r++) {
                    for (int c = 0; c < output_width; c++) {
                        output_data[r][c] = 0;
                    }
                }
            }
        }
    }
    
    static BufferedImage load_buffered_image(String path) throws IOException {
        return ImageIO.read(new FileInputStream(path));
    }
    
    static void save_buffered_image(BufferedImage image, String path) throws IOException {
        ImageIO.write(image,"png",new File(path));
    }
    
    static BufferedImage to_buffered_image(int[][] src) {
        int cols = src[0].length;
        int rows = src.length;
        BufferedImage result = new BufferedImage(cols,rows,BufferedImage.TYPE_INT_ARGB);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                result.setRGB(c,r,src[r][c]);
            }
        } return result;
    }
    
    static int[][] from_buffered_image(BufferedImage src) {
        int cols = src.getWidth();
        int rows = src.getHeight();
        int[][] result = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                result[r][c] = src.getRGB(c,r);
            }
        } return result;
    }
    
    
    private static final class KeySignal extends KeyAdapter {
        private boolean pressed;
        public void keyPressed(KeyEvent e) { synchronized (this) { pressed = true; } }
        public void keyReleased(KeyEvent e) { synchronized (this) { pressed = false; } }
        public synchronized boolean isPressed() { return pressed; }
    }
}
