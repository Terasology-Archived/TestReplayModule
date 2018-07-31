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
import org.terasology.recording.RecordAndReplayStatus;


/**
 * This is a template with comments to aid in the creation of a replay test.
 * For more information about Replay Tests, see https://github.com/MovingBlocks/Terasology/wiki/Replay-Tests
 */
public class ReplayTestTemplate extends ReplayTestingEnvironment { //Replay tests should extend ReplayTestingEnvironment

    /*
     * To test the replay while it is executing, it is necessary to create threads which will run the replays.
     */
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
        //these last two lines are important to correctly shutdown the game after the tests are done.
        super.getHost().shutdown();
        replayThread.join();
    }

    @Ignore("This is just a template and should be ignored by Jenkins.")
    @Test
    public void testTemplate() {
        replayThread.start(); //always start the thread before the test, so the replay can execute.
        try {

            /*
            This 'waitUntil' is useful because when it is over it means the replay was loaded, which means it
            is possible to test the replay's initial state, such as the player character's initial position.
             */
            waitUntil(() -> (isInitialised() && getRecordAndReplayStatus() == RecordAndReplayStatus.REPLAYING));
            //the checks of initial states should be written here, between the 'waitUntil' statements.

            //to test things in the middle of a replay, checks can be written between the 'waitUntils'
            //Example of a test in the middle of a replay:
            /*
            EventSystemReplayImpl eventSystem = (EventSystemReplayImpl) CoreRegistry.get(EventSystem.class);
            waitUntil(() -> eventSystem.getLastRecordedEventIndex() >= 1810);
            location = character.getComponent(LocationComponent.class);
            assertNotEquals(initialPosition, location.getLocalPosition());
             */

            waitUntil(() -> getRecordAndReplayStatus() == RecordAndReplayStatus.REPLAY_FINISHED);
            //tests can be written here to test something at the end of a replay.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
