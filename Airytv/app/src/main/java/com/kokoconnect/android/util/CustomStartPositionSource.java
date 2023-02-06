//package com.airytv.android;
//
//import android.os.Handler;
//import android.provider.MediaStore;
//
//import androidx.annotation.Nullable;
//
//import com.google.android.exoplayer2.C;
//import com.google.android.exoplayer2.ExoPlayerProxy;
//import com.google.android.exoplayer2.Timeline;
//import com.google.android.exoplayer2.audio.AudioCapabilitiesReceiver;
//import com.google.android.exoplayer2.source.ForwardingTimeline;
//import com.google.android.exoplayer2.source.MediaPeriod;
//import com.google.android.exoplayer2.source.MediaSource;
//import com.google.android.exoplayer2.source.MediaSourceEventListener;
//import com.google.android.exoplayer2.upstream.Allocator;
//import com.google.android.exoplayer2.upstream.TransferListener;
//
//import java.io.IOException;
//
//public class CustomStartPositionSource implements MediaSource {
//
//    private final MediaSource wrappedSource;
//    private final long startPositionUs;
//
//    public CustomStartPositionSource(MediaSource wrappedSource, long startPositionMs) {
//        this.wrappedSource = wrappedSource;
//        this.startPositionUs = C.msToUs(startPositionMs);
//    }
//
//    @Override
//    public void addEventListener(Handler handler, MediaSourceEventListener eventListener) {
//
//    }
//
//    @Override
//    public void removeEventListener(MediaSourceEventListener eventListener) {
//
//    }
//
//    @Nullable
//    @Override
//    public Object getTag() {
//        return null;
//    }
//
//    @Override
//    public void prepareSource(SourceInfoRefreshListener listener, @Nullable TransferListener mediaTransferListener) {
//        wrappedSource.prepareSource(listener, new SourceInfoRefreshListener() {
//            @Override
//            public void onSourceInfoRefreshed(MediaSource source, Timeline timeline, @Nullable Object manifest) {
//                listener.onSourceInfoRefreshed((MediaSource) new CustomStartPositionTimeline(timeline, startPositionUs), timeline, manifest);
//            }
//        });
//    }
//
//    @Override
//    public void maybeThrowSourceInfoRefreshError() throws IOException {
//        wrappedSource.maybeThrowSourceInfoRefreshError();
//    }
//
//    @Override
//    public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator, long startPositionUs) {
//        return wrappedSource.createPeriod(id, allocator, startPositionUs);
//    }
//
//    @Override
//    public void releasePeriod(MediaPeriod mediaPeriod) {
//        wrappedSource.releasePeriod(mediaPeriod);
//    }
//
//    @Override
//    public void releaseSource(SourceInfoRefreshListener listener) {
//        wrappedSource.releaseSource(listener);
//    }
//
//    private static final class CustomStartPositionTimeline extends ForwardingTimeline {
//
//        private final long defaultPositionUs;
//
//        public CustomStartPositionTimeline(Timeline wrapped, long defaultPositionUs) {
//            super(wrapped);
//            this.defaultPositionUs = defaultPositionUs;
//        }
//
//        @Override
//        public Timeline.Window getWindow(int windowIndex, Window window, boolean setIds,
//                                         long defaultPositionProjectionUs) {
//            super.getWindow(windowIndex, window, setIds, defaultPositionProjectionUs);
//            window.defaultPositionUs = defaultPositionUs;
//            return window;
//        }
//
//    }
//
//}