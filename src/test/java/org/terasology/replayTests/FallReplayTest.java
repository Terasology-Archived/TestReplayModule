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

import org.junit.After;
import org.junit.Test;
import org.terasology.AcceptanceTestEnvironment;
import org.terasology.ReplayTestingEnvironment;
import org.terasology.TestUtils;
import org.terasology.engine.GameThread;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.health.HealthComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.math.geom.Vector3i;
import org.terasology.recording.RecordAndReplayStatus;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.WorldProvider;

import static org.junit.Assert.assertEquals;

public class FallReplayTest{
    private ReplayTestingEnvironment environment = new ReplayTestingEnvironment();

    private Thread replayThread = new Thread() {

        @Override
        public void run() {
            try {
                String replayTitle = "DirtsTest";
                environment.openReplay(replayTitle, true);
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

    @Test
    public void testFallToDeath(){

        replayThread.start();

        LocalPlayer localPlayer = CoreRegistry.get(LocalPlayer.class);
        TestUtils.waitUntil(() -> (environment.isInitialised() && environment.getRecordAndReplayStatus() == RecordAndReplayStatus.REPLAYING && localPlayer.isValid()));
        EntityRef character = localPlayer.getCharacterEntity();

        HealthComponent healthComponent = character.getComponent(HealthComponent.class);
        assertEquals(20, healthComponent.currentHealth);

        TestUtils.waitUntil(() -> environment.getRecordAndReplayStatus() == RecordAndReplayStatus.REPLAY_FINISHED);

        //checks the same blocks again after the replay.
        assertEquals(0, healthComponent.currentHealth);

    }
}
