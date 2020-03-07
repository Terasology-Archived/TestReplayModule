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

import com.google.common.collect.Lists;
import org.terasology.config.Config;
import org.terasology.engine.TerasologyEngine;
import org.terasology.engine.TerasologyEngineBuilder;
import org.terasology.engine.modes.StateLoading;
import org.terasology.engine.modes.StateMainMenu;
import org.terasology.engine.paths.PathManager;
import org.terasology.engine.subsystem.common.hibernation.HibernationSubsystem;
import org.terasology.engine.subsystem.config.BindsSubsystem;
import org.terasology.engine.subsystem.headless.HeadlessAudio;
import org.terasology.engine.subsystem.headless.HeadlessGraphics;
import org.terasology.engine.subsystem.headless.HeadlessInput;
import org.terasology.engine.subsystem.headless.HeadlessTimer;
import org.terasology.engine.subsystem.lwjgl.LwjglAudio;
import org.terasology.engine.subsystem.lwjgl.LwjglGraphics;
import org.terasology.engine.subsystem.lwjgl.LwjglInput;
import org.terasology.engine.subsystem.lwjgl.LwjglTimer;
import org.terasology.engine.subsystem.openvr.OpenVRInput;
import org.terasology.game.GameManifest;
import org.terasology.network.NetworkMode;
import org.terasology.recording.RecordAndReplayCurrentStatus;
import org.terasology.recording.RecordAndReplayStatus;
import org.terasology.recording.RecordAndReplayUtils;
import org.terasology.registry.CoreRegistry;
import org.terasology.rendering.nui.layers.mainMenu.savedGames.GameInfo;
import org.terasology.rendering.nui.layers.mainMenu.savedGames.GameProvider;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * A base class for tests involving a full {@link TerasologyEngine} instance that runs a replay. For a better understanding
 * of how to use this class, check the wiki page https://github.com/MovingBlocks/Terasology/wiki/Replay-Tests and also
 * the class ExampleReplayTest.
 * <p>
 * This class can initialize an engine through two methods: {@link #openMainMenu()} or {@link #openReplay(String, boolean)}.
 * The openMainMenu() method initializes a headed engine and opens the game in the main menu, while the openReplay(String, boolean)
 * initialises an engine and right after its initialisation, opens a replay. The engine of the later can be either headed
 * or headless.
 * <p>
 * <h2>Example of Usage<h2/> Generally this class is an attribute of a ReplayTest, and it is used to create an engine and
 * run a replay inside a thread: <pre>   {@code
 *     private ReplayTestingEnvironment environment = new ReplayTestingEnvironment();
 *
 *     private Thread replayThread = new Thread() {
 *
 *         @Override
 *         public void run() {
 *             try {
 *                 String replayTitle = "Example";
 *                 environment.openReplay(replayTitle, true);
 *             } catch (Exception e) {
 *                 throw new RuntimeException(e);
 *             }
 *         }
 *     };
 * }</pre>
 *
 * The thread is used inside a test method to run the game. Besides running the game, this class is important to replay
 * tests since it contains the Record and Replay current status that can be obtained through {@link #getRecordAndReplayStatus()},
 * which is used to know in which state the Replay is, so checks can be done on different stages of a replay. Checks are
 * usually done right after the Replay Status changes from PREPEARING_REPLAY to REPLAYING, during the duration of the
 * REPLAYING status and also when the replay ends and the status is set to REPLAY_FINISHED. Example:
 *
 * <pre>   {@code
 *
 *     @Test
 *     public void testExampleRecordingPlayerPosition() {
 *         replayThread.start();
 *
 *         TestUtils.waitUntil(() -> (environment.isInitialised() && environment.getRecordAndReplayStatus() == RecordAndReplayStatus.REPLAYING));
 *         LocalPlayer localPlayer = CoreRegistry.get(LocalPlayer.class);
 *         TestUtils.waitUntil(() -> localPlayer.isValid()); //waits for the local player to be loaded
 *
 *         EntityRef character = localPlayer.getCharacterEntity();
 *         Vector3f initialPosition = new Vector3f(19.79358f, 13.511584f, 2.3982882f);
 *         LocationComponent location = character.getComponent(LocationComponent.class);
 *         assertEquals(initialPosition, location.getLocalPosition()); // check initial position.
 *
 *         EventSystemReplayImpl eventSystem = (EventSystemReplayImpl) CoreRegistry.get(EventSystem.class);
 *         TestUtils.waitUntil(() -> eventSystem.getLastRecordedEventIndex() >= 1810); // tests in the middle of a replay needs "checkpoints" like this.
 *         location = character.getComponent(LocationComponent.class);
 *         assertNotEquals(initialPosition, location.getLocalPosition()); // checks that the player is not on the initial position after they moved.
 *         TestUtils.waitUntil(() -> environment.getRecordAndReplayStatus() == RecordAndReplayStatus.REPLAY_FINISHED);
 *
 *         location = character.getComponent(LocationComponent.class);
 *         Vector3f finalPosition = new Vector3f(25.189344f, 13.406443f, 8.6651945f);
 *         assertEquals(finalPosition, location.getLocalPosition()); // checks final position
 *     }
 *
 * }</pre>
 */
public class ReplayTestingEnvironment {
    private TerasologyEngine host;
    private List<TerasologyEngine> engines = Lists.newArrayList();
    private RecordAndReplayCurrentStatus recordAndReplayCurrentStatus;
    private boolean isInitialised;

    /**
     * Opens the game in the Main Menu.
     * @throws Exception
     */
    public void openMainMenu() throws Exception {
        host = createEngine(false);
        host.run(new StateMainMenu());
    }

    /**
     * Creates a headless or headed {@link TerasologyEngine} and uses it to open the game in a replay state.
     * It is important to know that Jenkins cannot execute tests that uses a headed engine.
     * @param replayTitle the title of the replay to be opened.
     * @param isHeadless if the engine should be headless.
     * @throws Exception
     */
    public void openReplay(String replayTitle, boolean isHeadless) throws Exception {
        host = createEngine(isHeadless);
        host.initialize();
        this.isInitialised = true;
        recordAndReplayCurrentStatus = host.getFromEngineContext(RecordAndReplayCurrentStatus.class);
        host.changeState(new StateMainMenu());
        host.tick();
        loadReplay(replayTitle);
        mainLoop();
        host.cleanup();
        engines = Lists.newArrayList();
        host = null;
        this.isInitialised = false;
    }

    /**
     * Load a replay while setting the RecordAndReplayStatus.
     * @param replayTitle the name of the replay to be loaded.
     * @throws Exception
     */
    private void loadReplay(String replayTitle) throws Exception {
        recordAndReplayCurrentStatus.setStatus(RecordAndReplayStatus.PREPARING_REPLAY);
        GameInfo replayInfo = getReplayInfo(replayTitle);
        GameManifest manifest = replayInfo.getManifest();
        CoreRegistry.get(RecordAndReplayUtils.class).setGameTitle(manifest.getTitle());
        Config config = CoreRegistry.get(Config.class);
        config.getWorldGeneration().setDefaultSeed(manifest.getSeed());
        config.getWorldGeneration().setWorldTitle(manifest.getTitle());
        host.changeState(new StateLoading(manifest, NetworkMode.NONE));
    }

    /**
     * The game Main Loop where most of the processing time will be spent on.
     */
    private void mainLoop() {
        while (host.tick()) {
            //do nothing;
        }
    }

    /**
     * Creates a full headed or headless TerasologyEngine. The homePath for this engine is the module's "assets" folder.
     * @param isHeadless if the engine should be headless.
     * @return the created engine.
     * @throws Exception
     */
    private TerasologyEngine createEngine(boolean isHeadless) throws Exception {
        TerasologyEngineBuilder builder = new TerasologyEngineBuilder();
        if (isHeadless) {
            populateHeadlessSubsystems(builder);
        } else {
            populateHeadedSubsystems(builder);
        }
        Path homePath = Paths.get("modules/TestReplayModule/assets");
        PathManager.getInstance().useOverrideHomePath(homePath);
        TerasologyEngine engine = builder.build();
        engines.add(engine);
        return engine;
    }

    /**
     * Populates the engine builder with headed subsystems.
     * @param builder the builder to be populated.
     */
    private void populateHeadedSubsystems(TerasologyEngineBuilder builder) {
        builder.add(new LwjglAudio())
                .add(new LwjglGraphics())
                .add(new LwjglTimer())
                .add(new LwjglInput())
                .add(new BindsSubsystem())
                .add(new OpenVRInput());

        builder.add(new HibernationSubsystem());
    }

    /**
     * Populates the engine builder with headless subsystems.
     * @param builder the builder to be populated.
     */
    private void populateHeadlessSubsystems(TerasologyEngineBuilder builder) {
        builder.add(new HeadlessGraphics())
                .add(new HeadlessTimer())
                .add(new HeadlessAudio())
                .add(new HeadlessInput());

        builder.add(new HibernationSubsystem());
    }

    /**
     * Searches the 'recordings' folder for a selected recording and returns its GameInfo if found.
     * @param title the title of the recording.
     * @return the GameInfo of the selected recording.
     * @throws Exception
     */
    private GameInfo getReplayInfo(String title) throws Exception {
        List<GameInfo> recordingsInfo = GameProvider.getSavedRecordings();
        for (GameInfo info : recordingsInfo) {
            if (title.equals(info.getManifest().getTitle())) {
                return info;
            }
        }
        throw new Exception("No replay found with this title: " + title);
    }

    public TerasologyEngine getHost() {
        return this.host;
    }

    public RecordAndReplayStatus getRecordAndReplayStatus() {
        return this.recordAndReplayCurrentStatus.getStatus();
    }

    public boolean isInitialised() {
        return this.isInitialised;
    }
}
