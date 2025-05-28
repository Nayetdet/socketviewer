package io.github.nayetdet;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;

public class Client {

    public static void main(String[] args) throws Exception {
        Socket imageSocket = new Socket("localhost", 5000);
        Socket controlSocket = new Socket("localhost", 5001);

        InputStream in = imageSocket.getInputStream();
        DataInputStream dis = new DataInputStream(in);
        DataOutputStream controlOut = new DataOutputStream(controlSocket.getOutputStream());

        JFrame frame = new JFrame("Tela Remota");
        JLabel label = new JLabel();
        frame.getContentPane().add(label);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setVisible(true);

        label.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int imageWidth = label.getIcon().getIconWidth();
                int imageHeight = label.getIcon().getIconHeight();
                int labelWidth = label.getWidth();
                int labelHeight = label.getHeight();

                // Escala
                double scaleX = (double) Toolkit.getDefaultToolkit().getScreenSize().width / labelWidth;
                double scaleY = (double) Toolkit.getDefaultToolkit().getScreenSize().height / labelHeight;

                int realX = (int) (e.getX() * scaleX);
                int realY = (int) (e.getY() * scaleY);

                try {
                    controlOut.writeUTF("CLICK " + realX + " " + realY);
                    controlOut.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        while (true) {
            int len = dis.readInt();
            byte[] imageBytes = new byte[len];
            dis.readFully(imageBytes);

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image != null) {
                ImageIcon icon = new ImageIcon(image.getScaledInstance(
                        label.getWidth(), label.getHeight(), java.awt.Image.SCALE_SMOOTH));
                label.setIcon(icon);
            }
        }
    }
}
