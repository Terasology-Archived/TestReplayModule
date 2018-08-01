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
package org.terasology.replayTests.examples.templates;


import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.terasology.ReplayTestingEnvironment;
import org.terasology.engine.GameThread;
import org.terasology.recording.RecordAndReplayStatus;

import java.util.function.BooleanSupplier;


/**
 * This is a template with comments to aid in the creation of a replay test.
 * For more information about Replay Tests, see https://github.com/MovingBlocks/Terasology/wiki/Replay-Tests
 * Replay tests should extend ReplayTestingEnvironment directly or indirectly.
 */
public class ReplayTestTemplate extends ReplayTestingEnvironment {

    /** To test the replay while it is executing, it is necessary to create a thread that will run the replay. */
    private Thread replayThread = new Thread() {

        @Override
        public void run() {
            try {
                //This is the title of the replay to be played. It is generally the name of the folder in the 'recordings' directory.
                String replayTitle = "REPLAY_TITLE";

                /*
                This opens the game and execute the replay desired for testing. It is always
                'TEST_CLASS_NAME.super.openReplay'. The first parameter of this method is the replay title,
                and the second one is if the replay should be headless.
                 */
                ReplayTestTemplate.super.openReplay(replayTitle, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @After
    public void closeReplay() throws Exception {
        //these last three lines are important to correctly shutdown the game after the tests are done.
        super.getHost().shutdown();
        GameThread.reset();
        replayThread.join();
    }

    /**
     * Template of a method that tests a Replay.
     * <p>
     * <h2>Code Explanation<h2/> Replay Tests should always start the thread that will initialise the game and run the
     * replay. After that, a try-catch block should be opened and inside it there should be two important calls to
     * {@link #waitUntil(BooleanSupplier)}. The first call waits for the replay to be loaded, and checks that tests
     * something in the beginning of a replay should be written right after it. The second call waits for the replay to
     * end, therefore checks that test something in the end of a replay should be written right after it. It is also
     * possible to test something in the middle of a replay, but for that it is necessary to put "checkpoints" with
     * {@link #waitUntil(BooleanSupplier)} between the two {@link #waitUntil(BooleanSupplier)} for that.
     * <p>
     * <h2>Example of Test in the middle of a Replay<h2/>
     * EventSystemReplayImpl eventSystem = (EventSystemReplayImpl) CoreRegistry.get(EventSystem.class);
     * waitUntil(() -> eventSystem.getLastRecordedEventIndex() >= 1810);
     * location = character.getComponent(LocationComponent.class);
     * assertNotEquals(initialPosition, location.getLocalPosition());
     */
    @Ignore("This is just a template and should be ignored by Jenkins.")
    @Test
    public void testTemplate() {
        replayThread.start();
        try {
            waitUntil(() -> (isInitialised() && getRecordAndReplayStatus() == RecordAndReplayStatus.REPLAYING));

            waitUntil(() -> getRecordAndReplayStatus() == RecordAndReplayStatus.REPLAY_FINISHED);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
