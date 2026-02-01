package aurelienribon.bodyeditor

import aurelienribon.bodyeditor.BodyEditorLoader.XYModel
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

    fun <T> accept(
        name:String,
        scale:XYModel,
        visitor:ShapeVisitor<T>,
    ):List<T> = synchronized(lockForReusableStuff)
    {
        val rbModel:RigidBodyModel = getRigidBody(name)

        val origin = rbModel.origin.scl(scale)

        val polygonShapeIds = rbModel.polygons.map { polygon ->
            val vertices = polygon.vertices.map { vertex -> vertex.scl(scale).sub(origin) }
            val polygonShapeId = visitor.visitPolygon(vertices)
            vertices.forEach { vectorPool.free(it) }
            polygonShapeId
        }

        val circleShapeIds = rbModel.circles.map { circle ->
            val center = circle.center.scl(scale).sub(origin)

            // I know you hate runtime exceptions, but this is a rare case, probably, because the GUI tool doesn't
            // support adding circles, anyway. it only supports polygons.
            require(scale.x == scale.y) {
                "Circle scaling must be uniform (x and y must be equal), but was x=${scale.x}, y=${scale.y}"
            }
            val radius = circle.radius*scale.x
            val circleShapeIds = visitor.visitCircle(center, radius)
            vectorPool.free(center)
            circleShapeIds
        }

        vectorPool.free(origin)

        polygonShapeIds+circleShapeIds
    }

    /**
     * Gets the origin point attached to the given name. Since the point is
     * normalized in [0,1] coordinates, it needs to be scaled to your body
     * size.
     */
    fun getOrigin(name:String,scale:XYModel):Vector2 = getRigidBody(name).origin.scl(scale)

    private fun XYModel.scl(scale:XYModel) = vectorPool.newVec().also {
        it.x = x*scale.x
        it.y = y*scale.y
    }

    private fun getRigidBody(name:String):RigidBodyModel =
        model.rigidBodies[name] ?: error("Name '$name' was not found.")

    interface ShapeVisitor<T>
    {
        fun visitCircle(center:Vector2,radius:Float):T
        fun visitPolygon(vertices:List<Vector2>):T
    }

    data class ProjectModel(
        val rigidBodies:Map<String,RigidBodyModel>,
    )

    data class RigidBodyModel(
        val name:String,
        val imagePath:String?,

        /** [origin] is where the center of the Box2D body is located. */
        val origin:XYModel,

        /** [polygons] is used to create the Box2D polygon shapes that are attached to the Box2D body. */
        val polygons:List<PolygonModel>,

        /** [polygons] is used to create the Box2D circle shapes that are attached to the Box2D body. */
        val circles:List<CircleModel>,
    )

    data class PolygonModel(
        val vertices:List<XYModel>,
    )

    data class CircleModel(
        val center:XYModel,
        val radius:Float,
    )

    data class XYModel(
        val x:Float,
        val y:Float,
    )

    companion object
    {
        @JvmStatic
        fun fromFile(file:FileHandle):BodyEditorLoader = BodyEditorLoader(readJson(file.readString()))

        @JvmStatic
        fun fromJsonString(str:String):BodyEditorLoader = BodyEditorLoader(readJson(str))

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

fun xyModel(xy:Float) = XYModel(x = xy,y = xy)
