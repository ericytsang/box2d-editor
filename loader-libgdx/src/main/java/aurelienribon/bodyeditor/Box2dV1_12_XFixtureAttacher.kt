package aurelienribon.bodyeditor

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape

/**
 * Attaches fixtures to your Box2D version 1.12 body.
 * You only need to give it a body and the corresponding fixture name, and it will attach these fixtures to your body.
 */
object Box2dV1_12_XFixtureAttacher
{
    /**
     * Creates and applies the fixtures defined in the editor. The name
     * parameter is used to retrieve the right fixture from the loaded file.
     * <br></br><br></br>
     *
     *
     * The body reference point (the red cross in the tool) is by default
     * located at the bottom left corner of the image. This reference point
     * will be put right over the BodyDef position point. Therefore, you should
     * place this reference point carefully to let you place your body in your
     * world easily with its BodyDef.position point. Note that to draw an image
     * at the position of your body, you will need to know this reference point
     * (see [.getOrigin].
     * <br></br><br></br>
     *
     *
     * Also, saved shapes are normalized. As shown in the tool, the width of
     * the image is considered to be always 1 meter. Thus, you need to provide
     * a scale factor so the polygons get resized according to your needs (not
     * every body is 1 meter large in your game, I guess).
     *
     * @param body  The Box2d body you want to attach the fixture to.
     * @param name  The name of the fixture you want to load.
     * @param fd    The fixture parameters to apply to the created body fixture.
     * @param scale The desired scale of the body. The default width is 1.
     */
    @JvmStatic
    fun attachFixture(
        loader:BodyEditorLoader,
        body:Body,
        name:String,
        fd:FixtureDef,
        scale:Float,
    )
    {
        val polygonShape = PolygonShape()
        val circleShape = CircleShape()
        loader.accept(
            name = name,
            scale = scale,
            visitor = Visitor(
                polygonShape = polygonShape,
                circleShape = circleShape,
                body = body,
                fd = fd,
            ),
        )
        polygonShape.dispose()
        circleShape.dispose()
    }

    private class Visitor(
        private val polygonShape:PolygonShape,
        private val circleShape:CircleShape,
        private val body:Body,
        private val fd:FixtureDef,
    ):BodyEditorLoader.ShapeVisitor
    {
        override fun visitPolygon(vertices:List<Vector2>)
        {
            polygonShape.set(vertices.toTypedArray())
            fd.shape = polygonShape
            body.createFixture(fd)
        }

        override fun visitCircle(center:Vector2,radius:Float)
        {
            circleShape.position.set(center)
            circleShape.radius = radius
            fd.shape = circleShape
            body.createFixture(fd)
        }
    }
}
