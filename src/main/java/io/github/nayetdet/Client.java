package io.github.nayetdet;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client extends JFrame implements MouseListener {

    private final JLabel label;

    private final transient Socket imageSocket;
    private final transient Socket controlSocket;
    private final transient DataOutputStream controlOutputStream;

    public Client(String host, int imagePort, int controlPort) throws IOException {
        imageSocket = new Socket(host, imagePort);
        controlSocket = new Socket(host, controlPort);
        controlOutputStream = new DataOutputStream(controlSocket.getOutputStream());

        label = new JLabel();
        label.addMouseListener(this);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(label, BorderLayout.CENTER);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("Client");
        setVisible(true);
    }

    public void connect() throws IOException {
        DataInputStream imageInputStream = new DataInputStream(imageSocket.getInputStream());
        while (true) {
            int length = imageInputStream.readInt();
            byte[] imageBytes = new byte[length];
            imageInputStream.readFully(imageBytes);

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image != null) {
                SwingUtilities.invokeLater(() -> {
                    label.setIcon(new ImageIcon(image));
                    label.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
                    pack();
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

            controlOutputStream.writeUTF("CLICK " + x + " " + y);
            controlOutputStream.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) { // Required method from MouseListener interface, not used
    }

    @Override
    public void mouseReleased(MouseEvent e) { // Required method from MouseListener interface, not used
    }

    @Override
    public void mouseEntered(MouseEvent e) { // Required method from MouseListener interface, not used
    }

    @Override
    public void mouseExited(MouseEvent e) { // Required method from MouseListener interface, not used
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
