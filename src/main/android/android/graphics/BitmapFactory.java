package android.graphics;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class BitmapFactory {
    @SuppressWarnings("unused")
    public static class Options {
        public Options() {
        }

        public Bitmap inBitmap;

        public boolean inMutable;

        public boolean inJustDecodeBounds;

        public int inSampleSize;

        public Bitmap.Config inPreferredConfig = Bitmap.Config.ARGB_8888;
    }

    public static Bitmap decodeStream(InputStream is) {
        try {
            BufferedImage image = ImageIO.read(is);
            return new Bitmap(image);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static Bitmap decodeStream(InputStream is, Rect outPadding, Options opts) {
        return decodeStream(is);
    }
}
