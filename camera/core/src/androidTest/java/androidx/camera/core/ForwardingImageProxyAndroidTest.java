/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.ImageFormat;
import android.graphics.Rect;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class ForwardingImageProxyAndroidTest {

    private final ImageProxy mBaseImageProxy = mock(ImageProxy.class);
    private final ImageProxy.PlaneProxy mYPlane = mock(ImageProxy.PlaneProxy.class);
    private final ImageProxy.PlaneProxy mUPlane = mock(ImageProxy.PlaneProxy.class);
    private final ImageProxy.PlaneProxy mVPlane = mock(ImageProxy.PlaneProxy.class);
    private ForwardingImageProxy mImageProxy;

    @Before
    public void setUp() {
        mImageProxy = new ConcreteImageProxy(mBaseImageProxy);
    }

    @Test
    public void close_closesWrappedImage() {
        mImageProxy.close();

        verify(mBaseImageProxy).close();
    }

    @Test(timeout = 2000)
    public void close_notifiesOnImageCloseListener_afterSetOnImageCloseListener()
            throws InterruptedException {
        Semaphore closedImageSemaphore = new Semaphore(/*permits=*/ 0);
        AtomicReference<ImageProxy> closedImage = new AtomicReference<>();
        mImageProxy.addOnImageCloseListener(
                image -> {
                    closedImage.set(image);
                    closedImageSemaphore.release();
                });

        mImageProxy.close();

        closedImageSemaphore.acquire();
        assertThat(closedImage.get()).isSameAs(mImageProxy);
    }

    @Test
    public void getCropRect_returnsCropRectForWrappedImage() {
        when(mBaseImageProxy.getCropRect()).thenReturn(new Rect(0, 0, 20, 20));

        assertThat(mImageProxy.getCropRect()).isEqualTo(new Rect(0, 0, 20, 20));
    }

    @Test
    public void setCropRect_setsCropRectForWrappedImage() {
        mImageProxy.setCropRect(new Rect(0, 0, 40, 40));

        verify(mBaseImageProxy).setCropRect(new Rect(0, 0, 40, 40));
    }

    @Test
    public void getFormat_returnsFormatForWrappedImage() {
        when(mBaseImageProxy.getFormat()).thenReturn(ImageFormat.YUV_420_888);

        assertThat(mImageProxy.getFormat()).isEqualTo(ImageFormat.YUV_420_888);
    }

    @Test
    public void getHeight_returnsHeightForWrappedImage() {
        when(mBaseImageProxy.getHeight()).thenReturn(480);

        assertThat(mImageProxy.getHeight()).isEqualTo(480);
    }

    @Test
    public void getWidth_returnsWidthForWrappedImage() {
        when(mBaseImageProxy.getWidth()).thenReturn(640);

        assertThat(mImageProxy.getWidth()).isEqualTo(640);
    }

    @Test
    public void getTimestamp_returnsTimestampForWrappedImage() {
        when(mBaseImageProxy.getTimestamp()).thenReturn(138990020L);

        assertThat(mImageProxy.getTimestamp()).isEqualTo(138990020L);
    }

    @Test
    public void setTimestamp_setsTimestampForWrappedImage() {
        mImageProxy.setTimestamp(138990020L);

        verify(mBaseImageProxy).setTimestamp(138990020L);
    }

    @Test
    public void getPlanes_returnsPlanesForWrappedImage() {
        when(mBaseImageProxy.getPlanes())
                .thenReturn(new ImageProxy.PlaneProxy[]{mYPlane, mUPlane, mVPlane});

        ImageProxy.PlaneProxy[] planes = mImageProxy.getPlanes();
        assertThat(planes.length).isEqualTo(3);
        assertThat(planes[0]).isEqualTo(mYPlane);
        assertThat(planes[1]).isEqualTo(mUPlane);
        assertThat(planes[2]).isEqualTo(mVPlane);
    }

    private static final class ConcreteImageProxy extends ForwardingImageProxy {
        private ConcreteImageProxy(ImageProxy baseImageProxy) {
            super(baseImageProxy);
        }
    }
}
