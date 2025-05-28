package io.github.nayetdet;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server {

    private final Robot robot;
    private final Rectangle screenBounds;

    private final Socket imageSocket;
    private final Socket controlSocket;

    public Server(int imagePort, int controlPort) throws IOException, AWTException {
        robot = new Robot();
        screenBounds = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        imageSocket = new ServerSocket(imagePort).accept();
        controlSocket = new ServerSocket(controlPort).accept();
    }

    public void host() throws IOException {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                BufferedImage screen = robot.createScreenCapture(screenBounds);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ImageIO.write(screen, "jpg", byteArrayOutputStream);

                byte[] bytes = byteArrayOutputStream.toByteArray();
                DataOutputStream dataOutputStream = new DataOutputStream(imageSocket.getOutputStream());
                dataOutputStream.writeInt(bytes.length);
                dataOutputStream.write(bytes);
                dataOutputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        scheduler.close();

        new Thread(() -> {
            try {
                DataInputStream controlInputStream = new DataInputStream(controlSocket.getInputStream());
                while (true) {
                    String[] actions =  controlInputStream.readUTF().split(" ");
                    if (actions.length >= 3 && actions[0].equals("MOVE")) {
                        int x = Integer.parseInt(actions[1]);
                        int y = Integer.parseInt(actions[2]);
                        robot.mouseMove(x, y);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public void close() throws IOException {
        imageSocket.close();
        controlSocket.close();
    }

    public static void main(String[] args) throws IOException, AWTException {
        if (args.length != 2) {
            throw new IllegalArgumentException();
        }

        int imagePort = Integer.parseInt(args[0]);
        int controlPort = Integer.parseInt(args[1]);

        Server server = new Server(imagePort, controlPort);
        server.host();
        server.close();
    }

}
