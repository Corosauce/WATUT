package com.corosus.watut.cloudRendering.test;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

import java.nio.FloatBuffer;
import java.util.Random;

public class BufferDataExample {

    public long window;
    public int vbo;
    public static FloatBuffer vertexBuffer = null;
    public static int triangles = 1000000;
    public static int floats_per_triangle = 6;
    public static int verts_per_triangle = 3;
    public static int bufferSize = triangles * floats_per_triangle;
    public static boolean running = true;
    public int arrayObjectId;

    public static void main(String[] args) {
        new BufferDataExample().run();
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
        /*GLFWErrorCallback.createPrint(System.err).set();
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        window = GLFW.glfwCreateWindow(800, 600, "Buffer Data Example", 0, 0);
        if (window == 0) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);

        GL.createCapabilities();*/

        this.arrayObjectId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(this.arrayObjectId);

        // Create VBO
        vbo = GL33.glGenBuffers();
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);

        // Initialize the vertex buffer
        vertexBuffer = BufferUtils.createFloatBuffer(bufferSize);
        float[] data = new float[bufferSize];
        for (int i = 0; i < data.length; i++) {
            data[i] = (float) (Math.random() - Math.random());
        }
        vertexBuffer.put(data).flip();

        // Upload the initial data to the buffer
        GL33.glBufferData(GL33.GL_ARRAY_BUFFER, vertexBuffer, GL33.GL_DYNAMIC_DRAW);

        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, 0);

       // new Thread(new UploadTask()).start();
    }

    public void loop() {
        /*try {
            if (vertexBuffer != null) {
                float[] data = new float[bufferSize];
                for (int i = 0; i < data.length; i++) {
                    data[i] = (float) (Math.random() - Math.random()) * 100;
                }
                vertexBuffer.clear();
                vertexBuffer.put(data);
                vertexBuffer.flip();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        /*long tickTime = 0;
        Random rand = new Random();
        while (!GLFW.glfwWindowShouldClose(window)) {
            GL33.glClear(GL33.GL_COLOR_BUFFER_BIT | GL33.GL_DEPTH_BUFFER_BIT);*/

            long tickStart = System.currentTimeMillis();

            GL30.glBindVertexArray(this.arrayObjectId);
            GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, vbo);

            // Update the buffer data with new vertex data
            //GL33.glBufferSubData(GL33.GL_ARRAY_BUFFER, 0, vertexBuffer);
            GL33.glBufferData(GL33.GL_ARRAY_BUFFER, vertexBuffer, GL33.GL_DYNAMIC_DRAW);
            //System.out.println("ms time after glBufferData: " + (System.currentTimeMillis() - tickStart));

            GL33.glEnableVertexAttribArray(0);
            //only needed to be done once
            GL33.glVertexAttribPointer(0, verts_per_triangle, GL33.GL_FLOAT, false, 0, 0);

            GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, 3 * triangles);
            //GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, 3 * 10000);
            //GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, 3);

            GL33.glDisableVertexAttribArray(0);

            //GLFW.glfwSwapBuffers(window);
            //GLFW.glfwPollEvents();

            GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, 0);
            GL30.glBindVertexArray(0);
            //System.out.println("ms time after glDrawArrays: " + (System.currentTimeMillis() - tickStart));
        //}
    }

    public static class UploadTask implements Runnable {
        @Override
        public void run() {
            while (running) {
                // Simulate some work
                try {
                    if (vertexBuffer != null) {
                        float[] data = new float[bufferSize];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = (float) (Math.random() - Math.random());
                        }
                        vertexBuffer.clear();
                        vertexBuffer.put(data);
                        vertexBuffer.flip();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
