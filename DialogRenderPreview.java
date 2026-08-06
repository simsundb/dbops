import com.sunzh.ui.BaseDialog;
import com.sunzh.datasource.DataSourceDialog;
import com.sunzh.utils.ThemeUtils;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class DialogRenderPreview {
    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(new FlatLightLaf());
        ThemeUtils.applyFlatLafTheme();

        SwingUtilities.invokeAndWait(() -> {
            JFrame owner = new JFrame();
            owner.setSize(200, 100);
            DataSourceDialog dlg = new DataSourceDialog(owner);
            dlg.setSize(1200, 800);
            dlg.setLocation(0, 0);
            dlg.setVisible(true);
            dlg.toFront();
            try { Thread.sleep(1800); } catch (InterruptedException ignored) {}
            BufferedImage img = new BufferedImage(dlg.getWidth(), dlg.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            dlg.paint(g2);
            g2.dispose();
            try {
                ImageIO.write(img, "png", new File("/tmp/db_datasource_preview.png"));
                System.out.println("SAVED /tmp/db_datasource_preview.png");
            } catch (Exception e) { e.printStackTrace(); }
            dlg.dispose();
            owner.dispose();
        });
        System.exit(0);
    }
}
