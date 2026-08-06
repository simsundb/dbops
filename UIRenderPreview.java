import com.sunzh.ui.MainFrame;
import com.sunzh.utils.ThemeUtils;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class UIRenderPreview {
    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(new FlatLightLaf());
        ThemeUtils.applyFlatLafTheme();

        SwingUtilities.invokeAndWait(() -> {
            MainFrame frame = new MainFrame();
            frame.setSize(1200, 750);
            frame.setLocation(0, 0);
            frame.setVisible(true);   // 必须显示后组件才会实际布局/绘制
            frame.toFront();
            frame.doLayout();
            try {
                Thread.sleep(1500);   // 等待绘制完成
            } catch (InterruptedException ignored) {}
            BufferedImage img = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            frame.paint(g2);
            g2.dispose();
            try {
                ImageIO.write(img, "png", new File("/tmp/db_mainframe_preview.png"));
                System.out.println("SAVED /tmp/db_mainframe_preview.png size=" + img.getWidth() + "x" + img.getHeight());
            } catch (Exception e) {
                e.printStackTrace();
            }
            frame.dispose();
        });
        System.exit(0);
    }
}
