package com.corosus.watut.cloudRendering.test;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;
import java.util.Random;

public class MapBufferExample {

    public long window;
    public int vbo;
    public static FloatBuffer mappedBuffer = null;
    public static int triangles = 1000000;
    public static int floats_per_triangle = 6;
    public static int verts_per_triangle = 3;
    public static int bufferSize = triangles * floats_per_triangle;
    public static boolean running = true;
    public int arrayObjectId;

    public static void main(String[] args) {
        new MapBufferExample().run();
    }

    public void run() {
        init();
        loop();
        running = false;

        GL30.glBindVertexArray(0);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        window = GLFW.glfwCreateWindow(800, 600, "Map Buffer Example", 0, 0);
        if (window == 0) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);

        GL.createCapabilities();

        this.arrayObjectId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(this.arrayObjectId);

        // Create VBO
        vbo = GL33.glGenBuffers();
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);

        FloatBuffer vertices = BufferUtils.createFloatBuffer(bufferSize);
        //ByteBuffer vertices2 = BufferUtils.createByteBuffer(bufferSize);
        float[] data = new float[MapBufferExample.bufferSize];
        for (int i = 0; i < data.length; i++) {
            data[i] = (float) (Math.random() - Math.random());
        }
        vertices.put(data);
        vertices.flip();

        // Create buffer with immutable storage
        ARBBufferStorage.glBufferStorage(GL33.GL_ARRAY_BUFFER, vertices, GL33.GL_MAP_WRITE_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT);
        //ARBBufferStorage.glBufferStorage(GL33.GL_ARRAY_BUFFER, vertices2, GL33.GL_MAP_WRITE_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT);

        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, 0);

        new Thread(new UploadTask()).start();
    }

    public void loop() {
        long tickTime = 0;
        Random rand = new Random();
        while (!GLFW.glfwWindowShouldClose(window)) {
            GL33.glClear(GL33.GL_COLOR_BUFFER_BIT | GL33.GL_DEPTH_BUFFER_BIT);

            long tickStart = System.currentTimeMillis();

            GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);

            if (mappedBuffer == null) {
                mappedBuffer = GL33.glMapBufferRange(GL33.GL_ARRAY_BUFFER, 0, bufferSize * Float.BYTES, GL33.GL_MAP_WRITE_BIT | ARBBufferStorage.GL_MAP_PERSISTENT_BIT).asFloatBuffer();
            }

            GL33.glEnableVertexAttribArray(0);
            //only needed to be done once
            GL33.glVertexAttribPointer(0, verts_per_triangle, GL33.GL_FLOAT, false, 0, 0);

            //GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, triangles);
            GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, 3 * triangles);
            //GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, 3 * 10000);
            //GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, 3);

            GL33.glDisableVertexAttribArray(0);

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();

            GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, 0);
            System.out.println("ms time after glDrawArrays: " + (System.currentTimeMillis() - tickStart));
        }
    }

    public static class UploadTask implements Runnable {
        @Override
        public void run() {
            while (running) {
                //System.out.println("run " + System.currentTimeMillis());
                    // Simulate some work
                    try {
                        //Thread.sleep(100);
                        if (mappedBuffer != null) {
                            //synchronized (MapBufferExample.mappedBuffer) {
                                float[] data = new float[MapBufferExample.bufferSize];
                                //float[] data = new float[10000 * 3];
                                for (int i = 0; i < data.length; i++) {
                                    data[i] = (float) (Math.random() - Math.random());
                                }
                                MapBufferExample.mappedBuffer.put(data);
                                MapBufferExample.mappedBuffer.flip();
                            //}
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // Update the mapped buffer here if necessary
            }
        }
    }
}
