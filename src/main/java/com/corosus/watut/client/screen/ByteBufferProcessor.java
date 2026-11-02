package com.corosus.watut.client.screen;

import java.util.concurrent.*;
import java.util.function.Function;

public class ByteBufferProcessor<T> {
    private final BlockingQueue<T> inputQueue;
    private final BlockingQueue<T> outputQueue;
    private final ExecutorService executorService;
    private final Function<T, T> processingFunction;
    private volatile boolean isRunning;
    
    public ByteBufferProcessor(Function<T, T> processingFunction) {
        this.inputQueue = new LinkedBlockingQueue<>();
        this.outputQueue = new LinkedBlockingQueue<>();
        this.executorService = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "watut-screen-processor");
            thread.setDaemon(true);
            return thread;
        });
        this.processingFunction = processingFunction;
        this.isRunning = true;
        
        startProcessing();
    }
    
    private void startProcessing() {
        executorService.submit(() -> {
            while (isRunning || !inputQueue.isEmpty()) {
                try {
                    T input = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (input != null) {
                        T result = processingFunction.apply(input);
                        outputQueue.put(result);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    public void submitForProcessing(T buffer) {
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
    
    public T getProcessedBuffer() throws InterruptedException {
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