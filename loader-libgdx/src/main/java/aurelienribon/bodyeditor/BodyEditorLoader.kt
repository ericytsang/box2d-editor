package aurelienribon.bodyeditor

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue

/**
 * Loads the collision fixtures defined with the Physics Body Editor application.
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
