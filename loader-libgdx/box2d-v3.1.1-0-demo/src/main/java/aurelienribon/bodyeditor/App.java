package aurelienribon.bodyeditor;

import static aurelienribon.bodyeditor.BodyEditorLoaderKt.xyModel;
import static aurelienribon.bodyeditor.ExtensionsKt.radians;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.box2d.Box2d;
import com.badlogic.gdx.box2d.enums.b2BodyType;
import com.badlogic.gdx.box2d.structs.b2BodyDef;
import com.badlogic.gdx.box2d.structs.b2BodyId;
import com.badlogic.gdx.box2d.structs.b2Circle;
import com.badlogic.gdx.box2d.structs.b2Polygon;
import com.badlogic.gdx.box2d.structs.b2ShapeDef;
import com.badlogic.gdx.box2d.structs.b2Vec2;
import com.badlogic.gdx.box2d.structs.b2WorldDef;
import com.badlogic.gdx.box2d.structs.b2WorldId;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.Random;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.TweenManager;

public class App extends ApplicationAdapter {

    // -------------------------------------------------------------------------
    // Static fields
    // -------------------------------------------------------------------------

    private static final float VIEWPORT_WIDTH = 10;
    private static final float BOTTLE_WIDTH = 8;
    private static final float BALL_RADIUS = 0.15f;
    private static final int MAX_BALLS = 200;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    // Models
    private b2WorldId worldId;
    private b2BodyId bottleModel;
    private Vector2 bottleModelOrigin;
    private b2BodyId[] ballModels;

    // Render
    private Texture bottleTexture;
    private Sprite bottleSprite;
    private Texture ballTexture;
    private Sprite[] ballSprites;
    private Sprite groundSprite;

    // Render general
    private SpriteBatch batch;
    private BitmapFont font;
    private OrthographicCamera camera;

    // Misc
    private final TweenManager tweenManager = new TweenManager();
    private final Random rand = new Random();

    @Override
    public void create() {
        // Models initialization

        Box2d.initialize();
        b2WorldDef worldDef = Box2d.b2DefaultWorldDef();
        worldDef.gravity().x(0.0f);
        worldDef.gravity().y(-10.0f);
        worldId = Box2d.b2CreateWorld(worldDef.asPointer());
        createGround();
        createBottle(); // <-- this method uses the BodyEditorLoader class
        createBalls();

        // Render initialization

        batch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.BLACK);

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        camera = new OrthographicCamera(VIEWPORT_WIDTH, VIEWPORT_WIDTH * h / w);
        camera.position.set(0, camera.viewportHeight / 2, 0);
        camera.update();

        createSprites();

