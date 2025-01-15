package com.corosus.watut.client.screen;

import java.nio.ByteBuffer;
import java.util.concurrent.*;
import java.util.function.Function;

public class ByteBufferProcessor {
    private final BlockingQueue<ByteBuffer> inputQueue;
    private final BlockingQueue<ByteBuffer> outputQueue;
    private final ExecutorService executorService;
    private final Function<ByteBuffer, ByteBuffer> processingFunction;
    private volatile boolean isRunning;
    
    public ByteBufferProcessor(Function<ByteBuffer, ByteBuffer> processingFunction) {
        this.inputQueue = new LinkedBlockingQueue<>();
        this.outputQueue = new LinkedBlockingQueue<>();
        this.executorService = Executors.newSingleThreadExecutor();
        this.processingFunction = processingFunction;
        this.isRunning = true;
        
        startProcessing();
    }
    
    private void startProcessing() {
        executorService.submit(() -> {
            while (isRunning || !inputQueue.isEmpty()) {
                try {
                    ByteBuffer input = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (input != null) {
                        ByteBuffer result = processingFunction.apply(input);
                        outputQueue.put(result);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    public void submitForProcessing(ByteBuffer buffer) {
        if (!isRunning) {
            throw new IllegalStateException("Processor has been shutdown");
        }
        try {
            inputQueue.put(buffer);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to submit buffer for processing", e);
        }
    }
    
    public ByteBuffer getProcessedBuffer() throws InterruptedException {
        return outputQueue.poll(100, TimeUnit.MILLISECONDS);
    }

    public boolean hasProcessedBuffers() {
        return !outputQueue.isEmpty();
    }

    public boolean hasWork() {
        return !inputQueue.isEmpty();
    }
    
    public void shutdown() {
        isRunning = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}