package eu.izadpanah.mcheck;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.ffmpeg.global.avutil;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class StreamInitializer implements Runnable {
    private final String streamUrl;
    private final LinkedBlockingQueue<Frame> frameQueue;
    private final AtomicBoolean running;
    private final int maxQueueSize = 100;

    public StreamInitializer(String streamUrl) {
        this.streamUrl = streamUrl;
        this.frameQueue = new LinkedBlockingQueue<>(maxQueueSize);
        this.running = new AtomicBoolean(false);
    }

    public LinkedBlockingQueue<Frame> getFrameQueue() {
        return frameQueue;
    }

    public void stop() {
        running.set(false);
    }

    @Override
    public void run() {
        // Set log level of javacv globally before starting
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(streamUrl)) {
            grabber.setPixelFormat(avutil.AV_PIX_FMT_BGR24);
            //grabber.setPixelFormat(3);

            grabber.setOption("timeout", "5000"); // 0.5 seconds in ^-^ microseconds

            grabber.start();
            running.set(true);

            System.out.println("Started streaming from: " + streamUrl);

            while (running.get()) {
                Frame frame = grabber.grab();
                /*if (frame == null) {
                    System.out.println("Stream reached end or lost connection.");
                    continue;
                }*/

                if (frame.image == null) continue;

                Frame clonedFrame = frame.clone();

                if (!frameQueue.offer(clonedFrame)) {
                    Frame oldFrame = frameQueue.poll();
                    if (oldFrame != null) {
                        oldFrame.close(); // MANUALLY RELEASE NATIVE MEMORY
                    }
                    //System.out.println("Fram is: "+clonedFrame.imageHeight+"X"+clonedFrame.imageWidth);
                    frameQueue.offer(clonedFrame);
                }
            }
            grabber.stop();
        } catch (Exception e) {
            System.err.println("Error processing stream: " + e.getMessage());
        } finally {
            cleanupQueue();
        }
    }

    private void cleanupQueue() {
        Frame f;
        while ((f = frameQueue.poll()) != null) {
            f.close();
        }
    }
}