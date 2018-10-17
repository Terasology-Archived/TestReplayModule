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

import org.junit.Test;
import org.terasology.AcceptanceTestEnvironment;
import org.terasology.TestUtils;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.internal.EventSystem;
import org.terasology.logic.location.LocationComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.math.geom.Vector3f;
import org.terasology.recording.EventSystemReplayImpl;
import org.terasology.registry.CoreRegistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class CuttingGrassPlayerLocationAcceptanceTest extends AcceptanceTestEnvironment {

    private EntityRef character;
    private Vector3f initialPosition;

    @Test
    public void run() throws Exception {
        runTest("CuttingGrass", true);
    }

    @Override
    protected void testOnReplayStart() throws Exception {
        LocalPlayer localPlayer = CoreRegistry.get(LocalPlayer.class);
        TestUtils.waitUntil(() -> localPlayer.isValid());
        character = localPlayer.getCharacterEntity();
        initialPosition = new Vector3f(0.0f, 1.3f, 0.0f);
        LocationComponent location = character.getComponent(LocationComponent.class);
        assertEquals(initialPosition, location.getLocalPosition()); // check initial position.
    }

    @Override
    protected void testDuringReplay() throws Exception {
        EventSystemReplayImpl eventSystem = (EventSystemReplayImpl) CoreRegistry.get(EventSystem.class);
        TestUtils.waitUntil(() -> eventSystem.getLastRecordedEventIndex() >= 100); // tests in the middle of a replay needs "checkpoints" like this.
        LocationComponent location = character.getComponent(LocationComponent.class);
        assertNotEquals(initialPosition, location.getLocalPosition()); // checks that the player is not on the initial position after they moved.
    }

    @Override
    protected void testOnReplayEnd() throws Exception {
        LocationComponent location = character.getComponent(LocationComponent.class);
        Vector3f finalPosition = new Vector3f(0.0f, 0.4099998f, 0.0f);
        assertEquals(finalPosition, location.getLocalPosition()); // checks final position
    }
}
