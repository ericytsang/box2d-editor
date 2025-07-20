package aurelienribon.bodyeditor

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import kotlin.collections.get

class VectorPool
{
    private val pool:MutableList<Vector2> = ArrayList()

    fun newVec():Vector2?
    {
        return if (pool.isEmpty()) Vector2() else pool.removeAt(pool.size-1)
    }

    fun free(vec:Vector2?)
    {
        if (vec != null) pool.add(vec)
    }
}

/**
 * Loads the collision fixtures defined with the Physics Body Editor application.
 * You only need to give it a body and the corresponding fixture name, and it will attach these fixtures to your body.
 */
class BodyEditorLoader(
    val model:Model,
)
{

    // Reusable stuff
    private val vectorPool = VectorPool()
    private val vec = Vector2()
    private val polygonShape = PolygonShape()
    private val circleShape = CircleShape()

    constructor(file:FileHandle):this(readJson(file.readString()))

    constructor(str:String):this(readJson(str))

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
        body:Body,
        name:String?,
        fd:FixtureDef,
        scale:Float,
    ) = synchronized(vectorPool)
    {
        val rbModel:RigidBodyModel = getRigidBody(name)

        // TODO: Verify correct, updated method from mul to scl
        val origin = vec.set(rbModel.origin).scl(scale)

        run {
            var i = 0
            val n = rbModel.polygons.size
            while (i < n)
            {
                val polygon = rbModel.polygons[i]
                val vertices = polygon.buffer

                run {
                    var ii = 0
                    val nn = vertices.size
                    while (ii < nn)
                    {
                        vertices[ii] = vectorPool.newVec()!!.set(polygon.vertices.get(ii)).scl(scale)
                        vertices[ii]!!.sub(origin)
                        ii++
                    }
                }

                polygonShape.set(vertices)
                fd.shape = polygonShape
                body.createFixture(fd)

                var ii = 0
                val nn = vertices.size
                while (ii < nn)
                {
                    vectorPool.free(vertices[ii])
                    ii++
                }
                i++
            }
        }

        var i = 0
        val n = rbModel.circles.size
        while (i < n)
        {
            val circle = rbModel.circles.get(i)
            val center = vectorPool.newVec()!!.set(circle.center).scl(scale)
            val radius = circle.radius*scale

            circleShape.position = center
            circleShape.radius = radius
            fd.shape = circleShape
            body.createFixture(fd)

            vectorPool.free(center)
            i++
        }
    }

    fun getRigidBody(name:String?):RigidBodyModel =
        model.rigidBodies[name] ?: error("Name '$name' was not found.")

    /**
     * Gets the image path attached to the given name.
     */
    fun getImagePath(name:String):String = getRigidBody(name).imagePath

    /**
     * Gets the origin point attached to the given name. Since the point is
     * normalized in [0,1] coordinates, it needs to be scaled to your body
     * size. Warning: this method returns the same Vector2 object each time, so
     * copy it if you need it for later use.
     */
    fun getOrigin(name:String,scale:Float):Vector2 = vec.set(getRigidBody(name).origin).scl(scale)

    data class Model(
        val rigidBodies:Map<String,RigidBodyModel>,
    )

    class RigidBodyModel(
        val name:String,
        val imagePath:String,
        val origin:Vector2,
        val polygons:List<PolygonModel>,
        val circles:List<CircleModel>,
    )

    class PolygonModel(
        val vertices:List<Vector2?>,
        val buffer:Array<Vector2?>, // used to avoid allocation in attachFixture()
    )

    class CircleModel(
        val center:Vector2,
        val radius:Float,
    )

    companion object
    {
        private fun readJson(str:String):Model = Model(
            rigidBodies = JsonReader().parse(str)["rigidBodies"].associate { rbJson ->
                val rbModel = readRigidBody(rbJson)
                rbModel.name to rbModel
            },
        )

        private fun readRigidBody(rbJson:JsonValue):RigidBodyModel = RigidBodyModel(
            name = rbJson["name"].asString(),
            imagePath = rbJson["imagePath"].asString(),
            origin = rbJson["origin"].readOrigin(),
            polygons = rbJson["polygons"].map { it.readPolygon() },
            circles = rbJson["circles"].map { it.readCircle() },
        )

        private fun JsonValue.readOrigin():Vector2 = vector2(
            x = get("x").asFloat(),
            y = get("y").asFloat(),
        )

        private fun JsonValue.readPolygon():PolygonModel
        {
            val polygonModelVertices = map { it.readVertex() }
            return PolygonModel(
                vertices = polygonModelVertices,
                buffer = arrayOfNulls(polygonModelVertices.size),
            )
        }

        private fun JsonValue.readVertex():Vector2 = vector2(
            x = get("x").asFloat(),
            y = get("y").asFloat(),
        )

        private fun JsonValue.readCircle():CircleModel = CircleModel(
            center = vector2(
                x = get("cx").asFloat(),
                y = get("cy").asFloat(),
            ),
            radius = get("r").asFloat(),
        )

        private fun vector2(x:Float,y:Float):Vector2 = Vector2(x,y)
    }
}
