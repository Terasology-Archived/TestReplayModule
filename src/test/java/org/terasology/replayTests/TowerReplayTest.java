package org.terasology.replayTests;

import org.junit.After;
import org.junit.Test;
import org.terasology.ReplayTestingEnvironment;
import org.terasology.TestUtils;
import org.terasology.engine.GameThread;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.internal.EventSystem;
import org.terasology.logic.location.LocationComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.math.geom.Vector3f;
import org.terasology.recording.EventSystemReplayImpl;
import org.terasology.recording.RecordAndReplayStatus;
import org.terasology.registry.CoreRegistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TowerReplayTest {

    private ReplayTestingEnvironment environment = new ReplayTestingEnvironment();

    private Thread replayThread = new Thread() {

        @Override
        public void run() {
            try {
                String replayTitle = "Tower";
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
    public void testTower() {
        replayThread.start();

        TestUtils.waitUntil(() -> (environment.isInitialised() && environment.getRecordAndReplayStatus() == RecordAndReplayStatus.REPLAYING));
        LocalPlayer localPlayer = CoreRegistry.get(LocalPlayer.class);
        TestUtils.waitUntil(() -> localPlayer.isValid()); //waits for the local player to be loaded

        EntityRef character = localPlayer.getCharacterEntity();
        Vector3f initialPosition = new Vector3f(0.0f, 37.409996f, 0.0f);
        LocationComponent location = character.getComponent(LocationComponent.class);
        assertEquals(initialPosition, location.getLocalPosition()); // check initial position.

        EventSystemReplayImpl eventSystem = (EventSystemReplayImpl) CoreRegistry.get(EventSystem.class);
        TestUtils.waitUntil(() -> eventSystem.getLastRecordedEventIndex() >= 500);
        location = character.getComponent(LocationComponent.class);
        assertNotEquals(initialPosition, location.getLocalPosition()); // checks that the player is not on the initial position after they moved in the middle of the replay
        TestUtils.waitUntil(() -> environment.getRecordAndReplayStatus() == RecordAndReplayStatus.REPLAY_FINISHED);

        location = character.getComponent(LocationComponent.class);
        Vector3f finalPosition = new Vector3f(-0.020252455f, 38.400967f, 2.5305471f);
        assertEquals(finalPosition, location.getLocalPosition()); // checks final position
    }
}
