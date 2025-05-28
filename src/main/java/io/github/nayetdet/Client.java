package io.github.nayetdet;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client extends JFrame implements MouseMotionListener {

    private final Socket imageSocket;
    private final Socket controlSocket;
    private final JLabel label;

    public Client(String imageHost, String controlHost, int imagePort, int controlPort) throws IOException {
        imageSocket = new Socket(imageHost, imagePort);
        controlSocket = new Socket(controlHost, controlPort);

        label = new JLabel();
        JScrollPane scrollPane = new JScrollPane(label);
        getContentPane().add(scrollPane);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setTitle("Client");
        setVisible(true);
        label.addMouseMotionListener(this);
    }

    public void connect() throws IOException {
        DataInputStream imageInputStream = new DataInputStream(imageSocket.getInputStream());
        while (true) {
            int length = imageInputStream.readInt();
            byte[] imageBytes = new byte[length];
            imageInputStream.readFully(imageBytes);

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image != null) {
                ImageIcon icon = new ImageIcon(image);
                label.setIcon(icon);
                label.setSize(icon.getIconWidth(), icon.getIconHeight());
                label.setPreferredSize(label.getSize());
                label.revalidate();
            }
        }
    }

    public void close() throws IOException {
        imageSocket.close();
        controlSocket.close();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        try {
            DataOutputStream controlOutputStream = new DataOutputStream(controlSocket.getOutputStream());
            controlOutputStream.writeUTF("MOVE " + e.getX() + " " + e.getY());
            controlOutputStream.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            throw new IllegalArgumentException();
        }

        String imageHost =  args[0];
        String controlHost = args[1];
        int imagePort = Integer.parseInt(args[2]);
        int controlPort = Integer.parseInt(args[3]);

        Client client = new Client(imageHost, controlHost, imagePort, controlPort);
        client.connect();
        client.close();
    }

}