        // Input initialization

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int x, int y, int pointer, int button) {
                restart();
                return true;
            }
        });

        // Run

        restart();
    }

    private void createGround() {
        b2BodyDef bd = Box2d.b2DefaultBodyDef();
        bd.position().x(0.0f);
        bd.position().y(0.0f);
        bd.type(b2BodyType.b2_staticBody);

        b2Polygon shape = Box2d.b2MakeBox(VIEWPORT_WIDTH, 1.0f);

        b2ShapeDef fd = Box2d.b2DefaultShapeDef();
        fd.density(1.0f);
        fd.material().friction(0.5f);
        fd.material().restitution(0.5f);

        b2BodyId groundBody = Box2d.b2CreateBody(worldId, bd.asPointer());
        Box2d.b2CreatePolygonShape(groundBody, fd.asPointer(), shape.asPointer());
    }

    private void createBottle() {
        // 0. Create a loader for the file saved from the editor.
        BodyEditorLoader loader = BodyEditorLoader.fromFile(Gdx.files.internal("data/test.json"));

        // 1. Create a BodyDef, as usual.
        b2BodyDef bd = Box2d.b2DefaultBodyDef();
        bd.type(b2BodyType.b2_dynamicBody);

        // 2. Create a FixtureDef, as usual.
        b2ShapeDef fd = Box2d.b2DefaultShapeDef();
        fd.density(1.0f);
        fd.material().friction(0.5f);
        fd.material().restitution(0.3f);

        // 3. Create a Body, as usual.
        bottleModel = Box2d.b2CreateBody(worldId, bd.asPointer());

        // 4. Create the body fixture automatically by using the loader.
        Box2dV3_1_1_0XFixtureAttacher.attachFixture(loader, bottleModel, "test01", fd, xyModel(BOTTLE_WIDTH));
        bottleModelOrigin = loader.getOrigin("test01", xyModel(BOTTLE_WIDTH));
    }

    private void createBalls() {
        b2BodyDef ballBodyDef = Box2d.b2DefaultBodyDef();
        ballBodyDef.type(b2BodyType.b2_dynamicBody);

        b2Circle shape = new b2Circle();
        shape.radius(BALL_RADIUS);

        b2ShapeDef fd = Box2d.b2DefaultShapeDef();
        fd.density(1.0f);
        fd.material().friction(0.5f);
        fd.material().restitution(0.5f);

        ballModels = new b2BodyId[MAX_BALLS];
        for (int i = 0; i < MAX_BALLS; i++) {
            b2BodyId ballBody = Box2d.b2CreateBody(worldId, ballBodyDef.asPointer());
            Box2d.b2CreateCircleShape(ballBody, fd.asPointer(), shape.asPointer());
            ballModels[i] = ballBody;
        }
    }

    private void createSprites() {
        bottleTexture = new Texture(Gdx.files.internal("data/gfx/bottle.png"));
        bottleTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        bottleSprite = new Sprite(bottleTexture);
        bottleSprite.setSize(BOTTLE_WIDTH, BOTTLE_WIDTH * bottleSprite.getHeight() / bottleSprite.getWidth());

        ballTexture = new Texture(Gdx.files.internal("data/gfx/ball.png"));
        ballTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        ballSprites = new Sprite[MAX_BALLS];
        for (int i = 0; i < MAX_BALLS; i++) {
            ballSprites[i] = new Sprite(ballTexture);
            ballSprites[i].setSize(BALL_RADIUS * 2, BALL_RADIUS * 2);
            ballSprites[i].setOrigin(BALL_RADIUS, BALL_RADIUS);
        }

        Texture whiteTexture = new Texture(Gdx.files.internal("data/gfx/white.png"));

        groundSprite = new Sprite(whiteTexture);
        groundSprite.setSize(VIEWPORT_WIDTH, 1);
        groundSprite.setPosition(-VIEWPORT_WIDTH / 2, 0);
        groundSprite.setColor(Color.BLACK);
    }

    @Override
    public void dispose() {
        bottleTexture.dispose();
        ballTexture.dispose();
        batch.dispose();
        font.dispose();
        Box2d.b2DestroyWorld(worldId);
    }

    @Override
    public void render() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // Update
        tweenManager.update(1 / 60f);
        Box2d.b2World_Step(worldId, 1 / 60f, 10);

        b2Vec2 bottlePos = Box2d.b2Body_GetPosition(bottleModel);
        bottleSprite.setPosition(bottlePos.x()-bottleModelOrigin.x, bottlePos.y()-bottleModelOrigin.y);
        bottleSprite.setOrigin(bottleModelOrigin.x, bottleModelOrigin.y);
        bottleSprite.setRotation(radians(Box2d.b2Body_GetRotation(bottleModel)) * MathUtils.radiansToDegrees);

        for (int i = 0; i < MAX_BALLS; i++) {
            b2Vec2 ballPos = Box2d.b2Body_GetPosition(ballModels[i]);
            ballSprites[i].setPosition(ballPos.x() - ballSprites[i].getWidth() / 2, ballPos.y() - ballSprites[i].getHeight() / 2);
            ballSprites[i].setRotation(radians(Box2d.b2Body_GetRotation(ballModels[i])) * MathUtils.radiansToDegrees);
        }

        // Render
        GL20 gl = Gdx.gl20;
        gl.glClearColor(1, 1, 1, 1);
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        groundSprite.draw(batch);
        bottleSprite.draw(batch);
        for (int i = 0; i < MAX_BALLS; i++) ballSprites[i].draw(batch);
        batch.end();

        batch.getProjectionMatrix().setToOrtho2D(0, 0, w, h);
        batch.begin();
        font.draw(batch, "Touch the screen to restart", 5, h - 5);
        batch.end();
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private void restart() {
        Box2d.b2Body_SetTransform(bottleModel, createB2Vec2(0, 3), Box2d.b2MakeRot(0.2f));
        Box2d.b2Body_SetLinearVelocity(bottleModel, createB2Vec2(0, 0));
        Box2d.b2Body_SetAngularVelocity(bottleModel, 0);

        for (int i = 0; i < MAX_BALLS; i++) {
            float tx = rand.nextFloat() - 0.5f;
            float ty = camera.position.y + camera.viewportHeight / 2 + BALL_RADIUS;
            float angle = rand.nextFloat() * MathUtils.PI * 2;

            Box2d.b2Body_Disable(ballModels[i]);
            Box2d.b2Body_SetLinearVelocity(ballModels[i], createB2Vec2(0, 0));
            Box2d.b2Body_SetAngularVelocity(ballModels[i], 0);
            Box2d.b2Body_SetTransform(ballModels[i], createB2Vec2(tx, ty), Box2d.b2MakeRot(angle));
        }

        tweenManager.killAll();

        Tween.call(new TweenCallback() {
            private int idx = 0;

            @Override
            public void onEvent(int type, BaseTween<?> source) {
                if (idx < ballModels.length) {
                    Box2d.b2Body_SetAwake(ballModels[idx], true);
                    Box2d.b2Body_Enable(ballModels[idx]);
                    idx += 1;
                }
            }
        }).repeat(-1, 0.1f).start(tweenManager);
    }

    private b2Vec2 createB2Vec2(float x, float y) {
        b2Vec2 vec = new b2Vec2();
        vec.x(x);
        vec.y(y);
        return vec;
    }
}
