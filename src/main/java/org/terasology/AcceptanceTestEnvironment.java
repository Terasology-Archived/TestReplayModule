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
import org.terasology.engine.GameThread;
import org.terasology.recording.RecordAndReplayStatus;

/**
 * An environment that extends {@link ReplayTestingEnvironment} and sets the workflow of a replay test used for
 * acceptance testing.
 * <p>
 * To write tests that uses this class, it is necessary to extend it, write implementations for the abstract methods and
 * write a test method that calls {@link #runTest(String, boolean)}. For more information about the abstract methods and
 * example of implementation, check their JavaDoc and the ExampleAcceptanceTest class.
 */
public abstract class AcceptanceTestEnvironment {

    private String recordingTitle;
    private boolean isHeadless;
    private ReplayTestingEnvironment environment = new ReplayTestingEnvironment();

    /** To test the replay while it is executing, it is necessary to create a thread that will run the replay. */
    private Thread replayThread = new Thread() {

        @Override
        public void run() {
            try {
                String replayTitle = recordingTitle;
                environment.openReplay(replayTitle, isHeadless);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    };

    @After
    public void closeReplay() throws Exception {
        environment.getHost().shutdown();
        GameThread.reset();
        replayThread.join();
    }

    /**
     * Executes the replay test.
     * @param replayTitle the title of the replay.
     * @param headless if the engine should be headless.
     */
    protected void runTest(String replayTitle, boolean headless) throws Exception {
        this.isHeadless = headless;
        this.recordingTitle = replayTitle;
        replayThread.start();
        TestUtils.waitUntil(() -> (environment.isInitialised() && environment.getRecordAndReplayStatus() == RecordAndReplayStatus.REPLAYING));
        testOnReplayStart();
        testDuringReplay();
        TestUtils.waitUntil(() -> environment.getRecordAndReplayStatus() == RecordAndReplayStatus.REPLAY_FINISHED);
        testOnReplayEnd();
    }

    /**
     * @return the ReplayTestingEnvironment that creates and executes a TerasologyEngine.
     */
    protected ReplayTestingEnvironment getEnvironment() {
        return this.environment;
    }

    /**
     * This method is executed by {@link #runTest(String, boolean)} right after the record and replay status is set to
     * REPLAYING, right after the replay is loaded. Therefore, this method should contain tests that checks values in the
     * beginning of a replay, such as the player's initial position. Example:
     * <pre>   {@code
     * LocalPlayer localPlayer = CoreRegistry.get(LocalPlayer.class);
     * TestUtils.waitUntil(() -> localPlayer.isValid());
     * character = localPlayer.getCharacterEntity();
     * initialPosition = new Vector3f(19.79358f, 13.511584f, 2.3982882f);
     * LocationComponent location = character.getComponent(LocationComponent.class);
     * assertEquals(initialPosition, location.getLocalPosition()); // check initial position.
     * }</pre>
     * If desired, this method can be left in blank.
     * @throws Exception
     */
    protected abstract void testOnReplayStart() throws Exception;

    /**
     * This method is executed by {@link #runTest(String, boolean)} right after {@link #testOnReplayStart()} is called,
     * which means that the replay status is REPLAYING. This method should contain "checkpoints" created with "waitUntil"
     * to check some values after certain events are sent during a replay. Example:
     * <pre>   {@code
     * EventSystemReplayImpl eventSystem = (EventSystemReplayImpl) CoreRegistry.get(EventSystem.class);
     * TestUtils.waitUntil(() -> eventSystem.getLastRecordedEventIndex() >= 1810); // tests in the middle of a replay needs "checkpoints" like this.
     * LocationComponent location = character.getComponent(LocationComponent.class);
     * assertNotEquals(initialPosition, location.getLocalPosition()); // checks that the player is not on the initial position after they moved.
     * }</pre>
     * If desired, this method can be left in blank.
     * @throws Exception
     */
    protected abstract void testDuringReplay() throws Exception;

    /**
     * This method is executed by {@link #runTest(String, boolean)} right after the record and replay status is set to
     * REPLAY_FINISHED, right after the replay ends. Therefore, this method should contain tests that checks values in the
     * end of a replay, such as the player's final position. Example:
     * <pre>   {@code
     * LocationComponent location = character.getComponent(LocationComponent.class);
     * Vector3f finalPosition = new Vector3f(25.189344f, 13.406443f, 8.6651945f);
     * assertEquals(finalPosition, location.getLocalPosition()); // checks final position
     * }</pre>
     * If desired, this method can be left in blank.
     * @throws Exception
     */
    protected abstract void testOnReplayEnd() throws Exception;

}
