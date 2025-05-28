package io.github.nayetdet;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args) throws Exception {
        ServerSocket imageSocket = new ServerSocket(5000);
        ServerSocket controlSocket = new ServerSocket(5001);

        System.out.println("Aguardando conexão do cliente...");
        Socket imageClient = imageSocket.accept();
        Socket controlClient = controlSocket.accept();
        System.out.println("Cliente conectado.");

        Robot robot = new Robot();
        Rectangle screenRectangle = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        OutputStream outputStream = imageClient.getOutputStream();

        new Thread(() -> {
            try (DataInputStream in = new DataInputStream(controlClient.getInputStream())) {
                while (true) {
                    String command = in.readUTF();
                    String[] parts = command.split(" ");
                    if (parts.length >= 3 && parts[0].equals("CLICK")) {
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);

                        robot.mouseMove(x, y);
                        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        while (true) {
            BufferedImage screen = robot.createScreenCapture(screenRectangle);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(screen, "jpg", baos);

            byte[] imageBytes = baos.toByteArray();
            DataOutputStream dos = new DataOutputStream(outputStream);
            dos.writeInt(imageBytes.length);
            dos.write(imageBytes);
            dos.flush();

            Thread.sleep(100);
        }
    }

}
