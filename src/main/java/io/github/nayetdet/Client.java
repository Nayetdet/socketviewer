package io.github.nayetdet;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client extends JFrame implements MouseListener {

    private final Socket imageSocket;
    private final Socket controlSocket;
    private final JLabel label;
    private final int serverScreenWidth;
    private final int serverScreenHeight;
    private final DataOutputStream controlOutputStream;

    public Client(String host, int imagePort, int controlPort) throws IOException {
        imageSocket = new Socket(host, imagePort);
        controlSocket = new Socket(host, controlPort);

        DataInputStream controlInputStream = new DataInputStream(controlSocket.getInputStream());
        serverScreenWidth = controlInputStream.readInt();
        serverScreenHeight = controlInputStream.readInt();
        controlOutputStream = new DataOutputStream(controlSocket.getOutputStream());

        label = new JLabel();
        JScrollPane scrollPane = new JScrollPane(label);
        getContentPane().add(scrollPane);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setTitle("Client");
        setVisible(true);
        label.addMouseListener(this);
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
                SwingUtilities.invokeLater(() -> {
                    label.setIcon(icon);
                    label.setSize(icon.getIconWidth(), icon.getIconHeight());
                    label.setPreferredSize(label.getSize());
                    label.revalidate();
                });
            }
        }
    }

    public void close() throws IOException {
        imageSocket.close();
        controlSocket.close();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        try {
            int x = e.getX();
            int y = e.getY();
            int labelWidth = label.getWidth();
            int labelHeight = label.getHeight();

            int actualX = x * serverScreenWidth / labelWidth;
            int actualY = y * serverScreenHeight / labelHeight;

            controlOutputStream.writeUTF("CLICK " + actualX + " " + actualY);
            controlOutputStream.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: java Client <host> <imagePort> <controlPort>");
        }

        String host = args[0];
        int imagePort = Integer.parseInt(args[1]);
        int controlPort = Integer.parseInt(args[2]);

        Client client = new Client(host, imagePort, controlPort);
        client.connect();
        client.close();
    }

}
