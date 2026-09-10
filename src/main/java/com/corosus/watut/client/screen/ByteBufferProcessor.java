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
        this.inputQueue = new LinkedBlockingQueue<>(4);
        this.outputQueue = new LinkedBlockingQueue<>(4);
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
                        ByteBuffer result = null;
                        try {
                            result = processingFunction.apply(input);
                        } catch (Throwable t) {
                            com.corosus.coroutil.util.CULog.dbg("Watut: error in ByteBufferProcessor worker: " + t.getMessage());
                        }
                        if (result != null) {
                            while (!outputQueue.offer(result)) {
                                outputQueue.poll();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Throwable t) {
                    com.corosus.coroutil.util.CULog.dbg("Watut: unexpected error in ByteBufferProcessor loop: " + t.getMessage());
                }
            }
        });
    }
    
    public void submitForProcessing(ByteBuffer buffer) {
        if (!isRunning) return;
        while (!inputQueue.offer(buffer)) {
            inputQueue.poll();
        }
    }
    
    public ByteBuffer getProcessedBuffer() {
        return outputQueue.poll();
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