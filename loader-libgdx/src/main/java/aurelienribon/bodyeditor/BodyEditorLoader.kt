package aurelienribon.bodyeditor

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue

class VectorPool
{
    private val pool:MutableList<Vector2> = ArrayList()

    fun newVec():Vector2
    {
        return if (pool.isEmpty()) Vector2() else pool.removeAt(pool.size-1)
    }

    fun free(vec:Vector2)
    {
        pool.add(vec)
    }
}

class Box2dV1_12_XFixtureAttacher
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

    private inner class Visitor(
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

/**
 * Loads the collision fixtures defined with the Physics Body Editor application.
 * You only need to give it a body and the corresponding fixture name, and it will attach these fixtures to your body.
 */
class BodyEditorLoader(
    val model:ProjectModel,
)
{
    // Reusable stuff
    private val lockForReusableStuff = Any()
    private val vectorPool = VectorPool()

    constructor(file:FileHandle):this(readJson(file.readString()))

    constructor(str:String):this(readJson(str))

    fun accept(
        name:String,
        scale:Float,
        visitor:ShapeVisitor,
    ) = synchronized(lockForReusableStuff)
    {
        val rbModel:RigidBodyModel = getRigidBody(name)

        // TODO: Verify correct, updated method from mul to scl
        val origin = rbModel.origin.cpy().scl(scale)

        rbModel.polygons.forEach { polygon ->
            val vertices = polygon.vertices.map { vertex -> vertex.cpy().scl(scale).sub(origin) }
            visitor.visitPolygon(vertices)
            vertices.forEach { vectorPool.free(it) }
        }

        rbModel.circles.forEach { circle ->
            val center = circle.center.cpy().scl(scale).sub(origin)
            val radius = circle.radius*scale
            visitor.visitCircle(center, radius)
            vectorPool.free(center)
        }

        vectorPool.free(origin)
    }

    /**
     * Gets the image path attached to the given name.
     */
    fun getImagePath(name:String):String = getRigidBody(name).imagePath

    /**
     * Gets the origin point attached to the given name. Since the point is
     * normalized in [0,1] coordinates, it needs to be scaled to your body
     * size.
     */
    fun getOrigin(name:String,scale:Float):Vector2 = getRigidBody(name).origin.cpy().scl(scale)

    private fun XYModel.cpy():Vector2 = vectorPool.newVec().set(x,y)

    private fun getRigidBody(name:String):RigidBodyModel =
        model.rigidBodies[name] ?: error("Name '$name' was not found.")

    interface ShapeVisitor
    {
        fun visitCircle(center:Vector2,radius:Float)
        fun visitPolygon(vertices:List<Vector2>)
    }

    data class ProjectModel(
        val rigidBodies:Map<String,RigidBodyModel>,
    )

    class RigidBodyModel(
        val name:String,
        val imagePath:String,
        val origin:XYModel,
        val polygons:List<PolygonModel>,
        val circles:List<CircleModel>,
    )

    class PolygonModel(
        val vertices:List<XYModel>,
    )

    class CircleModel(
        val center:XYModel,
        val radius:Float,
    )

    class XYModel(
        val x:Float,
        val y:Float,
    )

    companion object
    {
        private fun readJson(str:String):ProjectModel = JsonReader().parse(str).readProject()

        private fun JsonValue.readProject():ProjectModel = ProjectModel(
            rigidBodies = get("rigidBodies").map { it.readRigidBody() }.associateBy { it.name },
        )

        private fun JsonValue.readRigidBody():RigidBodyModel = RigidBodyModel(
            name = get("name").asString(),
            imagePath = get("imagePath").asString(),
            origin = get("origin").readOrigin(),
            polygons = get("polygons").map { it.readPolygon() },
            circles = get("circles").map { it.readCircle() },
        )

        private fun JsonValue.readOrigin():XYModel = readXY(
            x = get("x"),
            y = get("y"),
        )

        private fun JsonValue.readPolygon():PolygonModel = PolygonModel(
            vertices = map { it.readVertex() },
        )

        private fun JsonValue.readVertex():XYModel = readXY(
            x = get("x"),
            y = get("y"),
        )

        private fun JsonValue.readCircle():CircleModel = CircleModel(
            center = readXY(
                x = get("cx"),
                y = get("cy"),
            ),
            radius = get("r").asFloat(),
        )

        private fun readXY(x:JsonValue,y:JsonValue):XYModel = XYModel(x.asFloat(),y.asFloat())
    }
}
