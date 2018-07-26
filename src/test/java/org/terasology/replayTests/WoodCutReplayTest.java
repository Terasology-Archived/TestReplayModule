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
package org.terasology.replayTests;

import org.junit.Ignore;
import org.junit.Test;
import org.terasology.ReplayTestingEnvironment;
import org.terasology.math.geom.Vector3i;
import org.terasology.recording.RecordAndReplayStatus;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.WorldProvider;

import static org.junit.Assert.assertEquals;

public class WoodCutReplayTest extends ReplayTestingEnvironment {

    private Thread replayThread = new Thread() {

        @Override
        public void run() {
        try {
            String replayTitle = "Woodcut";
            WoodCutReplayTest.super.openReplay(replayTitle);
        } catch (Exception e) {
            e.printStackTrace();
        }
        }
    };

    @Ignore("These are headed tests and should be ignored by Jenkins.")
    @Test
    public void testWoodcut() {
        replayThread.start();
        try {
            while (!isInitialised() || getRecordAndReplayStatus() != RecordAndReplayStatus.REPLAYING) {
                Thread.sleep(1000); //wait for the replay to finish prepearing things before we get the data to test things.
            }
            Vector3i blockLocation1 = new Vector3i(-73, 43, 84);
            Vector3i blockLocation2 = new Vector3i(-73, 44, 84);

            //checks the block initial type of two chunks that will be modified during the replay.
            WorldProvider worldProvider = CoreRegistry.get(WorldProvider.class);
            assertEquals(worldProvider.getBlock(blockLocation1).getDisplayName(), "Oak Log");
            assertEquals(worldProvider.getBlock(blockLocation2).getDisplayName(), "Oak Log");

            while (getRecordAndReplayStatus() != RecordAndReplayStatus.REPLAY_FINISHED) {
                Thread.sleep(1000);
            }//The replay is finished at this point

            //checks the same blocks again after the replay.
            assertEquals(worldProvider.getBlock(blockLocation1).getDisplayName(), "Air");
            assertEquals(worldProvider.getBlock(blockLocation2).getDisplayName(), "Air");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
