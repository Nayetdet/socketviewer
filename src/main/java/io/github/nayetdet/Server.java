package io.github.nayetdet;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.InputEvent;
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
    private final ServerSocket imageServerSocket;
    private final ServerSocket controlServerSocket;
    private final ScheduledExecutorService scheduler;

    public Server(int imagePort, int controlPort) throws IOException, AWTException {
        robot = new Robot();
        screenBounds = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        imageServerSocket = new ServerSocket(imagePort);
        controlServerSocket = new ServerSocket(controlPort);
        imageSocket = imageServerSocket.accept();
        controlSocket = controlServerSocket.accept();
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void host() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                BufferedImage screen = robot.createScreenCapture(screenBounds);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ImageIO.write(screen, "png", byteArrayOutputStream);

                byte[] bytes = byteArrayOutputStream.toByteArray();
                DataOutputStream dataOutputStream = new DataOutputStream(imageSocket.getOutputStream());
                dataOutputStream.writeInt(bytes.length);
                dataOutputStream.write(bytes);
                dataOutputStream.flush();
            } catch (IOException e) {
                scheduler.shutdown();
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        Thread controlThread = new Thread(() -> {
            try {
                DataInputStream controlInputStream = new DataInputStream(controlSocket.getInputStream());
                while (true) {
                    String[] actions = controlInputStream.readUTF().split(" ");
                    if (actions.length >= 3 && actions[0].equals("CLICK")) {
                        int x = Integer.parseInt(actions[1]);
                        int y = Integer.parseInt(actions[2]);
                        robot.mouseMove(x, y);
                        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        controlThread.start();
        try {
            controlThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void close() throws IOException {
        scheduler.shutdown();
        imageSocket.close();
        controlSocket.close();
        imageServerSocket.close();
        controlServerSocket.close();
    }

    public static void main(String[] args) throws IOException, AWTException {
        if (args.length != 2) {
            throw new IllegalArgumentException();
        }

        int imagePort = Integer.parseInt(args[0]);
        int controlPort = Integer.parseInt(args[1]);

        Server server = new Server(imagePort, controlPort);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));

        server.host();
    }

}
