/*
 * Copyright 2018 MovingBlocks
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
package org.terasology;

import org.junit.After;
import org.junit.Test;
import org.terasology.engine.GameThread;
import org.terasology.recording.RecordAndReplayStatus;

/**
 * An environment that extends {@link ReplayTestingEnvironment} and sets the workflow of a replay test used for
 * acceptance testing. The tests that extends this class instead of {@link ReplayTestingEnvironment} will be more limited
 * but easier to write.
 * <p>
 * To write tests that uses this class, it is necessary to extend it and fill the three abstract methods {@link #testOnReplayStart()} ,
 * {@link #testDuringReplay()} and {@link #testOnReplayEnd()} . If it is not desired to test something in one or more of
 * the replay stages, it is possible to leave the methods in black.
 * <p>
 * After the implementation of the abstract methods, it is necessary to write a test method that calls {@link #runTest(String, boolean)} .
 */
public abstract class AcceptanceTestEnvironment extends ReplayTestingEnvironment {

    private String recordingTitle;
    private boolean isHeadless;

    /** To test the replay while it is executing, it is necessary to create a thread that will run the replay. */
    private Thread replayThread = new Thread() {

        @Override
        public void run() {
            try {
                String replayTitle = recordingTitle;
                AcceptanceTestEnvironment.super.openReplay(replayTitle, isHeadless);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @After
    public void closeReplay() throws Exception {
        super.getHost().shutdown();
        GameThread.reset();
        replayThread.join();
    }

    /**
     * Executes the replay test.
     * @param replayTitle the title of the replay.
     * @param headless if the engine should be headless.
     */
    protected void runTest(String replayTitle, boolean headless) {
        this.isHeadless = headless;
        this.recordingTitle = replayTitle;
        replayThread.start();
        try {
            waitUntil(() -> (isInitialised() && getRecordAndReplayStatus() == RecordAndReplayStatus.REPLAYING));
            testOnReplayStart();
            testDuringReplay();
            waitUntil(() -> getRecordAndReplayStatus() == RecordAndReplayStatus.REPLAY_FINISHED);
            testOnReplayEnd();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests that are executed right after the replay is loaded.
     * @throws Exception
     */
    protected abstract void testOnReplayStart() throws Exception;

    /**
     * Tests that are executed when the replay is on execution.
     * @throws Exception
     */
    protected abstract void testDuringReplay() throws Exception;

    /**
     * Tests that are executed when the replay ends.
     * @throws Exception
     */
    protected abstract void testOnReplayEnd() throws Exception;

}
