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
package org.terasology.replayTests.examples;

import org.junit.Test;
import org.terasology.AcceptanceTestEnvironment;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.internal.EventSystem;
import org.terasology.logic.location.LocationComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.math.geom.Vector3f;
import org.terasology.recording.EventSystemReplayImpl;
import org.terasology.registry.CoreRegistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * This is an example of acceptance test that uses a Replay and the AcceptanceTestEnvironment.
 */
public class ExampleAcceptanceTest extends AcceptanceTestEnvironment {

    private EntityRef character;
    private Vector3f initialPosition;

    @Test
    @Override
    public void run() {
        runTest("Example", true);
    }

    @Override
    protected void testOnReplayStart() throws Exception {
        LocalPlayer localPlayer = CoreRegistry.get(LocalPlayer.class);
        waitUntil(() -> localPlayer.isValid());
        character = localPlayer.getCharacterEntity();
        initialPosition = new Vector3f(19.79358f, 13.511584f, 2.3982882f);
        LocationComponent location = character.getComponent(LocationComponent.class);
        assertEquals(initialPosition, location.getLocalPosition()); // check initial position.

    }

    @Override
    protected void testDuringReplay() throws Exception {
        EventSystemReplayImpl eventSystem = (EventSystemReplayImpl) CoreRegistry.get(EventSystem.class);
        waitUntil(() -> eventSystem.getLastRecordedEventIndex() >= 1810); // tests in the middle of a replay needs "checkpoints" like this.
        LocationComponent location = character.getComponent(LocationComponent.class);
        assertNotEquals(initialPosition, location.getLocalPosition()); // checks that the player is not on the initial position after they moved.
    }

    @Override
    protected void testOnReplayEnd() throws Exception {
        LocationComponent location = character.getComponent(LocationComponent.class);
        Vector3f finalPosition = new Vector3f(25.189344f, 13.406443f, 8.6651945f);
        assertEquals(finalPosition, location.getLocalPosition()); // checks final position
    }
}
